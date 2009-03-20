#!/bin/bash
. /opt/SecureSpan/Appliance/libexec/envclean

#set the current working directory to where this script lives
cd `dirname $0`

if [ ! -x "${JAVA_HOME}/bin/java" ] ; then
  echo "Java not found."
  exit 1
fi

if [ $UID -eq 0 ]; then
    su layer7 -c "${JAVA_HOME}/bin/java -jar SystemConfigWizard.jar $*"
else
    ${JAVA_HOME}/bin/java -jar SystemConfigWizard.jar $*
fi

