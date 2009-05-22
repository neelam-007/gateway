#!/bin/bash
# -----------------------------------------------------------------------------
# LAYER 7 TECHNOLOGIES
# November 2008
#
# Launches the SSG Restore Utility
# -----------------------------------------------------------------------------
#

# Saves current directory as base dir for all relative paths.
REL_BASE_DIR=`pwd`

# Assumes this script file is in the SSG_HOME/config/backup and deduce SSG_HOME from there.
pushd `dirname $0` > /dev/null
RESTORE_HOME=`pwd`
cd ../..
SSG_HOME=`pwd`
popd > /dev/null

#Set environment variables
SSGNODE="default"
GATEWAY_PID="${SSG_HOME}/node/${SSGNODE}/var/ssg.pid"

# Helper functions
pid_exited() {
  if [ -f "${GATEWAY_PID}" ]  && [ -d "/proc/$(< ${GATEWAY_PID})" ] ; then
    return 1;
  else
    return 0;
  fi
}

# Checks for the gateway pid file.
checkRunningLocalGateway() {
    if [ ! -z "${GATEWAY_PID}" ]; then
        if ! pid_exited ; then
            echo "Local gateway may be running.  Please ensure the local gateway is not running before proceeding."
            exit 33
        fi
    fi
}


# This will set the location of the jdk into SSG_JAVA_HOME.
. ${SSG_HOME}/runtime/etc/profile

# The backup Java app must be launched from the backup home folder.
cd ${RESTORE_HOME}

#
if [ "$1" == "cfgdeamon" ]; then
    ${SSG_JAVA_HOME}/bin/java \
        -Dcom.l7tech.server.home=${SSG_HOME} \
        -Dcom.l7tech.server.backuprestore.basedir=${REL_BASE_DIR} \
        -jar ${RESTORE_HOME}/SSGBackupUtility.jar cfgdeamon
    chown layer7.layer7 *.log
    chmod 644 *.log
    exit
fi

checkRunningLocalGateway

# This must be run as layer7
if [ $UID -eq 0 ]; then
    # invoke flasher as layer7
    su layer7 -c "${SSG_JAVA_HOME}/bin/java -Xmx256m \
        -Dcom.l7tech.server.home=${SSG_HOME} \
        -Dcom.l7tech.server.backuprestore.basedir=${REL_BASE_DIR} \
        -jar ${RESTORE_HOME}/SSGBackupUtility.jar import $*"
elif [ "$USER" == "layer7" ]; then
    ${SSG_JAVA_HOME}/bin/java -Xmx256m \
        -Dcom.l7tech.server.home=${SSG_HOME} \
        -Dcom.l7tech.server.backuprestore.basedir=${REL_BASE_DIR} \
        -jar ${RESTORE_HOME}/SSGBackupUtility.jar import $*
else
    echo "Must be layer7 to invoke ssgrestore.sh"
fi
