#!/bin/bash
clear

#cd /home/muntasir/hadoop_natjam_deadline/hadoop-common
#git pull
#mvn3 package -Pdist -DskipTests -Dtar -Dmaven.javadoc.skip=true
#cd /home/muntasir/hadoop_natjam_deadline/hadoop-common/hadoop-dist/target
#sudo scp hadoop-0.23.3-SNAPSHOT.tar.gz mrahman2@node-00.natjam-test.iss.emulab.net:/proj/ISS/scheduling

NUM_NODES=$1
#START_NODE=$2
#END_NODE=$3
#MASTER_NODE=0

#cd /proj/ISS/scheduling
#sudo rm -rf slaves
#touch slaves
#for (( i = $START_NODE + 1; i < $NUM_NODES; i++ ))
#do
#    echo "node-0$i">> slaves
#done

JAVA_COMMAND="sudo apt-get update; sudo apt-get install -y openjdk-7-jdk"

for (( c = 0; c < $NUM_NODES; c++ ))
do
    ssh node-0$c $JAVA_COMMAND
done


