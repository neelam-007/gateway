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

if [ $UID -eq 0 ]; then
    # set the current working directory to where this script lives
    cd `dirname $0`
    # assume we're running from SSG_ROOT/flasher and deduct SSG_ROOT from there
    pushd .. > /dev/null
    SSG_ROOT=`pwd`
    popd > /dev/null
    # assume location of the jdk
    JAVA_HOME=${SSG_ROOT}/jdk
    # invoke flasher
    ${JAVA_HOME}/bin/java -Dcom.l7tech.server.home=${SSG_ROOT} -jar SSGMigration.jar $*
    # chown all files that could potentially have been overwritten
    chown -R $SSG_USER ..
else
    echo "Must be root to invoke ssgmigration.sh"
fi
