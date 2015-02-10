#!/bin/bash

N=$1
RDELAY=$2

echo $N $RDELAY
for (( i = 0; i < $N; i++ ))
do
    #echo node-0$i
    ssh -oStrictHostKeyChecking=no node-0$i "$RIAK/LatestTimestampedYCSB/Scripts/ycsb_run_riak.sh $RDELAY node-0$i" &
done

wait
sleep 2s #wait for nodes to cool down
