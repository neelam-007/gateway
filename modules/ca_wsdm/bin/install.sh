#!/bin/bash
#
# Copyright (C) 2006 Layer 7 Technologies Inc.
#
# Install script for Observer for CA Unicenter WSDM.

if [ `id -nu` != "root" ]; then
    echo "Must execute by user 'root'."
    echo "Installation of Observer for CA Unicenter WSDM failed."
    exit 1
fi

pushd "`dirname "$0"`" > /dev/null

SSG_HOME_DEFAULT=/ssg

read -p "SecureSpan Gateway home folder [${SSG_HOME_DEFAULT}]: " SSG_HOME
if [ -z "${SSG_HOME}" ]; then
    SSG_HOME="${SSG_HOME_DEFAULT}"
fi

GATEWAY_LIB="${SSG_HOME}/tomcat/webapps/ROOT/WEB-INF/lib"
GATEWAY_CONF="${SSG_HOME}/etc/conf"

# Verify that the SSG home folder exist.
if [ ! -d "${SSG_HOME}" ]; then
    echo '!! SecureSpan Gateway folder not found: '"${SSG_HOME}"
    echo '!! Please ensure the folder has not been moved or deleted.'
    exit 1
fi

# Edit WsdmSOMMA_Basic.properties.
if [ -e WsdmSOMMA_Basic.properties.EDIT ]; then rm WsdmSOMMA_Basic.properties.EDIT; fi
sed "s|^log.file.path=.*|log.file.path=${SSG_HOME}/logs/ca_wsdm_observer|" ssg/tomcat/webapps/ROOT/WEB-INF/classes/WsdmSOMMA_Basic.properties > WsdmSOMMA_Basic.properties.EDIT
mv WsdmSOMMA_Basic.properties.EDIT ssg/tomcat/webapps/ROOT/WEB-INF/classes/WsdmSOMMA_Basic.properties

# Copy files with forced overwrite.
cp -f ssg/tomcat/webapps/ROOT/WEB-INF/CaWsdmObserverContext.xml "${SSG_HOME}/tomcat/webapps/ROOT/WEB-INF/"
cp -f ssg/tomcat/webapps/ROOT/WEB-INF/lib/axis-1.3.jar "${SSG_HOME}/tomcat/webapps/ROOT/WEB-INF/lib/"
cp -f ssg/tomcat/webapps/ROOT/WEB-INF/lib/ca_wsdm_observer.jar "${SSG_HOME}/tomcat/webapps/ROOT/WEB-INF/lib/"
cp -f ssg/tomcat/webapps/ROOT/WEB-INF/lib/ca_wsdm-3.50-core.jar "${SSG_HOME}/tomcat/webapps/ROOT/WEB-INF/lib/"
cp -f ssg/tomcat/webapps/ROOT/WEB-INF/lib/ca_wsdm-3.50-handler_common.jar "${SSG_HOME}/tomcat/webapps/ROOT/WEB-INF/lib/"
cp -f ssg/tomcat/webapps/ROOT/WEB-INF/lib/ca_wsdm-3.50-wsdm35mmi-axis-stubskel.jar "${SSG_HOME}/tomcat/webapps/ROOT/WEB-INF/lib/"
cp -f ssg/tomcat/webapps/ROOT/WEB-INF/lib/tmxmltoolkit.jar "${SSG_HOME}/tomcat/webapps/ROOT/WEB-INF/lib/"

# Copy files without forced overwrite.
if [ -e "${SSG_HOME}/tomcat/webapps/ROOT/WEB-INF/classes/WsdmSOMMA_Basic.properties" ]; then
    echo "The file ${SSG_HOME}/tomcat/webapps/ROOT/WEB-INF/classes/WsdmSOMMA_Basic.properties already exists. It will not be overwritten."
else
    cp ssg/tomcat/webapps/ROOT/WEB-INF/classes/WsdmSOMMA_Basic.properties "${SSG_HOME}/tomcat/webapps/ROOT/WEB-INF/classes/"
fi

if [ -e "${SSG_HOME}/etc/conf/ca_wsdm_observer.properties" ]; then
    echo "The file ${SSG_HOME}/etc/conf/ca_wsdm_observer.properties already exists. It will not be overwritten."
else
    cp ssg/etc/conf/ca_wsdm_observer.properties "${SSG_HOME}/etc/conf/"
fi

chown -R gateway.gateway /ssg/*

echo "Installation of Observer for CA Unicenter WSDM complete."
echo "You may need to review and update the configuration in:"
echo "    ${SSG_HOME}/etc/conf/ca_wsdm_observer.properties"
echo "    ${SSG_HOME}/tomcat/webapps/ROOT/WEB-INF/classes/WsdmSOMMA_Basic.properties"

popd > /dev/null
