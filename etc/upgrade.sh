#!/bin/bash
#############################################################################
# Upgrade script for pre 5.0 Gateway configuration files
#############################################################################
#

# Detect install directory
INSTALL_DIR="$(dirname $0)/../.."

# Ext Jars to ignore
IGNORE_EXT='
fiorano-proxy.jar
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

function calculateNodeId(){
    declare MAC="${1}"
    declare NAME="${2}"
    # use digest on solaris, else md5sum
    if [ -x "/usr/bin/digest" ] ; then
        export "${NAME}"="$(echo -n "${MAC}-default_" | digest -a md5)"
    else
        export "${NAME}"="$(echo -n "${MAC}-default_" | md5sum | awk '{print $1}')"
    fi
}

function extractProperty() {
    declare EP_PROP=$1
    declare EP_ENV=$2
    declare EP_FILE=$3
    declare EP_EXPR=$(grep "${EP_PROP}" "${EP_FILE}" | sed "s/[ ]*${EP_PROP}[ ]*=//" )
    export "${EP_ENV}"="$(echo ${EP_EXPR} | sed 's/^[ ]*//g')"
}

function extractPort() {
    declare NAME="${1}"
    declare HOSTANDPORT="${2}"
    declare DEFAULT="${3}"

    echo -n "${HOSTANDPORT}" | grep ':' &>/dev/null
    if [ ${?} -eq 0 ] ; then
        if [ -z "${HOSTANDPORT/#*:/}" ] ; then
            export "${NAME}"="${DEFAULT}"
        else
            export "${NAME}"="${HOSTANDPORT/#*:/}"
        fi
    else
        export "${NAME}"="${DEFAULT}"
    fi
}

function setPermissions() {
    declare DIRECTORY="${1}"
    declare OWNERSHIP="${2}"
    declare PERMISSIONS="${3}"
    if [ -d "${DIRECTORY}" ] ; then
        for UPGRADE_FILE in $(ls -p "${DIRECTORY}" | grep -v '/') ; do
            echo "Permission update "${OWNERSHIP}" "${PERMISSIONS}" ${DIRECTORY}/${UPGRADE_FILE}"
            chown "${OWNERSHIP}"   "${DIRECTORY}/${UPGRADE_FILE}"
            chmod "${PERMISSIONS}" "${DIRECTORY}/${UPGRADE_FILE}"
        done
    fi
}

function fail() {
    echo "Upgrade failed due to : ${1}"
    # fail without error code to be rpm friendly
    exit 0
}

function domove() {
    echo "Moving file ${1} to ${2}"
    mv -f "${1}" "${2}"
}

MV="domove"

# Create upgrade directory
mkdir "${INSTALL_DIR}/node/default/var/upgrade"
[ ${?} -eq 0 ] || fail "Error creating upgrade directory."
chown gateway:gateway "${INSTALL_DIR}/node/default/var/upgrade"
[ ${?} -eq 0 ] || fail "Error setting permissions on upgrade directory."

echo "Starting upgrade at $(date)"

# Upgrade configuration files
echo "Upgrading configuration.";
[ ! -f "/ssg/etc/conf/partitions/default_/omp.dat" ]              || ${MV} "/ssg/etc/conf/partitions/default_/omp.dat"              "${INSTALL_DIR}/node/default/etc/conf/omp.dat"
[ ! -f "/ssg/etc/conf/partitions/default_/system.properties" ]    || ${MV} "/ssg/etc/conf/partitions/default_/system.properties"    "${INSTALL_DIR}/node/default/etc/conf/system.properties"
[ ! -f "/ssg/etc/conf/partitions/default_/ssglog.properties" ]    || ${MV} "/ssg/etc/conf/partitions/default_/ssglog.properties"    "${INSTALL_DIR}/node/default/etc/conf/ssglog.properties"
[ ! -f "/ssg/etc/conf/partitions/default_/keystore.properties" ]  || ${MV} "/ssg/etc/conf/partitions/default_/keystore.properties"  "${INSTALL_DIR}/node/default/var/upgrade/keystore.properties"

# copy hibernate.properties to new location but comment out everything, also copy to upgrade directory for processing
if [ -f "/ssg/etc/conf/partitions/default_/hibernate.properties" ] && [ ! -f "${INSTALL_DIR}/node/default/etc/conf/hibernate.properties" ] ; then
    echo "Migrating /ssg/etc/conf/partitions/default_/hibernate.properties to ${INSTALL_DIR}/node/default/etc/conf/"
    cat "/ssg/etc/conf/partitions/default_/hibernate.properties" | sed '/#/!s/^/#/' > "${INSTALL_DIR}/node/default/etc/conf/hibernate.properties"
