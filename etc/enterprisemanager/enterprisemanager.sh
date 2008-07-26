#!/bin/bash
#############################################################################
# SecureSpan Enterprise Manager Launch Script
#############################################################################

#
if [ -z "${SSEM_HOME}" ] ; then
  SSEM_HOME="/opt/SecureSpan/EnterpriseManager"
fi
if [ -z "${JAVA_HOME}" ] ; then
  JAVA_HOME="/ssg/jdk"
fi

function fail() {
  echo "$1"
  exit 1
}

#
cd "${SSEM_HOME}" || fail "Directory not found: ${SSEM_HOME}" 

#
"${JAVA_HOME}/bin/java" -jar EnterpriseManager.jar || fail "Error starting Enterprise Manager." &>/dev/null <&- &
