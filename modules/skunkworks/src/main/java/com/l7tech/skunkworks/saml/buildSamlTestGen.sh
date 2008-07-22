#!/bin/bash
# build -- Build Script for SAML Generator Test Tool
#
# JAVA_HOME - Determines the version of Java used to compile the build.

# If you have problems sprinkle calls to this around the code and
# run the script.
#
SRC_ROOT=`pwd`

reportenv()
{
    echo "SRC_ROOT=$SRC_ROOT"
    echo "JAVA_HOME=$JAVA_HOME"
    echo "PATH=$PATH"
    echo "CLASSPATH=$CLASSPATH"
}

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

ANT_JARS="$SRC_ROOT/lib/ant.jar:$SRC_ROOT/lib/ant-launcher.jar:$SRC_ROOT/lib/ant-nodeps.jar:$SRC_ROOT/lib/xercesImpl.jar:$SRC_ROOT/lib/xml-apis.jar:$SRC_ROOT/lib/tools/junit-4.4.jar:$SRC_ROOT/lib/ant-junit.jar:$SRC_ROOT/lib/ant-contrib-1.0b3.jar:$SRC_ROOT/lib/xalan-2.5.2.jar:$SRC_ROOT/lib/ant-apache-bcel.jar:$SRC_ROOT/lib/tools/ant-trax.jar:$SRC_ROOT/lib/tools/bcel-5.2.jar"
CLASSPATH="${JDK_CLASSES}:${ANT_JARS}"
export CLASSPATH

OPTIONS_PERF=""
if [ -z "${ANT_PERFORMANCE}" ] || [ "yes" = "${ANT_PERFORMANCE}" ] || [ "true" = "${ANT_PERFORMANCE}" ] ; then
  OPTIONS_PERF="-listener net.sf.antcontrib.perf.AntPerformanceListener"
fi

echo ""
echo "Be sure to run './build.sh compile-test-forms' prior to packaging."
echo ""
echo "1) ./build.sh compile-test-forms"
echo "2) ./tests/com/l7tech/skunkworks/saml/buildSamlTestGen.sh"
echo ""
echo "Run using java -jar SamlTestTool.jar (the SecureSpan Bridge 'libs' directory is required)"
echo ""
sleep 3

"${JAVA_HOME}/bin/java" -Dtomcat.home=$TOMCAT_HOME ${JAVA_OPTS} ${OPTIONS_PROPS} org.apache.tools.ant.Main -f tests/com/l7tech/skunkworks/saml/buildSamlTestGen.xml ${OPTIONS_PERF} ${OPTIONS_ARGS}
RESULT=${?}
if [ "${1}" == "package" ] || [ "${1}" == "compile" ] ; then
  if [ ${RESULT} -eq 0 ] ; then
    echo -e "BUILD SUCCESSFUL\n"
  else 
    echo -e "BUILD FAILED\n"
  fi
fi
exit ${RESULT}
