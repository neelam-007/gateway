#!/bin/bash

#set the current working directory to where this script lives
cd `dirname $0`
pushd .. > /dev/null
SSG_ROOT=`pwd`
popd > /dev/null

JAVA_HOME=${SSG_ROOT}/jdk
launchtype=${1}

launch_wizard(){
	#check if we're root
	if [ "$USER" != "ssgconfig" ]; then
        	su ssgconfig -c "${JAVA_HOME}/bin/java -Djava.library.path=${SSG_ROOT}/lib -Dcom.l7tech.server.home=${SSG_ROOT} -jar ConfigWizard.jar $*"
    	else
        	${JAVA_HOME}/bin/java -Djava.library.path=${SSG_ROOT}/lib -Dcom.l7tech.server.home=${SSG_ROOT} -jar ConfigWizard.jar $*
    	fi
}

if [ -z "${launchtype}" ] || [ ${launchtype} == "-graphical" ]; then
    #check if xhost exists
    which xhost &> /dev/null
    if [ $? -eq 0 ]; then
        #set the display to local 0.0 if there isn't already one
        if [ -z "${DISPLAY}" ]; then
            export DISPLAY=:0.0
        fi
        xhost +local: > /dev/null
    else
        #if there's no xhosts, then there's no X so force the console mode
	    echo "No graphical environment found, running in console only mode"
	    launchtype="-console"
    fi
    launch_wizard "${launchtype}"
else 
	launch_wizard "${launchtype}"
fi

