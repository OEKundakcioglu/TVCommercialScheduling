#!/bin/bash
# Submit with: sbatch run_cpu_full.slurm

#  SLURM directives 
#SBATCH --job-name=runsh_cpu_full               # Job name
#SBATCH --output=logs/%x_%j.out                 # Stdout log -> logs/<jobname>_<jobid>.out
#SBATCH --error=logs/%x_%j.err                  # Stderr log -> logs/<jobname>_<jobid>.err
#SBATCH --time=15-00:00:00                         # Wall time limit
#SBATCH --nodes=1                               # One node
#SBATCH --ntasks=1                              # One task (process)
#SBATCH --exclusive                             # Reserve entire node
#SBATCH --mem=0                                 # Take all memory on the node

# Pick ONE partition. Uncomment the one you want:
#SBATCH --partition=longer_mdbf                    # Typical default
#SBATCH --qos=longer_mdbf

# #SBATCH --partition=short_mdbf
# #SBATCH --partition=long_mdbf
# #SBATCH --partition=longer_mdbf
# #SBATCH --partition=mem                       # If you need the "mem" queue

# Ensure we never land on GPU nodes:
#SBATCH --exclude=cn[05-06,08-13,15-16,17-18]   # Exclude K2 and P4 GPU nodes
# #SBATCH --nodelist=cn[01-04,19-25]

# Optional for deterministic CPU perf (no SMT/HT):
# #SBATCH --hint=nomultithread

#  Shell safety & prep 
set -euo pipefail
mkdir -p logs

# Determine CPU count to use (prefer SLURMs number; fallback to nproc  1)
CPUS=${SLURM_CPUS_ON_NODE:-$(nproc 2>/dev/null || echo 1)}

#  Node summary to both job log and a separate file 
LOG="logs/node_summary_${SLURM_JOB_ID}.log"
{
  echo "=== SLURM JOB SUMMARY ==="
  echo "Job ID:        ${SLURM_JOB_ID}"
  echo "Job Name:      ${SLURM_JOB_NAME:-unknown}"
  echo "Partition:     ${SLURM_JOB_PARTITION:-unknown}"
  echo "Node(s):       ${SLURM_NODELIST}"
  echo "Exclusive:     yes"
  echo "Mem request:   all-node (--mem=0)"
  echo

  echo "=== SLURM NODE DETAIL (scheduler view) ==="
  FIRST_NODE=${SLURM_JOB_NODELIST%%,*}
  scontrol show node "${FIRST_NODE}" 2>/dev/null \
    | sed 's/ \+/\n/g' \
    | egrep 'NodeName=|CoresPerSocket=|ThreadsPerCore=|Sockets=|CPUTot=|RealMemory=' || true
  echo

  echo "=== OS VIEW (on allocated node) ==="
  command -v lscpu >/dev/null 2>&1 && lscpu || echo "lscpu not found"
  echo
  free -h || echo "free not found"
  echo
  command -v numactl >/dev/null 2>&1 && numactl --hardware || echo "numactl not found"
  echo
} | tee "$LOG"

#  Environment (uncomment as needed) 
module load python/3.11.7
# source ./setup_gurobi_env.sh

# Threading hygiene for BLAS/OpenMP-backed libs (not for Gurobi Threads param)
export OMP_NUM_THREADS=${CPUS}
export MKL_NUM_THREADS=${CPUS}
export OPENBLAS_NUM_THREADS=${CPUS}

#  Optional: CPU utilization sampler (requires mpstat from sysstat) 
# CPU_SAMPLER_LOG="logs/cpu_util_${SLURM_JOB_ID}.txt"
# if command -v mpstat >/dev/null 2>&1; then
#   mpstat 5 9999 > "${CPU_SAMPLER_LOG}" &
#   CPU_SAMPLER_PID=$!
#   trap 'kill ${CPU_SAMPLER_PID} 2>/dev/null || true' EXIT
# fi

#  Run workload 
echo "[INFO] SLURM job ${SLURM_JOB_ID:-N/A} starting on $(hostname) at $(date)"
# Go to the directory where the script was submitted from
cd "$SLURM_SUBMIT_DIR"
# Take the first positional argument to the SLURM script as the command index.
# Default to 1 if nothing is provided.
CMD_INDEX="${1:-1}"
chmod +x ./run.sh
echo "[INFO] Launching run.sh with index ${CMD_INDEX} using ${CPUS} CPU threads"
srun --ntasks=1 --cpus-per-task=${CPUS} ./run.sh "$CMD_INDEX"
echo "[INFO] SLURM job ${SLURM_JOB_ID:-N/A} finished at $(date)"

#  Build a one-line human summary 
CPU_MODEL=$(lscpu 2>/dev/null | awk -F: '/Model name/ {gsub(/^ +| +$/,"",$2); print $2; exit}')
CPU_MAX_MHZ=$(lscpu 2>/dev/null | awk -F: '/CPU max MHz/ {gsub(/^ +| +$/,"",$2); print $2; exit}')
CPU_GHZ=""
if [ -n "${CPU_MAX_MHZ}" ]; then
  CPU_GHZ=$(awk -v mhz="$CPU_MAX_MHZ" 'BEGIN{printf("%.1f GHz", mhz/1000.0)}')
elif echo "${CPU_MODEL}" | grep -Eoi '[0-9.]+ghz' >/dev/null; then
  CPU_GHZ=$(echo "${CPU_MODEL}" | grep -Eoi '[0-9.]+ghz' | head -1 | tr '[:lower:]' '[:upper:]')
fi

CPU_LOGICAL=$(lscpu 2>/dev/null | awk -F: '/^CPU\(s\)/ {gsub(/^ +| +$/,"",$2); print $2; exit}')
[ -z "${CPU_LOGICAL}" ] && CPU_LOGICAL="${CPUS}"

TOTAL_RAM=$(free -h 2>/dev/null | awk '/^Mem:/ {print $2}')

SUMMARY="CPU: ${CPU_GHZ:+${CPU_GHZ} }${CPU_MODEL:-unknown} (${CPU_LOGICAL}-logical)  RAM: ${TOTAL_RAM:-unknown}  GPU: none"

echo
echo "=== HUMAN SUMMARY ==="
echo "${SUMMARY}"
echo

# Append to node summary log as well
{
  echo
  echo "=== HUMAN SUMMARY ==="
  echo "${SUMMARY}"
} >> "$LOG"
