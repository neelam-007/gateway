#!/bin/bash
# -----------------------------------------------------------------------------
# LAYER 7 TECHNOLOGIES
# November 2006
#
# Launches the SSG Migration Utility
# -----------------------------------------------------------------------------
#

# Saves current directory as base dir for all relative paths.
REL_BASE_DIR=`pwd`

# Assumes this script file is in the SSG_HOME/migration and deduce SSG_HOME from there.
pushd `dirname $0` > /dev/null
MIGRATION_HOME=`pwd`
cd ../..
SSG_HOME=`pwd`
popd > /dev/null

# This will set the location of the jdk into SSG_JAVA_HOME.
. ${SSG_HOME}/runtime/etc/profile

# The migration Java app must be launched from the migration home folder.
cd ${MIGRATION_HOME}

if [ "$1" == "cfgdeamon" ]; then
    ${SSG_JAVA_HOME}/bin/java -Dcom.l7tech.server.home=${SSG_HOME} -Dcom.l7tech.server.flasher.basedir=${REL_BASE_DIR} -jar ${MIGRATION_HOME}/SSGMigration.jar $*
    chown layer7.layer7 *.log
    chmod 644 *.log
    exit
fi

# This must be run as layer7
if [ $UID -eq 0 ]; then
    # invoke flasher as layer7
    su layer7 -c "${SSG_JAVA_HOME}/bin/java -Xmx256m -Dcom.l7tech.server.home=${SSG_HOME} -Dcom.l7tech.server.flasher.basedir=${REL_BASE_DIR} -jar ${MIGRATION_HOME}/SSGMigration.jar $*"
elif [ "$USER" == "layer7" ] || [ "${1}" == "export"  ]; then
    ${SSG_JAVA_HOME}/bin/java -Xmx256m -Dcom.l7tech.server.home=${SSG_HOME} -Dcom.l7tech.server.flasher.basedir=${REL_BASE_DIR} -jar ${MIGRATION_HOME}/SSGMigration.jar $*
else
    echo "Must be layer7 to invoke ssgmigration.sh"
fi
