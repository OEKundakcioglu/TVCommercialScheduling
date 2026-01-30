#!/usr/bin/env python3
"""
Optuna Hyperparameter Optimization for GRASP Algorithm

This script performs hyperparameter optimization for the GRASP algorithm
using Optuna. Configuration is loaded from optuna_config.py.

Usage:
    # Quick test (5 trials, 1 instance, 30s limit)
    python optuna_search.py --quick-test

    # Standard optimization
    python optuna_search.py --n-trials 100 --time-limit 60

    # Extended with timeout and parallelism
    python optuna_search.py --n-trials 500 --timeout 8h --n-jobs 4

    # Different sampler
    python optuna_search.py --sampler CMA-ES --n-trials 200

    # Resume existing study
    python optuna_search.py --resume --study-name my_study

    # Single specific instance
    python optuna_search.py --instance "instances/density=HIGH_nInventory=20_nHours=5_seed=1.json"

    # Dry run (show config without running)
    python optuna_search.py --dry-run
"""

import argparse
import json
import os
import re
import shutil
import signal
import subprocess
import sys
import tempfile
import time
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

try:
    import optuna
    from optuna.samplers import TPESampler, RandomSampler, CmaEsSampler
    from optuna.pruners import MedianPruner, SuccessiveHalvingPruner, NopPruner
except ImportError:
    print("Error: optuna is not installed. Install with: pip install optuna")
    sys.exit(1)

# Import configuration
try:
    from optuna_config import (
        PARAMETER_SPACE,
        PRIORITY_INSTANCES,
        QUICK_INSTANCES,
        ALL_INSTANCES,
        INSTANCES_DIR,
        REFERENCE_DIR,
        STUDY_DEFAULTS,
        EXECUTION_SETTINGS,
        OBJECTIVE_CONFIG,
        OUTPUT_SETTINGS,
    )
except ImportError:
    print("Error: optuna_config.py not found. Please create it first.")
    sys.exit(1)


class GracefulShutdown:
    """Handle graceful shutdown on Ctrl+C"""

    def __init__(self):
        self.shutdown_requested = False
        signal.signal(signal.SIGINT, self._handler)
        signal.signal(signal.SIGTERM, self._handler)

    def _handler(self, signum, frame):
        print("\n\nShutdown requested. Finishing current trial...")
        self.shutdown_requested = True


def parse_timeout(timeout_str: str) -> Optional[float]:
    """Parse timeout string like '8h', '30m', '3600s' to seconds"""
    if timeout_str is None:
        return None

    match = re.match(r'^(\d+(?:\.\d+)?)\s*(h|m|s)?$', timeout_str.lower().strip())
    if not match:
        raise ValueError(f"Invalid timeout format: {timeout_str}. Use format like '8h', '30m', or '3600s'")

    value = float(match.group(1))
    unit = match.group(2) or 's'

    if unit == 'h':
        return value * 3600
    elif unit == 'm':
        return value * 60
    else:
        return value


def get_sampler(sampler_name: str, seed: int = None, n_startup_trials: int = 10):
    """Get Optuna sampler by name"""
    samplers = {
        "TPE": lambda: TPESampler(seed=seed, n_startup_trials=n_startup_trials),
        "Random": lambda: RandomSampler(seed=seed),
        "CMA-ES": lambda: CmaEsSampler(seed=seed, n_startup_trials=n_startup_trials),
    }
    if sampler_name not in samplers:
        raise ValueError(f"Unknown sampler: {sampler_name}. Available: {list(samplers.keys())}")
    return samplers[sampler_name]()


def get_pruner(pruner_name: str):
    """Get Optuna pruner by name"""
    pruners = {
        "Median": lambda: MedianPruner(),
        "SuccessiveHalving": lambda: SuccessiveHalvingPruner(),
        "None": lambda: NopPruner(),
    }
    if pruner_name not in pruners:
        raise ValueError(f"Unknown pruner: {pruner_name}. Available: {list(pruners.keys())}")
    return pruners[pruner_name]()


