#!/bin/bash
# -----------------------------------------------------------------------------
# FILE [rootcaGen.sh]
# LAYER 7 TECHNOLOGIES
# $Id$:
#
# THIS SCRIPT GENERATES THE PRIVATE KEYSTORE OF THE SSG
#
# PREREQUISITES
# 1. THE DIRECTORY JAVA_HOME/bin IS PART OF THE PATH
# 2. TOMCAT_HOME is set
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
KEYSTORE_FILE="$KEYSTORE_DIR/ssgroot"
WEB_XML_FILE="$TOMCAT_HOME/webapps/ROOT/WEB-INF/web.xml"
WAR_FILE="$TOMCAT_HOME/webapps/ROOT.war"
WEBAPPS_PATH="$TOMCAT_HOME/webapps"
KEYSTORE_FILE_OSPATH="$KEYSTORE_DIR_OSPATH/ssgroot"
CERTIFICATE_FILE="$TOMCAT_HOME/kstores/ssgroot.cer"
CERTIFICATE_FILE_OSPATH="$TOMCAT_HOME_OSPATH/kstores/ssgroot.cer"

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
echo "Please provide a keystore password"
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
$KEYTOOL -genkey -alias ssgroot -dname $DN -v -keystore "$KEYSTORE_FILE_OSPATH" -keyalg RSA -keypass $KEYSTORE_PASSWORD -storepass "$KEYSTORE_PASSWORD"

# CHECK THAT THIS KEYSTORE WAS SET SUCCESSFULLY
if [ -e "$KEYSTORE_FILE" ]
then
# EXPORT THE SERVER CERTIFICATE
        $KEYTOOL -export -alias ssgroot -storepass "$KEYSTORE_PASSWORD" -file "$CERTIFICATE_FILE_OSPATH" -keystore "$KEYSTORE_FILE_OSPATH"
else
# INFORM THE USER OF THE FAILURE
        echo "ERROR: The keystore file was not generated"
        echo
        exit 255
fi

# REMEMBER THE KEYSTORE PASSWORD IN THE WEB.XML FILE

# IF THE WAR FILE IS PRESENT BUT NOT YET EXPANDED, EXPAND IT
if [ -e "$WEB_XML_FILE" ]; then
        echo
else
        echo "expanding the war file so that web.xml can be updated with kstore password"
        unzip $WAR_FILE -d $WEBAPPS_PATH/ROOT
fi

if [ -e "$WEB_XML_FILE" ]; then
        echo "Recording the keystore password in web.xml"
        SUBSTITUTE_FROM='RootKeyStorePasswd\<\/param-name\>\s*\<param-value\>.*\<\/param-value\>'
        SUBSTITUTE_TO='RootKeyStorePasswd\<\/param-name\>\<param-value\>'
        SUBSTITUTE_TO=$SUBSTITUTE_TO${KEYSTORE_PASSWORD}
        SUBSTITUTE_TO=$SUBSTITUTE_TO'\<\/param-value\>'
        perl -pi.bak -e s/$SUBSTITUTE_FROM/$SUBSTITUTE_TO/ "$WEB_XML_FILE"
        echo "Recording the location of the root cert in web.xml"
        # fla: can anyone read this next line ? ;^) I am substituing the char '/' for '\/' because this is a path
        CERT_FILE_WITH_SLASH_SUBSTITUTED=${CERTIFICATE_FILE_OSPATH//\//\\\/}
        SUBSTITUTE_FROM='RootCertLocation\<\/env-entry-name\>\s*\<env-entry-value\>.*\<\/env-entry-value\>'
        SUBSTITUTE_TO='RootCertLocation\<\/env-entry-name\>\<env-entry-value\>'
        SUBSTITUTE_TO=$SUBSTITUTE_TO$CERT_FILE_WITH_SLASH_SUBSTITUTED
        SUBSTITUTE_TO=$SUBSTITUTE_TO'\<\/env-entry-value\>'
        perl -pi.bak -e s/$SUBSTITUTE_FROM/$SUBSTITUTE_TO/ "$WEB_XML_FILE"
else
# INFORM THE USER OF THE FAILURE
        echo "ERROR"
        echo "The root keystore password was not recorded in web.xml because the file was not found."
        echo "This should be done manually"
fi
