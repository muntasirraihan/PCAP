#!/bin/bash

HOME_DIR=/proj/ISS/conbench
SCRIPT_DIR=$HOME_DIR/scripts

CLUSTER_NAME=node-0

#./insert_random_delay.sh 

#COMMAND="cd /u1/mr2rahman/BEC/scripts; ./ping_hosts.sh; mv avg_delay_cluster.txt avg_delay_from_marroo$c"

rm -rf avg_delay_from*.txt


# N is the number of hosts in the cluster 
N=`wc -l hosts.txt | cut -d' ' -f1`

echo 'Number of hosts is:' $N

#INJECT_COMMAND="cd $SCRIPT_DIR; ./insert_random_delay.sh $CLUSTER $N"

#ssh mrahman2@users.emulab.net $INJECT_COMMAND

#for (( c = 0; c < $N; c++ ))
for host in `cat hosts.txt`
do
  COMMAND="cd $SCRIPT_DIR; ./ping_hosts.sh; mv avg_delay_cluster.txt avg_delay_from_$host.txt"
  ssh $host $COMMAND
done

rm -rf all_to_all_delay.txt
touch all_to_all_delay.txt
chmod 777 all_to_all_delay.txt

for host in `cat hosts.txt`
do
  COMMAND="cd $SCRIPT_DIR; cat avg_delay_from_$host.txt >> all_to_all_delay.txt"
  ssh $host $COMMAND
done

rm -rf final_delay_cluster.txt

cat all_to_all_delay.txt | awk 'BEGIN{c=0;sum=0;}\
/^[^#]/{a[c++]=$1;sum+=$1;}\
END{ave=sum/c;\
print ave}' >> final_delay_cluster.txt
