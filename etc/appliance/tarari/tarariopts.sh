# LAYER 7 TECHNOLOGIES
# Defines TARARIROOT, SSGTARARI,  etc

function extractProperty() {
    declare EP_PROP=$1
    declare EP_ENV=$2
    declare EP_FILE=$3
    declare EP_EXPR=$(grep "${EP_PROP}" "${EP_FILE}" 2>/dev/null | sed "s/[ ]*${EP_PROP}[ ]*=//" )
    declare EP_TMP="$(echo ${EP_EXPR} | sed 's/^[ ]*//g')"
    export "${EP_ENV}"="${EP_TMP//\\}"
}

if [ -e /usr/local/Tarari ]; then
    NODE_PROPS_PATH="${SSG_HOME}/node/${SSGNODE}/etc/conf/node.properties"
    TARARIROOT=/usr/local/Tarari
    SSGTARARI=""
    if [ -f "/opt/SecureSpan/Controller/etc/host.properties" ] ; then
        extractProperty "node.tarari.enabled" SSGTARARI "${NODE_PROPS_PATH}"
    fi
    if [ -z "${SSGTARARI}" ] ; then
        SSGTARARI="true"
    fi

    if ! echo $PATH | /bin/egrep -q "(^|:)$TARARIROOT/bin($|:)" ; then
        PATH=$TARARIROOT/bin:$PATH
    fi

    if [ -z "${LD_LIBRARY_PATH}" ] ; then
        LD_LIBRARY_PATH=$TARARIROOT/lib
    elif ! echo $LD_LIBRARY_PATH | /bin/egrep -q "(^|:)$TARARIROOT/lib($|:)" ; then
        LD_LIBRARY_PATH=$TARARIROOT/lib:$LD_LIBRARY_PATH
    fi                                 

    export TARARIROOT
    export SSGTARARI
    export LD_LIBRARY_PATH
    export PATH
    export XCX_JOB_MODE=sqb,1000
fi