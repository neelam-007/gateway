#!/bin/bash

# Ensure PATH is sane, reasonably minimal, and includes no directories under the user's control
PATH="/sbin:/usr/sbin:/bin:/usr/bin:/usr/X11R6/bin:/usr/local/bin"
export PATH

# Ensure JAVA_HOME is present
JAVA_HOME="/opt/SecureSpan/JDK"
export JAVA_HOME

#set the current working directory to where this script lives
umask 0002
whereami=$0
cd `dirname $whereami`

if [ ! -x "${JAVA_HOME}/bin/java" ] ; then
  echo "Java not found."
  exit 1
fi

# this puts the necessary "id" that supports -u first in the path on Solaris, and is a noop on other OSes
OLDPATH=$PATH
PATH="/usr/xpg4/bin:$PATH"
USERID=`id -u`
EMCONFIG_USERID=`id -u layer7`
PATH=$OLDPATH

launch(){
    #check who we are
    if [ "${USERID}" != "${EMCONFIG_USERID}" ]; then
        su layer7 -c "${JAVA_HOME}/bin/java ${OPTIONS} -jar ${1}.jar ${2}"
    else
        ${JAVA_HOME}/bin/java ${OPTIONS} -jar ${1}.jar ${2}
    fi

    return $?
}

launch "ConfigWizard" "$*"
