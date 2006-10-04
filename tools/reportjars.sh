#!/bin/bash
#############################################################################
#
# Script to build the Jars vX.Y Wiki page content
#
#############################################################################
#
# Creates a Wiki table suitable for reporting on usage of each JAR in the 
# libs directory.
#

#
# Sanity check
#
if [ ! -e "build.xml" ] || [ ! -d "lib" ] ; then
  echo "Please run from the root directory."
  exit 1
fi

if [ -z "$(ls -d installer/Bridge-*)" ] ; then
  echo "You need to run './build.sh package'."
  exit 1
fi

#
# Find JARs and ZIPs
#
JARS_AND_ZIPS=$(find lib -type f | grep -v CVS | egrep '.jar|.zip' | awk -F/ '{print $NF}' | sort -u)

#
# Determine version
#
VERSION="$(find installer -maxdepth 1 -name 'Bridge-*' -type d | head -1 | awk -F'-' '{print $NF}')"

#
# Build import list for compile time deps
#
# NOTE: this import list will contain entries like com/abc/* or com/abc/SomeClass
#
IMPORTS="$(find src -name '*.java' -type f -exec cat {} \; | grep '^import' | awk '{print $NF}' | awk -F';' '{print $1}' | sed 's/\./\//g' | sort -u | grep -v '^java/' | grep -v '^javax/xml/parsers' | grep -v '^javax/xml/transform' | grep -v '^javax/xml/xpath' | grep -v '^javax/swing' | grep -v '^javax/security' | grep -v '^javax/net' | grep -v '^org/w3c/dom' | grep -v '^org/xml/sax' | grep -v '^com/l7tech' | grep -v '^com/sun' | grep -v '^sun/')"

#
# JARs required for compile
#
COMPILE_JARS=""

#
# Create report
#
for JAR_OR_ZIP in ${JARS_AND_ZIPS}; do
  # Used at compile time?
  LIB_FILE=$(find lib -name "${JAR_OR_ZIP}" -type f | head -1)
  zipinfo -1 "${LIB_FILE}" > /tmp/jarzipinfo.txt
  FOR_COMPILE="n"
  for IMPORT in ${IMPORTS}; do
    IMPORT="${IMPORT%'*'}"
    grep -q "^${IMPORT}" /tmp/jarzipinfo.txt
    if [ "${?}" == "0" ] ; then
      FOR_COMPILE="y"
      COMPILE_JARS="${COMPILE_JARS}:${LIB_FILE}"
      break
    fi
  done 

  # Used in the gateway?
  zipinfo -1 "build/ROOT.war" | grep -q "WEB-INF/lib/${JAR_OR_ZIP}"
  if [ "${?}" == "0" ] ; then
    FOR_GATEWAY="y"
  else
    FOR_GATEWAY="n"
  fi

  # Used in the bridge?
  if [ -f "installer/Bridge-${VERSION}/lib/${JAR_OR_ZIP}" ] ; then
    FOR_BRIDGE="y"
  else
    FOR_BRIDGE="n"
  fi

  # Used in the manager?
  if [ -f "installer/Manager-${VERSION}/lib/${JAR_OR_ZIP}" ] ; then
    FOR_MANAGER="y"
  else
    FOR_MANAGER="n"
  fi

  # Report row for this file
  echo "| ${JAR_OR_ZIP} || || ${FOR_COMPILE} || ${FOR_GATEWAY} || ${FOR_BRIDGE} || ${FOR_MANAGER}"
  echo "|-"
done

echo "WARNING: The compile time requirement above is a best effort."
echo "         Some libs are required but not directly imported, e.g."
echo "         - lib/commons-logging-1.0.4.jar"
echo "         - lib/crypto/rsa/jsafeJCE.jar"
echo "         - lib/gateway/cluster/jboss-jmx.jar"
echo "         - lib/gateway/cluster/jboss-system.jar"
echo "It looks like you should be able to build with this classpath:"
echo ${COMPILE_JARS}
