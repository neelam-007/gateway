#!/bin/bash
# -----------------------------------------------------------------------------
# FILE [resetAdmin.sh]
# LAYER 7 TECHNOLOGIES
# 30-06-2003, flascelles
# THIS SCRIPT RESETS THE ROOT ADMIN ACCOUNT FOR THE SSG ADMIN SERVICE
#
# PREREQUISITES
# 1. THE DATABASE IS UP AND RUNNING
#
# This script asks for a username and password and adds an entry in the
# database.
# The database username/passwd must be provided as arguments
# -----------------------------------------------------------------------------
#

USERS_FILE=$TOMCAT_HOME/conf/tomcat-users.xml

echo
echo

# VERIFY THAT WE HAVE THE DB USERNAME AND PASSWD AS ARGUMENTS
if [ ! $1 ]; then
    echo "please provide database account name"
	echo "USAGE resetAdmin.sh dbaccountname dbpasswd"
	echo
	exit -1
fi
if [ ! $2 ]; then
    echo "please provide database account passwd"
	echo "USAGE resetAdmin.sh dbusername dbpasswd"
	echo
	exit -1
fi

# GET AN ADMIN ACCOUNT NAME
echo "Please choose your ssg admin account name"
read ACCOUNT_NAME

# GET AN ADMIN PASSWORD FROM CALLER
echo "Please provide an administrator password"
read -s ADMIN_PASSWORD
echo "Please repeat"
read -s ADMIN_PASSWORD_REPEAT

# VERIFY THAT PASSWORDS ARE EQUAL
if [ ! $ADMIN_PASSWORD = $ADMIN_PASSWORD_REPEAT ]; then
	echo "ERROR : passwords do not match"
	exit -1
fi

# VERIFY THAT THE PASSWORD IS LONG ENOUGH
PASSWORD_LENGTH=${#ADMIN_PASSWORD}
if [ "$PASSWORD_LENGTH" -lt 6 ]; then
	echo "ERROR : the admin password must be at least 6 characters long"
	exit -1
fi

# ENCODE PASSWORD
# RESOLVE THE DIRECTORY WHERE THESE SCRIPTS RESIDE
PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
PRGDIR=`dirname "$PRG"`
ENCODED_ADMIN_PASSWD=`perl $PRGDIR/md5passwd.pl $ACCOUNT_NAME:L7SSGDigestRealm:$ADMIN_PASSWORD`

# VERIFY THAT THE LAST OPERATION SUCCEEDED
PASSWORD_LENGTH=${#ENCODED_ADMIN_PASSWD}
if [ "$PASSWORD_LENGTH" -lt 31 ]; then
	echo "ERROR : the operation failed"
	exit -1
fi

# UPDATE DATABASE
UPDATE_SYNTAX="UPDATE internal_user SET password='$ENCODED_ADMIN_PASSWD', login='$ACCOUNT_NAME', name='$ACCOUNT_NAME' WHERE objectid=3"
RES=`mysql -h localhost -u $1 -p$2 ssg -e "${UPDATE_SYNTAX}"`
echo $RES
