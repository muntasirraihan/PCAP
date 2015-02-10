import numpy as np
import sys, os

def add_delay_poisson(num_nodes, expected_delay):
    s = np.random.poisson(expected_delay, num_nodes)
    print "delay values:", s

    for i in xrange(num_nodes):
        ssh = "ssh sonnbc@node-01.riak.Confluence.emulab.net tevc -e Confluence/riak now lan-0-node-0%s modify delay=%s" % (i, s[i])
        os.system(ssh)
            



if __name__ == '__main__':
    num_nodes = int(sys.argv[1])
    expected_delay = int(sys.argv[2])
    add_delay_poisson(num_nodes, expected_delay)