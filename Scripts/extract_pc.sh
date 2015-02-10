#!/bin/bash 

awk -F "," '{print $2}' tcpc.csv > pc.csv

tail -1 pc.csv > pc.txt

awk -F "," '{print $2}' tapa.csv > pa.csv

tail -1 pa.csv > pa.txt
