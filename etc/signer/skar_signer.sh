#!/bin/bash
#
# Script to launch the SkarSigner.jar
#

THIS_DIR=$(dirname $0)

# Under cygwin?.
cygwin=false;
case "$(uname)" in
  CYGWIN*) cygwin=true ;;
esac

# For Cygwin, switch paths to Unix .
if ${cygwin}; then
  THIS_DIR=$(cygpath --path --unix "${THIS_DIR}")
  JAVA_HOME=$(cygpath --path --unix "${JAVA_HOME}")
fi

source ${THIS_DIR}/jdk_utils.sh

#
# Run client
#
"${JAVA_HOME}/bin/java" ${JAVA_OPTS} -jar "${THIS_DIR}/SkarSigner.jar" "$@"
