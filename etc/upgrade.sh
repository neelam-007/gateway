#!/bin/bash
#############################################################################
# Upgrade script for pre 5.0 Gateway configuration files
#############################################################################
#

# Detect install directory
INSTALL_DIR="$(dirname $0)/../.."

# Ext Jars to ignore
IGNORE_EXT='
jms-1.1.jar
tarari_raxj.jar
'

# Custom Assertion Jars to ignore (and other modules)
IGNORE_ASS='
ct_agent.jar
netegrity-sm.jar
netegrity-tm.jar
oracle-coreid.jar
ssg-uddi-module-genericv3.jar
sun-jsam.jar
symantec_antivirus.jar
tivoli-tam.jar
'

# Utility functions
function notIgnore() {
    FOUND=0;
    for IGNORE_FILE in ${2}; do
        if [ "${IGNORE_FILE}" == "${1}" ] ; then
            FOUND=1;
            break;
        fi
    done
    return ${FOUND};
}

function fail() {
    echo "Upgrade failed due to : ${1}"
    # fail without error code to be rpm friendly
    exit 0
}

# Create upgrade directory
mkdir "${INSTALL_DIR}/node/default/var/upgrade"
[ ${?} -eq 0 ] || fail "Error creating upgrade directory."
chown gateway.gateway "${INSTALL_DIR}/node/default/var/upgrade"
[ ${?} -eq 0 ] || fail "Error setting permissions on upgrade directory."

echo "Starting upgrade at $(date)"

# Upgrade configuration files
echo "Upgrading configuration.";
[ ! -f "/ssg/etc/conf/partitions/default_/omp.dat" ]              || mv -fv "/ssg/etc/conf/partitions/default_/omp.dat"              "${INSTALL_DIR}/node/default/etc/conf/omp.dat"
[ ! -f "/ssg/etc/conf/partitions/default_/system.properties" ]    || mv -fv "/ssg/etc/conf/partitions/default_/system.properties"    "${INSTALL_DIR}/node/default/etc/conf/system.properties"
[ ! -f "/ssg/etc/conf/partitions/default_/ssglog.properties" ]    || mv -fv "/ssg/etc/conf/partitions/default_/ssglog.properties"    "${INSTALL_DIR}/node/default/etc/conf/ssglog.properties"
[ ! -f "/ssg/etc/conf/partitions/default_/keystore.properties" ]  || mv -fv "/ssg/etc/conf/partitions/default_/keystore.properties"  "${INSTALL_DIR}/node/default/var/upgrade/keystore.properties"

# copy hibernate.properties to new location but comment out everything, also copy to upgrade directory for processing
if [ -f "/ssg/etc/conf/partitions/default_/hibernate.properties" ] && [ ! -f "${INSTALL_DIR}/node/default/etc/conf/hibernate.properties" ] ; then
    echo "Migrating /ssg/etc/conf/partitions/default_/hibernate.properties to ${INSTALL_DIR}/node/default/etc/conf/"
    cat "/ssg/etc/conf/partitions/default_/hibernate.properties" | sed '/#/!s/^/#/' > "${INSTALL_DIR}/node/default/etc/conf/hibernate.properties"
fi
[ ! -f "/ssg/etc/conf/partitions/default_/hibernate.properties" ] || mv -fv "/ssg/etc/conf/partitions/default_/hibernate.properties" "${INSTALL_DIR}/node/default/var/upgrade/hibernate.properties"

# Upgrade configuration artifacts
[ ! -f "/ssg/etc/conf/partitions/default_/kerberos.keytab" ] || mv -fv "/ssg/etc/conf/partitions/default_/kerberos.keytab" "${INSTALL_DIR}/node/default/var/"
if [ -d "/ssg/etc/conf/partitions/default_/keys" ] ; then
    echo "Migrating keystore files.";
    find "/ssg/etc/conf/partitions/default_/keys" -name '*.ks' -exec cp -pfv {} "${INSTALL_DIR}/node/default/var/upgrade/" \;
else
    echo "Not migrating keystore files.";
fi


# Upgrade runtime files
if [ -d "/ssg/logs" ] ; then
    echo "Upgrading runtime files.";
    find "/ssg/logs" -name '*.log' -exec mv -fv {} "${INSTALL_DIR}/node/default/var/logs/" \;
else
    echo "Not upgrading runtime files.";
fi

# Upgrade software (JMS libraries)
if [ -d "/ssg/lib/ext" ] ; then
    echo "Upgrading JMS libraries.";
    for JARPATH in $(find "/ssg/lib/ext" -name '*.jar') ; do
        JARNAME=$(basename ${JARPATH})
        notIgnore "${JARNAME}" "${IGNORE_EXT}"
        if [ ${?} -eq 0 ] ; then
            mv -fv "${JARPATH}" "${INSTALL_DIR}/runtime/lib/ext/"
        else
            echo "Ignoring lib/ext Jar : ${JARNAME}"
        fi
    done
else
    echo "Not upgrading JMS libraries.";
fi

# Upgrade software (assertions)
if [ -d "/ssg/modules/lib" ] ; then
    echo "Upgrading Custom Assertions.";
    for ASSPATH in $(find "/ssg/modules/lib" -name '*.jar') ; do
        ASSNAME=$(basename ${ASSPATH})
        notIgnore "${ASSNAME}" "${IGNORE_ASS}"
        if [ ${?} -eq 0 ] ; then
            mv -fv "${ASSPATH}" "${INSTALL_DIR}/runtime/modules/lib/"
        else
            echo "Ignoring Custom Assertion Jar : ${JARNAME}"
        fi
    done
else
    echo "Not upgrading Custom Assertions.";
fi

echo "Upgrade completed at $(date)"
