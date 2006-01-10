#!/bin/bash

#set the current working directory to where this script lives
cd `dirname $0`
pushd ..
SSG_ROOT=`pwd`
popd

JAVA_HOME=${SSG_ROOT}/jdk
echo "using ${SSG_ROOT} as the SSG_ROOT\n"
exec ${JAVA_HOME}/bin/java -Djava.library.path=${SSG_ROOT}/lib -Dcom.l7tech.server.home=${SSG_ROOT} -jar ConfigWizard.jar
