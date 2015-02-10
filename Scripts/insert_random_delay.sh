#!/bin/bash

#clear

EXPECTED_ARGS=4

if [ $# -ne $EXPECTED_ARGS ]
then
    echo "USAGE: ./insert_random_delay.sh cluster_name node_count delay spread"
fi

#DELAY=$1
CLUSTER=$1
NUM_NODES=$2
DELAY=$3
R=$4

echo "-----------STARTING TO INSERT DELAY IN NETWORK---------"

I=$(($NUM_NODES/$R))
echo "delay every " $I "th node"

for (( i = 0; i < $NUM_NODES; i += I ))
do
    echo "i=" $i
    tevc -e ISS/$CLUSTER now lan-0-node-0$i modify delay=$(($DELAY))ms
   
done

for job in $(jobs -p); do
  wait $job
done

echo "----------------DONE INSERTING DELAY"



