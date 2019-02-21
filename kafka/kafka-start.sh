#!/bin/bash

# Stop executing after first command fails
set -e

function printUsage {
  echo "USAGE: $0 [-s|--single] number_of_servers instance_to_run [zookeeper_servers]"
  echo "-s                  run on single machine, ignores instance_to_run and runs all instances"
  echo "number_of_servers   number of servers to start"
  echo "instance_to_run     particular instance of server to start (zero-based)"
  echo "[zookeeper_servers] overrides zookeeper servers in kafka starting configuration file"
  exit 1
}

case $1 in
-s | --single)
  SINGLE="0"
  CONFIG_BASE="single"
  NUM_SERVERS="$2"
  INSTANCE="0"
  if [ "$#" -eq "3" ]; then
    ZOO_SERVERS="$3"
  else
    ZOO_SERVERS="$4"
  fi
  ;;
*)
  SINGLE="1"
  CONFIG_BASE="server"
  NUM_SERVERS="$1"
  INSTANCE="$2"
  ZOO_SERVERS="$3"
  ;;
esac

if [ "$#" -ne "2" -a "$#" -ne "3" -a "$#" -ne "4" ] || [ "$NUM_SERVERS" -le "$INSTANCE" ] || [ "$INSTANCE" -lt "0" ]; then
  printUsage
fi

# Location from which this script was executed
CMD_LOCATION=$(dirname $0)

################################################
# SETTINGS OF EXECUTION

# Location of kafka binaries
BINARY_LOCATION="${CMD_LOCATION}/bin"
# Where are configs located
CONFIGS_LOCATION="${CMD_LOCATION}/config/kafka-configs-${NUM_SERVERS}"
# Common name for all files of configs
CONFIGS_FILE=$(printf %s-%02d-%02d.properties $CONFIG_BASE $NUM_SERVERS $INSTANCE)

################################################

if [ ! -d ${CONFIGS_LOCATION} ]; then
  echo "Properties for ${NUM_SERVERS} server(s) does not exists."
  exit 1
fi

if [ $SINGLE -eq "0" ]; then
  for (( i=0; i<${NUM_SERVERS}; i++ )); do
    CONFIGS_FILE=$(printf %s-%02d-%02d.properties $CONFIG_BASE $NUM_SERVERS $i)
    if [ "x$ZOO_SERVERS" != "x" ]; then
      echo "CMD - ${BINARY_LOCATION}/kafka-server-start.sh -daemon ${CONFIGS_LOCATION}/${CONFIGS_FILE} --override zookeeper.connect=$ZOO_SERVERS && echo \"Kafka server $i started\""
      ${BINARY_LOCATION}/kafka-server-start.sh -daemon ${CONFIGS_LOCATION}/${CONFIGS_FILE} --override zookeeper.connect=$ZOO_SERVERS && echo "Kafka server $i started"
    else
      echo "CMD - ${BINARY_LOCATION}/kafka-server-start.sh -daemon ${CONFIGS_LOCATION}/${CONFIGS_FILE} && echo \"Kafka server $i started\""
      ${BINARY_LOCATION}/kafka-server-start.sh -daemon ${CONFIGS_LOCATION}/${CONFIGS_FILE} && echo "Kafka server $i started"
    fi
  done
else
  if [ "x$ZOO_SERVERS" != "x" ]; then
    echo "CMD - ${BINARY_LOCATION}/kafka-server-start.sh -daemon ${CONFIGS_LOCATION}/${CONFIGS_FILE}  --override zookeeper.connect=$ZOO_SERVERS && echo \"Kafka server $INSTANCE/$NUM_SERVERS started\""
    ${BINARY_LOCATION}/kafka-server-start.sh -daemon ${CONFIGS_LOCATION}/${CONFIGS_FILE}  --override zookeeper.connect=$ZOO_SERVERS && echo "Kafka server $INSTANCE/$NUM_SERVERS started"
  else
    echo "CMD - ${BINARY_LOCATION}/kafka-server-start.sh -daemon ${CONFIGS_LOCATION}/${CONFIGS_FILE} && echo \"Kafka server $INSTANCE/$NUM_SERVERS started\""
    ${BINARY_LOCATION}/kafka-server-start.sh -daemon ${CONFIGS_LOCATION}/${CONFIGS_FILE} && echo "Kafka server $INSTANCE/$NUM_SERVERS started"
  fi
fi
