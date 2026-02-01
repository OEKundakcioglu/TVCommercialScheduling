#!/usr/bin/env python3
"""
Script for generating CLI commands for different algorithms.
Creates command files organized by algorithm type.
"""

import argparse
import glob
import os
from pathlib import Path
from typing import List, Dict, Any, Tuple

import yaml


def parse_args():
    """Parse command-line arguments."""
    parser = argparse.ArgumentParser(description='Generate CLI commands for different algorithms')
    parser.add_argument('-p', '--platform', choices=['win', 'linux'],
                        default=None, help='Target platform (win/linux). Auto-detects if not specified.')
    return parser.parse_args()


def get_gradle_command(platform: str) -> str:
    """Get the appropriate gradle command for the target platform."""
    if platform == 'win':
        return 'gradlew.bat'
    return './gradlew'


def normalize_path(path: str, platform: str) -> str:
    """Normalize path separators for the target platform."""
    if platform == 'win':
        return path.replace('/', '\\')
    return path.replace('\\', '/')


def format_time(seconds: int) -> str:
    """Format seconds into human-readable time string."""
    hours = seconds // 3600
    minutes = (seconds % 3600) // 60
    if hours > 0:
        return f"{hours}h {minutes}m"
    else:
        return f"{minutes}m {seconds % 60}s"


def read_config(config_path: str) -> Dict[str, Any]:
    """Read configuration from YAML file."""
    with open(config_path, 'r') as file:
        config = yaml.safe_load(file)

    if not config.get('instancePaths'):
        instance_folder = config.get('instanceFolder', 'instances')
        if os.path.exists(instance_folder):
            config['instancePaths'] = glob.glob(os.path.join(instance_folder, '*.json'))
        else:
            config['instancePaths'] = []

    return config


def generate_mip_commands(config_path: str, platform: str) -> Tuple[List[str], int]:
    """Generate CLI commands for MIP Solver."""
    config = read_config(config_path)
    commands = []
    gradle_cmd = get_gradle_command(platform)

    instance_paths = config.get('instancePaths', [])
    model_type = config.get('modelType', 'Discrete')
    time_limit = config.get('timeLimit', 7200)
    output_directory = normalize_path(config.get('outputDirectory', 'output'), platform)

    for instance_path in instance_paths:
        instance_path = normalize_path(instance_path, platform)
        instance_name = os.path.basename(instance_path)
        log_path = normalize_path(os.path.join(output_directory, instance_name, "mip.log"), platform)

        params = [
            f'-PinstancePath="{instance_path}"',
            f'-PoutputPath="{output_directory}"',
            f'-PmodelType="{model_type}"',
            f'-PtimeLimit={time_limit}',
            f'-PlogPath="{log_path}"'
        ]

        command = f"{gradle_cmd} runMipSolver {' '.join(params)}"
        commands.append(command)

    return commands, time_limit


def generate_grasp_commands(config_path: str, parallel: bool, platform: str) -> Tuple[List[str], int]:
    """Generate CLI commands for GRASP algorithm."""
    config = read_config(config_path)
    commands = []
    gradle_cmd = get_gradle_command(platform)

    instance_paths = config.get('instancePaths', [])
    search_modes = config.get('searchModes', ['BEST_IMPROVEMENT'])
    skip_probabilities = config.get('neighborhoodSkipProbabilities', [0.0])
    local_search_moves = config.get('localSearchMoves', [])
    time_limit = config.get('timeLimit', 60)
    random_run_n = config.get('randomRunN', 1)
    output_directory = normalize_path(config.get('outputDirectory', 'output'), platform)
    threads = config.get('threads', -1)

    for instance_path in instance_paths:
        instance_path = normalize_path(instance_path, platform)
        for search_mode in search_modes:
            for skip_prob in skip_probabilities:
                for run_number in range(random_run_n):
                    local_search_moves_str = ",".join(local_search_moves)

                    params = [
                        f'-PinstancePath="{instance_path}"',
                        f'-PoutputPath="{output_directory}"',
                        f'-PtimeLimit={time_limit}',
                        f'-PsearchMode="{search_mode}"',
                        f'-PskipProbability={skip_prob}',
                        f'-PlocalSearchMoves="{local_search_moves_str}"',
                        f'-Pseed={run_number}'
                    ]

                    if parallel:
                        params.append('-Pparallel')

                    command = f"{gradle_cmd} runGrasp {' '.join(params)}"
                    commands.append(command)

    return commands, time_limit


