#!/bin/bash
# -----------------------------------------------------------------------------
# FILE [deploy.sh]
# LAYER 7 TECHNOLOGIES
# $Id$
#
# DEPLOYS THE SERVER USING DISTRIBUTION FILES PRODUCED BY BUILD
#
# PREREQUISITES
#
# -----------------------------------------------------------------------------
#

# VERIFY THAT THE TOMCAT_HOME VARIABLE IS SET, TRY DEFAULT VALUE IF NOT
if [ ! "$TOMCAT_HOME" ]; then
    TOMCAT_HOME=/usr/java/tomcat-4.1.27-l7p2
    if [ ! "$TOMCAT_HOME" ]; then
        echo "ERROR: $TOMCAT_HOME not set"
        echo
        exit
    fi
    export TOMCAT_HOME
fi

# VERIFY THAT THE JAVA_HOME VARIABLE IS SET, TRY DEFAULT VALUE IF NOT
if [ ! "$JAVA_HOME" ]; then
    JAVA_HOME=/usr/java/j2sdk1.4.1
    if [ ! "$JAVA_HOME" ]; then
        echo "ERROR: $JAVA_HOME not set"
        echo
        exit
    fi
    export JAVA_HOME
fi

# RESOLVE THE DIRECTORY WHERE THESE SCRIPTS RESIDE
PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
PRGDIR=`dirname "$PRG"`

# LOOK FOR THE ROOT.WAR FILE
# see if it was passwd as argument
if [ "$1" ]; then
    if [ -e "$1" ]; then
        ROOT_WAR_PATH=$1
    fi
fi
if [ ! "$ROOT_WAR_PATH" ]; then
    ROOT_WAR_PATH=$PRGDIR/ROOT.war
    if [ ! -e "$ROOT_WAR_PATH" ]; then
        echo "could not find ROOT.war"
        echo "usage ${0} the_root_war_path"
        exit
    fi
fi

# MAKE SURE THIS HAS NOT BEEN DEPLOYED ALREADY
if [ -e "$TOMCAT_HOME/webapps/ROOT" ]; then
    echo "TOMCAT ROOT APPLICATION ALREADY EXISTS, USE REDEPLOY OR CLEAN EXISTING DEPLOYMENT"
    exit
fi

# EXPLODE THE WAR FILE IN THE TOMCAT DIR
unzip $ROOT_WAR_PATH -d $TOMCAT_HOME/webapps/ROOT

# -----------------------------------------------------------------------------
# HIBERNATE STUFF
# -----------------------------------------------------------------------------
HIBERNATE_PROPERTIES_PATH=$TOMCAT_HOME/webapps/ROOT/WEB-INF/classes/hibernate.properties
# EDIT hibernate.properties
# hibernate.connection.driver_class = org.postgresql.Driver
# hibernate.connection.url = jdbc:postgresql://localhost/ssg
# hibernate.connection.username = gateway
# hibernate.connection.password = prompt_for_password
echo "todo edit ${HIBERNATE_PROPERTIES_PATH}"

# COPY APPROPRIATE SSG.hbm.xml
echo "todo copy SSG.hbm.xml"
