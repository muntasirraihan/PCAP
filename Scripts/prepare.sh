#!/bin/bash
RDELAY=$1
rm -f $RIAK/log0/log$RDELAY/*
rm -f $RIAK/riak/log/*
rm -rf out
mkdir out


