#!/usr/bin/env python3
"""
Script for generating and executing CLI commands from GRASP configuration files.
"""

import glob
import os
import subprocess
import sys
import time
import yaml
from os import write
from pathlib import Path
from typing import List, Dict, Any, Optional, Tuple


def read_grasp_config(config_path: str) -> Dict[str, Any]:
    """
    Read GRASP configuration from YAML file.
    
    Args:
        config_path: Path to the YAML configuration file
        
    Returns:
        Dictionary containing the configuration
    """
    with open(config_path, 'r') as file:
        config = yaml.safe_load(file)

    # Handle instancePaths - if empty, read from instanceFolder
    if not config.get('instancePaths'):
        instance_folder = config.get('instanceFolder', 'instances')
        if os.path.exists(instance_folder):
            config['instancePaths'] = glob.glob(os.path.join(instance_folder, '*.json'))
        else:
            config['instancePaths'] = []

    return config


def generate_bee_cli_commands(config_path: str, base_command: str = "gradlew runBeeColony") -> List[tuple[str, int]]:
    """
    Generate CLI commands for Bee Colony algorithm based on configuration file.
    
    Args:
        config_path: Path to the YAML configuration file
        base_command: Base command to use (default: "gradlew runBeeColony")
        
    Returns:
        List of CLI command strings
    """
    config = read_grasp_config(config_path)
    commands = []

    # Extract configuration parameters
    instance_paths = config.get('instancePaths', [])
    T0_options = config.get('T0Options', [])
    alpha_options = config.get('alphaOptions', [])
    population_size_options = config.get('populationSizeOptions', [])
    time_limit = config.get('timeLimit', 300)
    random_run_n = config.get('randomRunN', 1)
    output_directory = config.get('outputDirectory', 'output')

    # Generate all combinations
    for instance_path in instance_paths:
        for T0 in T0_options:
            for alpha in alpha_options:
                for population_size in population_size_options:
                    for run_number in range(random_run_n):
                        # Extract instance name for output path
                        instance_name = os.path.basename(instance_path)

                        # Build command parameters
                        params = [f'-PinstancePath="{instance_path}"', f'-PoutputPath="{output_directory}"',
                                  f'-PtimeLimit={time_limit}', f'-PpopulationSize={population_size}',
                                  f'-Palpha={alpha}',
                                  f'-PnIter=10',  # Default value from BeeColonySettings
                                  f'-PT0={T0}', f'-Pseed={run_number}']

                        # Construct full command
                        command = f"{base_command} {' '.join(params)}"
                        commands.append(command)

    return list(zip(commands, [5 for _ in commands]))


def generate_mip_cli_commands(config_path: str, base_command: str = "./gradlew runMipSolver") -> List[tuple[str, int]]:
    """
    Generate CLI commands for MIP Solver based on configuration file.
    
    Args:
        config_path: Path to the YAML configuration file
        base_command: Base command to use (default: "./gradlew runMipSolver")
        
    Returns:
        List of CLI command strings
    """
    config = read_grasp_config(config_path)
    commands = []

    # Extract configuration parameters
    instance_paths = config.get('instancePaths', [])
    model_type = config.get('modelType', 'Discrete')
    checkpoint_times = config.get('checkPointTimes', [])
    output_directory = config.get('outputDirectory', 'output')

    # Generate commands for each instance
    for instance_path in instance_paths:
        # Extract instance name for output path
        instance_name = os.path.basename(instance_path)

        # Create log path
        log_path = os.path.join(output_directory, instance_name, "mip.log")

        # Convert checkpoint times to comma-separated string
        checkpoint_times_str = ','.join(map(str, checkpoint_times))

        # Build command parameters
        params = [f'-PinstancePath="{instance_path}"', f'-PoutputPath="{output_directory}"',
                  f'-PmodelType="{model_type}"', f'-PcheckPointTimes="{checkpoint_times_str}"',
                  f'-PlogPath="{log_path}"']

        # Construct full command
        command = f"{base_command} {' '.join(params)}"
        commands.append(command)

    return list(zip(commands, [120 for _ in commands]))


