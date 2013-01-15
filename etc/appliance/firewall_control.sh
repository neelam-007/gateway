#!/bin/bash
#
# Script to update the firewall to open / close listen ports for applications.
#
# 1 - {ipv4|ipv6}
# 2 - The rules file to apply
# 3 - start / stop to add or remove the rules
#

RULES_HOME="/opt/SecureSpan/Appliance/var/firewall"

if [ "ipv4" == ${1} ]; then
    RULES_FILES="/opt/SecureSpan/Appliance/var/firewall/rules.d"
    RULES_SOURCE="/etc/sysconfig/iptables"
    RULES_ALL="iptables-combined"
    RULES_EXT="iptables-extras"
    RESTORE_COMMAND="/sbin/iptables-restore"
elif [ "ipv6" == ${1} ]; then
    RULES_FILES="/opt/SecureSpan/Appliance/var/firewall/rules6.d"
    RULES_SOURCE="/etc/sysconfig/ip6tables"
    RULES_ALL="ip6tables-combined"
    RULES_EXT="ip6tables-extras"
    RESTORE_COMMAND="/sbin/ip6tables-restore"
else
    exit 0
fi

if [ ! -z "${2}" ] ; then
    RULESID=$(echo "${2}" | md5sum | sed 's/ //g')
    RULES_NAME="${RULESID}$(basename ${2})"

    if [ "start" == "${3}" ] ; then
        cp "${2}" "${RULES_FILES}/${RULES_NAME}"
    else
        [ ! -e "${RULES_FILES}/${RULES_NAME}" ] || rm -f "${RULES_FILES}/${RULES_NAME}"
    fi

    find "${RULES_FILES}" -type f -exec cat {} \; > "${RULES_HOME}/${RULES_EXT}"
    cat ${RULES_SOURCE} ${RULES_HOME}/${RULES_EXT} > ${RULES_HOME}/${RULES_ALL}
    cat "${RULES_HOME}/${RULES_ALL}" | "${RESTORE_COMMAND}"
fi
