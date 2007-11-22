#!/bin/bash
# LAYER 7 TECHNOLOGIES
# Defines JAVA_HOME, etc

cd `dirname $0`
pushd .. > /dev/null
SSG_HOME=`pwd`
popd > /dev/null

default_java_opts="-server -Dcom.l7tech.common.http.prov.apache.CommonsHttpClient.maxConnectionsPerHost=750 -Djava.net.preferIPv4Stack=true "
default_java_opts="$default_java_opts -Dsun.rmi.dgc.server.gcInterval=3600000 -Dsun.rmi.dgc.client.gcInterval=3600000 "
default_java_opts="$default_java_opts -Dcom.l7tech.common.http.prov.apache.CommonsHttpClient.maxTotalConnections=7500"
default_java_opts="$default_java_opts -Djavax.xml.transform.TransformerFactory=org.apache.xalan.processor.TransformerFactoryImpl"
default_java_opts="$default_java_opts -Dfile.encoding=UTF-8 -Dsun.net.inetaddr.ttl=30 "
default_java_opts="$default_java_opts -Djava.awt.headless=true -XX:CompileThreshold=1500 "
default_java_opts="$default_java_opts -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger"

# Set rmi hostname for cluster correctness
if  [ -e "/ssg/etc/conf/partitions/default_/cluster_hostname" ];
then
        default_java_opts="$default_java_opts -Djava.rmi.server.hostname=`cat /ssg/etc/conf/partitions/default_/cluster_hostname`"
else
        default_java_opts="$default_java_opts -Djava.rmi.server.hostname=`hostname -f`"
fi

SSG_JAVA_OPTS=$default_java_opts;

ALL_PARTITIONS=`ls ${SSG_HOME}/etc/conf/partitions/ | grep -v template_`
PARTITION_COUNT=`echo ${ALL_PARTITIONS} | wc -w`

unset default_java_opts

export SSG_HOME
export SSG_JAVA_OPTS
export ALL_PARTITIONS
export PARTITION_COUNT

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$SSG_HOME/lib

