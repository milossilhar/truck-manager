#!/bin/bash

# Stop executing after first command fails
set -e

# Location from which this script was executed
CMD_LOCATION=$(dirname $0)

################################################
# SETTINGS OF EXECUTION
# No settings
################################################

${CMD_LOCATION}/kafka-stop.sh
${CMD_LOCATION}/zoo-stop.sh