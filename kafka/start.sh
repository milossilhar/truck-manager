#!/bin/bash

# Stop executing after first command fails
set -e

# Location from which this script was executed
CMD_LOCATION=$(dirname $0)

################################################
# SETTINGS OF EXECUTION
# No settings
################################################

echo "INFO - Zookeeper"
${CMD_LOCATION}/zoo-start.sh alone

echo "INFO - Sleep 5"
sleep 5

echo "INFO - Kafka"
${CMD_LOCATION}/kafka-start.sh -s 3

echo "INFO - Sleep 5"
sleep 5

echo "INFO - Topics"
${CMD_LOCATION}/kafka-init-topics.sh localhost:2181 3 ibm ups usb telekom