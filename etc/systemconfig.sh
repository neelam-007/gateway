#!/bin/bash

#set the current working directory to where this script lives
cd `dirname $0`
pushd ..
SSG_ROOT=`pwd`
popd

JAVA_HOME=${SSG_ROOT}/jdk
echo "using ${SSG_ROOT} as the SSG_ROOT\n"
exec ${JAVA_HOME}/bin/java -Dcom.l7tech.server.home=${SSG_ROOT} -jar SystemConfigWizard.jar $*
