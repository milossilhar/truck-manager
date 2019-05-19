#!/bin/bash

# Stop executing after first command fails
set -e

function printUsage {
  echo "USAGE: $0 [zookeeper_server] [1|3|5|9] [topic_names]"
  echo "zookeeper_server    zookeeper connection in the form host:port"
  echo "                    multiple hosts can be given to allow fail-over"
  echo "1|3|5|9             number of running instances of kafka"
  echo "topic_names         comma-separated values of company keys"
  exit 1
}

# Location from which this script was executed
CMD_LOCATION=$(dirname $0)

################################################
# SETTINGS OF EXECUTION

# Location of kafka binaries
BINARY_LOCATION="${CMD_LOCATION}/bin"

################################################

if [ "$#" -lt "3" ]; then
  printUsage
fi

case $2 in
1)
  factor="1"
  partitions="2"
  ;;
3)
  factor="3"
  partitions="6"
  ;;
5)
  factor="5"
  partitions="10"
  ;;
9)
  factor="9"
  partitions="18"
  ;;
*)
  printUsage
  exit 1
  ;;
esac

echo "INFO - Creating topic: auth-keys"
${BINARY_LOCATION}/kafka-topics.sh --create --zookeeper ${1} --topic "auth-keys" --partitions ${partitions} --replication-factor ${factor}

for arg in "${@:3}"; do
  echo "INFO - Creating topic: $arg"
  ${BINARY_LOCATION}/kafka-topics.sh --create --zookeeper ${1} --topic "company-${arg}" --partitions ${partitions} --replication-factor ${factor}
done
