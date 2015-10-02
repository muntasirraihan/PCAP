PCAP
=====================

Summary:
-------------------
Key-value store customers expect both fast response time and freshness of data (a read should return the latest written value), but are constrained by the CAP theorem trade-offs. In this work, we build a system called PCAP that allows applications of key-value stores to specify either a probabilistic latency (A) SLA or a probabilistic consistency (C) SLA. Given a probabilistic A (C) SLA, PCAP's adaptive techniques meet the specified A (C) requirement, while optimizing the other metric C (A), under real and continuously changing network conditions. An A SLA can be beneficial to a shopping cart application, where users demand fast response times, but are willing to tolerate stale data. A use case for C SLA is a web search application, which require bounded staleness for search results, and would like to minimize the response time.

Given an application C or A SLA, we add control knobs and an adaptive controller to convert the target system into a PCAP system that can meet the specified SLAs. We mainly use a read delay knob, which adds a tunable time delay before any read operation. Controlling this delay allows fine grained trade-off between C and A. Our adaptive controller uses an active approach, where we inject a small number of test operations to estimate current metric values, so that we can tune the control knobs based on how far we are from the SLA. The active approach allows better control on the SLA convergence. We integrated our PCAP adaptive methodology into two popular key-value stores: Apache Cassandra and Riak, and our experiments indicate that PCAP Cassandra and PCAP Riak can adapt in real-time and under changing network conditions to meet the SLA, while optimizing the other metric.

A technical report on our PCAP system is available here:
http://arxiv.org/abs/1509.02464

PCAP SLA Specification:
-------------------
We need to define 4 terms before we can specify PCAP SLAs.

tc: a read is tc-fresh if it returns the value of a write that starts at-mots tc time before the read starts.

pic: pic is the likelihood a read is not tc-fresh.

ta: ta is a deadline on read operation latency

pua: pua is the likelihood a read does not return an answer within ta time units

Latency SLA: 
-------------------
A latency SLA meets a desired time deadline for read operations, while maximizing the chance of getting fresh results (minimize staleness). 

An example application using this SLA would be the Amazon shopping cart, which doesn’t want to lose customers due to high latency. 

A latency SLA can be specified in PCAP as follows:

SLA: (pua, ta, tc) = (0.1, 300ms, 5ms) 

This indicates only 10% read operations can take longer than 300ms, while the number of 5ms-fresh reads (reads that return write values at-most 5ms in the past) will be minimized. Such latency SLAs are commonly used in industry today (except the optimization part, Pileus (http://research.microsoft.com/en-us/projects/capcloud/) offers similar types of SLA).

Consistency SLA:
-------------------
A consistency SLA meets a desired freshness probability, while maximizing the chance of receiving the read result within a deadline. An example application using this SLA would be Google/Twitter search, where users want to receive ‘recent’ data as search results. 
A consistency SLA can be specified in PCAP as follows:

SLA: (pic, tc, ta) = (0.1, 5min, 100ms) 

This indicates only 10% read operations can be older than 5min, while the number of operations finishing within 100ms-fresh reads will be maximized. 

Given such SLAs, our adaptive control loop (independent of a key-value store) will tune control knobs to meet the SLA. Details can be found in our paper: http://arxiv.org/abs/1509.02464.


Codebase Overview
-------------------
There are 3 repos:
  1. https://github.com/Sonnbc/riak

    This contains modified Riak and code to set up the Riak cluster

  2. https://github.com/Sonnbc/riakycsb

    YCSB's Riak client

  3. https://github.com/muntasirraihan/PCAP

    Scripts for control algorithm and analytics code

The code is designed to run on Emulab node only. You have to ssh to
Emulab and follow the instructions below.

Configurations:
-------------------
  You need to mkdir a directory in the NFS and clone the 3 repos above
  as subdirectories.

  Export $RIAK as a envonriment variable to this newly created directory.
  So 3 cloned repos will be:

    $RIAK/riak

    $RIAK/riakycsb

    $RIAK/PCAP

  Also, replace "N" in all commands below with the number of nodes in your
  cluster

Compile riak:
-------------------
  You might need to install Erlang first

  Then compile riak by:

    cd $RIAK/riak/riak-1.4.2

    make rel

  The binary will be placed in $RIAK/riak/riak-1,4,2/rel/riak

Set up cluster:
-------------------
1. Change hosts filie

  cd $RIAK/riak

  Change the file "nodes" with the correct information of your nodes. (Most
  importantly the number of nodes and the DNS). You can get this info
  from Emulab's email when you swap in.

  Run this command to extract local and internet IP addresseses of your nodes.
  IMPORTANT: The command doesn't work if you have more than 10 nodes. So you
  would need to ping the nodes manually to get the IP addresses.

    cat nodes | awk '{print $4}' | xargs -L1 host | grep -Eo '[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}' > /tmp/2 && cat nodes | awk {'print $1'} | xargs -L1 getent ahosts | grep 'RAW' | awk '{print $1}' > /tmp/1 && paste /tmp/1 /tmp/2

  Put the local and internet IP addresses gotten above to "hosts" file

2. Deploy the Riak binary to individual nodes:

  ./run.sh -u yourID -d

3. Start the cluster:

  ./start.sh N

4. Check if cluster is properly started:

  From any node, run:

    /riak/riak/bin/riak-admin member-status

  Check output to see if all nodes are recognized

Run experiments:
-------------------
1. Go to PCAP/Scripts
2. Change the field 'hosts' in ycsb_run_riak.sh if necessary (if you have less or more nodes)
3. python control_riak.py N sla pa/pc

  For example, if you have 9 nodes and pc_sla = 0.15, then run:

    python control_riak.py 9 0.15 pc

Contact:
-------------------
Muntasir - muntasir.raihan@gmail.com

Son - sonnbc@gmail.com
