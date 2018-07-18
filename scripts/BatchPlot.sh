#!/bin/bash

# Adjust the number of simulation groups
G_CNT=6
# Adjust the number of simulations in a groups
SUB=8
OFFSET=2

for i in $(seq 0 $((${G_CNT}-1))); do
  from=`echo $OFFSET+$i*${SUB} | bc`
  to=`echo $OFFSET+\($i+1\)*${SUB}-1 | bc`
  python Plot.py $(seq $from $to);
  echo "Plot $i" > ../results/figures/description_$i.txt;
  for j in $(seq $from $to); do
    echo $j >> ../results/figures/description_$i.txt;
    cat ../results/logs/scenario_$j/description.txt >> ../results/figures/description_$i.txt;
  done
done
