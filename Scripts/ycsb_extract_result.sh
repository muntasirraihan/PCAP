#!/bin/bash

TC=$1
TA=$2
RDELAY=$3

./exp.sh $RIAK/log0/log$RDELAY $TC $TA > /dev/null 2>&1
#pa=$(tail -1 tapa.csv | awk -F "," '{print $2}')
#pc=$(tail -1 tcpc.csv | awk -F "," '{print $2}')

#echo $pa $pc > ycsb.result
