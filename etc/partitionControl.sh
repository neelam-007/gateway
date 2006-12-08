#!/bin/bash

##################################################################################
#Control script to start or stop an individual partition.
##################################################################################

. /etc/profile.d/ssgruntimedefs.sh
USER=`whoami`
SSGUSER="gateway"

usage() {
	echo
	echo "Usage: partitionControl.sh [start|run|stop|usage] partition-name"
	echo "  start - start the given partition and detach from the console"
	echo "  run - start the given partition but do not detach from the console"
	echo "  stop - stop the given partition"
	echo "  usage - show this message"
	echo "  partition-name - the name of the partition to control"
	echo
}

explain_not_enabled() {
    echo
    echo "The \"${PARTITION_NAME}\" partition is not enabled."
    echo "To enable it, configure this partition using the SecureSpan Configuration Wizard or "
    echo "create a file called \"enabled\" in the partition directory [${PARTITION_DIR}]"
    echo
}

build_paths() {
    PARTITION_DIR=${SSG_HOME}/etc/conf/partitions/${PARTITION_NAME}
    CONFIG_FILE="${PARTITION_DIR}/server.xml"
    ENABLED_FILE="${PARTITION_DIR}/enabled"
    FIREWALL_FILE="${PARTITION_DIR}/firewall_rules"
}

do_control() {
    . ${SSG_HOME}/bin/partition_defs.sh true "${PARTITION_COUNT}"
    (perl ${SSG_HOME}/bin/partition_firewall.pl ${FIREWALL_FILE} ${COMMAND})

    export JAVA_OPTS="${ORIGINAL_JAVA_OPTS} ${partition_opts}"
    export CATALINA_OPTS=-Dcom.l7tech.server.partitionName=${PARTITION_NAME}

    (su $SSGUSER -c "${SSG_HOME}/tomcat/bin/catalina.sh ${COMMAND} -config ${CONFIG_FILE}")

}

COMMAND=${1}
PARTITION_NAME=${2}

if [ ${USER} != "root" ] ; then
    echo
    echo "this script must be run as root"
    echo
    exit 1;
fi

if [ -z "${COMMAND}" ] ; then
	echo
	echo "Please specify a command."
	usage
	exit 1;
fi

if [ -z "${PARTITION_NAME}" ] ; then
	echo
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
        echo
        echo "SSG_HOME is not set! Please set SSG_HOME to the installation root of the SecureSpan Gateway (ex. /ssg)"
        exit 1;
    else
        echo
        echo "Invalid SSG_HOME! Please set SSG_HOME to the installation root of the SecureSpan Gateway (ex. /ssg)"
        exit 1;
    fi
fi

build_paths

if [ -e ${ENABLED_FILE} ] ; then
    do_control
else
    explain_not_enabled
fi
