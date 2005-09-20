#!/bin/bash
# -----------------------------------------------------------------------------
# FILE [setkeys.sh]
# LAYER 7 TECHNOLOGIES
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

java_version_15=`$JAVA_HOME/bin/java -version 2>&1 | grep 1.5`; 
if  [ "$java_version_15" ]; then
	KEYSTORE_TYPE=PKCS12
else
	KEYSTORE_TYPE=BCPKCS12
fi
echo "INFO: keystore type set to $KEYSTORE_TYPE"

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

if $cygwin; then
KEYSTORE_PROPERTIES_FILE=`cygpath --path --windows $KEYSTORE_PROPERTIES_FILE`
KEYSTORE_DIR=`cygpath --path --mixed $KEYSTORE_DIR`
SERVER_XML_FILE=`cygpath --path --windows $SERVER_XML_FILE`
WEBAPPS_ROOT=`cygpath --path --windows $WEBAPPS_ROOT`
WAR_FILE=`cygpath --path --windows $WAR_FILE`
fi

CA_KEYSTORE_FILE="$KEYSTORE_DIR/ca.ks"
SSL_KEYSTORE_FILE="$KEYSTORE_DIR/ssl.ks"


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
    ANSWER_SSLKEYS_CREATION="y"
elif [ $MENU_CHOICE = "2" ]
then
    ANSWER_SSLKEYS_CREATION="y"
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
if [ "$ANSWER_CAKEYS_CREATION" = "y" ]
then
    # GET A NEW KEYSTORE PASSWORD FROM CALLER
    echo "Please choose a keystore password"
    read -s KEYSTORE_PASSWORD
    if [ ${#KEYSTORE_PASSWORD} -gt 0 ]; then
        INDEXOFAT=`expr index $KEYSTORE_PASSWORD '@'`
        if [ $INDEXOFAT -gt 0 ]; then
          echo "ERROR: character @ not allowed in passwords - please re-run $0"
          exit
        fi
    fi
    echo "Please repeat"
    read -s KEYSTORE_PASSWORD_REPEAT
    # VERIFY THAT PASSWORDS ARE EQUAL
    if [ ! "$KEYSTORE_PASSWORD" = "$KEYSTORE_PASSWORD_REPEAT" ]; then
        echo "ERROR : passwords do not match - please re-run $0"
        exit 
    fi
    # VERIFY THAT THE PASSWORD IS LONG ENOUGH
    PASSWORD_LENGTH=${#KEYSTORE_PASSWORD}
    if [ "$PASSWORD_LENGTH" -lt 6 ]; then
        echo "ERROR : the CA keystore password must be at least 6 characters long - please re-run $0"
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
if [ "$ANSWER_CAKEYS_CREATION" = "y" ]; then
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
$JAVA_HOME/bin/java -classpath "$CP" $setKeysClassname $HOST_NAME "$KEYSTORE_DIR" $KEYSTORE_PASSWORD $KEYSTORE_PASSWORD $KEYSTORE_TYPE

# Edit the server.xml file so that the appropriate keystore location and paswords are remembered
KS_QUOTED_SLASHES=${SSL_KEYSTORE_FILE//\//\\\/}
echo "KS_QUOTED_SLASHES = $KS_QUOTED_SLASHES"
echo "Updating <$SERVER_XML_FILE>..."
perl -pi.bak - "$SERVER_XML_FILE" <<!
s/(keystoreFile)="[^"]*"/\1="${KS_QUOTED_SLASHES}"/;
s/(keystoreType)="[^"]*"/\1="${KEYSTORE_TYPE}"/;
s/(keystorePass)=".*?"/\1="${KEYSTORE_PASSWORD}"/;
!


# -----------------------------------------------------------------------------
# REMEMBER THE KEYSTORE PASSWORDS IN THE KEYSTORE.PROPERTIES FILE
# -----------------------------------------------------------------------------
if [ -e "$KEYSTORE_PROPERTIES_FILE" ]; then
    echo "Updating <$KEYSTORE_PROPERTIES_FILE>..."

    SUBSTITUTE_FROM=rootcakspasswd=.*
    SUBSTITUTE_TO=rootcakspasswd=${KEYSTORE_PASSWORD}
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
    echo "The keystore password was not recorded because the properties file was not found."
    echo "This should be done manually"
    exit
fi

echo
echo "Done".
