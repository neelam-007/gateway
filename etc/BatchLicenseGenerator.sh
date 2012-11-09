#!/bin/sh
# BatchLicenseGenerator Startup script for *nix systems

if [ `expr "$JAVA_OPTS" : ".*headless.*"` != 0 ]; then
	# We look in $JAVA_OPTS ... if we've done java.awt.headless mode 
        # then we've likely got the default options for SSG and it would prevent a gui
        # from coming up. So we over-write them with the following
	JAVA_OPTS=" -Xms96M -Xmx96M -Xss256k -server -XX:NewSize=48M -XX:MaxNewSize=48M ";
fi

# if we don't have an L7_OPTS and we DO have a java opts, 
# e.g. from above code or from the user's environment
# we set the l7opts to be equal to java opts

if [ "$L7_OPTS" = "" -a "$JAVA_OPTS" != "" ]; then
	L7_OPTS=$JAVA_OPTS
fi

# set current dir to where this script is

cd `dirname $0`

# include startup options 
extra="-server -Dfile.encoding=UTF-8"
keystore="-DlicenseGenerator.keystorePath=/home/jwilliams/testBatchLicenseGeneratorKey.p12 -DlicenseGenerator.keystorePass=password"

run="-jar BatchLicenseGenerator.jar";

$JAVA_HOME/bin/java $L7_OPTS $extra $keystore $run "$@"
