#!/bin/bash

#set the current working directory to where this script lives
umask 0002
cd `dirname $0`
pushd .. > /dev/null
SSG_ROOT=`pwd`
USER=${LOGNAME}
SCADIAG="/opt/sun/sca6000/sbin/scadiag"
popd > /dev/null

JAVA_HOME=${SSG_ROOT}/jdk
launchtype=${1}

OPTIONS="-Djava.library.path=${SSG_ROOT}/lib -Dcom.l7tech.server.home=${SSG_ROOT}"

check_options(){
    if [ -s "${SCADIAG}" ] ; then
        OPTIONS="$OPTIONS -Dcom.l7tech.server.keystore.enablehsm=true"
    fi
}

launch_wizard(){
	check_options
	#check if we're root
	if [ "$USER" != "ssgconfig" ]; then
        	su -m ssgconfig -c "${JAVA_HOME}/bin/java ${OPTIONS} -jar ConfigWizard.jar $*"
    	else
        	${JAVA_HOME}/bin/java ${OPTIONS} -jar ConfigWizard.jar $*
    	fi
}

if [ "${launchtype}z" == "-exportsharedkeyz" ] || [ "${launchtype}z" == "-changeMasterPassphrasez" ]; then
    ${JAVA_HOME}/bin/java ${OPTIONS} -jar ConfigWizard.jar $*
else
    launch_wizard "${launchtype}"
fi


