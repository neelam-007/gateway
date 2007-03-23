#!/bin/bash

#set the current working directory to where this script lives
cd `dirname $0`
pushd .. > /dev/null
SSG_ROOT=`pwd`
USER=$(whoami)
popd > /dev/null

JAVA_HOME=${SSG_ROOT}/jdk

launch_wizard(){
	#get root creds for updates
	echo "Package updates to the SSG require the root password."
	su -c "${JAVA_HOME}/bin/java -jar UpdateWizard.jar $*"
}

launch_wizard
