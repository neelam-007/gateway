#!/bin/bash

#set the current working directory to where this script lives
umask 0002
whereami=$0
cd `dirname $whereami`
pushd .. > /dev/null
SSG_ROOT=`pwd`
popd > /dev/null

. ${SSG_ROOT}/etc/profile

OLDPATH=$PATH
#this puts the necessary "id" that supports -u first in the path on Solaris, and is a noop on other OSes
PATH="/usr/xpg4/bin:$PATH"
USERID=`id -u`
SSGCONFIG_USERID=`id -u ssgconfig`
PATH=$OLDPATH

SCADIAG="/opt/sun/sca6000/sbin/scadiag"
OPTIONS="-Djava.library.path=${SSG_ROOT}/lib -Dcom.l7tech.server.home=${SSG_ROOT} -Djava.security.egd=file:/dev/./urandom"

check_options(){
    if [ -s "${SCADIAG}" ] && [ -s "${SSG_ROOT}/appliance/libexec/masterkey-manage.pl" ] ; then
            OPTIONS="$OPTIONS -Dcom.l7tech.server.keystore.enablehsm=true"
    fi
}

launch_wizard(){
	check_options
	#check who we are
	if [ "${USERID}" != "${SSGCONFIG_USERID}" ]; then
        do_command_as_user ssgconfig "${SSG_JAVA_HOME}/bin/java ${OPTIONS} -jar ConfigWizard.jar $*"
    else
        ${SSG_JAVA_HOME}/bin/java ${OPTIONS} -jar ConfigWizard.jar $*
    fi
}

ensure_JDK
launchtype=${1}
check_user

launch_wizard "$*"
