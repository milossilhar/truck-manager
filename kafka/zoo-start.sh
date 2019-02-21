#!/bin/bash

# Stop executing after first command fails
set -e

function printUsage {
  echo "USAGE: $0 alone|single|multi [id]"
  echo "alone          runs standalone instance of zookeeper as daemon"
  echo "single         runs replicated zookeeper on single server as daemons"
  echo "mutli id:1-3   runs one of three instances of replicated zookeeper as daemon"
  exit 1
}

if [ "$#" -ne "1" -a "$#" -ne "2" ]; then
  printUsage
fi
if [ "$1" == "multi" -a "$#" -eq "1" ]; then
  printUsage
fi

# Location from which this script was executed
CMD_LOCATION=$(dirname $0)

################################################
# SETTINGS OF EXECUTION

# Zookeeper folder
DATA_DIR="/tmp/xsilhar-zookeeper"
# Location of kafka binaries
BINARY_LOCATION="${CMD_LOCATION}/bin"
# Where are configs located
CONFIGS_LOCATION="${CMD_LOCATION}/config/zoo"

################################################

case $1 in
"alone")
  echo "INFO - Zookeeper standalone server starting ..."
  ${BINARY_LOCATION}/zookeeper-server-start.sh -daemon ${CONFIGS_LOCATION}/zookeeper_alone.properties && echo "INFO - Zookeeper standalone server started"
  ;;
"single")
  mkdir -p $DATA_DIR/server1
  mkdir -p $DATA_DIR/server2
  mkdir -p $DATA_DIR/server3
  echo "1">$DATA_DIR/server1/myid
  echo "2">$DATA_DIR/server2/myid
  echo "3">$DATA_DIR/server3/myid
  echo "INFO - Zookeeper server 1 starting ..."
  ${BINARY_LOCATION}/zookeeper-server-start.sh -daemon ${CONFIGS_LOCATION}/zookeeper_replica_01.properties && echo "INFO - Zookeeper server 1 started"
  echo "INFO - Zookeeper server 2 starting ..."
  ${BINARY_LOCATION}/zookeeper-server-start.sh -daemon ${CONFIGS_LOCATION}/zookeeper_replica_02.properties && echo "INFO - Zookeeper server 2 started"
  echo "INFO - Zookeeper server 3 starting ..."
  ${BINARY_LOCATION}/zookeeper-server-start.sh -daemon ${CONFIGS_LOCATION}/zookeeper_replica_03.properties && echo "INFO - Zookeeper server 3 started"
  ;;
"multi")
  if [ "$2" -lt "1" -o "$2" -gt "3" ]; then
    printUsage
  fi
  mkdir -p $DATA_DIR  
  echo "$2">$DATA_DIR/myid
  echo "INFO - Zookeeper replicated server $2 starting ..."
  ${BINARY_LOCATION}/zookeeper-server-start.sh -daemon ${CONFIGS_LOCATION}/zookeeper_replica_multi_0${2}.properties && echo "INFO - Zookeeper replicated server $2 started"
  ;;
*)
  printUsage
  ;;
esac
