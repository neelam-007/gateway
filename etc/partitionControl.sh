#!/bin/bash

##################################################################################
#Control script to start or stop an individual partition.
##################################################################################

. /etc/profile.d/ssgruntimedefs.sh
USER=`whoami`
SSGUSER="gateway"

usage() {
	echo "Usage: partitionControl.sh [start|run|stop|usage] partition-name"
	echo "  start - start the given partition and detach from the console"
	echo "  run - start the given partition but do not detach from the console"
	echo "  stop - stop the given partition"
	echo "  usage - show this message"
	echo "  partition-name - the name of the partition to control"
}

COMMAND=${1}
PARTITION_NAME=${2}

if [ -z "${COMMAND}" ] ; then
	echo "Please specify a command."
	usage
	exit 1;
fi

if [ -z "${PARTITION_NAME}" ] ; then
	echo "No partition name specified."
	usage
	exit 1;
fi

if [ ${COMMAND} == "usage" ] ; then
	usage;
	exit 0;
fi

if [ ! -d "${SSG_HOME}" ] ; then
    if [ -z "${SSG_HOME}" ] ; then
        echo "SSG_HOME is not set! Please set SSG_HOME to the installation root of the SecureSpan Gateway (ex. /ssg)"
        exit 1;
    fi
    echo "Invalid SSG_HOME! Please set SSG_HOME to the installation root of the SecureSpan Gateway (ex. /ssg)"
    exit 1;
fi

PARTITION_DIR=${SSG_HOME}/etc/conf/partitions/${PARTITION_NAME}
CONFIG_FILE="${PARTITION_DIR}/server.xml"

. ${SSG_HOME}/bin/partition_defs.sh true "${PARTITION_COUNT}"
export JAVA_OPTS="${ORIGINAL_JAVA_OPTS} ${partition_opts}"
export CATALINA_OPTS=-Dcom.l7tech.server.partitionName=${PARTITION_NAME}

if [ "$USER" = "root" ]; then
    (su $SSGUSER -c "${SSG_HOME}/tomcat/bin/catalina.sh ${COMMAND} -config ${CONFIG_FILE}")
else
    (${SSG_HOME}/tomcat/bin/catalina.sh ${COMMAND} -config ${CONFIG_FILE})
fi

