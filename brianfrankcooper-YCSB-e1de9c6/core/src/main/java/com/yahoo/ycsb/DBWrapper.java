/**                                                                                                                                                                                
 * Copyright (c) 2010 Yahoo! Inc. All rights reserved.                                                                                                                             
 *                                                                                                                                                                                 
 * Licensed under the Apache License, Version 2.0 (the "License"); you                                                                                                             
 * may not use this file except in compliance with the License. You                                                                                                                
 * may obtain a copy of the License at                                                                                                                                             
 *                                                                                                                                                                                 
 * http://www.apache.org/licenses/LICENSE-2.0                                                                                                                                      
 *                                                                                                                                                                                 
 * Unless required by applicable law or agreed to in writing, software                                                                                                             
 * distributed under the License is distributed on an "AS IS" BASIS,                                                                                                               
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or                                                                                                                 
 * implied. See the License for the specific language governing                                                                                                                    
 * permissions and limitations under the License. See accompanying                                                                                                                 
 * LICENSE file.                                                                                                                                                                   
 */

package com.yahoo.ycsb;

import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.io.*;  // added by Muntasir
import java.util.*;

import com.yahoo.ycsb.measurements.Measurements;
import java.nio.ByteBuffer;

/**
 * Wrapper around a "real" DB that measures latencies and counts return codes.
 */
public class DBWrapper extends DB
{
	DB _db;
	Measurements _measurements;
        
        static FileWriter fstream;
        static BufferedWriter out;
        static Object obj;
        static int numThreads;
        
        static {
            obj = new Object();
	    numThreads = 0;
        }
        
        
        
        
	public DBWrapper(DB db)
	{
		_db=db;
		_measurements=Measurements.getMeasurements();
	}

	/**
	 * Set the properties for this DB.
	 */
	public void setProperties(Properties p)
	{
		_db.setProperties(p);
	}

	/**
	 * Get the set of properties for this DB.
	 */
	public Properties getProperties()
	{
		return _db.getProperties();
	}

	/**
	 * Initialize any state for this DB.
	 * Called once per DB instance; there is one DB instance per client thread.
	 */
	public void init() throws DBException
	{
		_db.init();
                
                synchronized(obj)  {
		    numThreads++;
                    //obj = new Object();
                    if(out == null) {
                        try { // added by Muntasir
                            String logfile;
                            logfile = _db.getProperties().getProperty("clog");
                            System.out.println("logfile: " + logfile);
                            if (logfile != null) {
                                fstream = new FileWriter(logfile, true);
                                out = new BufferedWriter(fstream);
                            }
                            
                        } catch(IOException ioe) {}   
                    }         
                }
                   
	}

	/**
	 * Cleanup any state for this DB.
	 * Called once per DB instance; there is one DB instance per client thread.
	 */
	public void cleanup() throws DBException
	{
	    _db.cleanup();
	    synchronized(obj) {
		numThreads--;
		if (numThreads == 0) {
		    try {
			out.close();
			fstream.close();
			out = null;
			fstream = null;
		    } catch(IOException ioe) {}
		}
	    }
	}

	/**
	 * Read a record from the database. Each field/value pair from the result will be stored in a HashMap.
	 *
	 * @param table The name of the table
	 * @param key The record key of the record to read.
	 * @param fields The list of fields to read, or null for all of them
	 * @param result A HashMap of field/value pairs for the result
	 * @return Zero on success, a non-zero error code on error
	 */
	public int read(String table, String key, Set<String> fields, HashMap<String,ByteIterator> result)
	{
                HashMap<String,ByteIterator> result2;
                com.rits.cloning.Cloner cloner = new com.rits.cloning.Cloner();
                
                long st1, en1;
                st1 = en1 = 0;
                if (out != null) {
                
                    st1 = System.currentTimeMillis();
                }
		long st=System.nanoTime();
                //long st1 = System.currentTimeMillis();
		int res=_db.read(table,key,fields,result);  // hack by Muntasir
                
		long en=System.nanoTime();
                
                if (out != null) {
                    en1 = System.currentTimeMillis();
                }
                //long en1 = System.currentTimeMillis();
                
                result2 = cloner.deepClone(result);
                
                String svalue [] = new String [20];
                String skey [] = new String [20];
                int vidx = 0;
                int kidx = 0;
                boolean empty = false;
                
                if (out != null && res == 0) {
                    
                    try {
                    synchronized(obj) {
                        Iterator iter = result2.keySet().iterator();
                        if (result2.keySet().isEmpty()) {
                            empty = true;
                        }
                        while (iter.hasNext()) {
                            String key1 = (String) iter.next();
                
                            skey[kidx] = key1;
                            kidx++;
                            ByteIterator b2 = result2.get(key1);
                            String val5 = new String(b2.toArray());
                            svalue[vidx] = val5;
                            
                            vidx++;
                            
                        }
                            
                            
                            out.write(key);
                            out.newLine();
                            
                            
                            if (empty) {
                                out.write("empty");
                                out.newLine();
                            }
                            else {
                                for(int i = 0; i < vidx; i++) {
                                    out.write(svalue[i]);
                                    out.newLine();

                                }
                            }
                            out.write(String.valueOf(st1));
                            out.newLine();
                            out.write(String.valueOf(en1));
                            out.newLine();
                            out.write("0");  // type 0 is READ
                            out.newLine();
                        }
                            
                    } catch(IOException ioe) {}
                    
                }
                
                
                
		_measurements.measure("READ",(int)((en-st)/1000));
		_measurements.reportReturnCode("READ",res);
		return res;
	}

