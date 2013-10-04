# LAYER 7 TECHNOLOGIES
# SET SITEMINDER ENVIRONMENT FOR LINUX OS
# This script is meant to be run in the profile for the SSG

CAROOT=/opt/CA
CALIBS=""
SM_JAVA_OPTS=""

if [ -d "${CAROOT}" ] ; then
    MYARCH=`arch`
    case ${MYARCH} in
        x86_64)
            CALIBS=${CAROOT}/sdk/bin64
            SM_JAVA_OPTS="${SM_JAVA_OPTS} -Dcom.l7tech.server.siteminder.enabled=true"
            ;;
        i686)
            CALIBS=${CAROOT}/sdk/bin
            SM_JAVA_OPTS="${SM_JAVA_OPTS} -Dcom.l7tech.server.smreghost.path=/opt/CA/sdk/bin"
            SM_JAVA_OPTS="${SM_JAVA_OPTS} -Dcom.l7tech.server.siteminder.enabled=true"
            ;;
        *)
            CALIBS=""
            ;;
    esac

    if [ -z "${LD_LIBRARY_PATH}" ] ; then
        LD_LIBRARY_PATH=${CALIBS}
    elif ! echo ${LD_LIBRARY_PATH} | /bin/egrep -q "(^|:)${CALIBS}($|:)" ; then
        LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:${CALIBS}
    fi

    CAPKIHOME=${CAROOT}/CAPKI
    export CAROOT LD_LIBRARY_PATH CAPKIHOME

    SSG_JAVA_OPTS="${SSG_JAVA_OPTS} ${SM_JAVA_OPTS}"

    export SSG_JAVA_OPTS

fi