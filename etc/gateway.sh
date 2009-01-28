#!/bin/bash
#######################################################################
# Script to run the SecureSpan Gateway
#######################################################################
#
# This script will set environment options using the shared profile
# and control the Gateway process.
#
# Usage:
#
#   ./gateway.sh COMMAND [OPTION]*
#
# Commands are:
#
#   run   - run in foreground (CTRL-C to exit)
#   start - start as a daemon
#   stop  - stop daemon
#
# Options starting with -J are passed through to the Java VM, other
# options are passed to the Gateway process.
#

# Set environment
umask 0002
SSGUSER="gateway"
SSGNODE="default"
SSGJDK_VERSION="1.6"
export SSGNODE SSGUSER

# Source profile for standard environment
cd `dirname $0`
. ../etc/profile

ensure_JDK "${SSGJDK_VERSION}"

# Process script options
declare -a SSGARGS
declare -a SSGOPTS
for OPTION in "$@" ; do
    if [ "${OPTION}" = "${OPTION#-J}" ] ; then
        SSGARGS[$((${#SSGARGS[*]} + 1))]="${OPTION}"
    else
        SSGOPTS[$((${#SSGOPTS[*]} + 1))]="${OPTION#-J}"
    fi
done

# Process options
GATEWAY_PID="${SSG_HOME}/node/${SSGNODE}/var/ssg.pid"
JAVA_OPTS="-Dcom.l7tech.server.home=${SSG_HOME}/node/${SSGNODE}"
if [ -d "${SSG_JAVA_HOME}/jre/lib/ext" ] ; then
    JAVA_OPTS="${JAVA_OPTS} -Djava.ext.dirs=${SSG_JAVA_HOME}/jre/lib/ext:${SSG_HOME}/runtime/lib/ext"
else
    JAVA_OPTS="${JAVA_OPTS} -Djava.ext.dirs=${SSG_JAVA_HOME}/lib/ext:${SSG_HOME}/runtime/lib/ext"
fi
if [ "${SSGTARARI}"  == "true" ] && [ -e  /usr/local/Tarari ] ; then
    JAVA_OPTS="-Dcom.l7tech.common.xml.tarari.enable=true ${JAVA_OPTS}"
fi
if [ -n "${SSG_JAVA_OPTS}" ] ; then
  JAVA_OPTS="${JAVA_OPTS} ${SSG_JAVA_OPTS}"
fi
if [ -n "${NODE_OPTS}" ] ; then
  JAVA_OPTS="${JAVA_OPTS} ${NODE_OPTS}"
fi
for OPT in "${SSGOPTS[@]}"; do
    if [[ "${JAVA_OPTS}" =~ "${OPT%%=*}=[a-zA-Z0-9_\.-]{1,245}" ]] ; then
        JAVA_OPTS=${JAVA_OPTS/${BASH_REMATCH}/${OPT}}
    else
        JAVA_OPTS="${JAVA_OPTS} ${OPT}"
    fi
done

# Sanity checks
if [ -z "${SSG_JAVA_HOME}" ] ; then
    echo "Java is not configured for gateway."
    exit 13
fi
if [ ! -x "${SSG_JAVA_HOME}/bin/java" ] ; then
    echo "Java not found: ${SSG_JAVA_HOME}"
    exit 13
fi
if [ "$(whoami)" != "${SSGUSER}" ] ; then
    echo "Please run as the user: ${SSGUSER}"
    exit 13
fi

# Helper functions
pid_exited() {
  if [ -f "${GATEWAY_PID}" ]  && [ -d "/proc/$(< ${GATEWAY_PID})" ] ; then
    return 1;
  else
    return 0;
  fi
}

wait_for_pid() {
  TRYCOUNT=0
  while [ $TRYCOUNT -lt 45 ]
  do
    if pid_exited; then
      return 0 
    fi
    TRYCOUNT=`expr $TRYCOUNT + 1`
    sleep 1
  done
  # Too many tries; give up
  return 1
}

echoOptions() {
    echo "Gateway commands:"
    echo -e "\trun   - Run the gateway with console output"
    echo -e "\tstart - Start the gateway (option: -console)"
    echo -e "\tstop  - Stop the gateway (option: -force)"
    echo ""
}

# Debug options
if [ "$1" = "jpda" ] ; then
  if [ -z "$JPDA_TRANSPORT" ]; then
    JPDA_TRANSPORT="dt_socket"
  fi
  if [ -z "$JPDA_ADDRESS" ]; then
    JPDA_ADDRESS="8000"
  fi
  if [ -z "$JPDA_OPTS" ]; then
    JPDA_OPTS="-Xdebug -Xrunjdwp:transport=$JPDA_TRANSPORT,address=$JPDA_ADDRESS,server=y,suspend=n"
  fi
  JAVA_OPTS="$JAVA_OPTS $JPDA_OPTS"
  shift
fi

# Move to node directory and perform action
cd "${SSG_HOME}/node/${SSGNODE}"

if [ "$1" = "start" ] ; then
    shift

    if [ "$1" = "-console" ]; then
        shift
        JAVA_OPTS="${JAVA_OPTS} -Dcom.l7tech.server.log.console=true"
    fi

    #enable logging of stdout/stderr using JDK logging as well as the standard SSG logging facilities
    "${SSG_JAVA_HOME}/bin/java" -Djava.util.logging.config.class=com.l7tech.server.log.JdkLogConfig ${JAVA_OPTS} -jar "${SSG_HOME}/runtime/Gateway.jar" "${SSGARGS[@]}" &

    if [ ! -z "${GATEWAY_PID}" ]; then
        rm -f "${GATEWAY_PID}"
        echo $! > "${GATEWAY_PID}"
    fi

elif [ "$1" = "run" ] ; then
    shift

    if [ -n "${GATEWAY_PID}" ]; then
        rm -f "${GATEWAY_PID}"
        echo $$ > "${GATEWAY_PID}"
    fi

    exec "${SSG_JAVA_HOME}/bin/java" ${JAVA_OPTS} -jar "${SSG_HOME}/runtime/Gateway.jar" "${SSGARGS[@]}"

elif [ "$1" = "stop" ] ; then
  shift

  FORCE=0
  if [ "$1" = "-force" ]; then
    shift
    FORCE=1
  fi

  if [ $FORCE -eq 0 ]; then
    if [ -n "${GATEWAY_PID}" ] && [ -f "${GATEWAY_PID}" ]; then
        # signal process to stop
        kill -TERM $(<"${GATEWAY_PID}") >/dev/null 2>&1

        if wait_for_pid; then
          exit 0
        fi
    else
        echo "Shutdown failed -- unable to determine process ID, use --force"
        exit 21
    fi

    echo "Shutdown failed -- must use -force"
    exit 21
  fi

  if [ $FORCE -eq 0 ]; then
    echo "Shutdown failed -- to forcibly terminate the process, use -force"
    exit 21
  fi

  if [ $FORCE -eq 1 ]; then
    if [ ! -z "$GATEWAY_PID" ]; then
       echo "Killing: `cat $GATEWAY_PID`"
       kill -9 `cat $GATEWAY_PID`
       exit 21
    else
       echo "Kill failed: \$GATEWAY_PID not set"
       exit 21;
    fi
  fi
elif [ -n "$1" ] ; then
  echo -e "\nInvalid command \"${1}\"\n"
  echoOptions
  exit 17
else
  echoOptions
  exit 17
fi


