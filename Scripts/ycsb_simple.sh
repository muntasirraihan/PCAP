#!/bin/sh

echo 'YCSB START ...................'

rm -rf /users/mrahman2/scripts/CLOG/write.txt
rm -rf /users/mrahman2/scripts/CLOG/*.log

RDELAY=$1

#For load:
java -cp /proj/ISS/conbench/ycsb-0.1.4/core/lib/core-0.1.4.jar:/proj/ISS/conbench/ycsb-0.1.4/lib/*:/proj/ISS/conbench/ycsb-0.1.4/cassandra-binding/lib/cassandra-binding-0.1.4.jar -DConBench.RCON="ONE" -DConBench.WCON="ONE" -DConBench.RDELAY=$1 com.yahoo.ycsb.Client -load -db com.yahoo.ycsb.db.CassandraClient10 -P /proj/ISS/conbench/ycsb-0.1.4/workloads/workloada -threads 1 -s -p hosts="10.1.1.10,10.1.1.11,10.1.1.2,10.1.1.3,10.1.1.4,10.1.1.5,10.1.1.6,10.1.1.7,10.1.1.8,10.1.1.9" -i -clog /users/mrahman2/scripts/CLOG/write.txt

#For run:
for i in `seq 1 64`; 
do 
	java -cp /proj/ISS/conbench/ycsb-0.1.4/core/lib/core-0.1.4.jar:/proj/ISS/conbench/ycsb-0.1.4/lib/*:/proj/ISS/conbench/ycsb-0.1.4/cassandra-binding/lib/cassandra-binding-0.1.4.jar -DConBench.RCON="ONE" -DConBench.WCON="ONE" -DConBench.RDELAY=$1 com.yahoo.ycsb.Client -t -db com.yahoo.ycsb.db.CassandraClient10 -P /proj/ISS/conbench/ycsb-0.1.4/workloads/workloada -threads 1 -target 1000 -s -p hosts="10.1.1.10,10.1.1.11,10.1.1.2,10.1.1.3,10.1.1.4,10.1.1.5,10.1.1.6,10.1.1.7,10.1.1.8,10.1.1.9" -i -clog /users/mrahman2/scripts/CLOG/$i.log &   

done

wait

echo 'YCSB DONE  ........... ...'


