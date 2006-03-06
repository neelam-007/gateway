#!/bin/bash
#############################################################################
#
# Script to check and update SSG configuration depending on environment.
#
#############################################################################
#
# $Id$
# 

# Check valid environment
#
echo "INFO: SSG_HOME   : ${SSG_HOME}"
echo "INFO: TOMCAT_HOME: ${TOMCAT_HOME}"
if [ -z "${TOMCAT_HOME}" ] || [ -z "${SSG_HOME}" ] ; then
    echo "You must set TOMCAT_HOME and SSG_HOME to run this script."
    exit 1
fi

# Check hibernate package names
#
HIB_OLD='net.sf.'
HIB_NEW='org.'
HIB2JAR='hibernate2.jar'
HIB3JAR='hibernate3.jar'

if [ -e "${TOMCAT_HOME}/webapps/ROOT/WEB-INF/lib/${HIB2JAR}" ] && \
   [ -e "${TOMCAT_HOME}/webapps/ROOT/WEB-INF/lib/${HIB3JAR}" ] ; then
    echo "WARNING: Multiple versions of Hibernate found (2 and 3)"
elif [ -e "${TOMCAT_HOME}/webapps/ROOT/WEB-INF/lib/${HIB2JAR}" ] ; then
    grep -q "${HIB_NEW}" "${SSG_HOME}/etc/conf/hibernate.properties"
    if [ ${?} -eq 0 ] ; then
      echo "INFO: Reverting Hibernate configuration file to 2.x format."
      sed "s/${HIB_NEW}/${HIB_OLD}/g" "${SSG_HOME}/etc/conf/hibernate.properties" > "/tmp/hibernate.properties.editing"
      if [ ${?} -eq 0 ] ; then
        mv -f "/tmp/hibernate.properties.editing" "${SSG_HOME}/etc/conf/hibernate.properties"
      fi  
    fi 
elif  [ -e "${TOMCAT_HOME}/webapps/ROOT/WEB-INF/lib/${HIB3JAR}" ] ; then
    grep -q "${HIB_OLD}" "${SSG_HOME}/etc/conf/hibernate.properties"
    if [ ${?} -eq 0 ] ; then
      echo "INFO: Updating Hibernate configuration file to 3.x format."
      sed "s/${HIB_OLD}/${HIB_NEW}/g" "${SSG_HOME}/etc/conf/hibernate.properties" > "/tmp/hibernate.properties.editing"
      if [ ${?} -eq 0 ] ; then
        mv -f "/tmp/hibernate.properties.editing" "${SSG_HOME}/etc/conf/hibernate.properties"
      fi  
    fi 
else
    echo "WARNING: Could not find relevant Hibernate version (2 or 3)"
fi
[ ! -e "/tmp/hibernate.properties.editing" ] || rm -f "/tmp/hibernate.properties.editing" 

