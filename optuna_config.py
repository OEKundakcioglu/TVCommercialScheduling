"""
Optuna Hyperparameter Optimization Configuration for GRASP Algorithm

This file defines the parameter search space, instance configuration,
and default settings for the optimization process.

Edit this file to:
- Change parameter ranges and search space
- Add/remove parameters from optimization
- Configure which instances to use
- Set default study settings (sampler, pruner, trials)
- Adjust execution settings (time limits, seeds)
"""

# ============================================================
# PARAMETER SPACE CONFIGURATION
# ============================================================
# Set optimize=True to include in search, optimize=False to fix value

PARAMETER_SPACE = {
    "alpha_type": {
        "type": "categorical",
        "choices": ["FIXED", "REACTIVE"],
        "default": "REACTIVE",
        "optimize": True,
        "description": "Alpha generator type for greedy randomization"
    },
    "fixed_alpha": {
        "type": "float",
        "low": 0.0,
        "high": 1.0,
        "step": 0.1,
        "default": 0.5,
        "optimize": True,
        "condition": "alpha_type == 'FIXED'",
        "description": "Fixed alpha value (only used when alpha_type=FIXED)"
    },
    "min_alpha": {
        "type": "float",
        "low": 0.0,
        "high": 0.5,
        "step": 0.05,
        "default": 0.1,
        "optimize": True,
        "condition": "alpha_type == 'UNIFORM'",
        "description": "Minimum alpha value for uniform generator"
    },
    "max_alpha": {
        "type": "float",
        "low": 0.5,
        "high": 1.0,
        "step": 0.05,
        "default": 0.9,
        "optimize": True,
        "condition": "alpha_type == 'UNIFORM'",
        "description": "Maximum alpha value for uniform generator"
    },
    "skip_probability": {
        "type": "float",
        "low": 0.0,
        "high": 0.5,
        "step": 0.1,
        "default": 0.0,
        "optimize": True,
        "description": "Probability of skipping a neighborhood in local search"
    },
    "adaptive_moves": {
        "type": "bool",
        "default": False,
        "optimize": True,
        "description": "Enable adaptive move selection based on performance"
    },
    "perturbation_enabled": {
        "type": "bool",
        "default": False,
        "optimize": True,
        "description": "Enable perturbation mechanism for escaping local optima"
    },
    "destruction_rate": {
        "type": "float",
        "low": 0.1,
        "high": 0.5,
        "step": 0.05,
        "default": 0.3,
        "optimize": True,
        "condition": "perturbation_enabled == True",
        "description": "Fraction of solution to destroy in perturbation"
    },
    "reconstruction_alpha": {
        "type": "float",
        "low": 0.5,
        "high": 0.95,
        "step": 0.05,
        "default": 0.8,
        "optimize": True,
        "condition": "perturbation_enabled == True",
        "description": "Alpha value used during reconstruction phase"
    },
    "stagnation_threshold": {
        "type": "int",
        "low": 10,
        "high": 100,
        "step": 10,
        "default": 50,
        "optimize": True,
        "condition": "perturbation_enabled == True",
        "description": "Iterations without improvement before triggering perturbation"
    },
    "moves": {
        "type": "subset",
        "options": ["insert", "outOfPool", "interSwap", "shift",
                    "transfer", "intraSwap", "chainSwap"],
        "min_size": 3,
        "default": ["insert", "outOfPool", "interSwap", "shift",
                    "transfer", "intraSwap", "chainSwap"],
        "optimize": True,
        "optimize_order": True,  # Also optimize the order of moves
        "description": "Local search moves to include (order is also optimized)"
    },
    # Fixed parameters (not optimized by default)
    "search_mode": {
        "type": "categorical",
        "choices": ["FIRST_IMPROVEMENT", "BEST_IMPROVEMENT"],
        "default": "FIRST_IMPROVEMENT",
        "optimize": False,
        "description": "Local search improvement strategy"
    },
    "time_limit": {
        "type": "int",
        "default": 300,
        "optimize": False,
        "description": "Time limit per solver run in seconds"
    }
}