def sample_parameters(trial: optuna.Trial, param_space: Dict) -> Dict[str, Any]:
    """Sample parameters from the search space using Optuna trial"""
    params = {}

    for name, config in param_space.items():
        # Check if parameter should be optimized
        if not config.get("optimize", False):
            params[name] = config["default"]
            continue

        # Check conditional parameters
        condition = config.get("condition")
        if condition:
            # Evaluate condition based on already sampled parameters
            try:
                if not eval(condition, {"__builtins__": {}}, params):
                    params[name] = config["default"]
                    continue
            except (NameError, KeyError):
                # Condition references parameter not yet sampled
                params[name] = config["default"]
                continue

        # Sample based on type
        param_type = config["type"]

        if param_type == "categorical":
            params[name] = trial.suggest_categorical(name, config["choices"])

        elif param_type == "float":
            if "step" in config:
                params[name] = trial.suggest_float(
                    name, config["low"], config["high"], step=config["step"]
                )
            else:
                params[name] = trial.suggest_float(name, config["low"], config["high"])

        elif param_type == "int":
            if "step" in config:
                params[name] = trial.suggest_int(
                    name, config["low"], config["high"], step=config["step"]
                )
            else:
                params[name] = trial.suggest_int(name, config["low"], config["high"])

        elif param_type == "bool":
            params[name] = trial.suggest_categorical(name, [True, False])

        elif param_type == "subset":
            # Sample which moves to include and their order
            selected = []
            priorities = []
            optimize_order = config.get("optimize_order", False)

            for option in config["options"]:
                include = trial.suggest_categorical(f"move_{option}", [True, False])
                if include:
                    if optimize_order:
                        # Sample a priority for ordering
                        priority = trial.suggest_float(f"order_{option}", 0.0, 1.0)
                        selected.append((option, priority))
                    else:
                        selected.append((option, 0))

            # Ensure minimum size
            min_size = config.get("min_size", 1)
            if len(selected) < min_size:
                # Add moves until minimum is reached
                remaining = [m for m in config["options"] if m not in [s[0] for s in selected]]
                import random
                random.shuffle(remaining)
                for m in remaining[:min_size - len(selected)]:
                    if optimize_order:
                        priority = trial.suggest_float(f"order_{m}", 0.0, 1.0)
                        selected.append((m, priority))
                    else:
                        selected.append((m, 0))

            # Sort by priority (lower priority = earlier in list)
            if optimize_order:
                selected.sort(key=lambda x: x[1])

            # Extract just the move names in order
            params[name] = [s[0] for s in selected]

            # Store the order info as a separate attribute for visibility
            if optimize_order:
                params[f"{name}_order"] = ",".join(params[name])

        else:
            raise ValueError(f"Unknown parameter type: {param_type}")

    # Re-evaluate conditional parameters now that all primary params are set
    for name, config in param_space.items():
        condition = config.get("condition")
        if condition and config.get("optimize", False):
            try:
                if not eval(condition, {"__builtins__": {}}, params):
                    # Keep default value for inactive conditional params
                    params[name] = config["default"]
            except Exception:
                pass

    return params


def build_solver_command(
    params: Dict[str, Any],
    instance_path: str,
    output_path: str,
    seed: int,
    execution_settings: Dict,
    working_dir: str = "."
) -> List[str]:
    """Build the command to run the GRASP solver"""

    if execution_settings.get("use_gradle", True):
        # Use Gradle - construct full path to gradlew
        working_path = Path(working_dir).resolve()

        if sys.platform == "win32":
            gradle_exec = working_path / "gradlew.bat"
        else:
            gradle_exec = working_path / "gradlew"

        # Build command with full path
        cmd = [str(gradle_exec), "runGrasp", "-q"]

        # Add parameters as Gradle project properties
        cmd.append(f"-PinstancePath={instance_path}")
        cmd.append(f"-PoutputPath={output_path}")
        cmd.append(f"-PtimeLimit={params['time_limit']}")
        cmd.append(f"-PsearchMode={params['search_mode']}")
        cmd.append(f"-PalphaType={params['alpha_type']}")

        # Alpha-specific parameters
        if params["alpha_type"] == "FIXED":
            cmd.append(f"-Palpha={params['fixed_alpha']}")
        elif params["alpha_type"] == "UNIFORM":
            cmd.append(f"-PminAlpha={params['min_alpha']}")
            cmd.append(f"-PmaxAlpha={params['max_alpha']}")

        cmd.append(f"-PskipProbability={params['skip_probability']}")
        cmd.append(f"-Pseed={seed}")

        # Moves as comma-separated list
        moves_str = ",".join(params["moves"])
        cmd.append(f"-PlocalSearchMoves={moves_str}")

        # Adaptive moves
        if params["adaptive_moves"]:
            cmd.append("-PadaptiveMoves=true")

    else:
        # Direct Java execution
        raise NotImplementedError("Direct Java execution not implemented. Use Gradle.")

    return cmd


