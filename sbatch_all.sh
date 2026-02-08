#!/bin/bash

for i in {1..10}
do
   sbatch run.slurm "commands/BeeColony/commands_${i}.txt"
done

