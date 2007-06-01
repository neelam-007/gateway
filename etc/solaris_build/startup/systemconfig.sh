#!/bin/bash

#set the current working directory to where this script lives
cd `dirname $0`
pushd .. > /dev/null
SSG_ROOT=`pwd`
popd > /dev/null

if [ $UID -eq 0 ]; then
    su ssgconfig -c "${JAVA_HOME}/bin/java -Dcom.l7tech.server.home=${SSG_ROOT} -jar SystemConfigWizard.jar $*"
else
    ${JAVA_HOME}/bin/java -Dcom.l7tech.server.home=${SSG_ROOT} -jar SystemConfigWizard.jar $*
fi

