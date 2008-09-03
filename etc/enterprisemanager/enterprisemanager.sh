#!/bin/bash
#############################################################################
# SecureSpan Enterprise Manager Launch Script
#############################################################################

#
if [ -z "${SSEM_HOME}" ] ; then
  SSEM_HOME="/opt/SecureSpan/EnterpriseManager"
fi
if [ -z "${JAVA_HOME}" ] ; then
  JAVA_HOME="/opt/SecureSpan/JDK"
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
cd "${SSEM_HOME}" || fail "Directory not found: ${SSEM_HOME}" 

#
"${JAVA_HOME}/bin/java" -jar EnterpriseManager.jar &>/dev/null <&- &
if [ ${?} -eq 0 ] ; then
  [ -z "${PID_FILE}" ] || echo "${!}" > "${PID_FILE}"
else
  [ -z "${PID_FILE}" ] || rm -f "${PID_FILE}" &>/dev/null
  fail "${?}" "Error starting Enterprise Manager."
fi

exit 0;
