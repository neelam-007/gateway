#!/bin/bash
# -----------------------------------------------------------------------------
# FILE [importTrustedCertificate.sh]
# LAYER 7 TECHNOLOGIES
# 02-07-2003, flascelles
# THIS SCRIPT IMPORTS A SSG CERTIFICATE INTO THE TRUSTED STORE OF THE SSG CONSOLE
# OR SSG PROXY
#
# PREREQUISITES
# 0. pass the certificate as argument
# 1. $JAVA_HOME be defined
# 2. $JAVA_HOME/bin/keytool be there
#
# -----------------------------------------------------------------------------
#


STOREDIR=$HOME/.l7tech
STOREFILE=$STOREDIR/trustStore
KEYTOOLBIN=$JAVA_HOME/bin/keytool

# CHECK THAT WE ARE RECIEVING THE CERTIFICATE FILE AS AN ARGUMENT
if [ ! $1 ]; then
    echo USAGE: $0 filetoimport.cer
    exit -1
fi

# VERIFY THAT THE KEYSTORE IS NOT YET SET
if [ -e $STOREFILE ]; then
        echo "THE KEYSTORE ALREADY EXIST AT $STOREFILE"
        echo "DO YOU WANT TO OVERWRITE? [y/n]"
        read USR_ANSWER
        if [ $USR_ANSWER = "n" ]; then
            exit -1
            echo
        fi
        if [ $USR_ANSWER = "y" ]; then
            rm $STOREFILE
            echo
        fi
fi

# ENSURE THE DIRECTORY IS THERE
if [ ! -e $STOREDIR ]; then
    mkdir $STOREDIR
fi


# IMPORT THE CERTIFICATE
$KEYTOOLBIN -import -v -trustcacerts -alias tomcat -file $1 -keystore $STOREFILE -keypass password -storepass password
