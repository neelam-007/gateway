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
USER=$LOGNAME
SSGUSER="gateway"

usage() {
    echo
    echo "Usage: partitionControl.sh [start|run|stop|usage] partition-name"
    echo "  start - start the given partition and detach from the console"
    echo "  run - start the given partition but do not detach from the console"
    echo "  stop - stop the given partition"
    echo "  forcestop - forcibly stop the given partition. The partition will shutdown immediately, ungracefully."
    echo "  status - query the status of the given partition (check the running state)."
    echo "  usage - show this message"
    echo "  partition-name - the name of the partition to control"
    echo
    exit 0;
}

check_environment() {
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
}

check_user() {
    if [ ${USER} != "root" ] ; then
        echo
        echo "This script must be run as root"
        echo
        exit 1;
    fi
}

check_enabled() {
    if [ -e ${ENABLED_FILE} ] ; then
        return 0
    else
        explain_not_enabled
        return 1
    fi
}

explain_not_enabled() {
    echo
    echo "The \"${PARTITION_NAME}\" partition is not enabled."
    echo "To enable it, configure this partition using the SecureSpan Configuration Wizard or "
    echo "create a file called \"enabled\" in the partition directory [${PARTITION_DIR}]"
    echo
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

control_one_or_more_partitions() {
    if [ -z "${PARTITION_NAME}" ] ; then
        for partition_dir in ${ALL_PARTITIONS}
        do
            PARTITION_NAME="${partition_dir}"
            control_single_partition
        done
    else
        control_single_partition
    fi

}

control_single_partition() {
    fail_if_no_partition
    build_paths

    #stop here if this is a status check
    if [ "${COMMAND}" == "status" ] ; then
        do_status
        return
    fi

    echo
    echo "Attempting to ${COMMAND} the ${PARTITION_NAME} partition ..."
    check_enabled
    if [ $? -eq 0 ] ; then

        do_firewall

        if [ "${PARTITION_NAME}"  == "default_" ] && [ -e  /usr/local/Tarari ] ; then
            ORIGINAL_JAVA_OPTS="-Dcom.l7tech.xml.tarari.enable=true $ORIGINAL_JAVA_OPTS"
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

            if [ "$LOG_REDIRECTION_OPERATOR" = "|" -a -n "$LOG_REDIRECTION_DEST" ]; then
                (su $SSGUSER -c "${SSG_HOME}/bin/gateway.sh ${COMMAND}" <&- 2>&1 | `${LOG_REDIRECTION_DEST/<PARTITION_NAME>/$PARTITION_NAME}`) &>/dev/null &
            else
                if [ "$LOG_REDIRECTION_OPERATOR" = ">" -a -n "$LOG_REDIRECTION_DEST" ]; then
                    output_file="${LOG_REDIRECTION_DEST/<PARTITION_NAME>/$PARTITION_NAME}"
                else
                    output_file="/dev/null"
                fi
                (su $SSGUSER -c "${SSG_HOME}/bin/gateway.sh ${COMMAND}") <&- &>"$output_file" &
            fi
        elif [ "${COMMAND}" == "forcestop" ] ; then
            if [ "$LOG_REDIRECTION_OPERATOR" = "|" -a -n "$LOG_REDIRECTION_DEST" ]; then
                (su $SSGUSER -c "${SSG_HOME}/bin/gateway.sh stop -force" <&- 2>&1 | `${LOG_REDIRECTION_DEST/<PARTITION_NAME>/$PARTITION_NAME}`) &/dev/null &
            else
                if [ "$LOG_REDIRECTION_OPERATOR" = ">" -a -n "$LOG_REDIRECTION_DEST" ]; then
                    output_file="${LOG_REDIRECTION_DEST/<PARTITION_NAME>/$PARTITION_NAME}"
                else
                    output_file="/dev/null"
                fi
                (su $SSGUSER -c "${SSG_HOME}/bin/gateway.sh stop -force") <&- &>"$output_file"
            fi
            GATEWAY_RET=$?
            do_firewall
            return $GATEWAY_RET
        else
            (su $SSGUSER -c "${SSG_HOME}/bin/gateway.sh ${COMMAND}")
            GATEWAY_RET=$?
            do_firewall
            return $GATEWAY_RET
        fi
    fi  
}

do_status() {
    STATUS=1
    if [ -f "${GATEWAY_PID}" ]  && [ -d "/proc/$(< ${GATEWAY_PID})" ] ; then
        STATUS=0
    fi

    if [ ${STATUS} -eq 0 ] ; then
        echo "${PARTITION_NAME} is active."
    else
        echo "${PARTITION_NAME} is inactive."
    fi
    return $STATUS
}

fail_if_no_partition() {
    if [ -z "${PARTITION_NAME}" ] ; then
        echo
        echo "A partition name must be specified for the \"${COMMAND}\" command"
        usage
        exit 1;
    fi
}

ensure_JDK
COMMAND=${1}
PARTITION_NAME=${2}

if [ -z "${COMMAND}" ] ; then
    echo
    echo "Please specify a command."
    usage
    exit 1;
fi

check_environment

case "$COMMAND" in
    start)
        check_user
        control_one_or_more_partitions
        ;;
    stop)
        check_user
        control_one_or_more_partitions
        ;;
    forcestop)
        check_user
        control_one_or_more_partitions
        ;;
    status)
        check_user
        control_one_or_more_partitions
        ;;
    run)
        check_user
        control_single_partition
        ;;
    usage)
        usage
        ;;
    *)
        echo "${COMMAND} is not a valid command."
        echo
        usage
        ;;
esac
