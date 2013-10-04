# LAYER 7 TECHNOLOGIES
# SET SITEMINDER ENVIRONMENT
# This script is meant to be run in the profile for the SSG

CAROOT=/opt/CA
CALIBS=""
SM_JAVA_OPTS=""

if [ -d "${CAROOT}" ] ; then

    WHATOS=`uname -s`
	WHATARCHAMI=`arch`

	case ${WHATOS} in
		Linux)
			case ${WHATARCHAMI} in
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
					# Unsupported architecture for Linux
					CALIBS=""
                    ;;
			esac
			;;
		SunOS)
			case ${WHATARCHAMI} in
				sun4)
					CALIBS=${CAROOT}/sdk/bin64
                    SM_JAVA_OPTS="${SM_JAVA_OPTS} -Dcom.l7tech.server.siteminder.enabled=true"
					;;
				i86pc)
					CALIBS=${CAROOT}/sdk/bin64
                    SM_JAVA_OPTS="${SM_JAVA_OPTS} -Dcom.l7tech.server.siteminder.enabled=true"
                    ;;
				*)
					# Unknown processor type for Solaris"
					;;
			esac
			;;
		*)
			# unsupported OS
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