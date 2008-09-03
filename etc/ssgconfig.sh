#!/bin/bash

#set the current working directory to where this script lives
umask 0002
whereami=$0
cd `dirname $whereami`

# TODO [steve] fix JDK home
JAVA_HOME="/opt/SecureSpan/JDK"

# this puts the necessary "id" that supports -u first in the path on Solaris, and is a noop on other OSes
OLDPATH=$PATH
PATH="/usr/xpg4/bin:$PATH"
USERID=`id -u`
SSGCONFIG_USERID=`id -u ssgconfig`
PATH=$OLDPATH

launch_wizard(){
    #check who we are
    if [ "${USERID}" != "${SSGCONFIG_USERID}" ]; then
        su ssgconfig -c "${JAVA_HOME}/bin/java ${OPTIONS} -jar ConfigWizard.jar $*"
    else
        ${JAVA_HOME}/bin/java ${OPTIONS} -jar ConfigWizard.jar $*
    fi
}

launch_wizard "$*"
