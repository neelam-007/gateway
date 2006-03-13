#!/bin/bash
# build -- Build Script for UneasyRooster
#
# TOMCAT_HOME   - This must point to the home directory of your Tomcat
#             instance.
# JAVA_HOME - Determines the version of Java used to compile the build.

# If you have problems sprinkle calls to this around the code and
# run the script.
#
SRC_ROOT=`pwd`

reportenv()
{
    echo "SRC_ROOT=$SRC_ROOT"
    echo "TOMCAT_HOME=$TOMCAT_HOME"
    echo "JAVA_HOME=$JAVA_HOME"
    echo "PATH=$PATH"
    echo "CLASSPATH=$CLASSPATH"
}

# Under cygwin?.
cygwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
esac 

# For Cygwin, switch paths to Unix .
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

PATH="$JAVA_HOME/bin:$PATH"; export PATH
JDK_CLASSES="$JAVA_HOME/lib/rt.jar:$JAVA_HOME/lib/tools.jar"
if [ ! -e "$TOMCAT_HOME/bin/startup.sh" ]; then
    echo ""
    echo "The Tomcat Server wasn't found in directory ${TOMCAT_HOME}."
    echo "Please set your environment so that the TOMCAT_HOME variable "
    echo "refers to the Tomcat home installation directory."
    echo ""
    exit 1
fi

if [ ! -e $SRC_ROOT/build.sh ]; then
    echo ""
    echo "The source root wasn't found in directory ${SRC_ROOT}."
    echo "Please set your environment so that the SRC_ROOT variable "
    echo "variable refers to the valid source root."
    echo ""
    exit 1
fi

OPTIONS_PROPS=""
OPTIONS_ARGS=""
for arg in "$@" ; do
  if [ "${arg}" == "${arg#-D}" ] ; then
    OPTIONS_ARGS="${OPTIONS_ARGS} ${arg}"
  else
    OPTIONS_PROPS="${OPTIONS_PROPS} ${arg}"
  fi
done

ANT_JARS="$SRC_ROOT/lib/ant.jar:$SRC_ROOT/lib/optional.jar:$SRC_ROOT/lib/xercesImpl.jar:$SRC_ROOT/lib/xml-apis.jar:$SRC_ROOT/lib/junit.jar:$SRC_ROOT/lib/ant-contrib.jar"
CLASSPATH="${JDK_CLASSES}:${ANT_JARS}"
# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
  SRC_ROOT=`cygpath --path --windows "$SRC_ROOT"`
fi
export CLASSPATH

OPTIONS_PERF=""
if [ -z "${ANT_PERFORMANCE}" ] || [ "yes" = "${ANT_PERFORMANCE}" ] || [ "true" = "${ANT_PERFORMANCE}" ] ; then
  OPTIONS_PERF="-listener net.sf.antcontrib.perf.AntPerformanceListener"
fi

"${JAVA_HOME}/bin/java" ${JAVA_OPTS} ${OPTIONS_PROPS} org.apache.tools.ant.Main ${OPTIONS_PERF} ${OPTIONS_ARGS}
RESULT=${?}
if [ "${1}" == "package" ] || [ "${1}" == "compile" ] ; then
  if [ ${RESULT} -eq 0 ] ; then
    echo -e "BUILD SUCCESSFUL\n"
  else 
    echo -e "BUILD FAILED\n"
  fi
fi
exit ${RESULT}
