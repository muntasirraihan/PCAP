#!/bin/bash

#echo 'YCSB START ...................'

RDELAY=$1
prefix=$2

#echo "YCSB $prefix $RDELAY"

#For load:
# java -cp $RIAK/riakycsb/*:$RIAK/riakycsb/lib/*: com.yahoo.ycsb.Client -load -db com.son.riakycsb.RiakClient -P $RIAK/riakycsb/workloada -s -p hosts="node-00.riak.Confluence.emulab.net:8098,node-01.riak.Confluence.emulab.net:8098,node-02.riak.Confluence.emulab.net:8098,node-03.riak.Confluence.emulab.net:8098,node-04.riak.Confluence.emulab.net:8098,node-05.riak.Confluence.emulab.net:8098,node-06.riak.Confluence.emulab.net:8098" -i -clog $RIAK/log/write.txt > out/write.out 2>&1

#For run:

for i in `seq 1 16`;
do
    java -cp $RIAK/riakycsb/*:$RIAK/riakycsb/lib/*: com.yahoo.ycsb.Client -t -db com.son.riakycsb.RiakClient -P $RIAK/riakycsb/workloada -s -p hosts="node-00:8098,node-01:8098,node-02:8098,node-03:8098,node-04:8098,node-05:8098,node-06:8098,node-07:8098,node-08:8098" -p readDelay=$RDELAY -i -clog $RIAK/log0/log$RDELAY/$prefix-$i.log > $RIAK/LatestTimestampedYCSB/Scripts/out/$prefix-$i.out 2>&1 &
done

wait

#echo 'YCSB DONE  ...............'
