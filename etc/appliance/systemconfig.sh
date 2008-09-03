#!/bin/bash

#set the current working directory to where this script lives
cd `dirname $0`

JAVA_HOME="/opt/SecureSpan/JDK"

if [ $UID -eq 0 ]; then
    su ssgconfig -c "${JAVA_HOME}/bin/java -jar SystemConfigWizard.jar $*"
else
    ${JAVA_HOME}/bin/java -jar SystemConfigWizard.jar $*
fi

