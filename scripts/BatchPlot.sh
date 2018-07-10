#!/bin/bash

# Adjust the number of simulation groups
G_CNT=6
# Adjust the number of simulations in a groups
SUB=8

for i in $(seq 0 $((${G_CNT}-1))); do
  python Plot.py $(seq $(($i*${SUB})) $((($i+1)*${SUB}-1)));
  echo "Plot $i" > ../results/figures/description_$i.txt;
  for j in $(seq $(($i*${SUB})) $((($i+1)*${SUB}-1))); do
    echo $j >> ../results/figures/description_$i.txt;
    cat ../results/logs/scenario_$j/description.txt >> ../results/figures/description_$i.txt;
  done
done