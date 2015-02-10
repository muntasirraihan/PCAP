#!/usr/bin/env python                                                                                                                                                  
import os, sys, shutil, time, threading, math
import settings, numpy

def get_pc(p):
    with open("pc.csv") as f:
        floats = map(float, f)
    return numpy.percentile(floats, p)    

#def get_pc():
#    with open("pc.txt") as f:
#        floats = map(float, f)
#        return floats[0]

def get_pa(p):
    with open("pa.csv") as f:
        floats = map(float, f)
    return numpy.percentile(floats, p);    

#def get_pa():
#    with open("pa.txt") as f:
#        floats = map(float, f)
#        return floats[0]

def pc_pa_vs_readdelay(sla_tc, sla_ta, min_readdelay, max_readdelay):
    if (min_readdelay == 0): 
        db_cmd = "./all_experiments.sh"
        os.system(db_cmd)
        time.sleep(5)
        delay_cmd = "./insert_random_delay.sh dormi-test 10 5 10"
        os.system(delay_cmd)
        time.sleep(5)
        delay_cmd = "./insert_random_delay.sh dormi-test 10 7 3"
        os.system(delay_cmd)
    iteration = 0
    for rd in range(min_readdelay, max_readdelay + 1):
        run_ycsb_get_pc_pa(rd, sla_tc, sla_ta, "w" if iteration == 0 else "a", iteration)
        iteration += 1

# runs ycsb with 100 operations for given read delay, computes pc and pa based on the log, and returns pc, and pa values                                              
def run_ycsb_get_pc_pa(read_delay, sla_tc, sla_ta, write_append, load_test):

    ycsb_cmd = "./ycsb_simple.sh %s %s" % (str(read_delay), str(load_test))
    os.system(ycsb_cmd)
    cap_cmd = "./exp.sh %s %s %s" % (settings.clog_dir, sla_tc, sla_ta)
    os.system(cap_cmd)
    curr_pc =  get_pc(90)
    f = open("pc_hist.log", write_append)
    f.write(str(curr_pc)+"\n")
    f.close()
    curr_pa = get_pa(90)
    f = open("pa_hist.log", write_append)
    f.write(str(curr_pa)+"\n")
    f.close()
    f = open("rdelay_hist.log", write_append)
    f.write(str(read_delay)+"\n")
    f.close()
    # wait a wit to stabilize, will cut it later        
    return (curr_pc, curr_pa)

def update_step_size(old_step_size, pa_old, pa_new, pa_sla):
    # will add the update rule later
    return 1

def adaptive_control_v2(pa_sla, ta_sla, tc_sla):
    db_cmd = "./all_experiments.sh"
    os.system(db_cmd)
    read_delay = 0
    iteration = 0
    delay_cmd = "./insert_random_delay.sh dormi-test 8 5 8"
    os.system(delay_cmd)
    stepsize = 1
    while True:
        # for now manually change delay after every 100 timesteps
        if (iteration == 150):
            break
        if (iteration == 70):
            delay_cmd = "./insert_random_delay.sh dormi-test 8 6 4"
            os.system(delay_cmd)
        #if (iteration == 20):
        #    delay_cmd = "./insert_random_delay.sh dormi-test 10 7 2"
        #    os.system(delay_cmd)
        #if (iteration == 30):
        #    delay_cmd = "./insert_random_delay.sh dormi-test 10 7 4"
        #    os.system(delay_cmd)
        #if (iteration == 20):
        #    delay_cmd = "./insert_random_delay.sh dormi-test 10 7 6"
        #    os.system(delay_cmd)    

        pc, pa = run_ycsb_get_pc_pa(read_delay, tc_sla, ta_sla, "w" if iteration == 0 else "a", iteration)
        if (iteration > 0):
            stepsize = update_step_size(stepsize, old_pa, pa, pa_sla) 
        old_pc = pc
        old_pa = pa
        iteration += 1
        if (pa < pa_sla):
            print "increase read delay"
            read_delay = read_delay + stepsize
           
        else:
            print "decrease read delay"
            if (read_delay > 0):
                read_delay = read_delay - stepsize 
           
if __name__ == '__main__':
    adaptive_control_v2(0.1, 70, 0)
    #pc_pa_vs_readdelay(0, 60, 0, 65)
    #print get_pa(75)
    sys.exit(0)
