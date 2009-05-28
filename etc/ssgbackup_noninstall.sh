#!/bin/bash
# -----------------------------------------------------------------------------
# LAYER 7 TECHNOLOGIES
# May 2009
#
# Launches the SSG Backup Utility, from any directory where the SSGbackup.zip file is unzipped to
# -----------------------------------------------------------------------------
#

if [ ! -d "/opt/SecureSpan/Gateway" ]; then
    echo "SSG is not installed"
    exit 1
fi

REL_BASE_DIR=`pwd`

if [ -x /opt/SecureSpan/Gateway/runtime/etc/profile ]; then
    echo "Cannot execute /opt/SecureSpan/Gateway/runtime/etc/profile"
    exit 1
fi

SSGNODE="default"

# This will set the location of the jdk into SSG_JAVA_HOME.
. /opt/SecureSpan/Gateway/runtime/etc/profile

# This must be run as layer7
if [ $UID -eq 0 ]; then
    # invoke backup as layer7
    su layer7 -c "${SSG_JAVA_HOME}/bin/java -Xmx256m \
        -Dcom.l7tech.server.backuprestore.basedir=${REL_BASE_DIR} \
        -jar ${REL_BASE_DIR}/SSGBackupOnlyUtility.jar export $*"
elif [ "$USER" == "layer7" ]; then
    ${SSG_JAVA_HOME}/bin/java -Xmx256m \
        -Dcom.l7tech.server.backuprestore.basedir=${REL_BASE_DIR} \
        -jar ${REL_BASE_DIR}/SSGBackupOnlyUtility.jar export $*
else
    echo "Must be layer7 or root to invoke ssgbackup.sh"
    exit 1
fi
