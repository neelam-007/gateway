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
        exit -1
fi

# VERIFY THAT THE ROOT KSTORE PASSWORD IS PASSED
if [ ! "$1" ]; then
        echo "ERROR: you must provide the root keystore password as argument"
        echo
        exit -1
fi

# =================================================================================
KEYSTORE_DIR="$TOMCAT_HOME/kstores"
CSR_FILE="$TOMCAT_HOME/kstores/tomcatSsl.csr"
CERTIFICATE_FILE="$TOMCAT_HOME/kstores/ssl_rootcerted.cer"
TMP_WAR_CONTENTS="$TOMCAT_HOME/kstores/tmp"
WAR_FILE="$TOMCAT_HOME/webapps/ROOT.war"
ROOT_KEY_STORE="$TOMCAT_HOME/kstores/ssgroot"
ROOT_KEY_ALIAS=ssgroot
# =================================================================================

# extract our java stuff from war file
mkdir $TMP_WAR_CONTENTS
unzip $WAR_FILE -d $TMP_WAR_CONTENTS

# set a classpath for the execution of "com.l7tech.identity.cert.RSASigner"
CP=$TMP_WAR_CONTENTS/WEB-INF/classes:$TMP_WAR_CONTENTS/WEB-INF/lib/bcprov-jdk14-119.jar:$TMP_WAR_CONTENTS/WEB-INF/lib/jaxrpc.jar:$TMP_WAR_CONTENTS/WEB-INF/lib/wsdl4j.jar

# do it
java -cp $CP com.l7tech.identity.cert.RSASigner $ROOT_KEY_STORE $1 $ROOT_KEY_ALIAS $1 $CSR_FILE $CERTIFICATE_FILE

# clean tmp directory
rm -R $TMP_WAR_CONTENTS