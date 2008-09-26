#!/bin/bash
#############################################################################
# Enterprise Service Manager Launch Script
#############################################################################

#
if [ -z "${SSEM_HOME}" ] ; then
  SSEM_HOME="/opt/SecureSpan/EnterpriseManager"
fi
if [ -z "${JAVA_HOME}" ] ; then
  JAVA_HOME="/opt/SecureSpan/JDK"
fi

EM_USER=""
EM_PIDFILE=""
EM_PIDTEMP=""
if [ ! -z "${1}" ] && [ ! -z "${2}" ] ; then
  EM_USER="${1}"
  EM_PIDFILE="${2}"
  EM_PIDTEMP=$(mktemp -p /tmp $(basename "${EM_PIDFILE}").XXXX)
  chown "${EM_USER}" "${EM_PIDTEMP}"
fi

#
function fail() {
  echo "$2"
  exit ${1}
}

#
cd "${SSEM_HOME}" &>/dev/null || fail 2 "Directory not found: ${SSEM_HOME}" 

if [ -z "${EM_USER}" ] ; then
  "${JAVA_HOME}/bin/java" -jar EnterpriseManager.jar &>/dev/null <&- &
else
  export JAVA_HOME EM_PIDTEMP
  runuser "${EM_USER}" -c '"${JAVA_HOME}/bin/java" -jar EnterpriseManager.jar &>/dev/null <&- & echo "${!}" > "${EM_PIDTEMP}"'
fi

if [ ${?} -eq 0 ] ; then
  [ -z "${EM_PIDFILE}" ] || mv -f "${EM_PIDTEMP}" "${EM_PIDFILE}"
else
  [ -z "${EM_PIDFILE}" ] || rm -f "${EM_PIDFILE}" &>/dev/null
  fail "${?}" "Error starting Enterprise Service Manager."
fi

exit 0;
