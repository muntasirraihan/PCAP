from __future__ import print_function
import sys, os, time
from datetime import datetime

TC = 0
TA = 150

def measure_performance(delay):
    num_samples = 1
    pa, pc = 0, 0

    for i in xrange(num_samples):
        ycsb_preapre_cmd = "./prepare.sh %s" % delay
        ycsb_run_cmd = "./ycsb_distributed.sh %s %s" % (num_ycsb_clients, delay)
        ycsb_result_cmd = "./ycsb_extract_result.sh %s %s %s" % (TC, TA, delay)

        os.system(ycsb_preapre_cmd)
        os.system(ycsb_run_cmd)
        os.system(ycsb_result_cmd)

        time.sleep(1)
        with open("pc.csv") as f:
            pcs = [float(x) for x in f]
        with open ("pa.csv") as f:
            pas = [float(x) for x in f]
        if (len(pcs) < 10 or len(pas) < 10):
            return None, None
        #pc += numpy.percentile(pcs, 90)
        #pa += numpy.percentile(pas, 90)
        pa += sum(pas)/len(pas)
        pc += sum(pcs)/len(pcs)

        #print("pa percentile:", numpy.percentile(pas, 90), pas, file=sys.stderr)
        #print("pc percentile:", numpy.percentile(pcs, 90), pcs, file=sys.stderr)
        #print("pa average:", sum(pas)/len(pas), file=sys.stderr)
        #print("pc average:", sum(pcs)/len(pcs), file=sys.stderr)

        # print("pa percentile:", numpy.percentile(pas, 90))
        # print("pc percentile:", numpy.percentile(pcs, 90))
        # print("pa average:", sum(pas)/len(pas))
        # print("pc average:", sum(pcs)/len(pcs))

        #print(numpy.percentile(pcs, 90), pcs)
    pa = pa / num_samples
    pc = pc / num_samples
    return pa, pc

def run(sla, is_pa_controlled):
    print("sla is pa : %s" % is_pa_controlled)
    alpha = 2 #Growth factor
    step_size = 0.5
    max_step_size = 8
    delay = 0
    going_up = True
    print("delay: %s" % delay)
    start_time = datetime.now()

    iteration = 0
    while True:
        while True:
            try:
                pa, pc = measure_performance(delay)
                if not (isinstance(pa, float) and isinstance(pc, float)):
                    continue
                break
            except Exception as e:
                print(e)
                print("Error, rerun...")

        delta_time = datetime.now() - start_time
        time = delta_time.seconds + delta_time.microseconds/1E6
        print("iteration: ", iteration)
        print("%s: pa: %s, pc: %s - delay: %s" % (time, pa, pc, delay))
        print("%s: pa: %s, pc: %s - delay: %s" % (time, pa, pc, delay),file=sys.stderr)

        next_going_up = pa < sla if is_pa_controlled else pc > sla
        if next_going_up:
            step_size = step_size*alpha if going_up else 1
        else:
            step_size = -1 if going_up else step_size * alpha
        step_size = min(step_size, max_step_size)
        step_size = max(step_size, -max_step_size)

        going_up = next_going_up
        delay += int(step_size)
        delay = max(delay, 0)

        iteration += 1

def test():
    minDelay = 71
    maxDelay = 201
    step = 1
    #for delay in xrange(minDelay, maxDelay, step):
    for delay in [13, 33]:
        while True:
            try:
                pa, pc = measure_performance(delay)
                if not (isinstance(pa, float) and isinstance(pc, float)):
                    continue
                break
            except Exception as e:
                print(e)
                print("Error, rerun...")
        print("delay: %s, pa: %s, pc: %s" % (delay, pa, pc))
        print("delay: %s, pa: %s, pc: %s" % (delay, pa, pc), file=sys.stderr)

if __name__ == '__main__':
    print("START")
    num_ycsb_clients = int(sys.argv[1])
    sla = float(sys.argv[2])
    is_pa_controlled = sys.argv[3] == "pa"
    run(sla, is_pa_controlled)
    #test()