def generate_grasp_cli_commands(config_path: str, base_command: str = "./gradlew runGrasp") -> List[tuple[str, int]]:
    """
    Generate CLI commands for GRASP algorithm based on configuration file.
    
    Args:
        config_path: Path to the YAML configuration file
        base_command: Base command to use (default: "./gradlew runGrasp")
        
    Returns:
        List of CLI command strings
    """
    config = read_grasp_config(config_path)
    commands = []

    # Extract configuration parameters
    instance_paths = config.get('instancePaths', [])
    search_modes = config.get('searchModes', ['BEST_IMPROVEMENT'])
    alpha_options = config.get('alphaGeneratorOptions', [])
    skip_probabilities = config.get('neighborhoodSkipProbabilities', [0.0])
    local_search_moves = config.get('localSearchMoves', [])
    time_limit = config.get('timeLimit', 60)
    random_run_n = config.get('randomRunN', 1)
    output_directory = config.get('outputDirectory', 'output')

    # Generate all combinations
    for instance_path in instance_paths:
        for search_mode in search_modes:
            for alpha_option in alpha_options:
                for skip_prob in skip_probabilities:
                    for run_number in range(random_run_n):

                        # Extract instance name for output path
                        instance_name = os.path.basename(instance_path)

                        # Create unique output path
                        alpha_str = _get_alpha_string(alpha_option)

                        # Prepare local search moves string
                        local_search_moves_str = ",".join(local_search_moves)

                        # Build command parameters
                        params = [f'-PinstancePath="{instance_path}"', f'-PoutputPath="{output_directory}"',
                                  f'-PtimeLimit={time_limit}', f'-PsearchMode="{search_mode}"',
                                  f'-PskipProbability={skip_prob}', f'-PlocalSearchMoves="{local_search_moves_str}"',
                                  f'-Pseed={run_number}']
                        
                        if config.get('parallel', False):
                            params.append('-Pparallel')
                            threads = config.get('threads', 0)
                            if threads > 0:
                                params.append(f'-Pthreads={threads}')

                        # Add alpha parameters based on type
                        if alpha_option.get('type', '').lower() == 'fixed':
                            params.extend(['-PalphaType="FIXED"', f'-Palpha={alpha_option.get("alpha", 0.25)}'])
                        elif alpha_option.get('type', '').lower() == 'uniform':
                            params.extend(['-PalphaType="UNIFORM"', f'-PminAlpha={alpha_option.get("minAlpha", 0.1)}',
                                           f'-PmaxAlpha={alpha_option.get("maxAlpha", 1.0)}'])

                        # Construct full command
                        command = f"{base_command} {' '.join(params)}"
                        commands.append(command)

    return list(zip(commands, [5 for _ in commands]))


def _get_alpha_string(alpha_option: Dict[str, Any]) -> str:
    """
    Generate a string representation of alpha configuration for file naming.
    
    Args:
        alpha_option: Dictionary containing alpha configuration
        
    Returns:
        String representation of alpha configuration
    """
    if alpha_option.get('type', '').lower() == 'fixed':
        return f"fixed_{alpha_option.get('alpha', 0.25)}"
    elif alpha_option.get('type', '').lower() == 'uniform':
        min_alpha = alpha_option.get('minAlpha', 0.1)
        max_alpha = alpha_option.get('maxAlpha', 1.0)
        return f"uniform_{min_alpha}_{max_alpha}"
    else:
        return "unknown"


def run_cli_command(command: str) -> Tuple[bool, str, str]:
    """
    Execute a CLI command and return the result.
    
    Args:
        command: CLI command string to execute
        
    Returns:
        Tuple of (success, stdout, stderr)
    """
    print(f"Executing: {command}")

    try:
        result = subprocess.run(command, shell=True, capture_output=True, text=True)

        success = result.returncode == 0
        stdout = result.stdout or ''
        stderr = result.stderr or ''

        if not success:
            print(f"Command failed with return code: {result.returncode}")
            if stderr:
                print(f"Error: {stderr}")

        return success, stdout, stderr

    except Exception as e:
        return False, "", str(e)


