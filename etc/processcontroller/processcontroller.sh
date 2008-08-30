#!/bin/bash
#############################################################################
# SecureSpan Process Controller Launch Script
#############################################################################

#
if [ -z "${SSPC_HOME}" ] ; then
  SSPC_HOME="/opt/SecureSpan/Gateway/Controller"
fi
if [ -z "${JAVA_HOME}" ] ; then
  JAVA_HOME="/ssg/jdk"
fi

PID_FILE=""
if [ ! -z "${1}" ] ; then
  PID_FILE="${1}"
fi

#
function fail() {
  echo "$2"
  exit ${1}
}

#
cd "${SSPC_HOME}" || fail "Directory not found: ${SSPC_HOME}"

#
"${JAVA_HOME}/bin/java" -Dcom.l7tech.server.processcontroller.hostPropertiesFile="$SSPC_HOME/etc/host.properties" -jar Controller.jar &>/dev/null <&- &
if [ ${?} -eq 0 ] ; then
  [ -z "${PID_FILE}" ] || echo "${!}" > "${PID_FILE}"
else
  [ -z "${PID_FILE}" ] || rm -f "${PID_FILE}" &>/dev/null
  fail "${?}" "Error starting Process Controller."
fi

exit 0;
