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
DEPLOY_HOME="$(test -f ~/build.properties && grep "^dev.deploy.dir" ~/build.properties | awk -F'=' '{print $NF}' | sed 's/ //g')"
if [ -z "${DEPLOY_HOME}" ] ; then
  if [ "$(dirname $0)" == "." ] ; then 
    DEPLOY_HOME="$(pwd)/build/deploy"
  else 
    DEPLOY_HOME="$(dirname $0)/build/deploy"
  fi
fi
SSG_HOME="${DEPLOY_HOME}/Gateway"
ESM_HOME="${DEPLOY_HOME}/EnterpriseManager"
PC_HOME="${DEPLOY_HOME}/Controller"
INSTALLER_HOME="$(test -f ~/build.properties && grep "^build.installer" ~/build.properties | awk -F'=' '{print $NF}' | sed 's/ //g')"
if [ -z "${INSTALLER_HOME}" ] ; then INSTALLER_HOME="$(dirname $0)/build/installer"; fi


case "$foo" in 
	-?)
		echo "Usage: run.sh (gateway|console|bridge|textproxy|esm|<Main class>)"
		;;
	(console | manager | ssm)
		exec ${INSTALLER_HOME}/Manager-*/Manager.sh $*
		;;
	(gateway | ssg)
		cd ${SSG_HOME}
		exec $JAVA_HOME/bin/java $JAVA_OPTS \
			-Djavax.xml.transform.TransformerFactory=org.apache.xalan.processor.TransformerFactoryImpl \
                        -Dcom.l7tech.server.home=${SSG_HOME}/node/default \
			-jar runtime/Gateway.jar $*
		;;
	(enterprisemanager | esm)
		cd ${ESM_HOME}
		exec $JAVA_HOME/bin/java $JAVA_OPTS \
                        -Dcom.l7tech.server.log.console=true \
			-Dcom.l7tech.ems.outputDbScript=overwrite \
			-Dcom.l7tech.ems.development=true \
			-jar EnterpriseManager.jar $*
		;;
	(controller | pc | processcontroller)
		cd ${PC_HOME}
		exec $JAVA_HOME/bin/java $JAVA_OPTS \
                        -Dcom.l7tech.server.log.console=true \
			 -Dcom.l7tech.gateway.home=../../Gateway \
			-jar Controller.jar
		;;
	textproxy)
		target="com.l7tech.proxy.Main";
		exec $JAVA_HOME/bin/java $* $JAVA_OPTS ${target} $*
		;;
	(bridge | client | xvc)
		exec ${INSTALLER_HOME}/Client-*/Client.sh $*
		;;
	testagent)
		target="com.l7tech.proxy.AgentPerfClient";
		exec $JAVA_HOME/bin/java $JAVA_OPTS ${target} $*
		;;
	configwizard)
		cd ${SSG_HOME}/config
		exec $JAVA_HOME/bin/java -jar ConfigWizard.jar
		;;
	*)
		exec $JAVA_HOME/bin/java $JAVA_OPTS $foo $*
		;;
esac 
    
