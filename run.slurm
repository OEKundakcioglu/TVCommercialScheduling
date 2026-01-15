#!/bin/bash

#SBATCH --job-name=tv-commercial               # Job name
#SBATCH --output=logs/%x_%j.out                 # Stdout log -> logs/<jobname>_<jobid>.out
#SBATCH --error=logs/%x_%j.err                  # Stderr log -> logs/<jobname>_<jobid>.err
#SBATCH --time=1-00:00:00                      # Wall time limit
#SBATCH --nodes=1                               # One node
#SBATCH --ntasks=1                              # One task (process)
#SBATCH --exclusive                             # Reserve entire node
#SBATCH --mem=0                                 # Take all memory on the node

# Pick ONE partition. Uncomment the one you want:
#SBATCH --partition=mid_mdbf                    # Typical default
#SBATCH --qos=mid_mdbf

# Ensure we never land on GPU nodes:
#SBATCH --exclude=cn[05-06,08-13,15-16,17-18]   # Exclude K2 and P4 GPU nodes
# #SBATCH --nodelist=cn[01-04,19-25]

# Optional for deterministic CPU perf (no SMT/HT):
#SBATCH --hint=nomultithread


# Exit immediately if a command fails
set -e

module load python/3.11.7

VENV_DIR="./venv_commercial"

if [ ! -d "$VENV_DIR" ]; then
    echo "[INFO] Creating virtual environment..."
    python -m venv "$VENV_DIR"
    source "$VENV_DIR/bin/activate"

    echo "[INFO] Installing requirements..."
    pip install --upgrade pip
    pip install -r requirements.txt
else
    echo "[INFO] Activating existing environment..."
    source "$VENV_DIR/bin/activate"
fi

# 2. Configure Threading
export OMP_NUM_THREADS=$SLURM_CPUS_ON_NODE
export MKL_NUM_THREADS=$SLURM_CPUS_ON_NODE

export JAVA_HOME=$HOME/jdk-25.0.1
export PATH=$JAVA_HOME/bin:$PATH

# 3. Handle Arguments (File Path)
# $1 is the first argument passed to sbatch.
# If no argument is provided, we fail (or you could set a default).
INPUT_FILE="${1}"

if [ -z "$INPUT_FILE" ]; then
    echo "Error: No input file path provided."
    echo "Usage: sbatch run_python.slurm /path/to/data.json"
    exit 1
fi


# ==========================================
# 4. Log Node Information
# ==========================================
echo "=========================================="
echo " SLURM JOB STATISTICS"
echo "=========================================="
echo "Job ID:        $SLURM_JOB_ID"
echo "Node:          $(hostname)"
echo "User:          $USER"
echo "Date:          $(date)"
echo "Input File:    $INPUT_FILE"
echo "------------------------------------------"
echo " CPU Details:"
# grep captures just the model name to keep it clean
lscpu | grep "Model name"
echo "CPU Cores:     $SLURM_CPUS_ON_NODE"
echo "------------------------------------------"
echo " Memory Details:"
# free -h gives human readable RAM (e.g., 64Gi)
free -h
echo "=========================================="
echo ""


# 4. Run Python Script
# We assume your python script is named 'main.py' in the current folder.
srun python run.py "$INPUT_FILE"

echo "[INFO] Job finished"