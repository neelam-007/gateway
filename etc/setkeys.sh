#!/bin/bash
# -----------------------------------------------------------------------------
# FILE [setkeys.sh]
# LAYER 7 TECHNOLOGIES
# $Id$
#
# GENERATES A CA KEYSTORE AND OR AN SSL KEYSTORE
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
    echo "ERROR: TOMCAT_HOME not set"
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

KEYSTORE_TYPE=BCPKCS12
WAR_FILE="$TOMCAT_HOME/webapps/ROOT.war"
SERVER_XML_FILE="$TOMCAT_HOME/conf/server.xml"
WEBAPPS_ROOT="$TOMCAT_HOME/webapps/ROOT"
WEB_XML="$TOMCAT_HOME/webapps/ROOT/WEB-INF/web.xml"

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

CA_KEYSTORE_FILE="$KEYSTORE_DIR/ca.ks"
SSL_KEYSTORE_FILE="$KEYSTORE_DIR/ssl.ks"

if $cygwin; then
KEYSTORE_PROPERTIES_FILE=`cygpath --path --windows $KEYSTORE_PROPERTIES_FILE`
KEYSTORE_DIR=`cygpath --path --windows $KEYSTORE_DIR`
SERVER_XML_FILE=`cygpath --path --windows $SERVER_XML_FILE`
WEBAPPS_ROOT=`cygpath --path --windows $WEBAPPS_ROOT`
WAR_FILE=`cygpath --path --windows $WAR_FILE`
fi

# -----------------------------------------------------------------------------
# FIND OUT WHAT THE USER WANTS TO DO
# -----------------------------------------------------------------------------
echo
echo "-----------------------------"
echo " 1. CREATE CA AND SSL KEYS "
echo " 2. CREATE SSL KEYS ONLY     "
echo "-----------------------------"
echo "(please choose)"
read MENU_CHOICE
if [ $MENU_CHOICE = "1" ]
then
    ANSWER_CAKEYS_CREATION="y"
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
dflt=`hostname -f`

echo "Please provide host name [$dflt]"
read HOST_NAME
if [ -e $HOST_NAME ]; then
    HOST_NAME=$dflt
fi

