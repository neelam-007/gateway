#!/bin/bash

#set the current working directory to where this script lives
cd `dirname $0`
pushd .. > /dev/null
SSG_ROOT=`pwd`
USER=$(whoami)
popd > /dev/null

JAVA_HOME=${SSG_ROOT}/jdk
launchtype=${1}

launch_wizard(){
	#check if we're root
	if [ "$USER" != "ssgconfig" ]; then
        	su -m ssgconfig -c "${JAVA_HOME}/bin/java -Djava.library.path=${SSG_ROOT}/lib -Dcom.l7tech.server.home=${SSG_ROOT} -jar ConfigWizard.jar $*"
    	else
        	${JAVA_HOME}/bin/java -Djava.library.path=${SSG_ROOT}/lib -Dcom.l7tech.server.home=${SSG_ROOT} -jar ConfigWizard.jar $*
    	fi
}

if [ -z "${launchtype}" ] || [ ${launchtype} == "-graphical" ]; then
    if [ -z "${DISPLAY}" ]; then
        echo "No DISPLAY variable set, running in console only mode"
        launchtype="-console"
    fi
fi
launch_wizard "${launchtype}"

