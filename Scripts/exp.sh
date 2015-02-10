#!/bin/bash

LOG_FILE=$1
TC=$2
TA=$3

rm -rf *.csv

#for (( TC = 0; TC < 1; TC++ ))
#do
#    for (( TA = 0; TA < 1; TA++ ))
#    do
for Version in -1
   do
    for Overlap in 0
      do
	echo $TC $TA $Version $Overlap
	javac ./Main.java
        java -cp ./ Main $LOG_FILE $TC $TA 0 $Version $Overlap

	#javac ../ConsistencyAnalysis/Main.java
        #java -cp ../ConsistencyAnalysis Main $LOG_FILE $TC $TA 0 $Version $Overlap

  #javac Main.java
  #      java Main $LOG_FILE $TC $TA 0 $Version $Overlap
      done
   done
