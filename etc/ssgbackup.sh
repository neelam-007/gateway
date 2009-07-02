#!/bin/bash
# -----------------------------------------------------------------------------
# LAYER 7 TECHNOLOGIES
# November 2008
#
# Launches the SSG Backup Utility
# -----------------------------------------------------------------------------
#

# Saves current directory as base dir for all relative paths.
REL_BASE_DIR=`pwd`

# Assumes this script file is in the SSG_HOME/config/backup and deduce SSG_HOME from there.
pushd `dirname $0` > /dev/null
BACKUP_HOME=`pwd`
cd ../..
SSG_HOME=`pwd`
popd > /dev/null

if [ -z "${SSGNODE}" ] ; then
    SSGNODE="default"
fi

# This will set the location of the jdk into SSG_JAVA_HOME.
. ${SSG_HOME}/runtime/etc/profile

# The backup Java app must be launched from the backup home folder.
cd ${BACKUP_HOME}

# This must be run as layer7
if [ $UID -eq 0 ]; then
    # invoke backup as layer7
    su layer7 -c "${SSG_JAVA_HOME}/bin/java -Xmx256m \
        -Dcom.l7tech.server.home=${SSG_HOME} \
        -Dcom.l7tech.server.backuprestore.basedir=${REL_BASE_DIR} \
        -jar ${BACKUP_HOME}/SSGBackupUtility.jar export $*"
elif [ "$USER" == "layer7" ]; then
    ${SSG_JAVA_HOME}/bin/java -Xmx256m \
        -Dcom.l7tech.server.home=${SSG_HOME} \
        -Dcom.l7tech.server.backuprestore.basedir=${REL_BASE_DIR} \
        -jar ${BACKUP_HOME}/SSGBackupUtility.jar export $*
elif [ "$USER" == "gateway" ]; then
    ${SSG_JAVA_HOME}/bin/java -Xmx256m \
        -Dcom.l7tech.server.home=${SSG_HOME} \
        -Dcom.l7tech.server.backuprestore.basedir=${REL_BASE_DIR} \
        -Djava.util.logging.config.file=backupgatewaylogging.properties \
        -jar ${BACKUP_HOME}/SSGBackupUtility.jar export $*
else
    echo "Must be layer7 to invoke ssgbackup.sh"
    exit 1
fi
