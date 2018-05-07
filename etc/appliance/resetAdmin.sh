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
# This script resets the password to 'password' for the root admin account.
# Asks for the name of the gateway database and the name of the root admin account.
# The root admin should update their password immediately following this operation.
#
# The database username and database name must be provided as arguments
# -----------------------------------------------------------------------------
#

USERS_FILE=$TOMCAT_HOME/conf/tomcat-users.xml

echo
echo

# VERIFY THAT WE HAVE THE DB USERNAME AND PASSWD AS ARGUMENTS
if [ ! $1 ] || [ ! $2 ]; then
    echo "Not enough arguments."
    echo "Please provide the database account name and database account password"
    echo "to access the database"
	echo
	echo "USAGE: $0 dbaccountname dbpasswd"
	echo
	exit -1
fi

# GET GATEWAY DATABASE NAME
DATABASE_NAME=""
while [ -z "${DATABASE_NAME}" ] ; do
    echo -n "Please enter the Layer7 Gateway database name (required): "
    read DATABASE_NAME
done


# GET AN ADMIN ACCOUNT NAME
ACCOUNT_NAME=""
while [ -z "${ACCOUNT_NAME}" ] ; do
    echo -n "Please enter your ssg admin account name (required): "
    read ACCOUNT_NAME
done


ADMIN_HASH='$6$S7Z3HcudYNsObgs8$SjwZ3xtCkSjXOK2vHfOVEg2dJES3cgvtIUdHbEN/KdCBXoI6uuPSbxTEwcH.av6lpcb1p6Lu.gFeIX04FBxiJ.'

# UPDATE DATABASE
UPDATE_SYNTAX="UPDATE internal_user SET password='$ADMIN_HASH', login='$ACCOUNT_NAME', name='$ACCOUNT_NAME' WHERE goid=toGoid(0,3)"
RES=`mysql -h localhost -u $1 -p$2 $DATABASE_NAME -e "${UPDATE_SYNTAX}"`
echo $RES
