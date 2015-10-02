PCAP
=====================

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
1. Go to LatestTimestampedYCSB/Scripts
2. Change the field 'hosts' in ycsb_run_riak.sh if necessary (if you have less or more nodes)
3. python control_riak.py N sla pa/pc

  For example, if you have 9 nodes and pc_sla = 0.15, then run:

    python control_riak.py 9 0.15 pc

Contact:
-------------------
son - sonnbc@gmail.com
