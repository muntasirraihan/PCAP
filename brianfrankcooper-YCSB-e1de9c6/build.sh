#!/bin/sh
#../apache-maven-3.0.5/bin/mvn clean 
../apache-maven-3.0.5/bin/mvn package

cp cassandra/target/cassandra-binding-0.1.4.jar ../ycsb-0.1.4/cassandra-binding/lib/
cp core/target/core-0.1.4.jar ../ycsb-0.1.4/core/lib/
