#!/bin/sh
# XML VPN Client Startup script for *nix systems


if [ -z "$JAVA_OPTS" ]; then
	# we don't have java opts, so we set them ourselves
	JAVA_OPTS=" -Xms128M -Xmx256M -Xss256k -server ";
elif [ `expr "$JAVA_OPTS" : ".*headless.*"` != 0 ]; then
       # We look in $JAVA_OPTS ... if java.awt.headless mode is there
       # then we've likely got the default options for SSG and it would prevent a gui
       # from coming up. So we over-write them with the following
       JAVA_OPTS=" -Xms128M -Xmx256M -Xss256k -server ";
fi

# set current dir to where this script is
cd `dirname $0`

# include startup options 
extra="-server -Dcom.l7tech.proxy.listener.maxthreads=300  -Dsun.net.inetaddr.ttl=10 -Dnetworkaddress.cache.ttl=10 -Dfile.encoding=UTF-8"

if [ "$1" = "-bd" ]; then
	run="-classpath Client.jar com.l7tech.client.Main"
elif [ "$1" = "-config" ]; then
    run="-classpath Client.jar com.l7tech.client.cli.Main"
else
	run="-jar Client.jar"
fi



if [ -z "$JAVA_HOME" ]; then
        echo "No JAVA_HOME set, exiting"
        exit;
fi
       
$JAVA_HOME/bin/java $JAVA_OPTS $extra $run
