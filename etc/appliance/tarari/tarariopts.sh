# LAYER 7 TECHNOLOGIES
# Defines TARARIROOT, SSGTARARI,  etc

function extractProperty() {
    declare EP_PROP=$1
    declare EP_ENV=$2
    declare EP_FILE=$3
    declare EP_EXPR=$(grep "${EP_PROP}" "${EP_FILE}" | sed "s/[ ]*${EP_PROP}[ ]*=//" )
    export "${EP_ENV}"="$(echo ${EP_EXPR} | sed 's/^[ ]*//g')"
}

if [ -e /usr/local/Tarari ]; then
	TARARIROOT=/usr/local/Tarari
    SSGTARARI=""
    if [ -f "/opt/SecureSpan/Controller/etc/host.properties" ] ; then
        extractProperty "host.tarari" SSGTARARI "/opt/SecureSpan/Controller/etc/host.properties"
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