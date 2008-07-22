#!/bin/bash
#
# Utility script to run ReKey after grabbing the relevant DB information.
#

#
# Config
#
MYSQL="/usr/bin/mysql"
SHARED_KEY_TEMP=$(/bin/mktemp -t "sharedkeys.XXXXXXXXXX")
JAVA="/ssg/jdk/bin/java"
DBNAME="ssg"
MYSQL_USER="gateway"

#
# Functions
#
function failOnError() {
    if [ $? -ne 0 ] ; then
        echo "$1"
        exit 1
    fi
}

#
# Check args
#
PARTITION_DIR="${1}"
KEYSTORE_FILE="${2}"
KEYSTORE_ALIAS="${3}"

if [ -z "${KEYSTORE_FILE}" ] ; then
  echo -e "Usage:\n\t./ReKey.sh <partition-path> <keystore-path> <keystore-alias>\n\n"
  exit 1
fi

test -d "${PARTITION_DIR}"
failOnError "Partition directory not found: ${PARTITION_DIR}"

test -f "${KEYSTORE_FILE}"
failOnError "Keystore file not found: ${KEYSTORE_FILE}"

#
# Create data file
#
echo "Enter MySQL ${MYSQL_USER} password"
${MYSQL} "${DBNAME}" -u "${MYSQL_USER}" -p -N -B -e "select encodingid, replace(b64edval,'\n', '') from shared_keys" > "${SHARED_KEY_TEMP}"
failOnError "Error extracting shared key information from database (incorrect password?)."

#
# Get keystore passwords
#
read -s -p "Enter keystore password: " KEYSTORE_PASSWORD
echo
read -s -p "Enter key password: " KEYSTORE_KEY_PASSWORD
echo

#
# Run utility
#
"${JAVA}" -jar ReKey.jar "${SHARED_KEY_TEMP}" "${KEYSTORE_FILE}" "${KEYSTORE_PASSWORD}" "${KEYSTORE_ALIAS}" "${KEYSTORE_KEY_PASSWORD}" "${PARTITION_DIR}"