def run_solver(
    cmd: List[str],
    output_path: str,
    timeout: int,
    working_dir: str = "."
) -> Tuple[Optional[float], str]:
    """
    Run the solver and return the result.

    Returns:
        Tuple of (revenue or None, status message)
    """
    try:
        # Run the solver (using full path so shell=False is fine)
        result = subprocess.run(
            cmd,
            cwd=working_dir,
            capture_output=True,
            text=True,
            timeout=timeout + 30,  # Add buffer for process overhead
        )

        if result.returncode != 0:
            return None, f"Solver failed with code {result.returncode}: {result.stderr[:500]}"

        # Find and parse the solution file
        # The solver creates output in a subdirectory structure
        solution_files = list(Path(output_path).rglob("solution.json"))

        if not solution_files:
            return None, f"No solution.json found in {output_path}"

        # Read the most recent solution file
        solution_file = max(solution_files, key=lambda p: p.stat().st_mtime)

        with open(solution_file, 'r') as f:
            solution = json.load(f)

        revenue = solution.get("bestSolution", {}).get("revenue")
        if revenue is None:
            return None, "No revenue found in solution"

        return float(revenue), "success"

    except subprocess.TimeoutExpired:
        return None, "timeout"
    except FileNotFoundError as e:
        return None, f"File not found: {e}"
    except json.JSONDecodeError as e:
        return None, f"JSON parse error: {e}"
    except Exception as e:
        return None, f"Error: {e}"


def load_reference_solution(instance_name: str, reference_dir: str) -> Optional[float]:
    """Load reference solution revenue for gap calculation"""
    # Remove .json extension for directory name
    instance_base = instance_name.replace(".json", "")
    solution_path = Path(reference_dir) / instance_base / "solution.json"

    if not solution_path.exists():
        return None

    try:
        with open(solution_path, 'r') as f:
            solution = json.load(f)
        return float(solution.get("bestSolution", {}).get("revenue", 0))
    except Exception:
        return None


def create_objective(
    instances: List[str],
    param_space: Dict,
    execution_settings: Dict,
    objective_config: Dict,
    shutdown_handler: GracefulShutdown,
    verbose: bool = False
):
    """Create the Optuna objective function"""

    def objective(trial: optuna.Trial) -> float:
        if shutdown_handler.shutdown_requested:
            raise optuna.TrialPruned("Shutdown requested")

        # Sample parameters
        params = sample_parameters(trial, param_space)

        if verbose:
            print(f"\n  Trial {trial.number}: {params}")

        # Run solver on each instance and collect results
        results = []
        seeds = execution_settings.get("seeds", [0])

        for instance in instances:
            instance_path = os.path.join(INSTANCES_DIR, instance)

            if not os.path.exists(instance_path):
                print(f"    Warning: Instance not found: {instance_path}")
                continue

            for seed in seeds:
                if shutdown_handler.shutdown_requested:
                    raise optuna.TrialPruned("Shutdown requested")

                # Create temporary output directory
                with tempfile.TemporaryDirectory() as temp_dir:
                    working_dir = execution_settings.get("working_dir", ".")

                    # Build and run solver command
                    cmd = build_solver_command(
                        params,
                        instance_path,
                        temp_dir,
                        seed,
                        execution_settings,
                        working_dir
                    )

                    if verbose:
                        print(f"    Running: {instance} (seed={seed})")

                    revenue, status = run_solver(
                        cmd,
                        temp_dir,
                        params["time_limit"],
                        execution_settings.get("working_dir", ".")
                    )

                    if revenue is None:
                        if verbose:
                            print(f"      Failed: {status}")
                        # Use penalty for failed runs
                        results.append(objective_config.get("error_penalty", -1000000))
                    else:
                        # Calculate metric based on configuration
                        metric = objective_config.get("metric", "revenue")

                        if metric == "gap":
                            reference = load_reference_solution(instance, REFERENCE_DIR)
                            if reference and reference > 0:
                                gap = (reference - revenue) / reference * 100
                                # Return gap directly (lower is better for minimization)
                                results.append(gap)
                            else:
                                # Fallback: use penalty if no reference available
                                results.append(objective_config.get("error_penalty", 1000000))
                        else:
                            results.append(revenue)

                        if verbose:
                            if metric == "gap":
                                reference = load_reference_solution(instance, REFERENCE_DIR)
                                if reference and reference > 0:
                                    gap_val = (reference - revenue) / reference * 100
                                    print(f"      Revenue: {revenue:,.0f}, Gap: {gap_val:.2f}%")
                                else:
                                    print(f"      Revenue: {revenue:,.0f} (no reference)")
                            else:
                                print(f"      Revenue: {revenue:,.0f}")

        if not results:
            return objective_config.get("error_penalty", 1000000)

        # Aggregate results
        aggregation = execution_settings.get("aggregation", "mean")
        direction = STUDY_DEFAULTS.get("direction", "minimize")
        if aggregation == "mean":
            final_result = sum(results) / len(results)
        elif aggregation == "worst":
            # For minimize: worst is highest; for maximize: worst is lowest
            final_result = max(results) if direction == "minimize" else min(results)
        elif aggregation == "best":
            # For minimize: best is lowest; for maximize: best is highest
            final_result = min(results) if direction == "minimize" else max(results)
        else:
            final_result = sum(results) / len(results)

        if verbose:
            metric = objective_config.get("metric", "gap")
            if metric == "gap":
                print(f"    Gap: {final_result:.2f}%")
            else:
                print(f"    Result: {final_result:,.2f}")

        return final_result

    return objective


