#!/bin/bash
# -----------------------------------------------------------------------------
# FILE [resetAdmin.sh]
# LAYER 7 TECHNOLOGIES
# 30-06-2003, flascelles
#
# THIS SCRIPT RESETS THE ROOT ADMIN ACCOUNT FOR THE SSG ADMIN SERVICE
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

# VERIFY THAT THE TOMCAT_HOME VARIABLE IS SET
if [ ! $SSG_HOME ]; then
	echo "ERROR: SSG_HOME not set"
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

# SEND THIS TO ADMIN ACCOUNT HANDLER
# TODO
# encode the password by calling our own encoder in
# com.l7tech.identity.encodePasswd($ACCOUNT_NAME, $ADMIN_PASSWORD);
# update internal_user set password=encodedpasswd where oid=3;
# update internal_user set login=$ACCOUNT_NAME where oid=3;
echo $ACCOUNT_NAME
echo $ADMIN_PASSWORD


# END
