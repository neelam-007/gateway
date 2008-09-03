#!/bin/bash

##################################################################################
#Control script to start or stop an individual node.
##################################################################################

#SSG_HOME isn't set yet so we have to find it ourselves to get to the profile script
cd `dirname $0`
pushd .. > /dev/null	#now in /ssg
WHEREISSSG=`pwd`
popd >/dev/null

. ${WHEREISSSG}/etc/profile

#SSG_HOME should be set now

ORIGINAL_JAVA_OPTS="${SSG_JAVA_OPTS} ${NODE_OPTS}"
USER=$LOGNAME
SSGUSER="gateway"
ENABLED_FILE="${SSG_HOME}/etc/conf/enabled"
GATEWAY_PID="${SSG_HOME}/etc/conf/ssg.pid"
GATEWAY_SHUTDOWN="${SSG_HOME}/etc/conf/SHUTDOWN.NOW"

usage() {
    echo
    echo "Usage: control.sh [start|run|stop|usage]"
    echo "  start - start and detach from the console"
    echo "  run - start but do not detach from the console"
    echo "  stop - stop"
    echo "  forcestop - forcibly stop. The node will shutdown immediately, ungracefully."
    echo "  status - query the status of the given node (check the running state)."
    echo "  usage - show this message"
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
    echo "The SecureSpan Gateway is not enabled."
    echo "To enable, configure using the SecureSpan Configuration Wizard or "
    echo "create a file called \"enabled\" in the directory [${SSG_HOME}/etc/conf]"
    echo
}

control() {
    #stop here if this is a status check
    if [ "${COMMAND}" == "status" ] ; then
        do_status
        return
    fi

    echo
    echo "Attempting to ${COMMAND} ..."
    check_enabled
    if [ $? -eq 0 ] ; then
        if [ -e  /usr/local/Tarari ] ; then
            ORIGINAL_JAVA_OPTS="-Dcom.l7tech.common.xml.tarari.enable=true $ORIGINAL_JAVA_OPTS"
        fi

        JAVA_OPTS="${ORIGINAL_JAVA_OPTS} -Djava.security.properties==${SSG_HOME}/etc/conf/java.security"
        export JAVA_OPTS
        export SSG_HOME
        export GATEWAY_PID
        export GATEWAY_SHUTDOWN

        if [ "${COMMAND}" == "start" ] ; then
            if [ -f "${GATEWAY_PID}" ]  && [ -d "/proc/$(< ${GATEWAY_PID})" ] ; then
                return 1
            fi

            if [ "$LOG_REDIRECTION_OPERATOR" = "|" -a -n "$LOG_REDIRECTION_DEST" ]; then
                (su $SSGUSER -c "${SSG_HOME}/bin/gateway.sh ${COMMAND}" <&- 2>&1 | ${LOG_REDIRECTION_DEST}) &>/dev/null &
            else
                if [ "$LOG_REDIRECTION_OPERATOR" = ">" -a -n "$LOG_REDIRECTION_DEST" ]; then
                    output_file="${LOG_REDIRECTION_DEST}"
                else
                    output_file="/dev/null"
                fi
                (su $SSGUSER -c "${SSG_HOME}/bin/gateway.sh ${COMMAND}") <&- &>"$output_file" &
            fi
        elif [ "${COMMAND}" == "forcestop" ] ; then
            if [ "$LOG_REDIRECTION_OPERATOR" = "|" -a -n "$LOG_REDIRECTION_DEST" ]; then
                (su $SSGUSER -c "${SSG_HOME}/bin/gateway.sh stop -force" <&- 2>&1 | ${LOG_REDIRECTION_DEST}) &/dev/null &
            else
                if [ "$LOG_REDIRECTION_OPERATOR" = ">" -a -n "$LOG_REDIRECTION_DEST" ]; then
                    output_file="${LOG_REDIRECTION_DEST}"
                else
                    output_file="/dev/null"
                fi
                (su $SSGUSER -c "${SSG_HOME}/bin/gateway.sh stop -force") <&- &>"$output_file"
            fi
            GATEWAY_RET=$?
            return $GATEWAY_RET
        else
            (su $SSGUSER -c "${SSG_HOME}/bin/gateway.sh ${COMMAND}")
            GATEWAY_RET=$?
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
        echo "Active."
    else
        echo "Inactive."
    fi
    return $STATUS
}

ensure_JDK
COMMAND=${1}

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
        control
        ;;
    stop)
        check_user
        control
        ;;
    forcestop)
        check_user
        control
        ;;
    status)
        check_user
        control
        ;;
    run)
        check_user
        control
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
