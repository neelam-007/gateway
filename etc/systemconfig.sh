#!/bin/bash

#set the current working directory to where this script lives
cd `dirname $0`
pushd .. > /dev/null
SSG_ROOT=`pwd`
popd > /dev/null

JAVA_HOME=${SSG_ROOT}/jdk
exec ${JAVA_HOME}/bin/java -Dcom.l7tech.server.home=${SSG_ROOT} -jar SystemConfigWizard.jar $*
