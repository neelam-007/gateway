#!/bin/sh
# BatchLicenseGenerator Startup script for *nix systems

# set JAVA_OPTS
JAVA_OPTS="-server -Dfile.encoding=UTF-8 -Xms256M -Xmx256M ";

# set current dir to where this script is

cd `dirname $0`

# include startup options 
keystore="-DlicenseGenerator.keystorePath=/home/jwilliams/testBatchLicenseGeneratorKey.p12 -DlicenseGenerator.keystorePass=password"

run="-jar BatchLicenseGenerator.jar";

$JAVA_HOME/bin/java $JAVA_OPTS $extra $keystore $run "$@"
