#!/bin/sh


if  grep keystoretype /ssg/etc/conf/keystore.properties > /dev/null ; then
	echo -n ""
else
	echo "keystoretype=BCPKCS12" >> /ssg/etc/conf/keystore.properties
fi

perl -pi.bak -e 's/keystoreFile="(.*)" keystorePass="(.*)"\s*\/>/keystoreFile="$1" keystorePass="$2" keystoreType="BCPKCS12" \/>/i' /ssg/tomcat/conf/server.xml

TOMCAT_HOME="/ssg/tomcat"
WEB_XML="$TOMCAT_HOME/webapps/ROOT/WEB-INF/web.xml"
WEBAPPS_ROOT="$TOMCAT_HOME/webapps/ROOT"
WAR_FILE="$TOMCAT_HOME/webapps/ROOT.war"

# IF THE WAR FILE IS PRESENT BUT NOT YET EXPANDED, EXPAND IT
if [ -e "$WEB_XML" ]; then
    echo
else
    echo "Expanding $WAR_FILE..."
    unzip $WAR_FILE -d $WEBAPPS_ROOT > /dev/null
fi


