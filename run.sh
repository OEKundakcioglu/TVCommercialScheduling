#!/usr/bin/env bash
set -euo pipefail

########################################
# Usage: ./run.sh <idx>
# Example: ./run.sh 5
# Runs:   python run.py commands/run_commands_5.txt
########################################

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <command_index>" >&2
  echo "Example: $0 5   # runs commands/run_commands_5.txt" >&2
  exit 1
fi

CMD_INDEX="$1"

# Go to the directory where this script lives
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

CMD_FILE="commands/run_commands_${CMD_INDEX}.txt"
if [[ ! -f "$CMD_FILE" ]]; then
  echo "[ERROR] Command file not found: $CMD_FILE" >&2
  exit 1
fi

echo "[INFO] Project root: $PROJECT_ROOT"
echo "[INFO] Command index: $CMD_INDEX"
echo "[INFO] Command file: $CMD_FILE"

########################################
# 1. Find / load a base Python
########################################

if [[ -z "${PYTHON:-}" ]]; then
  if command -v module &>/dev/null; then
    if [[ -f /etc/profile.d/modules.sh ]]; then
      # shellcheck disable=SC1091
      source /etc/profile.d/modules.sh
    fi
    module load python/3.11.7 2>/dev/null || true
  fi

  if command -v python3 &>/dev/null; then
    PYTHON="$(command -v python3)"
  elif command -v python &>/dev/null; then
    PYTHON="$(command -v python)"
  else
    echo "[ERROR] No python interpreter found in PATH." >&2
    exit 1
  fi
else
  if ! command -v "$PYTHON" &>/dev/null; then
    echo "[ERROR] PYTHON='$PYTHON' not found in PATH." >&2
    exit 1
  fi
  PYTHON="$(command -v "$PYTHON")"
fi

echo "[INFO] Base Python: $PYTHON"

########################################
# 2. Create a per-job/per-index venv
########################################

if [[ -n "${SLURM_JOB_ID:-}" ]]; then
  BASE_DIR="${SLURM_TMPDIR:-/tmp}"
  VENV_DIR="${BASE_DIR}/venv_${USER}_${SLURM_JOB_ID}_${CMD_INDEX}"
else
  VENV_DIR="${PROJECT_ROOT}/.venv_${CMD_INDEX}"
fi

echo "[INFO] Virtualenv directory: $VENV_DIR"

if [[ ! -d "$VENV_DIR" ]]; then
  echo "[INFO] Creating virtualenv..."
  "$PYTHON" -m venv "$VENV_DIR"
fi

# Activate venv
# shellcheck disable=SC1091
source "$VENV_DIR/bin/activate"

echo "[INFO] Using Python from venv: $(command -v python)"

########################################
# 3. Install requirements.txt if exists
########################################

if [[ -f "${PROJECT_ROOT}/requirements.txt" ]]; then
  echo "[INFO] Installing dependencies from requirements.txt..."
  pip install --upgrade pip >/dev/null 2>&1 || true
  pip install -r "${PROJECT_ROOT}/requirements.txt"
else
  echo "[INFO] No requirements.txt found — skipping dependency installation."
fi

########################################
# 4. Run the command
########################################

echo "[INFO] Running: python run.py \"$CMD_FILE\""
python run.py "$CMD_FILE"

echo "[INFO] Done."