# IF WE START FROM SCRATCH, GET A BRAND NEW PASSWD
if [ $ANSWER_CAKEYS_CREATION = "y" ]
then
    # GET A NEW KEYSTORE PASSWORD FROM CALLER
    echo "Please choose a CA keystore password"
    read -s CA_KEYSTORE_PASSWORD
    echo "Please repeat"
    read -s CA_KEYSTORE_PASSWORD_REPEAT
    # VERIFY THAT PASSWORDS ARE EQUAL
    if [ ! "$CA_KEYSTORE_PASSWORD" = "$CA_KEYSTORE_PASSWORD_REPEAT" ]; then
        echo "ERROR : passwords do not match"
        exit
    fi
    # VERIFY THAT THE PASSWORD IS LONG ENOUGH
    CA_PASSWORD_LENGTH=${#CA_KEYSTORE_PASSWORD}
    if [ "$CA_PASSWORD_LENGTH" -lt 6 ]; then
        echo "ERROR : the CA keystore password must be at least 6 characters long"
        exit
    fi
else
    echo "Please type in the existing CA keystore password"
    read -s CA_KEYSTORE_PASSWORD
fi

# GET A NEW SSL KEYSTORE PASSWORD FROM CALLER
echo "Please choose an SSL keystore password"
read -s SSL_KEYSTORE_PASSWORD
echo "Please repeat"
read -s SSL_KEYSTORE_PASSWORD_REPEAT
# VERIFY THAT PASSWORDS ARE EQUAL
if [ ! "$SSL_KEYSTORE_PASSWORD" = "$SSL_KEYSTORE_PASSWORD_REPEAT" ]; then
    echo "ERROR : passwords do not match"
    exit
fi
# VERIFY THAT THE PASSWORD IS LONG ENOUGH
SSL_PASSWORD_LENGTH=${#SSL_KEYSTORE_PASSWORD}
if [ "$SSL_PASSWORD_LENGTH" -lt 6 ]; then
    echo "ERROR : the SSL keystore password must be at least 6 characters long"
    exit
fi

# ENSURE THAT THE DIRECTORY EXISTS
if [ ! -e "$KEYSTORE_DIR" ]; then
    mkdir "$KEYSTORE_DIR"
fi

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

# 3. all jars in tomcat/common/classpath
for filename in "$TOMCAT_HOME/common/classpath"/*.jar
do
  CP=$CP:$filename
done

if $cygwin; then
    CP=`cygpath --path --windows $CP`
fi

# -----------------------------------------------------------------------------
# DO THE THING
# -----------------------------------------------------------------------------
if [ $ANSWER_CAKEYS_CREATION = "y" ]; then
    setKeysClassname='com.l7tech.server.util.SetKeys$NewCa'
else
    setKeysClassname='com.l7tech.server.util.SetKeys$ExistingCa'
    echo "Skipping CA keys creation"
fi

# IF THE WAR FILE IS PRESENT BUT NOT YET EXPANDED, EXPAND IT
if [ -e "$WEB_XML" ]; then
    echo
else
    echo "Expanding $WAR_FILE..."
    unzip $WAR_FILE -d $WEBAPPS_ROOT > /dev/null
fi

# GENERATE THE KEYSTORES & CERTS
java -classpath "$CP" $setKeysClassname $HOST_NAME "$KEYSTORE_DIR" $CA_KEYSTORE_PASSWORD $SSL_KEYSTORE_PASSWORD $KEYSTORE_TYPE

# Edit the server.xml file so that the appropriate keystore location and paswords are remembered
KS_QUOTED_SLASHES=${SSL_KEYSTORE_FILE//\//\\\/}
echo "Updating <$SERVER_XML_FILE>..."
perl -pi.bak - "$SERVER_XML_FILE" <<!
s/(keystoreFile)="[^"]*"/\1="${KS_QUOTED_SLASHES}"/;
s/(keystoreType)="[^"]*"/\1="${KEYSTORE_TYPE}"/;
s/(keystorePass)=".*?"/\1="${SSL_KEYSTORE_PASSWORD}"/;
!


# -----------------------------------------------------------------------------
# REMEMBER THE KEYSTORE PASSWORDS IN THE KEYSTORE.PROPERTIES FILE
# -----------------------------------------------------------------------------
if [ -e "$KEYSTORE_PROPERTIES_FILE" ]; then
    echo "Updating <$KEYSTORE_PROPERTIES_FILE>..."

    SUBSTITUTE_FROM=rootcakspasswd=.*
    SUBSTITUTE_TO=rootcakspasswd=${CA_KEYSTORE_PASSWORD}
    perl -pi.bak -e s/$SUBSTITUTE_FROM/$SUBSTITUTE_TO/ "$KEYSTORE_PROPERTIES_FILE"

    SUBSTITUTE_FROM=sslkspasswd=.*
    SUBSTITUTE_TO=sslkspasswd=${SSL_KEYSTORE_PASSWORD}
    perl -pi.bak -e s/$SUBSTITUTE_FROM/$SUBSTITUTE_TO/ "$KEYSTORE_PROPERTIES_FILE"

    SUBSTITUTE_FROM=keystoretype=.*
    SUBSTITUTE_TO=keystoretype=${KEYSTORE_TYPE}
    perl -pi.bak -e s/$SUBSTITUTE_FROM/$SUBSTITUTE_TO/ "$KEYSTORE_PROPERTIES_FILE"
else
# INFORM THE USER OF THE FAILURE
    echo "ERROR"
    echo "The CA keystore password was not recorded because the properties file was not found."
    echo "This should be done manually"
    exit
fi

echo
echo "Done".