# ============================================================
# INSTANCE CONFIGURATION
# ============================================================

# Priority instances for optimization (higher complexity, more signal)
# These instances take longer but provide more meaningful optimization signal
PRIORITY_INSTANCES = [
    "density=HIGH_nInventory=20_nHours=5_seed=1.json",
#     "density=HIGH_nInventory=15_nHours=4_seed=1.json",
#     "density=MEDIUM_nInventory=20_nHours=5_seed=1.json",
]

# Quick test instances (smaller, faster for initial validation)
QUICK_INSTANCES = [
    "density=HIGH_nInventory=10_nHours=3_seed=1.json",
]

# All available instances by category (for comprehensive tuning)
ALL_INSTANCES = {
    "HIGH_LARGE": [f"density=HIGH_nInventory=20_nHours=5_seed={i}.json" for i in range(1, 11)],
    "HIGH_MEDIUM": [f"density=HIGH_nInventory=15_nHours=4_seed={i}.json" for i in range(1, 11)],
    "HIGH_SMALL": [f"density=HIGH_nInventory=10_nHours=3_seed={i}.json" for i in range(1, 11)],
    "MEDIUM_LARGE": [f"density=MEDIUM_nInventory=20_nHours=5_seed={i}.json" for i in range(1, 11)],
    "MEDIUM_MEDIUM": [f"density=MEDIUM_nInventory=15_nHours=4_seed={i}.json" for i in range(1, 11)],
    "MEDIUM_SMALL": [f"density=MEDIUM_nInventory=10_nHours=3_seed={i}.json" for i in range(1, 11)],
    "LOW_LARGE": [f"density=LOW_nInventory=20_nHours=5_seed={i}.json" for i in range(1, 11)],
    "LOW_MEDIUM": [f"density=LOW_nInventory=15_nHours=4_seed={i}.json" for i in range(1, 11)],
    "LOW_SMALL": [f"density=LOW_nInventory=10_nHours=3_seed={i}.json" for i in range(1, 11)],
}

# Directory paths
INSTANCES_DIR = "instances"
REFERENCE_DIR = "output/discrete"

# ============================================================
# STUDY DEFAULTS
# ============================================================

STUDY_DEFAULTS = {
    "direction": "minimize",          # minimize gap (lower is better)
    "sampler": "TPE",                 # TPE, Random, CMA-ES
    "pruner": "Median",               # Median, SuccessiveHalving, None
    "n_trials": 1000,                 # Number of optimization trials
    "timeout_hours": None,            # Max time in hours (None for unlimited)
    "n_jobs": 8,                      # Parallel trial execution
}

# ============================================================
# EXECUTION SETTINGS
# ============================================================

EXECUTION_SETTINGS = {
    "time_limit_per_run": 300,         # seconds per solver invocation
    "seeds": [0],                     # seeds to average over (for robustness)
    "aggregation": "mean",            # mean, worst, best
    "use_gradle": True,               # Use Gradle to run solver
    "gradle_command": "gradlew runGrasp -q",  # Gradle command (without ./)
    "working_dir": ".",               # Working directory
    "java_classpath": None,           # Only if use_gradle=False
}

# ============================================================
# OBJECTIVE CONFIGURATION
# ============================================================

OBJECTIVE_CONFIG = {
    "metric": "gap",                  # "revenue" or "gap"
    "timeout_penalty": 1000000,       # Value when solver times out (high = bad for minimize)
    "error_penalty": 1000000,         # Value when solver fails (high = bad for minimize)
}

# ============================================================
# OUTPUT SETTINGS
# ============================================================

OUTPUT_SETTINGS = {
    "results_dir": "optuna_results",  # Base directory for results
    "save_trials_csv": True,          # Save all trials to CSV
    "save_best_params_json": True,    # Save best parameters to JSON
    "generate_plots": True,           # Generate Optuna visualizations
    "plot_formats": ["html"],         # html, png (png requires kaleido)
}
