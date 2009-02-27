#!/bin/bash
#
# Script to update the firewall to open / close listen ports for applications.
#
# 1 - The rules file to apply
# 2 - start / stop to add or remove the rules
#

RULES_HOME="/opt/SecureSpan/Appliance/var/firewall"
RULES_FILES="/opt/SecureSpan/Appliance/var/firewall/rules.d"
RULES_SOURCE="/etc/sysconfig/iptables"
RULES_ALL="iptables-combined"
RULES_EXT="iptables-extras"
RESTORE_COMMAND="/sbin/iptables-restore"

if [ ! -z "${1}" ] ; then
    RULESID=$(echo "${1}" | md5sum | sed 's/ //g')
    RULES_NAME="${RULESID}$(basename ${1})"

    if [ "start" == "${2}" ] ; then
        cp "${1}" "${RULES_FILES}/${RULES_NAME}"	        
    else
        [ ! -e "${RULES_FILES}/${RULES_NAME}" ] || rm -f "${RULES_FILES}/${RULES_NAME}"
    fi

    find "${RULES_FILES}" -type f -exec cat {} \; > "${RULES_HOME}/${RULES_EXT}"
    sed "/# ADD CUSTOM ALLOW RULES HERE/ r ${RULES_HOME}/${RULES_EXT}" "${RULES_SOURCE}" > "${RULES_HOME}/${RULES_ALL}" 
    
    cat "${RULES_HOME}/${RULES_ALL}" | "${RESTORE_COMMAND}"    
fi

