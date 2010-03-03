#!/bin/bash
#############################################################################
# SecureSpan Process Controller Launch Script
#############################################################################

#
umask 0007
if [ -z "${SSPC_HOME}" ] ; then
  cd $(dirname ${0})
  cd ..
  SSPC_HOME="$(pwd)"
fi
if [ -z "${JAVA_HOME}" -a -d /opt/SecureSpan/JDK ] ; then
  JAVA_HOME="/opt/SecureSpan/JDK"
fi

if [ -z "${JAVA_HOME}" -a -n "${SSG_JAVA_HOME}" ] ; then
  JAVA_HOME="${SSG_JAVA_HOME}"
fi

PC_USER=""
PC_PIDFILE=""
PC_PIDTEMP=""
PC_JAR="Controller.jar"
if [ ! -z "${1}" ] && [ ! -z "${2}" ] ; then
  PC_USER="${1}"
  PC_PIDFILE="${2}"
  PC_PIDTEMP=$(mktemp -p /tmp $(basename "${PC_PIDFILE}").XXXX)
  chown "${PC_USER}" "${PC_PIDTEMP}"
fi

#
function fail() {
  echo "$2"
  exit ${1}
}

# this puts the necessary "id" that supports -u first in the path on Solaris, and is a noop on other OSes
OLDPATH=$PATH
PATH="/usr/xpg4/bin:$PATH"
if [ "$(id -u)" != "$(id -u ${PC_USER})" ] ; then
    if [ "$1" = "run" ] || [ "$(id -u)" != "0" ] ; then
        echo "Please run as the user: ${PC_USER}"
        exit 13
    else
        RUNASUSER="su ${PC_USER} -c "
    fi
fi

PC_JAVAOPT="-Djava.security.egd=file:/dev/./urandom"

cd "${SSPC_HOME}" &>/dev/null || fail 2 "Directory not found: ${SSPC_HOME}"
[ -r "${PC_JAR}" ] || fail 2 "Missing or unreadable file: ${PC_JAR}"
[ -x "${JAVA_HOME}/bin/java" ] || fail 2 "Invalid JAVA_HOME: ${JAVA_HOME}"

if [ -f "${SSPC_HOME}/etc/DEBUG" ] ; then
  if [ -z "$JPDA_TRANSPORT" ]; then
    JPDA_TRANSPORT="dt_socket"
  fi
  if [ -z "$JPDA_ADDRESS" ]; then
    JPDA_ADDRESS="8001"
  fi
  if [ -z "$JPDA_OPTS" ]; then
    JPDA_OPTS="-Xdebug -Xrunjdwp:transport=$JPDA_TRANSPORT,address=$JPDA_ADDRESS,server=y,suspend=n"
  fi
  PC_JAVAOPT="$PC_JAVAOPT $JPDA_OPTS"
fi

export PC_JAVAOPT

BOOTSTRAP_CONFIG_LAUNCH="${JAVA_HOME}/bin/java ${PC_JAVAOPT} -classpath ${PC_JAR} com.l7tech.server.processcontroller.BootstrapConfig"

# Run the config bootstrapper if it looks like this is the first time we've run
if [ -z "${PC_USER}" -o -z "${RUNASUSER}" ] ; then
    ${BOOTSTRAP_CONFIG_LAUNCH}
else
    export JAVA_HOME SSPC_HOME PC_JAR
    ${RUNASUSER} "${BOOTSTRAP_CONFIG_LAUNCH}"
fi

if [ ${?} -ne 0 ]; then
    fail "${?}" "Error saving initial configuration."
fi


#
PC_LAUNCH="${JAVA_HOME}/bin/java ${PC_JAVAOPT} -jar ${PC_JAR}"
if [ -z "${PC_USER}" -o -z "${RUNASUSER}" ] ; then
  ${PC_LAUNCH} &>/dev/null <&- & echo "${!}" > "${PC_PIDTEMP}"
else
  export JAVA_HOME SSPC_HOME PC_JAR PC_PIDTEMP
  ${RUNASUSER} "${PC_LAUNCH}" &>/dev/null <&- & echo "${!}" > "${PC_PIDTEMP}"
fi


if [ ${?} -eq 0 ] ; then
  [ -z "${PC_PIDFILE}" ] || mv -f "${PC_PIDTEMP}" "${PC_PIDFILE}"
else
  [ -z "${PC_PIDFILE}" ] || rm -f "${PC_PIDFILE}" &>/dev/null
  fail "${?}" "Error starting Process Controller."
fi

exit 0;