	/**
	 * Perform a range scan for a set of records in the database. Each field/value pair from the result will be stored in a HashMap.
	 *
	 * @param table The name of the table
	 * @param startkey The record key of the first record to read.
	 * @param recordcount The number of records to read
	 * @param fields The list of fields to read, or null for all of them
	 * @param result A Vector of HashMaps, where each HashMap is a set field/value pairs for one record
	 * @return Zero on success, a non-zero error code on error
	 */
	public int scan(String table, String startkey, int recordcount, Set<String> fields, Vector<HashMap<String,ByteIterator>> result)
	{
		long st=System.nanoTime();
                long st1 = System.currentTimeMillis();
		int res=_db.scan(table,startkey,recordcount,fields,result);
		long en=System.nanoTime();
                long en1 = System.currentTimeMillis();
                
                try { 
                    synchronized(obj) {
                        out.write(String.valueOf(st1));
                        out.write(";");
                        out.write(String.valueOf(en1));
                        out.write(";");
                        out.write("1");  // type 1 is SCAN
                        out.newLine();
                    }    
                    
                } catch(IOException ioe) {}
                
		_measurements.measure("SCAN",(int)((en-st)/1000));
		_measurements.reportReturnCode("SCAN",res);
		return res;
	}
	
	/**
	 * Update a record in the database. Any field/value pairs in the specified values HashMap will be written into the record with the specified
	 * record key, overwriting any existing values with the same field name.
	 *
	 * @param table The name of the table
	 * @param key The record key of the record to write.
	 * @param values A HashMap of field/value pairs to update in the record
	 * @return Zero on success, a non-zero error code on error
	 */
	public int update(String table, String key, HashMap<String,ByteIterator> values)
	{   
                long st1, en1;
                st1 = en1 = 0;
                String instr = _db.getProperties().getProperty("i");
                
                com.rits.cloning.Cloner cloner = new com.rits.cloning.Cloner();
            
                HashMap<String, String> values2 = new HashMap<String, String>();
                values2 = StringByteIterator.getStringMap(values);
                
                HashMap<String,ByteIterator> values3 = new HashMap<String,ByteIterator> ();
                        
                values3 = StringByteIterator.getByteIteratorMap(values2);
                
                HashMap<String,ByteIterator> values4;
                
                    String svalue [] = new String [20];
                    String skey [] = new String [20];
                    int vidx = 0;
                    int kidx = 0;
                    
                    if (out != null) {
                    
                        values4 = cloner.deepClone(values3);
                        //try {
                        synchronized(obj) {
                            Iterator iter = values4.keySet().iterator();
                            
                            while (iter.hasNext()) {
                                String key1 = (String) iter.next();
                                //out.write("key: " + key1);
                                //out.newLine();
                                skey[kidx] = key1;
                                kidx++;
                                
                                ByteIterator b2 = values4.get(key1); 
                                String val5 = new String (b2.toArray());
                                svalue[vidx] = val5;
                                vidx++;
                            }
                        }
                    }
                    
                long st=System.nanoTime();
                
                if (out != null) {
                    st1 = System.currentTimeMillis();
                }
                
                //long st1 = System.currentTimeMillis();
		int res=_db.update(table,key,values3);
		long en=System.nanoTime();
                
                if (out != null) {
                    en1 = System.currentTimeMillis();
                }
                
                if (out != null && res == 0) {
                    
                    try { // added by Muntasir
                    
                        synchronized(obj) {

                            out.write(key);
                    
                            out.newLine();
                    
                            for(int i = 0; i < vidx; i++) {
                                out.write(svalue[i]);
                                out.newLine();

                            }
                            out.write(String.valueOf(st1));
                            
                            out.newLine();
                            out.write(String.valueOf(en1));
                            
                            out.newLine();
                            out.write("2");  // type 2 is UPDATE
                            out.newLine();
                        }
                        
                    } catch(IOException ioe) {}
                }
                
		_measurements.measure("UPDATE",(int)((en-st)/1000));
		_measurements.reportReturnCode("UPDATE",res);
		return res;
	}

