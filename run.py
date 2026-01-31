#!/usr/bin/env python3
"""
Script for executing CLI commands from a commands file.
"""

import subprocess
import sys
import time
from typing import List, Dict, Any, Optional, Tuple


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


def run_commands_sequentially(commands: List[str], stop_on_error: bool = False, log_file: Optional[str] = None) -> List[Dict[str, Any]]:
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
            f.write(f"Execution Log - {time.strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"Total commands: {len(commands)}\n")
            f.write("=" * 80 + "\n\n")

    for i, command in enumerate(commands, 1):
        cmd_start_time = time.time()

        print(f"\n[{i}/{len(commands)}] Starting command {i}...")

        success, stdout, stderr = run_cli_command(command)

        cmd_end_time = time.time()
        cmd_duration = cmd_end_time - cmd_start_time

        result = {
            'command_index': i,
            'command': command,
            'success': success,
            'stdout': stdout,
            'stderr': stderr,
            'duration': cmd_duration,
            'timestamp': time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(cmd_start_time))
        }

        print(stdout)

        results.append(result)

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
    if results:
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


def read_commands(path: str) -> List[str]:
    """Read commands from a file, one command per line."""
    with open(path, "r") as f:
        return [line for line in f.read().splitlines() if line.strip()]


def main():
    if len(sys.argv) != 2:
        print("Usage: python run.py <commands_file.txt>")
        sys.exit(1)

    commands_file = sys.argv[1]
    commands = read_commands(commands_file)
    run_commands_sequentially(commands)


if __name__ == "__main__":
    main()
