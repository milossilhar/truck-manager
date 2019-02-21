#!/bin/bash

# Stop executing after first command fails
set -e

function printUsage {
  echo "USAGE: $0 [name]"
  echo "name   name of topic to consume from"
  exit 1
}

if [ "$#" -ne "1" ]; then
  printUsage
fi

# Location from which this script was executed
CMD_LOCATION=$(dirname $0)

################################################
# SETTINGS OF EXECUTION

# Location of kafka binaries
BINARY_LOCATION="${CMD_LOCATION}/bin"

################################################

echo "INFO - Starting console consumer..."
${BINARY_LOCATION}/kafka-console-consumer.sh --bootstrap-server "localhost:9092,localhost:9093,localhost:9094" --group "debug-console-consumer" --topic "${1}" --from-beginning
