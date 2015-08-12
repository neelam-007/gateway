#!/bin/bash
#
# Script to launch the GatewayManagementClient.jar
#

THIS_DIR=$(dirname $0)

source ${THIS_DIR}/jdk_utils.sh

ensure_JDK 1.6

#
# Move to home directory
#
cd ${THIS_DIR}

#
# Run client
#
"${JAVA_HOME}/bin/java" ${JAVA_OPTS} -jar GatewayManagementClient.jar "$@"
