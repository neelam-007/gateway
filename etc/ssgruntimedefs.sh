#!/bin/sh
# /etc/profile.d/ssgruntimedefs.sh
# LAYER 7 TECHNOLOGIES
# Defines JAVA_HOME, TOMCAT_HOME, etc
# at the moment

SSG_HOME=/ssg

TOMCAT_HOME=/ssg/tomcat

default_java_opts="-server -Dcom.l7tech.common.http.prov.apache.CommonsHttpClient.maxConnectionsPerHost=750 -Djava.net.preferIPv4Stack=true "
default_java_opts="$default_java_opts -Dsun.rmi.dgc.server.gcInterval=3600000 -Dsun.rmi.dgc.client.gcInterval=3600000 "
default_java_opts="$default_java_opts -Dcom.l7tech.common.http.prov.apache.CommonsHttpClient.maxTotalConnections=7500"
default_java_opts="$default_java_opts -Djavax.xml.transform.TransformerFactory=org.apache.xalan.processor.TransformerFactoryImpl"
default_java_opts="$default_java_opts -Dfile.encoding=UTF-8 -Dsun.net.inetaddr.ttl=30 "
default_java_opts="$default_java_opts -Djava.awt.headless=true -XX:CompileThreshold=1500 "
default_java_opts="$default_java_opts -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger"


if [ -e /ssg/etc/conf/JVM ]; then
	# this means that if /ssg/etc/conf/JVM exists
	JAVA_HOME=`cat /ssg/etc/conf/JVM`
	# we use its contents to override the supplied JDK
else
	JAVA_HOME="/ssg/jdk"
fi

ulimit -s 2048

# add to path
PATH=$PATH:$JAVA_HOME/bin:$SSG_HOME/bin

# Set rmi hostname for cluster correctness

if  [ -e "/ssg/etc/conf/cluster_hostname" ];
then
        default_java_opts="$default_java_opts -Djava.rmi.server.hostname=`cat /ssg/etc/conf/cluster_hostname`"
else
        default_java_opts="$default_java_opts -Djava.rmi.server.hostname=`hostname -f`"
fi


# aliases to start and stop ssg
alias startssg='/etc/rc.d/init.d/ssg start'
alias stopssg='/etc/rc.d/init.d/ssg stop'



if [ -e  /usr/local/Tarari ]; then
	export TARARIROOT=/usr/local/Tarari
	export PATH=$TARARIROOT/bin:$PATH
	export LD_LIBRARY_PATH=$TARARIROOT/lib:$LD_LIBRARY_PATH
	export JAVA_OPTS="-Dcom.l7tech.common.xml.tarari.enable=true $JAVA_OPTS"
fi


if [ -e "/opt/oracle" ] ; then
	export ORACLE_HOME=/opt/oracle/product/9.2
	export PATH=$PATH:$ORACLE_HOME/bin
	export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$ORACLE_HOME/lib
	export NLS_LANG=AMERICAN_AMERICA.US7ASCII
fi

JAVA_OPTS=$default_java_opts;

unset default_java_opts


export SSG_HOME
export JAVA_HOME
export TOMCAT_HOME
export JAVA_OPTS
export CATALINA_PID

# define tomcat home
export PATH
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$SSG_HOME/lib