def save_results(study: optuna.Study, output_dir: Path, verbose: bool = False):
    """Save optimization results"""
    output_dir.mkdir(parents=True, exist_ok=True)

    # Save best parameters
    if OUTPUT_SETTINGS.get("save_best_params_json", True):
        best_params_path = output_dir / "best_params.json"
        best_params = {
            "best_value": study.best_value,
            "best_params": study.best_params,
            "best_trial_number": study.best_trial.number,
            "n_trials": len(study.trials),
            "timestamp": datetime.now().isoformat(),
        }
        with open(best_params_path, 'w') as f:
            json.dump(best_params, f, indent=2)
        if verbose:
            print(f"Best parameters saved to: {best_params_path}")

    # Save all trials to CSV
    if OUTPUT_SETTINGS.get("save_trials_csv", True):
        trials_path = output_dir / "trials.csv"
        try:
            df = study.trials_dataframe()
            df.to_csv(trials_path, index=False)
            if verbose:
                print(f"Trials saved to: {trials_path}")
        except ImportError:
            # Fallback: write CSV manually if pandas not available
            with open(trials_path, 'w') as f:
                # Header
                headers = ["number", "value", "state"]
                if study.trials:
                    headers.extend(sorted(study.trials[0].params.keys()))
                f.write(",".join(headers) + "\n")

                # Data rows
                for trial in study.trials:
                    row = [str(trial.number), str(trial.value), trial.state.name]
                    for key in sorted(trial.params.keys()):
                        row.append(str(trial.params.get(key, "")))
                    f.write(",".join(row) + "\n")

            if verbose:
                print(f"Trials saved to: {trials_path} (manual CSV, pandas not installed)")

    # Generate plots
    if OUTPUT_SETTINGS.get("generate_plots", True):
        generate_plots(study, output_dir, verbose)


def generate_plots(study: optuna.Study, output_dir: Path, verbose: bool = False):
    """Generate Optuna visualization plots"""
    try:
        from optuna.visualization import (
            plot_optimization_history,
            plot_param_importances,
            plot_parallel_coordinate,
            plot_slice,
        )
    except ImportError:
        if verbose:
            print("Warning: plotly not installed. Skipping plot generation.")
        return

    plot_formats = OUTPUT_SETTINGS.get("plot_formats", ["html"])

    plots = [
        ("optimization_history", plot_optimization_history),
        ("param_importances", plot_param_importances),
        ("parallel_coordinate", plot_parallel_coordinate),
        ("slice", plot_slice),
    ]

    for plot_name, plot_func in plots:
        try:
            fig = plot_func(study)

            for fmt in plot_formats:
                if fmt == "html":
                    fig.write_html(output_dir / f"{plot_name}.html")
                elif fmt == "png":
                    try:
                        fig.write_image(output_dir / f"{plot_name}.png")
                    except Exception as e:
                        if verbose:
                            print(f"Warning: Could not save PNG (need kaleido): {e}")

            if verbose:
                print(f"Plot saved: {plot_name}")
        except Exception as e:
            if verbose:
                print(f"Warning: Could not generate {plot_name}: {e}")


