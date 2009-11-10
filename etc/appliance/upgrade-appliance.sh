#!/bin/bash
#############################################################################
# Upgrade script for Appliance configuration files.
#############################################################################
#

# Detect install directory
INSTALL_DIR="$(dirname $0)/../.."

function extractProperty() {
    declare EP_PROP=$1
    declare EP_ENV=$2
    declare EP_FILE=$3
    declare EP_EXPR=$(grep "${EP_PROP}" "${EP_FILE}" | sed "s/[ ]*${EP_PROP}[ ]*=//" )
    export "${EP_ENV}"="$(echo ${EP_EXPR} | sed 's/^[ ]*//g')"
}

# Set flag to enable HSM if keystore.properties if for HSM, only when upgrading from a pre 5.0 version
if [ -d "/ssg/appliance" -a -f "${INSTALL_DIR}/Gateway/node/default/var/upgrade/keystore.properties" ] ; then
    echo "Checking keystore.properties for SCA"
    extractProperty "keystoretype" KEYSTORE_TYPE "${INSTALL_DIR}/Gateway/node/default/var/upgrade/keystore.properties"
    if [ "${KEYSTORE_TYPE}" == "PKCS11" ] ; then
        echo "SCA is in use, adding to override.properties"
        echo "host.sca = true" >> "/opt/SecureSpan/Controller/etc/override.properties"
        chown layer7:layer7 "/opt/SecureSpan/Controller/etc/override.properties"
    else
        echo "SCA not in use, not enabling via override.properties"
    fi
fi

# Move process controller configuration files to the new location
if [ -f "${INSTALL_DIR}/Appliance/controller/etc/host.properties" ] ; then
    echo "Moving Process Controller's host.properties config file to the new location"

    extractProperty "host.controller.keystore.file" PC_KEYSTORE "${INSTALL_DIR}/Appliance/controller/etc/host.properties"
    if  [ -f ${PC_KEYSTORE} ] ; then
        PC_KEYSTORE_NEW_LOCATION="${PC_KEYSTORE/Appliance\/controller/Controller}"
        if [ "${PC_KEYSTORE}" != "${PC_KEYSTORE_NEW_LOCATION}" ] ; then
            echo "Moving Process Controller's keystore to the new location"
            mv "${PC_KEYSTORE}" "${PC_KEYSTORE_NEW_LOCATION}"
            sed -i "s/\(host.controller.keystore.file.*\)Appliance\/controller/\1Controller/" "${INSTALL_DIR}/Appliance/controller/etc/host.properties"
        fi
    fi

    mv "${INSTALL_DIR}/Appliance/controller/etc/host.properties" "${INSTALL_DIR}/Controller/etc/host.properties"
fi
