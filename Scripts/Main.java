import java.io.*;
import java.util.*;

class Operation {
    int type; // 0 read, 1 write
    long start_time;
    long end_time;
        long LP; // linearization point for the operation
        boolean stale; // true if a read operation is marked stale, not needed for write operations
        long lastWriteTime;  // time between last committed write and this read
        String val;
        boolean available; // true if the operation satisfies the availability model, false if it is unavailable
        boolean partition_tolerance; //true if the operation is both fresh and available for a read, 
                                     //and available for a write, ow false
        
        Operation() {
            type = 0;
            start_time = 0;
            end_time = 0;
            LP = 0;
            stale = false;
            lastWriteTime = 0;
            val = "";
            available = true;
            partition_tolerance = false;
        }
}

class Logger {
    FileWriter fstream;
    BufferedWriter out;
    
    Logger(String logFileName) {
        
        try {
            fstream = new FileWriter(logFileName);
            out = new BufferedWriter(fstream);
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
    
    public void write(String s) {
        try {
            out.write(s);
            out.newLine();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
    
    public void csvWrite(String s1, String s2) {
        try {
            out.write(s1);
            out.write(',');
            out.write(s2);
            out.write('\n');
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }
    
    public void close () {
        try {
            out.close();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}

public class Main {
    
    Map<String, ArrayList<Vector>> history;
    BufferedReader in;
    ArrayList as;
    HashMap<String, Long> keyOpCount;
    HashMap<String, Vector> keyReadOpList;
    HashMap<String, Vector> keyWriteOpList;
    long bigbang_time; // this is the start time of the op with lowest time, it will be the reference time in the system 
    long tc; // probabilistic consistency time parameter
    long ta; // probabilistic availability time parameter
    long tp; // probabilistic partition tolerance time parameter
    long version; // represents k-version staleness, k = 1 means for read staleness, only consider the last write, k=2
    // means consider last two writes, k = -1 means consider the entire history of preceding writes
    long overlap; // whether we want to consider overlapping writes for read staleness, 0 = no, 1 = yes
    HashMap<String, Double> pc; // save pc for each key
    HashMap<String, Double> pa; // save pa for each key
    Vector writeOperations, readOperations;
    long writeCount;
    Logger capLogger, pcLogger, paLogger, tcpcLogger, tapaLogger;
    Vector danglingReads;
    long dangling_reads, correct_reads;
    long alpha_proportion; // proportion of operations for alpha estimation, alpha = alpha_proportion / total op count
    long reverseCount;
    
    Main() {
        history = new HashMap<String, ArrayList<Vector>>();
        keyOpCount = new HashMap<String, Long>();
        keyReadOpList = new HashMap<String, Vector>();
        keyWriteOpList = new HashMap<String, Vector>();
        tc = 0;
        ta = 0;
        tp = 0;
        pa = new HashMap<String, Double>();
        pc = new HashMap<String, Double>();
        bigbang_time = Long.MAX_VALUE;
        writeOperations = new Vector();  // hack for now, i will make sure the total count of operations is atmost 1000
        // for now, lets just work with a single key, the workload just has one key for now
        readOperations = new Vector();
        writeCount = 0;
        danglingReads = new Vector();
        dangling_reads = correct_reads = 0;
        reverseCount = 0;
        //capLogger = new Logger("cap.csv");
    }
    
    // for each operation, check whether end-time - start-time <= ta, count the freq of operations that satisfy the latency bound for each key
    double compute_pa(long t) {
        long totalUnavailable = 0;
        long totalOp = 0;
        List list;
        Set set = history.entrySet();
        Iterator iter = set.iterator();
        while(iter.hasNext()) {
            Map.Entry me = (Map.Entry) iter.next();
            String key = me.getKey()+"";
            // ta computation for "key"
            long count = 0;
            Vector<Operation> readOperations = new Vector<Operation>();
            readOperations = this.keyReadOpList.get(key);
            if (readOperations.size() == 0) {
                System.out.println("ERROR: READ SET EMPTY!!!!");
                System.exit(1); 
            }
            for (Operation rop : readOperations) {
                long rdiff = rop.end_time - rop.start_time;
                //debugLogger.write("Op latency: " + rdiff);
                if (rdiff > t) {
                    count++;
                    rop.available = false;
                } 
                else {
                    rop.available = true;
                }
            }
            
            Vector<Operation> writeOperations = new Vector<Operation>(); 
            writeOperations = this.keyWriteOpList.get(key);
            for (Operation wop : writeOperations) {
                long wdiff = wop.end_time - wop.start_time;
                //debugLogger.write("Op latency: " + wdiff);
                if (wdiff > t) {
                    count++;
                    wop.available = false;
                }
                else {
                    wop.available = true;
                }
            }
            
            totalUnavailable += count;
            totalOp += (readOperations.size() +writeOperations.size());
            
            long totalOpCount = (long)keyOpCount.get(key);
            double proportion = (double)(count)/(double)(totalOpCount);
            pa.put(key, (Double)proportion);
            
            //System.out.println("key: " + key + " pa: " + proportion + " unavailable ops: " + count + " total ops: " + totalOpCount);
        }
        double unavailability = totalUnavailable / (totalOp + 0.0);
        return unavailability;
        //this.tapaLogger.csvWrite(t+"", unavailability+"");
    }
    
boolean checkStaleReadsWithTimeParamV4(Vector<Operation>ws, int index, Operation rop, long t) {
    Operation wo = ws.get(index);
    if (wo.start_time >= rop.start_time - t) {
        rop.stale = false;
    }
    else {
        // wo.start_time < rop.start_time - tc
        // if there is a write won such that wo.start_time < won.start_time <= rop.start_time - tc
        if (index < ws.size() - 1) {
            Operation won = ws.get(index + 1);
            rop.stale = won.start_time <= rop.start_time -t;
        }
        else rop.stale = false;
    }
    
    return rop.stale;
}

    // Staleness rules:
    /*Ok, so the rules we converged on: 

        ** For the failure-free case:
        For a read rd, let wr be the matching write. This is guaranteed to exist in a well-defined history, 
        and it is guaranteed to be unique (e.g., in YCSB, or elsewhere by using the write timestamp along with the read value)

        If wr.start >= rd.start - tc ---> rd is fresh
        If wr.start < rd.start - tc 
            if there exists a write wr' such that wr.st < wr'st <= rd. st - tc ----> rd is stale
            else there does not exist any such wr' ---> rd is fresh

        ** For the failure case:

        Define a "valid write" as one that has a finish time, i.e., returns an ack to the client.

        For a read rd, let wr be the matching write. 
        If wr is invalid ---> rd is stale
        else wr is valid ---> Use rules for failure-free case.


        ** The reasons we consider start time instead of finish time as in the Bailis paper:

        - We can now overlap reads and writes.
        - We can now handle failures.*/
    // for now we will assume failure free executions.
    boolean checkStaleReadsWithTimeParamV3(Vector w, Operation rop, long t) {
        // first sort the set of writes according to increasting start time
                Collections.sort(w, new Comparator() {
                    public int compare( Object o1 , Object o2 )  
                    {  
                        Operation op1 = (Operation)o1 ;  
                        Operation op2 = (Operation)o2 ;  
                        Long first = (Long)op1.start_time;  
                        Long second = (Long)op2.start_time;  
                        return first.compareTo(second);  
                    }
                });
                Iterator i = w.iterator();
                Operation wo = new Operation();
                Operation won = new Operation();
                while(i.hasNext()) {
                    wo = (Operation)i.next();
                    // first find the first write in sorted order that wrote the read value
                    if (rop.val.equals(wo.val)) {
                        break;
                    }   
                }
                
                if (wo.start_time >= rop.start_time - t) {
                    rop.stale = false;
                    return false;
                }
                else {
                    // wo.start_time < rop.start_time - tc
                    // if there is a write won such that wo.start_time < won.start_time <= rop.start_time - tc 
                    while (i.hasNext()) {
                        won = (Operation)i.next();
                        // since the write set is sorted by increasing start time, won.start_time > wo.start_time
                        if (won.start_time <= rop.start_time -t) {
                            rop.stale = true;
                            return true;
                        }
                    }
                    // else there are no such intervening writes
                    rop.stale = false;
                    return false;
                }
    }
    
    // latest staleness checker
    // assume rop has a valid value
    boolean checkStaleReadsWithTimeParamV2(Vector w, Operation rop) {
        
        // first sort the set of writes according to increasting start time
        Collections.sort(w, new Comparator() {
            public int compare( Object o1 , Object o2 )  
            {  
                Operation op1 = (Operation)o1 ;  
                Operation op2 = (Operation)o2 ;  
                Long first = (Long)op1.start_time;  
                Long second = (Long)op2.start_time;  
                return first.compareTo(second);  
            }
        });
        Iterator i = w.iterator();
        Operation wo = new Operation();
        Operation won = new Operation();
        while(i.hasNext()) {
            wo = (Operation)i.next();
            // first find the first write in sorted order that wrote the read value
            if (rop.val.equals(wo.val)) {
                break;
            }   
        }
        if (wo.start_time > rop.start_time && wo.start_time - rop.start_time > 5) {
            reverseCount++;
        }
        if (rop.start_time <= wo.start_time + tc) {
            rop.stale = false;
            return false;
        }
        else {
            if (i.hasNext()) {
                won = (Operation)i.next();
                if (rop.start_time <= won.start_time + tc) {
                    rop.stale = false;
                    return false;
                }
                else {
                    rop.stale = true;
                    return true;
                }
            }
            else {
                // no other write after wo, so reads are fresh
                rop.stale = false;
                return false;
            }
        }
    }
    
    // input: set of writes to consider, read start, end time and value
    // returns true if the read is stale, false otherwise
    boolean checkStaleReadsWithTimeParam(Vector w, Operation rop) {
        Iterator i = w.iterator();
        while(i.hasNext()) {
            Operation wo = (Operation)i.next();
            // now here comes the staleness check
            // only consider reads that start atleast tc time after a write starts 
            if (rop.start_time <= wo.start_time + tc) {
                if (rop.val.equals(wo.val)) { // the read returns the written value, so it satisfies tc-consistency
                    rop.stale = false;
                    return false; // the read is ok according to the consistency model
                }
            }
        }
        // when the code comes here, it means the read did not return the value of any writes starting 
        // atmost tc time before the read the
        rop.stale = true;
        return true; // the read is stale according to the consistency model
    }
    
    
    double compute_pc(long t) {
        Set set = history.entrySet();
        Iterator iter = set.iterator();
        long iterCount = 0;
    long totalStale = 0;
    long totalRead = 0;
    double totalPerKey = 0;
    int ignoredReads = 0;
        while(iter.hasNext()) {
	    ignoredReads = 0;
            iterCount++;
            //System.out.println("pc iteration <should be <= 10>: " + iterCount);
            Map.Entry me = (Map.Entry) iter.next();
            String key = me.getKey()+"";
            long count = 0; // count the number of read operations that are stale
            
            Vector<Operation> readOperations = new Vector<Operation>();
            Vector<Operation> writeOperations = new Vector<Operation>();
            
            readOperations = this.keyReadOpList.get(key);
            writeOperations = this.keyWriteOpList.get(key);
            if (writeOperations.size() == 0) {
                System.out.println("ERROR: EMPTY WRITE SET\n");
                System.exit(0);
            }
            
        // first sort the set of writes according to increasing start time
            Collections.sort(writeOperations, new Comparator() {
                public int compare( Object o1 , Object o2 )  
                {  
                    Operation op1 = (Operation)o1 ;  
                    Operation op2 = (Operation)o2 ;  
                    Long first = (Long)op1.start_time;  
                    Long second = (Long)op2.start_time;  
                    return first.compareTo(second);  
                }
            });
            
            HashMap<String, Integer> hash = new HashMap<String, Integer>();
            for (int i = 0; i < writeOperations.size(); i++) {
                Operation w = writeOperations.get(i);
                hash.put(w.val, i);
            }

            
            
            for (Operation rop : readOperations) {
                if (!hash.containsKey(rop.val)) {
                    ignoredReads++;
                    continue;
                }
                int i = hash.get(rop.val);
                if (this.checkStaleReadsWithTimeParamV4(writeOperations, i, rop, t)) {
                    count++; //increase the count of stale reads based on our criteria
                }
            }
            
            if (readOperations.isEmpty()) {
                System.out.println("key: " + key + " pc: NO READS");
                pc.put(key, 0.0);   
            }
            else {
                double proportion = count/(readOperations.size() - ignoredReads + 0.0);
                totalPerKey += proportion;
                pc.put(key, (Double)proportion);
                //System.out.println("key: " + key + " pc: " + proportion);
                //System.out.println("Total read operation count: " + readOperations.size());
            }

            totalStale += count;
            totalRead += readOperations.size();
        }
        double staleness = totalStale / (totalRead - ignoredReads + 0.0);   
    System.out.println("first_method pc: " + staleness); 
    return staleness;
    }
    
    void computeKeyLoad() {
        
        List list;
        Set set = history.entrySet();
        
        Iterator iter = set.iterator();
        while(iter.hasNext()) {
            Map.Entry me = (Map.Entry) iter.next();
            String key = me.getKey()+"";
        //System.out.println("key: " + key);
            list = (List)history.get(key);
            List sublist;
            Vector<Operation> rOps = new Vector();
            Vector<Operation> wOps = new Vector();
            for (int j = 0; j < list.size(); j++) {
                sublist = (List)list.get(j); // this sublist has the start, end time, and value for this operation
                // the first element is the value, which doesn't matter for availability
                String value = (String)sublist.get(0);
                //System.out.println("Value: " + value);
                long start_time = Long.parseLong((String)sublist.get(1));
                long end_time = Long.parseLong((String)sublist.get(2));
                String type = (String)sublist.get(3);
                // piggy-back some code here to find the bigbang time, this also means compute_pa() has to be called before compute_pc()
                if (type.equals("0")) {
                    Operation op = new Operation();
                    op.start_time = start_time;
                    op.end_time = end_time;
                    op.val = value;
                    rOps.add(op);
                }
                if (type.equals("2") || type.equals("3")) {
                    Operation op = new Operation();
                    op.start_time = start_time;
                    op.end_time = end_time;
                    op.val = value;
                    wOps.add(op);
                }   
            }
            long size = list.size();
            keyOpCount.put(key, size);
            keyReadOpList.put(key, rOps);
            keyWriteOpList.put(key, wOps);
        }
        
        as = new ArrayList( keyOpCount.entrySet() );  
          
        Collections.sort( as , new Comparator() {  
            public int compare( Object o1 , Object o2 )  
            {  
                Map.Entry e1 = (Map.Entry)o1 ;  
                Map.Entry e2 = (Map.Entry)o2 ;  
                Long first = (Long)e1.getValue();  
                Long second = (Long)e2.getValue();  
                return second.compareTo( first );  
            }  
        });  
        //System.out.println(as.);  
        
        
    }
    
    
    long extractHistoryFromLogs(String path) {
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        
        String files;
        String key, value, start, finish, opType;
        //long recordCount = 0;
        
        long distinct_count = 0;
    long duplicate_count = 0;
        
        try {
            for (int i = 0; i < listOfFiles.length; i++) {
                files = listOfFiles[i].getName();
                if (files.endsWith(".log")) {
                    //System.out.println("filename: " + path + File.separator + files);
                    in = new BufferedReader(new FileReader(path +  File.separator +  files));
                    while ((key = in.readLine()) != null) {
                //System.out.println("key: " + key);
                //if (!key.startsWith("user")) {
                //System.out.println("key format error: log misaligned ...");
                //System.exit(2);
                //}
                        //recordCount++;
                        Vector v1 = new Vector();
                        value = in.readLine();
                //System.out.println("Value: " + value);
                        v1.add(value);
                        start = in.readLine();
			//System.out.println("start time: " + start);
                if (start == null || !Character.isDigit(start.charAt(0)) || !Character.isDigit(start.charAt(1))) {
                    System.out.println("start time format error: log misaligned ...");
                    finish = in.readLine();
                    opType = in.readLine();
                    continue;
                    //System.exit(2);
                }
                        v1.add(start);
                        finish = in.readLine();
                //System.out.println("finish time: " + finish);
                if (finish == null || !Character.isDigit(start.charAt(0)) || !Character.isDigit(start.charAt(1))) {
                    System.out.println("finish time format error: log misaligned ...");
                    opType = in.readLine();
                    continue;
                                    //System.exit(2);
                                }
                        v1.add(finish);
                        opType = in.readLine();
                if (opType.length() != 1) {
                    System.out.println("op type format error: log misaligned ...");
                    continue;
                    //System.exit(2);
                }
                        v1.add(opType);
                        if (history.containsKey(key)) {
                            history.get(key).add(v1);
                    duplicate_count++;
                        }
                        else {
                            ArrayList<Vector> av = new ArrayList<Vector>();
                            av.add(v1);
                            history.put(key, av);
                            distinct_count++;
                        }
                    }
                    in.close();
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return duplicate_count;
    }
    
    public static void main(String[] args) {
        
        if(args.length < 4) {
            
            System.out.println("Usage: java -cp . -Xms1024m Main <log file path> <tc> <ta> <tp> <version> <overlapping_writes [true|false]>" );
        }
        
        Main dc = new Main();
        
        long count = dc.extractHistoryFromLogs(args[0]);
        
        //System.out.println("Total Key Count: " + count);
        
        dc.computeKeyLoad();
        
        //System.out.println("Total Key Count: " + dc.keyOpCount.size());
        
        dc.tc = Long.parseLong(args[1]); // compute pc for t = 0 to tc
        
        dc.ta = Long.parseLong(args[2]); // compute pa for t = 0 to ta
        
        dc.tp = Long.parseLong(args[3]);
        
        //dc.version = Long.parseLong(args[4]); // how many previous versions of writes to consider for read staleness
        
        //dc.overlap = Long.parseLong(args[5]);
        
        //System.out.println("Operation count: " + count);
        
        //dc.capLogger = new Logger(args[0] +  File.separator + "tc" + args[1] + "ta" + args[2] + "tp" + args[3] + "Version" + args[4] + "Overlap" + args[5] + ".csv");
        dc.capLogger = new Logger("cap.csv");
        dc.pcLogger = new Logger("pc.csv");
        dc.paLogger = new Logger("pa.csv");
        
        dc.tcpcLogger = new Logger("tcpc.csv");
        dc.tapaLogger = new Logger("tapa.csv");
        
        //dc.debugLogger = new Logger("debug.log");
        
        //for (long t = 0; t <= dc.ta; t++) {
            double unavailability = dc.compute_pa(dc.ta);
            //dc.tapaLogger.csvWrite(t+"", unavailability+"");
	    //}
        
	    //for (long t = 0; t <= dc.tc; t++) {
            double staleness = dc.compute_pc(dc.tc);
            //dc.tcpcLogger.csvWrite(t+"", staleness+"");
	    //}
        
        
        Iterator i;
        
        i = dc.as.iterator();
        long key_count = 0;
        while ( i.hasNext() ) {
            key_count++;
            String s = i.next().toString();
            //System.out.println(s);
            StringTokenizer st = new StringTokenizer(s, "=");
            String key = st.nextElement().toString();
            
            String value = st.nextElement().toString();
            //System.out.println("key: " + key + " pa: " + dc.pa.get(key) + " pc: " + dc.pc.get(key));
            //dc.capLogger.write(dc.pa.get(key) + " " + dc.pc.get(key));
            dc.capLogger.csvWrite(dc.pa.get(key)+"", dc.pc.get(key)+"");
            dc.pcLogger.write(dc.pc.get(key)+"");
            dc.paLogger.write(dc.pa.get(key)+"");
        }
        //System.out.println("Total Key Count: " + key_count++);
        //System.out.println("CASE w.start > r.start COUNT: " + dc.reverseCount);
        //System.out.println("output file name: " + args[0]+ "tc" + args[1] + "ta" + args[2] + "tp" + args[3] + "Version" + args[4] + "Overlap" + args[5] + ".csv");
        dc.capLogger.close();
        //dc.debugLogger.close();
        dc.pcLogger.close();  
        dc.paLogger.close();
        dc.tcpcLogger.close();
        dc.tapaLogger.close();
    }
}