#!/bin/bash

##################################################################################
#Control script to start or stop an individual partition.
##################################################################################

#SSG_HOME isn't set yet so we have to find it ourselves to get to the profile script
cd `dirname $0`
pushd .. > /dev/null	#now in /ssg
WHEREISSSG=`pwd`
popd >/dev/null

. ${WHEREISSSG}/etc/profile

#SSG_HOME should be set now

ORIGINAL_JAVA_OPTS="${SSG_JAVA_OPTS} ${PARTITION_OPTS}"
USER=$(whoami)
SSGUSER="gateway"

usage() {
    echo
    echo "Usage: partitionControl.sh [start|run|stop|usage] partition-name"
    echo "  start - start the given partition and detach from the console"
    echo "  run - start the given partition but do not detach from the console"
    echo "  stop - stop the given partition"
    echo "  status = query the status of the given partition (check the running state)."
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
    return 1
}

build_paths() {
    PARTITION_DIR="${SSG_HOME}/etc/conf/partitions/${PARTITION_NAME}"
    ENABLED_FILE="${PARTITION_DIR}/enabled"
    FIREWALL_UPDATER="${SSG_HOME}/appliance/bin/partition_firewall.pl"
    FIREWALL_FILE="${PARTITION_DIR}/firewall_rules"
    GATEWAY_PID="${PARTITION_DIR}/ssg.pid"
    GATEWAY_SHUTDOWN="${PARTITION_DIR}/SHUTDOWN.NOW"
}

do_firewall() {
    if [ -e ${FIREWALL_UPDATER} ] ; then
        (perl ${FIREWALL_UPDATER} ${FIREWALL_FILE} ${COMMAND})
    fi
}

do_control() {
    do_firewall
    if [ "${PARTITION_NAME}"  == "default_" ] && [ -e  /usr/local/Tarari ] ; then
    	ORIGINAL_JAVA_OPTS="-Dcom.l7tech.common.xml.tarari.enable=true $ORIGINAL_JAVA_OPTS"
    fi

    if  [ -e "${SSG_HOME}/etc/conf/partitions/${PARTITION_NAME}/cluster_hostname" ]; then
    	RMI_HOSTNAME="$(<${SSG_HOME}/etc/conf/partitions/${PARTITION_NAME}/cluster_hostname)"
    else
        RMI_HOSTNAME="$(hostname)"
    fi

    echo ${ORIGINAL_JAVA_OPTS} | grep java.rmi.server.hostname &>/dev/null
    if [ $? -eq 0 ] ; then
        ORIGINAL_JAVA_OPTS=$(echo ${ORIGINAL_JAVA_OPTS} | sed "s/-Djava.rmi.server.hostname=[^ ]*/-Djava.rmi.server.hostname=${RMI_HOSTNAME}/")
    else
        ORIGINAL_JAVA_OPTS="${ORIGINAL_JAVA_OPTS} -Djava.rmi.server.hostname=${RMI_HOSTNAME}"
    fi

    JAVA_OPTS="${ORIGINAL_JAVA_OPTS} -Djava.security.properties==${PARTITION_DIR}/java.security -Dcom.l7tech.server.partitionName=${PARTITION_NAME}"
    export JAVA_OPTS
    export SSG_HOME
    export GATEWAY_PID
    export GATEWAY_SHUTDOWN

    if [ "${COMMAND}" == "start" ] ; then
        if [ -f "${GATEWAY_PID}" ]  && [ -d "/proc/$(< ${GATEWAY_PID})" ] ; then
            return 1
        fi

        (su $SSGUSER -c "${SSG_HOME}/bin/gateway.sh ${COMMAND} 2>&1") <&- &>/dev/null &
    else
        (su $SSGUSER -c "${SSG_HOME}/bin/gateway.sh ${COMMAND}") &>/dev/null
    fi
}

COMMAND=${1}
PARTITION_NAME=${2}

ensure_JDK

if [ ${USER} != "root" ] ; then
    echo
    echo "This script must be run as root"
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

if [ "${COMMAND}" == "usage" ] ; then
    usage;
    exit 0;
fi

if [ ! -d "${SSG_HOME}" ] ; then
    if [ -z "${SSG_HOME}" ] ; then
        echo ""
        echo "SSG_HOME is not set! Please set SSG_HOME to the installation root of the SecureSpan Gateway (ex. /ssg)"
        exit 1;
    else
        echo
        echo "Invalid SSG_HOME! Please set SSG_HOME to the installation root of the SecureSpan Gateway (ex. /ssg)"
        exit 1;
    fi
fi

build_paths

if [ "${COMMAND}" == "status" ] ; then
    STATUS=1
    if [ -f "${GATEWAY_PID}" ]  && [ -d "/proc/$(< ${GATEWAY_PID})" ] ; then
        STATUS=0
    fi

    if [ ${STATUS} -eq 0 ] ; then
        echo "${PARTITION_NAME} is active."
    else
        echo "${PARTITION_NAME} is inactive."
    fi

    exit ${STATUS}
fi

if [ -e ${ENABLED_FILE} ] ; then
    do_control
else
    explain_not_enabled
fi
