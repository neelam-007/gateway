#!/bin/bash
# -----------------------------------------------------------------------------
# LAYER 7 TECHNOLOGIES
# November 2006
#
# Launches the SSG Migration Utility
# -----------------------------------------------------------------------------
#

if [ ! "$SSG_USER" ]; then
    SSG_USER="gateway:gateway"
fi

# Saves current directory as base dir for all relative paths.
REL_BASE_DIR=`pwd`

# Assumes this script file is in the SSG_HOME/migration and deduce SSG_HOME from there.
pushd `dirname $0` > /dev/null
MIGRATION_HOME=`pwd`
cd ..
SSG_HOME=`pwd`
popd > /dev/null

# This will set the location of the jdk into SSG_JAVA_HOME.
. ${SSG_HOME}/etc/profile

# The migration Java app must be launched from the migration home folder.
cd ${MIGRATION_HOME}

if [ "$1" == "cfgdeamon" ]; then
    ${SSG_JAVA_HOME}/bin/java -Dcom.l7tech.server.home=${SSG_HOME} -Dcom.l7tech.server.flasher.basedir=${REL_BASE_DIR} -jar ${MIGRATION_HOME}/SSGMigration.jar $*
    chown ${SSG_USER} ${MIGRATION_HOME}/*
    chmod 666 *.log*
    exit
fi

# This must be run as ssgconfig
if [ $UID -eq 0 ]; then
    # invoke flasher as ssgconfig
    su ssgconfig -c "${SSG_JAVA_HOME}/bin/java -Dcom.l7tech.server.home=${SSG_HOME} -Dcom.l7tech.server.flasher.basedir=${REL_BASE_DIR} -jar ${MIGRATION_HOME}/SSGMigration.jar $*"
elif [ "$USER" == "ssgconfig" ]; then
    ${SSG_JAVA_HOME}/bin/java -Dcom.l7tech.server.home=${SSG_HOME} -Dcom.l7tech.server.flasher.basedir=${REL_BASE_DIR} -jar ${MIGRATION_HOME}/SSGMigration.jar $*
else
	echo "Must be ssgconfig to invoke ssgmigration.sh"
fi
