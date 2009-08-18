# LAYER 7 TECHNOLOGIES
# Defines settings for node specific options

function extractProperty() {
    declare EP_PROP=$1
    declare EP_ENV=$2
    declare EP_FILE=$3
    declare EP_EXPR=$(grep "${EP_PROP}" "${EP_FILE}" | sed "s/[ ]*${EP_PROP}[ ]*=//" )
    EP_TMP="$(echo ${EP_EXPR} | sed 's/^[ ]*//g')"
    export "${EP_ENV}"="${EP_TMP//\\}"
}

if [ ! -z "${SSGNODE}" ] ; then
    NODE_PROPS_PATH="${SSG_HOME}/node/${SSGNODE}/etc/conf/node.properties"
    NODE_DEFAULT_MAX_PERM_SIZE="128M"

    if [ -f "${NODE_PROPS_PATH}" ] ; then
        extractProperty "node.java.path" NODE_JAVA_HOME "${NODE_PROPS_PATH}"
        extractProperty "node.java.heap" NODE_JAVA_HEAP "${NODE_PROPS_PATH}"
        extractProperty "node.java.opts" NODE_JAVA_OPTS "${NODE_PROPS_PATH}"
        extractProperty "node.initial.admin.listenaddr" NODE_INIT_LISTEN_ADDR "${NODE_PROPS_PATH}"
        extractProperty "node.initial.admin.listenport" NODE_INIT_LISTEN_PORT "${NODE_PROPS_PATH}"

        if [ ! -z "${NODE_JAVA_HOME}" ] ; then
            SSG_JAVA_HOME="${NODE_JAVA_HOME}"       
            export SSG_JAVA_HOME
        fi

        if [ -z "${NODE_OPTS}" ] ; then
            NODE_OPTS=""
        fi
        if [ ! -z "${NODE_JAVA_HEAP}" ] || [ ! -z "${NODE_JAVA_OPTS}" ] ; then
            if [ ! -z "${NODE_JAVA_HEAP}" ] ; then
                NODE_OPTS="-Xmx${NODE_JAVA_HEAP}m"
            fi
            if [ ! -z "${NODE_JAVA_OPTS}" ] ; then
                NODE_OPTS="${NODE_OPTS} ${NODE_JAVA_OPTS}"
            fi
        fi

        if [ ! -z "${NODE_INIT_LISTEN_ADDR}" ] && [ ! -z "${NODE_INIT_LISTEN_PORT}" ] ; then
            if [ ! -z "${NODE_INIT_LISTEN_ADDR}" ] && [ "${NODE_INIT_LISTEN_ADDR}" != "*" ] && [ "${NODE_INIT_LISTEN_ADDR}" != "0.0.0.0" ]; then
                NODE_OPTS="${NODE_OPTS} -Dcom.l7tech.server.listener.initaddr=${NODE_INIT_LISTEN_ADDR}"            
            fi

            if [ ! -z "${NODE_INIT_LISTEN_PORT}" ] ; then
                NODE_OPTS="${NODE_OPTS} -Dcom.l7tech.server.listener.initport=${NODE_INIT_LISTEN_PORT}"
            fi
        fi

        echo "${NODE_OPTS}" | grep "\-XX:MaxPermSize" &>/dev/null
        if [ ${?} -ne 0 ] ; then
            NODE_OPTS="${NODE_OPTS} -XX:MaxPermSize=${NODE_DEFAULT_MAX_PERM_SIZE}"
        fi

        export NODE_OPTS

        unset NODE_JAVA_HOME
        unset NODE_JAVA_HEAP
        unset NODE_JAVA_OPTS
    fi
    unset NODE_PROPS_PATH
    unset NODE_DEFAULT_MAX_PERM_SIZE
fi
