#!/bin/bash
# -----------------------------------------------------------------------------
# LAYER 7 TECHNOLOGIES
# November 2006
#
# Launches the SSG Migration Utility
# -----------------------------------------------------------------------------
#

if [ ! "$SSG_USER" ]; then
    SSG_USER=gateway.gateway
fi

# set the current working directory to where this script lives
cd `dirname $0`

# assume we're running from SSG_ROOT/migration and deduct SSG_ROOT from there
pushd .. > /dev/null
SSG_ROOT=`pwd`
popd > /dev/null

# assume location of the jdk
JAVA_HOME=${SSG_ROOT}/jdk

if [ "$1" == "cfgdeamon" ]; then
    ${JAVA_HOME}/bin/java -Dcom.l7tech.server.home=${SSG_ROOT} -jar SSGMigration.jar $*
    chown ${SSG_USER} ${SSG_ROOT}/migration/*
    chmod 666 *.log*
    exit
fi

# This must be run as ssgconfig
if [ $UID -eq 0 ]; then
    # invoke flasher as ssgconfig
    su ssgconfig -c "${JAVA_HOME}/bin/java -Dcom.l7tech.server.home=${SSG_ROOT} -jar SSGMigration.jar $*"
elif [ "$USER" == "ssgconfig" ]; then
    ${JAVA_HOME}/bin/java -Dcom.l7tech.server.home=${SSG_ROOT} -jar SSGMigration.jar $*
else
	echo "Must be ssgconfig to invoke ssgmigration.sh"
fi
