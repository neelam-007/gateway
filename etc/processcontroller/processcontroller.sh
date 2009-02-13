#!/bin/bash
#############################################################################
# SecureSpan Process Controller Launch Script
#############################################################################

#
if [ -z "${SSPC_HOME}" ] ; then
  cd $(dirname ${0})
  cd ..
  SSPC_HOME="$(pwd)"
fi
if [ -z "${JAVA_HOME}" ] ; then
  JAVA_HOME="/opt/SecureSpan/JDK"
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

cd "${SSPC_HOME}" &>/dev/null || fail 2 "Directory not found: ${SSPC_HOME}"
[ -r "${PC_JAR}" ] || fail 2 "Missing or unreadable file: ${PC_JAR}"
[ -x "${JAVA_HOME}/bin/java" ] || fail 2 "Invalid JAVA_HOME: ${JAVA_HOME}"

# Run the config bootstrapper if it looks like this is the first time we've run
if [ ! -f "${SSPC_HOME}/etc/host.properties" ] ; then

  if [ -z "${PC_USER}" ] ; then
    "${JAVA_HOME}/bin/java" -classpath "${PC_JAR}" -Dcom.l7tech.server.processcontroller.homeDirectory="$SSPC_HOME" com.l7tech.server.processcontroller.BootstrapConfig
  else
    export JAVA_HOME SSPC_HOME PC_JAR
    runuser "${PC_USER}" -c '"${JAVA_HOME}/bin/java" -classpath "${PC_JAR}" -Dcom.l7tech.server.processcontroller.homeDirectory="$SSPC_HOME" com.l7tech.server.processcontroller.BootstrapConfig'
  fi

  if [ ${?} -ne 0 ]; then
    fail "${?}" "Error saving initial configuration."
  fi
fi

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
  PC_JAVAOPT="$JPDA_OPTS"
  export PC_JAVAOPT
fi

#
if [ -z "${PC_USER}" ] ; then
  "${JAVA_HOME}/bin/java" ${PC_JAVAOPT} -Dcom.l7tech.server.processcontroller.hostPropertiesFile="$SSPC_HOME/etc/host.properties" -jar ${PC_JAR} &>/dev/null <&- &
else
  export JAVA_HOME SSPC_HOME PC_JAR PC_PIDTEMP
  runuser "${PC_USER}" -c '"${JAVA_HOME}/bin/java" ${PC_JAVAOPT} -Dcom.l7tech.server.processcontroller.hostPropertiesFile="$SSPC_HOME/etc/host.properties" -jar ${PC_JAR} &>/dev/null <&- & echo "${!}" > "${PC_PIDTEMP}"'
fi

if [ ${?} -eq 0 ] ; then
  [ -z "${PC_PIDFILE}" ] || mv -f "${PC_PIDTEMP}" "${PC_PIDFILE}"
else
  [ -z "${PC_PIDFILE}" ] || rm -f "${PC_PIDFILE}" &>/dev/null
  fail "${?}" "Error starting Process Controller."
fi

exit 0;
