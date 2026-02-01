#!/bin/bash

for i in {1..10}
do
   sbatch run.slurm "commands/GRASPParallel/commands_${i}.txt"
done

for i in {1..10}
do
   sbatch run.slurm "commands/GRASPSingle/commands_${i}.txt"
done