	/**
	 * Insert a record in the database. Any field/value pairs in the specified values HashMap will be written into the record with the specified
	 * record key.
	 *
	 * @param table The name of the table
	 * @param key The record key of the record to insert.
	 * @param values A HashMap of field/value pairs to insert in the record
	 * @return Zero on success, a non-zero error code on error
	 */
	public int insert(String table, String key, HashMap<String,ByteIterator> values)
	{
                    long st1, en1;
                    st1 = en1 = 0;
                    String instr = _db.getProperties().getProperty("i");
                    
                    com.rits.cloning.Cloner cloner = new com.rits.cloning.Cloner();
            
                    HashMap<String, String> values2 = new HashMap<String, String>();
                    values2 = StringByteIterator.getStringMap(values);
                
                    HashMap<String,ByteIterator> values3 = new HashMap<String,ByteIterator> ();
                        
                    values3 = StringByteIterator.getByteIteratorMap(values2);
                
                    HashMap<String,ByteIterator> values4;
                
                    String svalue [] = new String [20];
                    String skey [] = new String [20];
                    int vidx = 0;
                    int kidx = 0;
                    
                    if (out != null) {
                    
                        values4 = cloner.deepClone(values3);
                    
                        synchronized(obj) {
                            Iterator iter = values4.keySet().iterator();
                            while (iter.hasNext()) {
                                String key1 = (String) iter.next();
                    
                                skey[kidx] = key1;
                                kidx++;
                                ByteIterator b2 = values4.get(key1); 
                                String val5 = new String (b2.toArray());
                                
                                svalue[vidx] = val5;
                                vidx++;
                                
                            }
                        }
                    }
                    
		long st=System.nanoTime();
                
                if (out != null) {
                    st1 = System.currentTimeMillis();
                }
                
                //st1 = System.currentTimeMillis();
		int res=_db.insert(table,key,values3);
		long en=System.nanoTime();
                
                if (out != null) {
                    en1 = System.currentTimeMillis();
                }
                //en1 = System.currentTimeMillis();
                
                
                if (out != null && res == 0) {
                    
                    try { // added by Muntasir
                    
                        synchronized(obj) {

                            out.write(key);
                            
                            out.newLine();
                    
                            for(int i = 0; i < vidx; i++) {
                            
                                out.write(svalue[i]);
                                out.newLine();

                            }
                            out.write(String.valueOf(st1));
                            
                            out.newLine();
                            out.write(String.valueOf(en1));
                            
                            out.newLine();
                            out.write("3");  // type 2 is UPDATE or INSERT
                            out.newLine();
                        }
                        
                    } catch(IOException ioe) {}
                }
                
		_measurements.measure("INSERT",(int)((en-st)/1000));
		_measurements.reportReturnCode("INSERT",res);
		return res;
	}

	/**
	 * Delete a record from the database. 
	 *
	 * @param table The name of the table
	 * @param key The record key of the record to delete.
	 * @return Zero on success, a non-zero error code on error
	 */
	public int delete(String table, String key)
	{
		long st=System.nanoTime();
                long st1 = System.currentTimeMillis();
		int res=_db.delete(table,key);
		long en=System.nanoTime();
                long en1 = System.currentTimeMillis();
                
                try { 
                    synchronized(obj) {
                        out.write(key);
                      
                        out.write(";");
                        out.write(String.valueOf(st1));
                        out.write(";");
                        out.write(String.valueOf(en1));
                        out.write(";");
                        out.write("4");  // type 4 is DELETE
                        out.newLine();
                      
                    }
                } catch(IOException ioe) {}
                
		_measurements.measure("DELETE",(int)((en-st)/1000));
		_measurements.reportReturnCode("DELETE",res);
		return res;
	}
}