def get_instances(args) -> List[str]:
    """Get list of instances based on arguments"""
    if args.instance:
        # Single instance specified
        instance_name = os.path.basename(args.instance)
        return [instance_name]

    if args.quick_test:
        return QUICK_INSTANCES

    if args.instances == "priority":
        return PRIORITY_INSTANCES
    elif args.instances == "quick":
        return QUICK_INSTANCES
    elif args.instances == "all":
        # Flatten all instances
        all_instances = []
        for category_instances in ALL_INSTANCES.values():
            all_instances.extend(category_instances)
        return all_instances
    else:
        return PRIORITY_INSTANCES


def main():
    parser = argparse.ArgumentParser(
        description="Optuna Hyperparameter Optimization for GRASP Algorithm",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python optuna_search.py --quick-test              # Quick validation (5 trials)
  python optuna_search.py --n-trials 100            # Standard optimization
  python optuna_search.py --n-trials 500 --timeout 8h --n-jobs 4  # Extended run
  python optuna_search.py --sampler CMA-ES          # Different sampler
  python optuna_search.py --resume --study-name prev_study  # Resume study
        """
    )

    # Study settings
    parser.add_argument("--study-name", type=str, default=None,
                        help="Name for the Optuna study (default: auto-generated)")
    parser.add_argument("--n-trials", type=int, default=None,
                        help=f"Number of optimization trials (default: {STUDY_DEFAULTS['n_trials']})")
    parser.add_argument("--timeout", type=str, default=None,
                        help="Max time (e.g., '8h', '30m', '3600s')")
    parser.add_argument("--n-jobs", type=int, default=None,
                        help=f"Parallel trial execution (default: {STUDY_DEFAULTS['n_jobs']})")

    # Sampler and pruner
    parser.add_argument("--sampler", type=str, default=None,
                        choices=["TPE", "Random", "CMA-ES"],
                        help=f"Optimization sampler (default: {STUDY_DEFAULTS['sampler']})")
    parser.add_argument("--pruner", type=str, default=None,
                        choices=["Median", "SuccessiveHalving", "None"],
                        help=f"Trial pruner (default: {STUDY_DEFAULTS['pruner']})")

    # Instance selection
    parser.add_argument("--instances", type=str, default="priority",
                        choices=["priority", "all", "quick"],
                        help="Instance set to use (default: priority)")
    parser.add_argument("--instance", type=str, default=None,
                        help="Single specific instance path")

    # Execution settings
    parser.add_argument("--time-limit", type=int, default=None,
                        help=f"Per-run time limit in seconds (default: {EXECUTION_SETTINGS['time_limit_per_run']})")
    parser.add_argument("--seeds", type=str, default=None,
                        help="Comma-separated seeds to average over (default: 0)")

    # Control flags
    parser.add_argument("--resume", action="store_true",
                        help="Resume existing study")
    parser.add_argument("--quick-test", action="store_true",
                        help="Quick test mode (5 trials, quick instances, 30s limit)")
    parser.add_argument("--dry-run", action="store_true",
                        help="Show configuration without running")
    parser.add_argument("--verbose", "-v", action="store_true",
                        help="Enable verbose output")
    parser.add_argument("--seed", type=int, default=42,
                        help="Random seed for sampler (default: 42)")

    args = parser.parse_args()

    # Apply quick-test overrides
    if args.quick_test:
        args.n_trials = args.n_trials or 5
        args.time_limit = args.time_limit or 30
        args.instances = "quick"

    # Merge defaults with arguments
    n_trials = args.n_trials or STUDY_DEFAULTS["n_trials"]
    timeout = parse_timeout(args.timeout) if args.timeout else None
    n_jobs = args.n_jobs or STUDY_DEFAULTS["n_jobs"]
    sampler_name = args.sampler or STUDY_DEFAULTS["sampler"]
    pruner_name = args.pruner or STUDY_DEFAULTS["pruner"]
    time_limit = args.time_limit or EXECUTION_SETTINGS["time_limit_per_run"]

    # Parse seeds
    if args.seeds:
        seeds = [int(s.strip()) for s in args.seeds.split(",")]
    else:
        seeds = EXECUTION_SETTINGS.get("seeds", [0])

    # Update execution settings
    execution_settings = EXECUTION_SETTINGS.copy()
    execution_settings["time_limit_per_run"] = time_limit
    execution_settings["seeds"] = seeds

    # Update parameter space with time limit
    param_space = PARAMETER_SPACE.copy()
    param_space["time_limit"]["default"] = time_limit

    # Get instances
    instances = get_instances(args)

    # Generate study name if not provided
    if args.study_name:
        study_name = args.study_name
    else:
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        study_name = f"grasp_optuna_{timestamp}"

    # Set up output directory
    output_dir = Path(OUTPUT_SETTINGS.get("results_dir", "optuna_results")) / study_name

    # Print configuration
    print("=" * 60)
    print("GRASP Hyperparameter Optimization with Optuna")
    print("=" * 60)
    print(f"Study name:     {study_name}")
    print(f"Trials:         {n_trials}")
    print(f"Timeout:        {args.timeout or 'None'}")
    print(f"Parallel jobs:  {n_jobs}")
    print(f"Sampler:        {sampler_name}")
    print(f"Pruner:         {pruner_name}")
    print(f"Time limit:     {time_limit}s per run")
    print(f"Seeds:          {seeds}")
    print(f"Metric:         {OBJECTIVE_CONFIG.get('metric', 'gap')}")
    print(f"Direction:      {STUDY_DEFAULTS.get('direction', 'minimize')}")
    print(f"Instances:      {len(instances)}")
    for inst in instances[:5]:
        print(f"  - {inst}")
    if len(instances) > 5:
        print(f"  ... and {len(instances) - 5} more")
    print(f"Output:         {output_dir}")
    n_startup_trials = STUDY_DEFAULTS.get("n_startup_trials", 10)
    print(f"Startup trials: {n_startup_trials}")
    print("=" * 60)

    if args.dry_run:
        print("\nDry run - not executing optimization")
        print("\nOptimizable parameters:")
        for name, config in param_space.items():
            if config.get("optimize", False):
                print(f"  - {name}: {config['type']}")
                if "condition" in config:
                    print(f"      (conditional: {config['condition']})")
        return

    # Set up graceful shutdown
    shutdown_handler = GracefulShutdown()

    # Create or load study
    storage_path = output_dir / "study.db"
    output_dir.mkdir(parents=True, exist_ok=True)
    storage = f"sqlite:///{storage_path}"

    sampler = get_sampler(sampler_name, seed=args.seed, n_startup_trials=n_startup_trials)
    pruner = get_pruner(pruner_name)

    if args.resume:
        print(f"\nResuming study from: {storage_path}")
        study = optuna.load_study(
            study_name=study_name,
            storage=storage,
            sampler=sampler,
            pruner=pruner,
        )
        print(f"Loaded {len(study.trials)} existing trials")
    else:
        study = optuna.create_study(
            study_name=study_name,
            storage=storage,
            sampler=sampler,
            pruner=pruner,
            direction=STUDY_DEFAULTS.get("direction", "minimize"),
            load_if_exists=False,
        )

    # Create objective function
    objective = create_objective(
        instances,
        param_space,
        execution_settings,
        OBJECTIVE_CONFIG,
        shutdown_handler,
        verbose=args.verbose
    )

    # Run optimization
    print("\nStarting optimization...")
    start_time = time.time()

    try:
        study.optimize(
            objective,
            n_trials=n_trials,
            timeout=timeout,
            n_jobs=n_jobs,
            show_progress_bar=not args.verbose,
        )
    except KeyboardInterrupt:
        print("\n\nOptimization interrupted by user")

    elapsed = time.time() - start_time

    # Print results
    print("\n" + "=" * 60)
    print("OPTIMIZATION COMPLETE")
    print("=" * 60)
    print(f"Total time:     {elapsed:.1f}s ({elapsed/3600:.2f}h)")
    print(f"Trials run:     {len(study.trials)}")
    metric = OBJECTIVE_CONFIG.get("metric", "gap")
    if metric == "gap":
        print(f"Best gap:       {study.best_value:.2f}%")
    else:
        print(f"Best value:     {study.best_value:,.2f}")
    print(f"Best trial:     #{study.best_trial.number}")
    print("\nBest parameters:")
    for param, value in study.best_params.items():
        print(f"  {param}: {value}")

    # Save results
    print("\nSaving results...")
    save_results(study, output_dir, verbose=args.verbose)

    print(f"\nResults saved to: {output_dir}")
    print("=" * 60)


if __name__ == "__main__":
    main()
