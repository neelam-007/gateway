#!/bin/bash
# -----------------------------------------------------------------------------
# FILE [setkeys.sh]
# LAYER 7 TECHNOLOGIES
# $Id$
#
# GENERATES A ROOT KEYSTORE AND OR AN SSL KEYSTORE
#
# PREREQUISITES
# 1. THE DIRECTORY JAVA_HOME/bin IS PART OF THE PATH
# 2. TOMCAT_HOME is set
# 3. server.xml has already been copied to the tomcat_home/conf/ directory and contains
#   Server/Service/Connector/Factory@keystorePass attribute
# 4. ROOT.war present in $TOMCAT_HOME/webapps
#
# -----------------------------------------------------------------------------
#

# VERIFY THAT THE TOMCAT_HOME VARIABLE IS SET
if [ ! "$TOMCAT_HOME" ]; then
    echo "ERROR: $TOMCAT_HOME not set"
    echo
    exit
fi

KEYSTORE_DIR="$TOMCAT_HOME/kstores"
KEYTOOL="$JAVA_HOME/bin/keytool"
ROOT_KEYSTORE_FILE="$KEYSTORE_DIR/ca.ks"
ROOT_CERT_FILE="$TOMCAT_HOME/kstores/ca.cer"
SSL_KEYSTORE_FILE="$KEYSTORE_DIR/ssl.ks"
SSL_SELF_CERT_FILE="$TOMCAT_HOME/kstores/ssl_self.cer"
SSL_CSR_FILE="$TOMCAT_HOME/kstores/ssl.csr"
SERVER_XML_FILE="$TOMCAT_HOME/conf/server.xml"
KEYSTORE_PROPERTIES_FILE="$TOMCAT_HOME/webapps/ROOT/WEB-INF/classes/keystore.properties"
WEBAPPS_ROOT="$TOMCAT_HOME/webapps/ROOT"
ROOT_KEY_ALIAS=ssgroot
SSL_CERT_FILE="$TOMCAT_HOME/kstores/ssl.cer"
WAR_FILE="$TOMCAT_HOME/webapps/ROOT.war"

# -----------------------------------------------------------------------------
# FIND OUT WHAT THE USER WANTS TO DO
# -----------------------------------------------------------------------------
echo
echo
echo "Do you want to create the root keys? (y/n)"
read ANSWER_ROOTKEYS_CREATION
if [ $ANSWER_ROOTKEYS_CREATION = "y" ]
then
    ANSWER_SSLKEYS_CREATION=y
else
    echo
    echo "Do you want to create the ssl keys? (y/n)"
    read ANSWER_SSLKEYS_CREATION
    if [ $ANSWER_SSLKEYS_CREATION = "y" ]
    then
            echo
    else
        echo "Nothing to do"
        exit
    fi
fi

