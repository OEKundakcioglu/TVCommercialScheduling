#!/bin/bash

#for i in {1..10}
#do
#   sbatch run.slurm "grasp_parallel_commands/run_commands_${i}.txt"
#done

for i in {1..10}
do
   sbatch run.slurm "grasp_single_thread_commands/run_commands_${i}.txt"
done
