#!/bin/bash
# run.sh -- set the required envionment and invoke the class
# passed as a parameter. The script includes all the jars from
# the $SRC_ROOT/lib
#
# JAVA_HOME - Determines the version of Java used.
# SRC_ROOT  - project root

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
CLASSPATH="${JDK_CLASSES}:${BUILD_CLASSES}:${LOCALCLASSPATH}"
# Cygwin - switch paths to Windows format before running java
if $cygwin; then
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
fi
export CLASSPATH
$JAVA_HOME/bin/java $@
    