# -----------------------------------------------------------------------------
# GET A HOSTNAME AND A PASSWORD
# -----------------------------------------------------------------------------
echo "Please provide host name"
read HOST_NAME
# IF WE START FROM SCRATCH, GET A BRAN NEW PASSWD
if [ $ANSWER_ROOTKEYS_CREATION = "y" ]
then
    # GET A NEW KEYSTORE PASSWORD FROM CALLER
    echo "Please choose a keystore password"
    read -s KEYSTORE_PASSWORD
    echo "Please repeat"
    read -s KEYSTORE_PASSWORD_REPEAT
    # VERIFY THAT PASSWORDS ARE EQUAL
    if [ ! "$KEYSTORE_PASSWORD" = "$KEYSTORE_PASSWORD_REPEAT" ]; then
        echo "ERROR : passwords do not match"
        exit
    fi
    # VERIFY THAT THE PASSWORD IS LONG ENOUGH
    PASSWORD_LENGTH=${#KEYSTORE_PASSWORD}
    if [ "$PASSWORD_LENGTH" -lt 6 ]; then
        echo "ERROR : the keystore password must be at least 6 characters long"
        exit
    fi
else
    echo "Please type in the existing keystore password"
    read -s KEYSTORE_PASSWORD
fi

# ENSURE THAT THE DIRECTORY EXISTS
if [ ! -e "$KEYSTORE_DIR" ]; then
    mkdir "$KEYSTORE_DIR"
fi

# -----------------------------------------------------------------------------
# DO THE ROOT KEY CREATION
# -----------------------------------------------------------------------------
if [ $ANSWER_ROOTKEYS_CREATION = "y" ]
then
    # SET THE DN
    DN="CN="root.$HOST_NAME
    # IF THE KEYSTORE EXIST, DELETE IT
    if [ -e "$ROOT_KEYSTORE_FILE" ]; then
        rm "$ROOT_KEYSTORE_FILE"
    fi
    # GENERATE THE KEYSTORE
    $KEYTOOL -genkey -alias ssgroot -dname $DN -v -keystore "$ROOT_KEYSTORE_FILE" -keyalg RSA -keypass $KEYSTORE_PASSWORD -storepass "$KEYSTORE_PASSWORD"

    # CHECK THAT THIS KEYSTORE WAS SET SUCCESSFULLY
    if [ -e "$ROOT_KEYSTORE_FILE" ]
    then
    # EXPORT THE SERVER CERTIFICATE
        $KEYTOOL -export -alias ssgroot -storepass "$KEYSTORE_PASSWORD" -file "$ROOT_CERT_FILE" -keystore "$ROOT_KEYSTORE_FILE"
    else
    # INFORM THE USER OF THE FAILURE
        echo "ERROR: The root keystore file was not generated"
        echo
        exit
    fi
else
    echo "Skipping root keys creation"
fi

# -----------------------------------------------------------------------------
# DO THE SSL KEY CREATION
# -----------------------------------------------------------------------------
# SET THE DN
DN="CN="$HOST_NAME
# GENERATE THE KEYSTORE
    # IF THE KEYSTORE EXIST, DELETE IT
    if [ -e "$SSL_KEYSTORE_FILE" ]; then
        rm "$SSL_KEYSTORE_FILE"
    fi
$KEYTOOL -genkey -v -alias tomcat -dname $DN -keystore "$SSL_KEYSTORE_FILE" -keyalg RSA -keypass $KEYSTORE_PASSWORD -storepass "$KEYSTORE_PASSWORD"
# CHECK THAT THIS KEYSTORE WAS SET SUCCESSFULLY
if [ -e "$SSL_KEYSTORE_FILE" ]
then
    # EXPORT THE SERVER CERTIFICATE
    $KEYTOOL -export -alias tomcat -storepass "$KEYSTORE_PASSWORD" -file "$SSL_SELF_CERT_FILE" -keystore "$SSL_KEYSTORE_FILE"

    # GENERATE A LOCAL CERTIFICATE SIGNING REQUEST
    $KEYTOOL -certreq -keyalg RSA -alias tomcat -file "$SSL_CSR_FILE" -keystore "$SSL_KEYSTORE_FILE" -storepass "$KEYSTORE_PASSWORD"

    # EDIT THE server.xml file so that the existing value is replaced by the actual password
    echo "recording the password in tomcat's server.xml"
    perl -pi.bak -e s/keystorePass=\".*\"/keystorePass=\"$KEYSTORE_PASSWORD\"/ "$SERVER_XML_FILE"
else
    # INFORM THE USER OF THE FAILURE
    echo "ERROR: The ssl keystore file was not generated"
    echo
    exit
fi


# -----------------------------------------------------------------------------
# REMEMBER THE KEYSTORE PASSWORDS IN THE KEYSTORE.PROPERTIES FILE
# -----------------------------------------------------------------------------
# IF THE WAR FILE IS PRESENT BUT NOT YET EXPANDED, EXPAND IT
if [ -e "$KEYSTORE_PROPERTIES_FILE" ]; then
    echo
else
    echo "expanding the war file so that properties can be updated with kstore password"
    unzip $WAR_FILE -d $WEBAPPS_ROOT
fi
if [ -e "$KEYSTORE_PROPERTIES_FILE" ]; then
    echo "Recording the keystore passwords"
    SUBSTITUTE_FROM=rootcakspasswd=.*
    SUBSTITUTE_TO=rootcakspasswd=${KEYSTORE_PASSWORD}
    perl -pi.bak -e s/$SUBSTITUTE_FROM/$SUBSTITUTE_TO/ "$KEYSTORE_PROPERTIES_FILE"
    SUBSTITUTE_FROM=sslkspasswd=.*
    SUBSTITUTE_TO=sslkspasswd=${KEYSTORE_PASSWORD}
    perl -pi.bak -e s/$SUBSTITUTE_FROM/$SUBSTITUTE_TO/ "$KEYSTORE_PROPERTIES_FILE"
else
# INFORM THE USER OF THE FAILURE
    echo "ERROR"
    echo "The root keystore password was not recorded because the properties file was not found."
    echo "This should be done manually"
    exit
fi

# -----------------------------------------------------------------------------
# SIGN A SSL CERT WITH THE ROOT CERT
# -----------------------------------------------------------------------------
# set a classpath for the execution of "com.l7tech.identity.cert.RSASigner"
# 1. the location of our own code
CP=$WEBAPPS_ROOT/WEB-INF/classes
# 2. all jars in project
for filename in $WEBAPPS_ROOT/WEB-INF/lib/*.jar
do
  CP=$CP:$filename
done

# do it
java -cp $CP com.l7tech.identity.cert.RSASigner $ROOT_KEYSTORE_FILE $KEYSTORE_PASSWORD $ROOT_KEY_ALIAS $KEYSTORE_PASSWORD $SSL_CSR_FILE $SSL_CERT_FILE

# CHECK IF THE CERT WAS CREATED
if [ -e "$SSL_CERT_FILE" ]; then
        echo "Importing ssl cert back into ssl keystore"
        $KEYTOOL -import -file $ROOT_CERT_FILE -alias ssgroot -keystore $SSL_KEYSTORE_FILE -storepass $KEYSTORE_PASSWORD
        $KEYTOOL -import -file $SSL_CERT_FILE -alias tomcat -keystore $SSL_KEYSTORE_FILE -storepass $KEYSTORE_PASSWORD
else
        # FAILURE
        echo "ERROR WILE SIGNING SSL CERT WITH ROOT CA"
fi

echo
echo "Done".