def generate_bee_commands(config_path: str, platform: str) -> Tuple[List[str], int]:
    """Generate CLI commands for Bee Colony algorithm."""
    config = read_config(config_path)
    commands = []
    gradle_cmd = get_gradle_command(platform)

    instance_paths = config.get('instancePaths', [])
    T0_options = config.get('T0Options', [])
    alpha_options = config.get('alphaOptions', [])
    population_size_options = config.get('populationSizeOptions', [])
    time_limit = config.get('timeLimit', 300)
    random_run_n = config.get('randomRunN', 1)
    output_directory = normalize_path(config.get('outputDirectory', 'output'), platform)

    for instance_path in instance_paths:
        instance_path = normalize_path(instance_path, platform)
        for T0 in T0_options:
            for alpha in alpha_options:
                for population_size in population_size_options:
                    for run_number in range(random_run_n):
                        params = [
                            f'-PinstancePath="{instance_path}"',
                            f'-PoutputPath="{output_directory}"',
                            f'-PtimeLimit={time_limit}',
                            f'-PpopulationSize={population_size}',
                            f'-Palpha={alpha}',
                            f'-PnIter=10',
                            f'-PT0={T0}',
                            f'-Pseed={run_number}'
                        ]

                        command = f"{gradle_cmd} runBeeColony {' '.join(params)}"
                        commands.append(command)

    return commands, time_limit


def write_commands_to_files(commands: List[str], output_dir: Path, num_files: int, time_limit: int):
    """Distribute commands evenly across files."""
    output_dir.mkdir(parents=True, exist_ok=True)

    if not commands:
        print(f"  No commands to write for {output_dir.name}")
        return

    commands_per_file = max(1, len(commands) // num_files)

    for i in range(num_files):
        start_idx = i * commands_per_file
        if i == num_files - 1:
            file_commands = commands[start_idx:]
        else:
            file_commands = commands[start_idx:start_idx + commands_per_file]

        if file_commands:
            file_path = output_dir / f"commands_{i + 1}.txt"
            with open(file_path, "w") as f:
                f.write("\n".join(file_commands))
            expected_time = len(file_commands) * time_limit
            print(f"  Written {len(file_commands)} commands to {file_path} (expected: {format_time(expected_time)})")


def main():
    args = parse_args()

    # Determine platform: use specified or auto-detect
    if args.platform:
        platform = args.platform
    else:
        platform = 'win' if os.name == 'nt' else 'linux'

    print(f"Generating commands for platform: {platform}")

    base_dir = Path("commands")

    algorithms = [
        ("MIPDiscrete", lambda: generate_mip_commands("yamlConfigDiscrete.yaml", platform), 2),
        ("MIPContinuous", lambda: generate_mip_commands("yamlConfigContinuous.yaml", platform), 2),
        ("GRASPSingle", lambda: generate_grasp_commands("yamlConfigGrasp.yaml", parallel=False, platform=platform), 10),
        ("GRASPParallel", lambda: generate_grasp_commands("yamlConfigGrasp.yaml", parallel=True, platform=platform), 10),
        ("BeeColony", lambda: generate_bee_commands("yamlConfigBee.yaml", platform), 10),
    ]

    for name, generator, num_files in algorithms:
        print(f"Generating {name} commands...")
        commands, time_limit = generator()
        print(f"  Generated {len(commands)} commands")
        write_commands_to_files(commands, base_dir / name, num_files, time_limit)


if __name__ == "__main__":
    main()
