#!/bin/bash

##################################################################################
#Control script to start or stop an individual partition. 
#Run from the partitions root directory (SSG_ROOT/etc/conf/partitions/<partition>)
##################################################################################
usage() {
	echo "Usage: partitionControl.sh [start|run|stop|usage]"
	echo "  start - start the given partition and detach from the console"
	echo "  run - start the given partition but do not detach from the console"
	echo "  stop - stop the given partition"
	echo "  usage - show this message"
}

COMMAND=${1}
if [ -z "${COMMAND}" ] ; then
	echo "Please specify a command."
	usage
	exit 1;
fi

if [ ${COMMAND} == "usage" ] ; then
	usage;
	exit 0;
fi

cd `dirname $0`
pushd ../../../../ > /dev/null
SSG_ROOT=`pwd`
popd > /dev/null

PARTITION_ROOT_DIR=${PWD}
CONFIG_FILE="${PARTITION_ROOT_DIR}/server.xml"
PARTITION_NAME=`basename ${PWD}`
export CATALINA_OPTS=-Dcom.l7tech.server.partitionName=${PARTITION_NAME}
(${SSG_ROOT}/tomcat/bin/catalina.sh ${COMMAND} -config ${CONFIG_FILE})
