#!/bin/sh


if  grep keystoretype /ssg/etc/conf/keystore.properties > /dev/null ; then
	echo -n ""
else
	echo "keystoretype=BCPKCS12" >> /ssg/etc/conf/keystore.properties
fi

perl -pi.bak -e 's/keystoreFile="(.*)" keystorePass="(.*)" \/>/keystoreFile="$1" keystorePass="$2" keystoreType="BCPKCS12" \/>/' /ssg/tomcat/conf/server.xml


