#!/bin/bash
# -----------------------------------------------------------------------------
# FILE [signsslcsr.sh]
# LAYER 7 TECHNOLOGIES
# $Id$
#
# THIS SCRIPT GENERATES A CERTIFICATE FOR SSL USE OF TOMCAT
#
# PREREQUISITES
# 1. A CSR FILE HAS BEEN EXPORTED FROM THE SSL KEYSTORE
# 2. THE ROOT KEYSTORE IS PRESENT AND AVAILABLE
#
# (optional) Command to import the certificate in the console store:
# keytool -import -v -trustcacerts -alias tomcat -file ssg.cer -keystore somedirectory/client.keystore -keypass changeit -storepass changeit
# Command to explicitely declare the trustStore on the console:
# System.setProperty("javax.net.ssl.trustStore", "somedirectory/client.keystore");
#
# -----------------------------------------------------------------------------
#

# VERIFY THAT THE TOMCAT_HOME VARIABLE IS SET
if [ ! "$TOMCAT_HOME" ]; then
        echo "ERROR: TOMCAT_HOME not set"
        echo
        exit 0
fi

# =================================================================================
KEYSTORE_DIR="$TOMCAT_HOME/kstores"
CSR_FILE="$TOMCAT_HOME/kstores/tomcatSsl.csr"
CERTIFICATE_FILE="$TOMCAT_HOME/kstores/ssl_rootcerted.cer"
WAR_FILE="$TOMCAT_HOME/webapps/ROOT.war"
ROOT_KEY_STORE="$TOMCAT_HOME/kstores/ssgroot"
ROOT_KEY_ALIAS=ssgroot
WEBAPPS_ROOT="$TOMCAT_HOME/webapps/ROOT"
# =================================================================================

# VERIFY THAT THE ROOT KEYSTORE IS PRESENT
if [ ! -e "$ROOT_KEY_STORE" ]; then
        echo "ROOT Key Store not found"
        exit
fi

# VERIFY THAT THE ROOT KSTORE PASSWORD IS PASSED. IF NOT, GET IT FROM CONSOLE
if [ ! "$1" ]; then
        # ax for the password
        echo "Please type in the root keystore password"
        read -s ROOT_KSTORE_PASSWORD
else
        ROOT_KSTORE_PASSWORD=$1
fi

# IF THE WAR FILE IS PRESENT BUT NOT YET EXPANDED, EXPAND IT
if [ -e "$WEBAPPS_ROOT" ]; then
        echo
else
        echo "expanding the war file..."
        unzip $WAR_FILE -d $WEBAPPS_ROOT
fi


# set a classpath for the execution of "com.l7tech.identity.cert.RSASigner"
# CP=$TMP_WAR_CONTENTS/WEB-INF/classes:$TMP_WAR_CONTENTS/WEB-INF/lib/bcprov-jdk14-119.jar:$TMP_WAR_CONTENTS/WEB-INF/lib/jaxrpc.jar:$TMP_WAR_CONTENTS/WEB-INF/lib/wsdl4j.jar
CP=$WEBAPPS_ROOT/WEB-INF/classes:$WEBAPPS_ROOT/WEB-INF/lib/bcprov-jdk14-119.jar:$WEBAPPS_ROOT/WEB-INF/lib/jaxrpc.jar:$WEBAPPS_ROOT/WEB-INF/lib/wsdl4j.jar

# do it
java -cp $CP com.l7tech.identity.cert.RSASigner $ROOT_KEY_STORE $ROOT_KSTORE_PASSWORD $ROOT_KEY_ALIAS $ROOT_KSTORE_PASSWORD $CSR_FILE $CERTIFICATE_FILE
