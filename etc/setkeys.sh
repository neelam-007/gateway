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

# Under cygwin?.
cygwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
esac

if $cygwin; then
    TOMCAT_HOME=`cygpath --path --unix $TOMCAT_HOME`
fi

KEYTOOL="$JAVA_HOME/bin/keytool"
WAR_FILE="$TOMCAT_HOME/webapps/ROOT.war"
SERVER_XML_FILE="$TOMCAT_HOME/conf/server.xml"
WEBAPPS_ROOT="$TOMCAT_HOME/webapps/ROOT"
WEB_XML="$TOMCAT_HOME/webapps/ROOT/WEB-INF/web.xml"
ROOT_KEY_ALIAS=ssgroot


# -----------------------------------------------------------------------------
# SET PATHS DEPENDING ON DIRECTORY STRUCTURE
# -----------------------------------------------------------------------------
if [ -e "/ssg/etc/conf/keystore.properties" ]; then
    KEYSTORE_PROPERTIES_FILE=/ssg/etc/conf/keystore.properties
    KEYSTORE_DIR="/ssg/etc/keys"
else
    KEYSTORE_PROPERTIES_FILE="$TOMCAT_HOME/webapps/ROOT/WEB-INF/classes/keystore.properties"
    KEYSTORE_DIR="$TOMCAT_HOME/kstores"
fi

ROOT_KEYSTORE_FILE="$KEYSTORE_DIR/ca.ks"
ROOT_CERT_FILE="$KEYSTORE_DIR/ca.cer"
SSL_KEYSTORE_FILE="$KEYSTORE_DIR/ssl.ks"
SSL_SELF_CERT_FILE="$KEYSTORE_DIR/ssl_self.cer"
SSL_CSR_FILE="$KEYSTORE_DIR/ssl.csr"
SSL_CERT_FILE="$KEYSTORE_DIR/ssl.cer"

if $cygwin; then
KEYSTORE_PROPERTIES_FILE=`cygpath --path --windows $KEYSTORE_PROPERTIES_FILE`
KEYSTORE_DIR=`cygpath --path --windows $KEYSTORE_DIR`
ROOT_KEYSTORE_FILE=`cygpath --path --windows $ROOT_KEYSTORE_FILE`
ROOT_CERT_FILE=`cygpath --path --windows $ROOT_CERT_FILE`
SSL_KEYSTORE_FILE=`cygpath --path --windows $SSL_KEYSTORE_FILE`
SSL_SELF_CERT_FILE=`cygpath --path --windows $SSL_SELF_CERT_FILE`
SSL_CSR_FILE=`cygpath --path --windows $SSL_CSR_FILE`
SERVER_XML_FILE=`cygpath --path --windows $SERVER_XML_FILE`
WEBAPPS_ROOT=`cygpath --path --windows $WEBAPPS_ROOT`
SSL_CERT_FILE=`cygpath --path --windows $SSL_CERT_FILE`
WAR_FILE=`cygpath --path --windows $WAR_FILE`
fi

# -----------------------------------------------------------------------------
# FIND OUT WHAT THE USER WANTS TO DO
# -----------------------------------------------------------------------------
echo
echo "-----------------------------"
echo " 1. CREATE ROOT AND SSL KEYS "
echo " 2. CREATE SSL KEYS ONLY     "
echo "-----------------------------"
echo "(please choose)"
read MENU_CHOICE
if [ $MENU_CHOICE = "1" ]
then
    ANSWER_ROOTKEYS_CREATION="y"
    ANSWER_SSLKEYS_CREATION=y
elif [ $MENU_CHOICE = "2" ]
then
    ANSWER_SSLKEYS_CREATION=y
else
    echo "invalid choice"
    exit
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
    $KEYTOOL -genkey -alias ssgroot -dname $DN -v -keystore "$ROOT_KEYSTORE_FILE" -keyalg RSA -keypass $KEYSTORE_PASSWORD -storepass "$KEYSTORE_PASSWORD" -validity 730

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
$KEYTOOL -genkey -v -alias tomcat -dname $DN -keystore "$SSL_KEYSTORE_FILE" -keyalg RSA -keypass $KEYSTORE_PASSWORD -storepass "$KEYSTORE_PASSWORD" -validity 730
# CHECK THAT THIS KEYSTORE WAS SET SUCCESSFULLY
if [ -e "$SSL_KEYSTORE_FILE" ]
then
    # EXPORT THE SERVER CERTIFICATE
    $KEYTOOL -export -alias tomcat -storepass "$KEYSTORE_PASSWORD" -file "$SSL_SELF_CERT_FILE" -keystore "$SSL_KEYSTORE_FILE"

    # GENERATE A LOCAL CERTIFICATE SIGNING REQUEST
    $KEYTOOL -certreq -keyalg RSA -alias tomcat -file "$SSL_CSR_FILE" -keystore "$SSL_KEYSTORE_FILE" -storepass "$KEYSTORE_PASSWORD"

    # Edit the server.xml file so that the appropriate keystore location and paswords are remembered
    KS_QUOTED_SLASHES=${SSL_KEYSTORE_FILE//\//\\\/}
    perl -pi.bak -e s/keystoreFile=\".*\"/keystoreFile=\"${KS_QUOTED_SLASHES}\"\ keystorePass=\"$KEYSTORE_PASSWORD\"/ "$SERVER_XML_FILE"
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
if [ -e "$WEB_XML" ]; then
    echo
else
    unzip $WAR_FILE -d $WEBAPPS_ROOT
fi
if [ -e "$KEYSTORE_PROPERTIES_FILE" ]; then
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
# set a classpath for the execution of "com.l7tech.identity.cert.RsaCertificateSigner"
# 1. the location of our own code
if $cygwin; then
    WEBAPPS_ROOT=`cygpath --path --unix $WEBAPPS_ROOT`
fi

CP=$WEBAPPS_ROOT/WEB-INF/classes
# 2. all jars in project
for filename in "$WEBAPPS_ROOT/WEB-INF/lib"/*.jar
do
  CP=$CP:$filename
done

if $cygwin; then
    CP=`cygpath --path --windows $CP`
fi
echo $CP
# do it
java -cp $CP com.l7tech.identity.cert.RsaCertificateSigner $ROOT_KEYSTORE_FILE $KEYSTORE_PASSWORD $ROOT_KEY_ALIAS $KEYSTORE_PASSWORD $SSL_CSR_FILE $SSL_CERT_FILE

# CHECK IF THE CERT WAS CREATED
if [ -e "$SSL_CERT_FILE" ]; then
        $KEYTOOL -import -noprompt -file $ROOT_CERT_FILE -alias ssgroot -keystore $SSL_KEYSTORE_FILE -storepass $KEYSTORE_PASSWORD
        $KEYTOOL -import -noprompt -file $SSL_CERT_FILE -alias tomcat -keystore $SSL_KEYSTORE_FILE -storepass $KEYSTORE_PASSWORD
else
        # FAILURE
        echo "ERROR WILE SIGNING SSL CERT WITH ROOT CA"
fi

echo
echo "Done".
