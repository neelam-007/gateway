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

# hack to ensure ownership/permissions are set correctly for what's needed on appliance
chown layer7:layer7 /opt/SecureSpan/Controller/etc/host.properties 2>/dev/null
chmod 660 /opt/SecureSpan/Controller/etc/host.properties 2>/dev/null
chown layer7:layer7 /opt/SecureSpan/Controller/etc/*.p12 2>/dev/null
chmod 660 /opt/SecureSpan/Controller/etc/*.p12 2>/dev/null

# process controller runs as layer7 on appliance, gateway shouldn't have any access to its files
find /opt/SecureSpan/Controller/etc -user gateway -exec chown layer7 '{}' \;
find /opt/SecureSpan/Controller/etc -group gateway -exec chgrp layer7 '{}' \;

# Update replication configuration (even if replication is not enabled)
MY_CNF="/etc/my.cnf"
if [ -f "${MY_CNF}" ] ; then
    grep '^slave-net-timeout=' "${MY_CNF}" &>/dev/null
    if [ ${?} -eq 0 ] ; then
        grep '^slave_exec_mode=' "${MY_CNF}" &>/dev/null
        if [ ${?} -ne 0 ] ; then
            echo "Updating ${MY_CNF} replication configuration (slave_exec_mode=IDEMPOTENT)"
            sed -i "/^slave-net-timeout=/a slave_exec_mode=IDEMPOTENT" "${MY_CNF}"
        fi
    fi
fi

# Ensure that configuration for a database with binary logging enabled will
# permit creation of the "next_hi" database identifier generator function.
# If binary logging is not currently enabled then add disabled configuration
# for later use.
if [ -f "${MY_CNF}" ] ; then
    grep '^log-bin=' "${MY_CNF}" &>/dev/null
    if [ ${?} -eq 0 ] ; then
        grep '^log_bin_trust_function_creators=' "${MY_CNF}" &>/dev/null
        if [ ${?} -ne 0 ] ; then
            echo "Updating ${MY_CNF} to permit function creation (log_bin_trust_function_creators=1)"
            sed -i "/^log-bin=/a log_bin_trust_function_creators=1" "${MY_CNF}"
        fi
    else
        grep '^#log_bin_trust_function_creators=' "${MY_CNF}" &>/dev/null
        if [ ${?} -ne 0 ] ; then
            echo "Updating ${MY_CNF} with function creation disabled (#log_bin_trust_function_creators=1)"
            sed -i "/^#log-bin=/a #log_bin_trust_function_creators=1" "${MY_CNF}"
        fi
        #log-bin=
    fi
fi

# Update MySQL configuration to remove use of deprecated names
if [ -f "${MY_CNF}" ] ; then
    grep '^\[safe_mysqld\]' "${MY_CNF}" &>/dev/null
    if [ ${?} -eq 0 ] ; then
        echo "Updating ${MY_CNF} mysqld_safe section name"
        sed -i "s/^\[safe_mysqld\]/[mysqld_safe]/" "${MY_CNF}"
    fi
    grep '^err-log=' "${MY_CNF}" &>/dev/null
    if [ ${?} -eq 0 ] ; then
        echo "Updating ${MY_CNF} log file configuration"
        sed -i "s/^err-log=/log-error=/" "${MY_CNF}"
    fi
fi
