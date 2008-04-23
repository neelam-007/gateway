#!/bin/sh
# runs ant

export JAVA_HOME=/ssg/jdk/
echo "Using JAVA_HOME: $JAVA_HOME"

export ANT_HOME=./tools/ant
echo "Using ANT_HOME: $ANT_HOME"

$ANT_HOME/ant build.configwiz

cp cfg_data.xml /ssg/configwizard/.
cd /ssg/configwizard
chown ssgconfig.gateway cfg_data.xml
chown ssgconfig.gateway AutoConfig.jar

# change the ssgconfig password so we don't get prompted when running our script
echo "qwerQWER1234!@#$" | passwd ssgconfig --stdin >/dev/null

su -m ssgconfig -c "$JAVA_HOME/bin/java -jar AutoConfig.jar"

