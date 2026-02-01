#!/usr/bin/env python3
"""
Script for generating CLI commands for different algorithms.
Creates command files organized by algorithm type.
"""

import glob
import os
from pathlib import Path
from typing import List, Dict, Any

import yaml


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


def generate_mip_commands(config_path: str) -> List[str]:
    """Generate CLI commands for MIP Solver."""
    config = read_config(config_path)
    commands = []

    instance_paths = config.get('instancePaths', [])
    model_type = config.get('modelType', 'Discrete')
    time_limit = config.get('timeLimit', 7200)
    output_directory = config.get('outputDirectory', 'output')

    for instance_path in instance_paths:
        instance_name = os.path.basename(instance_path)
        log_path = os.path.join(output_directory, instance_name, "mip.log")

        params = [
            f'-PinstancePath="{instance_path}"',
            f'-PoutputPath="{output_directory}"',
            f'-PmodelType="{model_type}"',
            f'-PtimeLimit={time_limit}',
            f'-PlogPath="{log_path}"'
        ]

        command = f"./gradlew runMipSolver {' '.join(params)}"
        commands.append(command)

    return commands


def generate_grasp_commands(config_path: str, parallel: bool) -> List[str]:
    """Generate CLI commands for GRASP algorithm."""
    config = read_config(config_path)
    commands = []

    instance_paths = config.get('instancePaths', [])
    search_modes = config.get('searchModes', ['BEST_IMPROVEMENT'])
    skip_probabilities = config.get('neighborhoodSkipProbabilities', [0.0])
    local_search_moves = config.get('localSearchMoves', [])
    time_limit = config.get('timeLimit', 60)
    random_run_n = config.get('randomRunN', 1)
    output_directory = config.get('outputDirectory', 'output')
    threads = config.get('threads', -1)

    for instance_path in instance_paths:
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

                    command = f"./gradlew runGrasp {' '.join(params)}"
                    commands.append(command)

    return commands


def generate_bee_commands(config_path: str) -> List[str]:
    """Generate CLI commands for Bee Colony algorithm."""
    config = read_config(config_path)
    commands = []

    instance_paths = config.get('instancePaths', [])
    T0_options = config.get('T0Options', [])
    alpha_options = config.get('alphaOptions', [])
    population_size_options = config.get('populationSizeOptions', [])
    time_limit = config.get('timeLimit', 300)
    random_run_n = config.get('randomRunN', 1)
    output_directory = config.get('outputDirectory', 'output')

    for instance_path in instance_paths:
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

                        command = f"./gradlew runBeeColony {' '.join(params)}"
                        commands.append(command)

    return commands


def write_commands_to_files(commands: List[str], output_dir: Path, num_files: int):
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
            print(f"  Written {len(file_commands)} commands to {file_path}")


def main():
    base_dir = Path("commands")

    algorithms = [
        ("MIPDiscrete", lambda: generate_mip_commands("yamlConfigDiscrete.yaml"), 2),
        ("MIPContinuous", lambda: generate_mip_commands("yamlConfigContinuous.yaml"), 2),
        ("GRASPSingle", lambda: generate_grasp_commands("yamlConfigGrasp.yaml", parallel=False), 10),
        ("GRASPParallel", lambda: generate_grasp_commands("yamlConfigGrasp.yaml", parallel=True), 10),
        ("BeeColony", lambda: generate_bee_commands("yamlConfigBee.yaml"), 10),
    ]

    for name, generator, num_files in algorithms:
        print(f"Generating {name} commands...")
        commands = generator()
        print(f"  Generated {len(commands)} commands")
        write_commands_to_files(commands, base_dir / name, num_files)


if __name__ == "__main__":
    main()
