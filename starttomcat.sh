#!/usr/bin/sh
#cygwin translation mess
#JAVA_CMD=$JAVA_HOME/bin/java
#debug command line
script=`cygpath -w /cygdrive/c/src/adsl/script/runtomcat.xml`
# JVM_ARGS="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"
ant -buildfile $script \
    -Dtomcathome=$TOMCAT_HOME -Djvm=$JAVA_HOME/bin/java $@ &
    
