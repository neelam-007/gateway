#!/usr/bin/sh
script=`cygpath -w /cygdrive/c/src/adsl/script/runtomcat.xml`
echo $script
$ANT_HOME/bin/ant -buildfile $script stop \
    -Dtomcathome=$TOMCAT_HOME -Djvm=$JAVA_HOME/bin/java
