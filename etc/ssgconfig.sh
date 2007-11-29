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
    if [ -s "${SCADIAG}" ] && [ -s "${SSG_ROOT}/appliance/bin/masterkey-manage.pl" ] ; then
            OPTIONS="$OPTIONS -Dcom.l7tech.server.keystore.enablehsm=true"
    fi
}

launch_wizard(){
	check_options
	#check if we're root
	if [ "$USER" != "ssgconfig" ]; then
        su -m ssgconfig -c "${SSG_JAVA_HOME}/bin/java ${OPTIONS} -jar ConfigWizard.jar -partitionMigrate &>/dev/null && ${SSG_JAVA_HOME}/bin/java ${OPTIONS} -jar ConfigWizard.jar $*"
    else
        su -m ssgconfig -c "${SSG_JAVA_HOME}/bin/java ${OPTIONS} -jar ConfigWizard.jar -partitionMigrate &>/dev/null"
        ${SSG_JAVA_HOME}/bin/java ${OPTIONS} -jar ConfigWizard.jar $*
    fi
}

ensure_JDK
launchtype=${1}
check_user

if [ "${launchtype}z" == "-exportsharedkeyz" ] || [ "${launchtype}z" == "-changeMasterPassphrasez" ]; then
    su -m ssgconfig -c "${SSG_JAVA_HOME}/bin/java ${OPTIONS} -jar ConfigWizard.jar -partitionMigrate &>/dev/null"
    ${SSG_JAVA_HOME}/bin/java ${OPTIONS} -jar ConfigWizard.jar $*
else
    launch_wizard "${launchtype}"
fi