fi
[ ! -f "/ssg/etc/conf/partitions/default_/hibernate.properties" ] || ${MV} "/ssg/etc/conf/partitions/default_/hibernate.properties" "${INSTALL_DIR}/node/default/var/upgrade/hibernate.properties"

#remove pkcs11 system property from system.properties if it exists. We handle the SCA differently in 5.0
[ ! -f "${INSTALL_DIR}/node/default/etc/conf/system.properties" ] || cat "${INSTALL_DIR}/node/default/etc/conf/system.properties" | sed '/Pkcs11JceProviderEngine/s/^/#/' > "${INSTALL_DIR}/node/default/etc/conf/system.properties"

# Upgrade configuration artifacts
[ ! -f "/ssg/etc/conf/partitions/default_/kerberos.keytab" ] || ${MV} "/ssg/etc/conf/partitions/default_/kerberos.keytab" "${INSTALL_DIR}/node/default/var/"
if [ -d "/ssg/etc/conf/partitions/default_/keys" ] ; then
    echo "Migrating keystore files.";
    find "/ssg/etc/conf/partitions/default_/keys" -name '*.ks' -print -exec cp -fp {} "${INSTALL_DIR}/node/default/var/upgrade/" \;
else
    echo "Not migrating keystore files.";
fi

# Upgrade runtime files
if [ -d "/ssg/logs" ] ; then
    echo "Upgrading runtime files.";
    find "/ssg/logs" -name '*.log' -print -exec mv -f {} "${INSTALL_DIR}/node/default/var/logs/" \;
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
            ${MV} "${JARPATH}" "${INSTALL_DIR}/runtime/lib/ext/"
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
            ${MV} "${ASSPATH}" "${INSTALL_DIR}/runtime/modules/lib/"
        else
            echo "Ignoring Custom Assertion (or other module) Jar : ${ASSNAME}"
        fi
    done
else
    echo "Not upgrading Custom Assertions.";
fi

# Genenerate new node.properties
if [ -f "${INSTALL_DIR}/node/default/var/upgrade/hibernate.properties" ] && [ -f "${INSTALL_DIR}/node/default/var/upgrade/keystore.properties" ] ; then
    echo "Found hibernate.properties, creating node.properties with existing settings (keystore passphrase used as cluster passphrase)"
    extractProperty "hibernate.connection.url"      HIBERNATE_URL  "${INSTALL_DIR}/node/default/var/upgrade/hibernate.properties"
    extractProperty "hibernate.connection.username" HIBERNATE_USER "${INSTALL_DIR}/node/default/var/upgrade/hibernate.properties"
    extractProperty "hibernate.connection.password" HIBERNATE_PASS "${INSTALL_DIR}/node/default/var/upgrade/hibernate.properties"

    NODE_MAC="";
    if [ -f "${INSTALL_DIR}/node/default/etc/conf/system.properties" ] ; then
        extractProperty "com.l7tech.cluster.macAddress" NODE_MAC "${INSTALL_DIR}/node/default/etc/conf/system.properties"
    fi
    if [ ! -z "${NODE_MAC}" ] ; then
        echo "Using MAC address from system.properties for Node ID generation (${NODE_MAC})."
    else
        /sbin/ifconfig -a | grep ether &>/dev/null
        if [ ${?} -eq 0 ] ; then
            # Solaris
            NODE_MAC=$(/sbin/ifconfig $(/sbin/route -n get default | awk '/interface/ {print $2}') | awk '/ether/ {print $2}')
            echo "Using MAC address from Solaris system information (${NODE_MAC})."
        else
            # Linux
            /sbin/ifconfig -a | grep eth0 &>/dev/null
            if [ ${?} -eq 0 ] ; then
                NODE_MAC=$(/sbin/ifconfig eth0 | grep HWaddr | awk '{print $5}')
                echo "Using MAC address from eth0 (${NODE_MAC})."
            else
                NODE_MAC=$(/sbin/ifconfig | head -1 | grep HWaddr | awk '{print $5}')
                echo "Using MAC address from first interface (${NODE_MAC})."   
            fi
        fi
    fi
    calculateNodeId "${NODE_MAC}" NODE_ID

    extractProperty "sslkspasswd" KEYSTORE_SSL_PASS "${INSTALL_DIR}/node/default/var/upgrade/keystore.properties"
    HIBERNATE_URL_HOSTS=$(echo "${HIBERNATE_URL}" | sed 's/[\/\?&=]/ /g' | awk '{print $2}')
    HIBERNATE_URL_DB=$(echo "${HIBERNATE_URL}" | sed 's/[\/\?&=]/ /g' | awk '{print $3}')
    echo -n "${HIBERNATE_URL_HOSTS}" | grep "," &>/dev/null
    if [ ${?} -ne 0 ] ; then
        # Single host configuration
        echo "Creating node.properties for single DB"
        extractPort HIBERNATE_PORT "${HIBERNATE_URL_HOSTS}" 3306
