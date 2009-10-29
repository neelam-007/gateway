#!/bin/bash
#############################################################################
# Upgrade script for pre 5.0 Appliance configuration files.
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

# Set flag to enable HSM if keystore.properties if for HSM
if [ -f "${INSTALL_DIR}/Gateway/node/default/var/upgrade/keystore.properties" ] ; then
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