def run_commands_sequentially(commands: List[str], stop_on_error: bool = False, log_file: Optional[str] = None) -> List[
    Dict[str, Any]]:
    """
    Run a list of CLI commands sequentially.
    
    Args:
        commands: List of CLI command strings
        stop_on_error: Whether to stop execution on first error
        log_file: Optional file to log results
        
    Returns:
        List of execution results for each command
    """
    results = []
    start_time = time.time()

    print(f"Starting execution of {len(commands)} commands...")
    if log_file:
        with open(log_file, 'w') as f:
            f.write(f"GRASP Execution Log - {time.strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"Total commands: {len(commands)}\n")
            f.write("=" * 80 + "\n\n")

    for i, command in enumerate(commands, 1):
        cmd_start_time = time.time()

        print(f"\n[{i}/{len(commands)}] Starting command {i}...")

        success, stdout, stderr = run_cli_command(command)

        cmd_end_time = time.time()
        cmd_duration = cmd_end_time - cmd_start_time

        result = {'command_index': i, 'command': command, 'success': success, 'stdout': stdout, 'stderr': stderr,
                  'duration': cmd_duration,
                  'timestamp': time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(cmd_start_time))}

        print(stdout)

        results.append(result)

        # Log result
        status = "SUCCESS" if success else "FAILED"
        print(f"Command {i} {status} (Duration: {cmd_duration:.2f}s)")

        if log_file:
            with open(log_file, 'a') as f:
                f.write(f"Command {i}: {status}\n")
                f.write(f"Duration: {cmd_duration:.2f}s\n")
                f.write(f"Command: {command}\n")
                if not success:
                    f.write(f"Error: {stderr}\n")
                f.write("-" * 40 + "\n\n")

        # Stop on error if requested
        if not success and stop_on_error:
            print(f"Stopping execution due to error in command {i}")
            break

    total_duration = time.time() - start_time
    successful_commands = sum(1 for r in results if r['success'])
    failed_commands = len(results) - successful_commands

    print(f"\n{'=' * 80}")
    print(f"Execution Summary:")
    print(f"Total commands: {len(commands)}")
    print(f"Executed: {len(results)}")
    print(f"Successful: {successful_commands}")
    print(f"Failed: {failed_commands}")
    print(f"Total duration: {total_duration:.2f}s")
    print(f"Average duration per command: {total_duration / len(results):.2f}s")

    if log_file:
        with open(log_file, 'a') as f:
            f.write(f"\nExecution Summary:\n")
            f.write(f"Total commands: {len(commands)}\n")
            f.write(f"Executed: {len(results)}\n")
            f.write(f"Successful: {successful_commands}\n")
            f.write(f"Failed: {failed_commands}\n")
            f.write(f"Total duration: {total_duration:.2f}s\n")

    return results


def generate_run_commands(number_of_files: int):
    commands_folder = Path("grasp_commands")
    if not commands_folder.exists():
        commands_folder.mkdir()

    grasp_commands = generate_grasp_cli_commands("yamlConfigGrasp.yaml", "./gradlew runGrasp")
#     mip_continuous_commands = generate_mip_cli_commands("yamlConfigContinuous.yaml", "./gradlew runMipSolver")
#     mip_discrete_commands = generate_mip_cli_commands("yamlConfigDiscrete.yaml", "./gradlew runMipSolver")
    # bee_commands = generate_bee_cli_commands("yamlConfigBee.yaml", "./gradlew runBeeColony")

    all_commands_tuples = grasp_commands
    run_times = [command_tuple[1] for command_tuple in all_commands_tuples]
    total_run_time = sum(run_times)

    run_time_per_file = total_run_time // number_of_files
    print(f"Total run time: {total_run_time}mins, Run time per file: {run_time_per_file}mins")

    file_duration = 0
    commands_to_write: list[list[str]] = []
    commands: list[str] = []
    for command, duration in all_commands_tuples:
        if file_duration + duration > run_time_per_file:
            if len(commands) > 0:
                commands_to_write.append(commands)
            commands = []
            file_duration = 0

        commands.append(command)
        file_duration += duration

    if len(commands) > 0:
        commands_to_write.append(commands)

    while len(commands_to_write) > number_of_files:
        last_file_commands = commands_to_write.pop()
        commands_to_write[-1].extend(last_file_commands)

    for i, commands in enumerate(commands_to_write, 1):
        with open(commands_folder / f"run_commands_{i}.txt", "w") as f:
            f.write("\n".join(commands))


def read_commands(path: str) -> list[str]:
    with open(path, "r") as f:
        return f.read().splitlines()


def main():
    commands = read_commands(sys.argv[1])
    results = run_commands_sequentially(commands)

#     generate_run_commands(10)


if __name__ == "__main__":
    main()