cat > "${INSTALL_DIR}/node/default/etc/conf/node.properties" <<-ENDOFNODEPROPERTIES
node.id = ${NODE_ID}
node.enabled = true
node.cluster.pass = ${KEYSTORE_SSL_PASS}
node.db.config.main.host = ${HIBERNATE_URL_HOSTS/%:*/}
node.db.config.main.port = ${HIBERNATE_PORT}
node.db.config.main.name = ${HIBERNATE_URL_DB}
node.db.config.main.user = ${HIBERNATE_USER}
node.db.config.main.pass = ${HIBERNATE_PASS}
ENDOFNODEPROPERTIES
    else
        # Failover configuration
        echo "Creating node.properties with failover DB"
        HIBERNATE_MASTER="${TEST/#*,/}"
        extractPort HIBERNATE_MASTER_PORT "${HIBERNATE_MASTER}" 3306

        HIBERNATE_FAILOVER="${TEST/%,*/}"
        extractPort HIBERNATE_FAILOVER_PORT "${HIBERNATE_FAILOVER}" 3306

cat > "${INSTALL_DIR}/node/default/etc/conf/node.properties" <<-ENDOFNODEPROPERTIES
node.id = ${NODE_ID}
node.enabled = true
node.cluster.pass = ${KEYSTORE_SSL_PASS}
node.db.clusterType = replicated
node.db.configs = main,failover
node.db.config.main.type = REPL_MASTER
node.db.config.main.host = ${HIBERNATE_MASTER/%:*/}
node.db.config.main.port = ${HIBERNATE_MASTER_PORT}
node.db.config.main.name = ${HIBERNATE_URL_DB}
node.db.config.main.user = ${HIBERNATE_USER}
node.db.config.main.pass = ${HIBERNATE_PASS}
node.db.config.failover.inheritFrom = main
node.db.config.failover.type = REPL_SLAVE
node.db.config.failover.host = ${HIBERNATE_FAILOVER/%:*/}
node.db.config.failover.port = ${HIBERNATE_FAILOVER_PORT}
ENDOFNODEPROPERTIES
    fi

    # cleanup 
    rm -f "${INSTALL_DIR}/node/default/var/upgrade/hibernate.properties"
fi

JAVA_PROFILE=""
if [ -f "/ssg/etc/profile.d/java.sh.rpmsave" ] && [ ! -d "/ssg/appliance" ] ; then
    JAVA_PROFILE="/ssg/etc/profile.d/java.sh.rpmsave"
fi
if [ -f "/ssg/etc/profile.d/java.sh" ] && [ ! -d "/ssg/appliance" ] ; then
    JAVA_PROFILE="/ssg/etc/profile.d/java.sh"
fi
if [ ! -z "${JAVA_PROFILE}" ] ; then
    echo "Importing Java VM configuration from ${JAVA_PROFILE}."
    source "${JAVA_PROFILE}"
    if [ -d "${SSG_JAVA_HOME}" ] ; then
        echo "node.java.path = ${SSG_JAVA_HOME}" >> "${INSTALL_DIR}/node/default/etc/conf/node.properties"
    fi
fi

echo "Setting permissions on modified files"
setPermissions "${INSTALL_DIR}/node/default/etc/conf" "layer7:gateway" 640
setPermissions "${INSTALL_DIR}/node/default/var" "gateway:gateway" 660
setPermissions "${INSTALL_DIR}/node/default/var/logs" "gateway:gateway" 660
setPermissions "${INSTALL_DIR}/node/default/var/upgrade" "gateway:gateway" 660

echo "Upgrade completed at $(date)"
