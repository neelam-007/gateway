#!/bin/bash
# -----------------------------------------------------------------------------
# FILE [sslGen.sh]
# LAYER 7 TECHNOLOGIES
# $Id$
#
# THIS SCRIPT GENERATES A KEYSTORE FOR SSL USE OF TOMCAT, GENERATES A CSR FILE SO THAT A PROPER CERT CAN BE
# CREATED AND UPDATES $TOMCAT_HOME/conf/server.xml so that it knows the kstore password.
#
# PREREQUISITES
# 1. THE DIRECTORY JAVA_HOME/bin IS PART OF THE PATH
# 2. TOMCAT_HOME is set
# 3. server.xml has already been copied to the tomcat_home/conf/ directory and contains
#   Server/Service/Connector/Factory@keystorePass attribute
#
# POSTREQUISITES:
# 1. Once the keystore and csr files are generated, the signsslcsr.sh script will create the actual ssl cert.
#
# (optional) Command to import the certificate in the console store:
# keytool -import -v -trustcacerts -alias tomcat -file ssg.cer -keystore somedirectory/client.keystore -keypass changeit -storepass changeit
# Command to explicitely declare the trustStore on the console:
# System.setProperty("javax.net.ssl.trustStore", "somedirectory/client.keystore");
#
# -----------------------------------------------------------------------------
#

# Under cygwin?.
cygwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
esac

# For Cygwin, get the Windows paths for keytool.
TOMCAT_HOME_OSPATH="$TOMCAT_HOME"
if $cygwin; then
  TOMCAT_HOME_OSPATH=`cygpath --path --windows "$TOMCAT_HOME"`
fi

KEYTOOL="$JAVA_HOME/bin/keytool"
KEYSTORE_DIR="$TOMCAT_HOME/kstores"
KEYSTORE_DIR_OSPATH="$TOMCAT_HOME_OSPATH/kstores"
KEYSTORE_FILE="$KEYSTORE_DIR/ssl.ks"
KEYSTORE_FILE_OSPATH="$KEYSTORE_DIR_OSPATH/ssl.ks"
CERTIFICATE_FILE="$TOMCAT_HOME/kstores/ssl_self.cer"
CERTIFICATE_FILE_OSPATH="$TOMCAT_HOME_OSPATH/kstores/ssl_self.cer"
CSR_FILE_OSPATH="$TOMCAT_HOME_OSPATH/kstores/ssl.csr"
SERVER_XML_FILE="$TOMCAT_HOME/conf/server.xml"
WAR_FILE="$TOMCAT_HOME/webapps/ROOT.war"
WEBAPPS_PATH="$TOMCAT_HOME/webapps"
KEYSTORE_PROPERTIES_FILE="$TOMCAT_HOME/webapps/ROOT/WEB-INF/classes/keystore.properties"

# VERIFY THAT THE TOMCAT_HOME VARIABLE IS SET
if [ ! "$TOMCAT_HOME" ]; then
        echo "ERROR: TOMCAT_HOME not set"
        echo
        exit -1
fi

# VERIFY THAT THE KEYSTORE IS NOT YET SET
if [ -e "$KEYSTORE_FILE" ]; then
        echo "THE KEYSTORE ALREADY EXIST."
        echo "DO YOU WANT TO OVERWRITE? [y/n]"
        read ADMIN_ANSWER
        if [ $ADMIN_ANSWER = "n" ]
        then
                exit -1
                echo
        else
                /bin/rm "$KEYSTORE_FILE"
        fi
fi

# ENSURE THAT THE DIRECTORY EXISTS
mkdir "$KEYSTORE_DIR"

# GET A KEYSTORE PASSWORD FROM CALLER
echo "Please provide an keystore password"
read -s KEYSTORE_PASSWORD
echo "Please repeat"
read -s KEYSTORE_PASSWORD_REPEAT

# VERIFY THAT PASSWORDS ARE EQUAL
if [ ! "$KEYSTORE_PASSWORD" = "$KEYSTORE_PASSWORD_REPEAT" ]; then
	echo "ERROR : passwords do not match"
	exit -1
fi

# VERIFY THAT THE PASSWORD IS LONG ENOUGH
PASSWORD_LENGTH=${#KEYSTORE_PASSWORD}
if [ "$PASSWORD_LENGTH" -lt 6 ]; then
	echo "ERROR : the admin password must be at least 6 characters long"
	exit -1
fi

# ASK FOR THE HOST NAME
echo "Please type in the host name"
read HOSTNAME
DN="CN="$HOSTNAME

# GENERATE THE KEYSTORE
$KEYTOOL -genkey -v -alias tomcat -dname $DN -keystore "$KEYSTORE_FILE_OSPATH" -keyalg RSA -keypass $KEYSTORE_PASSWORD -storepass "$KEYSTORE_PASSWORD"

# CHECK THAT THIS KEYSTORE WAS SET SUCCESSFULLY
if [ -e "$KEYSTORE_FILE" ]
then
        # EXPORT THE SERVER CERTIFICATE
        $KEYTOOL -export -alias tomcat -storepass "$KEYSTORE_PASSWORD" -file "$CERTIFICATE_FILE_OSPATH" -keystore "$KEYSTORE_FILE_OSPATH"

        # SHOW THE CERTIFICATE
        $KEYTOOL -printcert -file "$CERTIFICATE_FILE_OSPATH"

        # GENERATE A LOCAL CERTIFICATE SIGNING REQUEST
        $KEYTOOL -certreq -keyalg RSA -alias tomcat -file "$CSR_FILE_OSPATH" -keystore "$KEYSTORE_FILE_OSPATH" -storepass "$KEYSTORE_PASSWORD"

        # EDIT THE server.xml file so that the magic value "__FunkySsgMojo__" is replaced by the actual password
        echo "recording the password in tomcat's server.xml"
        perl -pi.bak -e s/keystorePass=\".*\"/keystorePass=\"$KEYSTORE_PASSWORD\"/ "$SERVER_XML_FILE"

        # RECORD THE PASSWORD IN KEYSTORE.PROPERTIES
        echo "recording the password in properties file"
        # IF THE WAR FILE IS PRESENT BUT NOT YET EXPANDED, EXPAND IT
        if [ -e "$KEYSTORE_PROPERTIES_FILE" ]; then
                echo
        else
                echo "expanding the war file so that properties can be updated with kstore password"
                unzip $WAR_FILE -d $WEBAPPS_PATH/ROOT
        fi

        if [ -e "$KEYSTORE_PROPERTIES_FILE" ]; then
                echo "Recording the keystore password"
                SUBSTITUTE_FROM=sslkspasswd=.*
                SUBSTITUTE_TO=sslkspasswd=${KEYSTORE_PASSWORD}
                perl -pi.bak -e s/$SUBSTITUTE_FROM/$SUBSTITUTE_TO/ "$KEYSTORE_PROPERTIES_FILE"
                echo "Recording the location of the root cert"
        else
        # INFORM THE USER OF THE FAILURE
                echo "ERROR"
                echo "The keystore password was not recorded because the properties file was not found."
                echo "This should be done manually"
        fi
else
        # INFORM THE USER OF THE FAILURE
        echo "ERROR: The keystore file was not generated"
        echo
        exit 255
fi
