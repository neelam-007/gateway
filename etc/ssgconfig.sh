#!/bin/bash

#set the current working directory to where this script lives
umask 0002
whereami=$0
cd `dirname $whereami`
pushd .. > /dev/null
SSG_ROOT=`pwd`
popd > /dev/null

. ${SSG_ROOT}/etc/profile

USER=${LOGNAME}
SCADIAG="/opt/sun/sca6000/sbin/scadiag"
OPTIONS="-Djava.library.path=${SSG_ROOT}/lib -Dcom.l7tech.server.home=${SSG_ROOT}"

check_options(){
    if [ -s "${SCADIAG}" ] && [ -s "${SSG_ROOT}/appliance/libexec/masterkey-manage.pl" ] ; then
            OPTIONS="$OPTIONS -Dcom.l7tech.server.keystore.enablehsm=true"
    fi
}

launch_wizard(){
	check_options
	#check if we're root
	if [ "$USER" != "ssgconfig" ]; then
        do_command_as_user ssgconfig "${SSG_JAVA_HOME}/bin/java ${OPTIONS} -jar ConfigWizard.jar $*"
    else
        ${SSG_JAVA_HOME}/bin/java ${OPTIONS} -jar ConfigWizard.jar $*
    fi
}

ensure_JDK
launchtype=${1}
check_user

if [ "${launchtype}z" == "-exportsharedkeyz" ] || [ "${launchtype}z" == "-changeMasterPassphrasez" ]; then
    do_command_as_user ssgconfig "${SSG_JAVA_HOME}/bin/java ${OPTIONS} -jar ConfigWizard.jar $*"
else
    launch_wizard "${launchtype}"
fi
