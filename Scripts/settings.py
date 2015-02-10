import os

# clusters used for running cassandra, ycsb, and consistency computation

db_cluster = "cloud-test"
comp_cluster = "cluster-test"

# Objects used for determining host lists
all_hosts = range(0,0)
bad_hosts = []
hosts_prefix = "10.1.1."

# Directories
#home_dir = os.environ['HOME']
home_dir = "/proj/ISS"
base_dir = home_dir + "/conbench"
script_dir = base_dir + "/scripts"
output_dir = base_dir + "/scripts"
cassandra_source_dir = base_dir + "/apache-cassandra-1.2.4"
cassandra_target_dir = "/mnt/cassandra_" + os.getenv("USER")
#cassandra_data_dir = "/tmp/data" + os.getenv("USER")
cassandra_data_dir = "/mnt/data_" + os.getenv("USER")
#cassandra_data_dir = "/tmp/data"
ycsb_dir = base_dir + "/ycsb-0.1.4"
slf_dir = base_dir + "/slf4j-1.6.4"
cloning_dir = base_dir + "/cloning-1.8.1"
jmxterm_jar = base_dir + "/jmxterm-1.0-alpha-4-uber/jmxterm-1.0-alpha-4-uber.jar"
comp_dir = base_dir + "/consistency_analysis/ConsistencyAnalysis/ProbCAPComputation"

# path to ntpq command
ntpq_dir = home_dir + "/ntp/ntp-4.2.6p5/ntpq"

# directory for logging consistency data
clog_dir = output_dir + "/CLOG"

# directory for logging readdelay data
dlog_dir = output_dir + "/RDLOG"

# How many YCSB threads to run
YCSB_threads = 8
YCSB_threads_for_load = 8

# Number of records and number of seconds for YCSB load phase
num_seconds_to_load = 20  # zero for unlimited time

# Number of seconds for YCSB run phase
num_seconds_to_run = 60

# Target number of operations per second per host
target_thr_per_host = 10000  # zero for unlimited rate

# turn consistency instrumentation on
instrument = 1
instrument_PBS = 0
instrument_YCSBPP = 0

# storage settings
replication_factor = 3

# Experiment should stop after load phase
stop_after_load_phase = False

# Set this to True to skip copying storage system binaries
skip_db_copy = False

# cosistency settings
read_consistency = "ONE"
write_consistency = "ONE"

read_delay = "7"

# service level agreement settings

# (1-PA) % of operations should complete before TA ms time

TA = 80
PA = .1
PC = 0

# for now, (1-PC) % of read operations that start atleast TC time after a write should return the value of that write 

TC = 6
#PC will be computed and adapted by the control system

TP = 0
# TP will be computed later, actually it is the average network delay

alpha = 0 

read_delay_inc = 1 # for now hack, using same variables to control both read delay and read repair rate

run_again = True

network_delay = False

error_tolerance = .01

read_repair_chance = 0.1
