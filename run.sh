#!/bin/bash
# run.sh -- set the required envionment and invoke the class
# passed as a parameter. The script includes all the jars from
# the $SRC_ROOT/lib
#
# JAVA_HOME - Determines the version of Java used.
# SRC_ROOT  - project root
SRC_ROOT="."
#Report environment settings
reportenv() {
    echo "SRC_ROOT=$SRC_ROOT"
    echo "JAVA_HOME=$JAVA_HOME"
    echo "PATH=$PATH"
    echo "CLASSPATH=$CLASSPATH"
}
#reportenv

# Under cygwin?.
cygwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
esac 

# Cygwin - switch paths to Unix .
if $cygwin; then
  CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
  SRC_ROOT=`cygpath --path --unix "$SRC_ROOT"`
  JAVA_HOME=`cygpath --path --unix "$JAVA_HOME"`
fi


if [ ! -e $JAVA_HOME/bin/java ]; then
  echo ""
  echo "The JDK wasn't found in directory ${JAVA_HOME}."
  echo "Please set your environment so that the JAVA_HOME variable "
  echo "variable refers to the location of your JDK."
  echo ""
  exit 1
fi

if [ ! -e $SRC_ROOT ]; then
  echo ""
  echo "The source tree wasn't found in directory ${SRC_ROOT}."
  echo "Please set your environment so that the SRC_ROOT variable "
  echo "variable refers to the location of your source tree."
  echo ""
  exit 1
fi


PATH=$JAVA_HOME/bin:$PATH; export PATH
JDK_CLASSES="$JAVA_HOME/lib/rt.jar:$JAVA_HOME/lib/tools.jar"
BUILD_CLASSES="$SRC_ROOT/build/classes"
BUILD_TESTCLASSES="$SRC_ROOT/build/test-classes"

for i in "$SRC_ROOT/lib"/*.jar
    do
    # if the directory is empty, then it will return the input string
    # this is stupid, so case for it
      if [ -f "$i" ] ; then
	if [ -z "$LOCALCLASSPATH" ] ; then
	  LOCALCLASSPATH="$i"
	else
	  LOCALCLASSPATH="$i":"$LOCALCLASSPATH"
    	fi
      fi
    done 
CLASSPATH=".:${JDK_CLASSES}:${BUILD_CLASSES}:${BUILD_TESTCLASSES}:${LOCALCLASSPATH}:${CLASSPATH}"
# Cygwin - switch paths to Windows format before running java
if $cygwin; then
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
fi
export CLASSPATH
#echo $CLASSPATH

foo=$1
shift

export JAVA_OPTS="-Djavax.xml.transform.TransformerFactory=org.apache.xalan.processor.TransformerFactoryImpl $JAVA_OPTS"

# determine locations for SSG and for installer builds
SSG_HOME="$(test -f ~/build.properties && grep "^deploy.dir" ~/build.properties | awk -F'=' '{print $NF}' | sed 's/ //g')"
if [ -z "${SSG_HOME}" ] ; then SSG_HOME="$(dirname $0)/build"; fi
INSTALLER_HOME="$(test -f ~/build.properties && grep "^build.installer" ~/build.properties | awk -F'=' '{print $NF}' | sed 's/ //g')"
if [ -z "${INSTALLER_HOME}" ] ; then INSTALLER_HOME="$(dirname $0)/build/installer"; fi


case "$foo" in 
	-?)
		echo "Usage: run.sh (ssg|console|proxy|textproxy|<Main class>)"
		;;
	ssg)
		# start tomcat?
		$TOMCAT_HOME/bin/startup.sh stop $* 2>&1 > /dev/null 
		exec $TOMCAT_HOME/bin/startup.sh start -security $* 
		;;
	console)
		exec ${INSTALLER_HOME}/Manager-*/Manager.sh $*
		;;
	manager)
		exec ${INSTALLER_HOME}/Manager-*/Manager.sh $*
		;;
	gateway)
		cd ${SSG_HOME}
		exec $JAVA_HOME/bin/java $JAVA_OPTS \
			-Djavax.xml.transform.TransformerFactory=org.apache.xalan.processor.TransformerFactoryImpl \
			-Dcom.l7tech.server.home=${SSG_HOME} \
			-Dcom.l7tech.server.runtime=${SSG_HOME} \
			-jar Gateway.jar $*
		;;
	textproxy)
		target="com.l7tech.proxy.Main";
		exec $JAVA_HOME/bin/java $* $JAVA_OPTS ${target} $*
		;;
	(bridge | client)
		exec ${INSTALLER_HOME}/Client-*/Client.sh $*
		;;
	testagent)
		target="com.l7tech.proxy.AgentPerfClient";
		exec $JAVA_HOME/bin/java $JAVA_OPTS ${target} $*
		;;
	configwizard)
		target="com.l7tech.server.config.ConfigurationWizardLauncher";
		exec $JAVA_HOME/bin/java $JAVA_OPTS ${target} $*
		;;
	*)
		exec $JAVA_HOME/bin/java $JAVA_OPTS $foo $*
		;;
esac 
    
