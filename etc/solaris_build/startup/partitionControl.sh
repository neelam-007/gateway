#!/bin/bash

##################################################################################
#Control script to start or stop an individual partition.
##################################################################################

#. /etc/profile.d/ssgruntimedefs.sh
/ssg/bin/ssgruntimedefs.sh

ORIGINAL_JAVA_OPTS=${JAVA_OPTS}

USER=$LOGUSER
SSGUSER="gateway"

echo $USER

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
    return 1
}

build_paths() {
    PARTITION_DIR="${SSG_HOME}/etc/conf/partitions/${PARTITION_NAME}"
    CONFIG_FILE="${PARTITION_DIR}/server.xml"
    ENABLED_FILE="${PARTITION_DIR}/enabled"
    FIREWALL_FILE="${PARTITION_DIR}/firewall_rules"
    CATALINA_PID="${PARTITION_DIR}/ssg.pid"
}

do_control() {
    . ${SSG_HOME}/bin/partition_defs.sh true "${PARTITION_COUNT}"
    (perl ${SSG_HOME}/bin/partition_firewall.pl ${FIREWALL_FILE} ${COMMAND})

    if [ -z "$TOMCAT_HOME" ] ; then
        TOMCAT_HOME="${SSG_HOME}/tomcat/"
    fi

    if [ "${PARTITION_NAME}"  == "default_" ]; then
        if [ -e  /usr/local/Tarari ]; then
            ORIGINAL_JAVA_OPTS="-Dcom.l7tech.common.xml.tarari.enable=true $ORIGINAL_JAVA_OPTS"
        fi
    fi

    export JAVA_OPTS="${ORIGINAL_JAVA_OPTS} ${partition_opts}"
    export TOMCAT_HOME
    export CATALINA_OPTS=-Dcom.l7tech.server.partitionName=${PARTITION_NAME}
    export CATALINA_PID

    if [ "${COMMAND}" == "start" ] ; then
        if [ -f "${CATALINA_PID}" ]  && [ -d "/proc/$(< ${CATALINA_PID})" ] ; then
            return 1
        fi
        (su $SSGUSER -c "${TOMCAT_HOME}/bin/catalina.sh ${COMMAND} -config ${CONFIG_FILE} 2>&1 | logger -t SSG-${PARTITION_NAME}") <&- &>/dev/null &
    else
        (su $SSGUSER -c "${TOMCAT_HOME}/bin/catalina.sh ${COMMAND} -config ${CONFIG_FILE}") &>/dev/null
    fi
}

COMMAND=${1}
PARTITION_NAME=${2}

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

if [ "${COMMAND}" == "status" ] ; then
    STATUS=1
    if [ -f "${CATALINA_PID}" ]  && [ -d "/proc/$(< ${CATALINA_PID})" ] ; then
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
