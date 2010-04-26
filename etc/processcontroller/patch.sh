#!/bin/bash
#
# Patch command line launcher script

THIS_SCRIPT=`basename $0`
if [ -z "${JAVA_HOME}" -a -d /opt/SecureSpan/JDK ] ; then
  JAVA_HOME="/opt/SecureSpan/JDK"
fi

if [ -z "${JAVA_HOME}" -a -n "${SSG_JAVA_HOME}" ] ; then
  JAVA_HOME="${SSG_JAVA_HOME}"
fi

DEFAULT_NODE_CONFIG="/opt/SecureSpan/Gateway/node/default/etc/conf/node.properties"
if [ -z "${JAVA_HOME}" -a -s "${DEFAULT_NODE_CONFIG}" ] ; then
  JAVA_HOME=$(grep node.java.path ${DEFAULT_NODE_CONFIG} | sed 's/node.java.path\s\{0,128\}=\s\{0,128\}//')
fi

PC_JAR="/opt/SecureSpan/Controller/Controller.jar"

function fail() {
  echo "$2"
  exit ${1}
}

[ -r "${PC_JAR}" ] || fail 2 "Missing or unreadable file: ${PC_JAR}"
[ -x "${JAVA_HOME}/bin/java" ] || fail 2 "Invalid JAVA_HOME: ${JAVA_HOME}"

JAVA_OPTS="${JAVA_OPTS}"
if [ -d "${JAVA_HOME}/jre/lib/ext" ] ; then
    JAVA_OPTS="${JAVA_OPTS} -Djava.ext.dirs=${JAVA_HOME}/jre/lib/ext"
else
    JAVA_OPTS="${JAVA_OPTS} -Djava.ext.dirs=${JAVA_HOME}/lib/ext"
fi

"${JAVA_HOME}/bin/java" -classpath "${PC_JAR}" ${JAVA_OPTS} com.l7tech.server.processcontroller.patching.client.PatchCli -scriptname "${THIS_SCRIPT}" "${@}"
