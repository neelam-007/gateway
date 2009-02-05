# LAYER 7 TECHNOLOGIES
# Defines SSG_JAVA_HOME, etc

ulimit -s 2048

function extractProperty() {
    declare EP_PROP=$1
    declare EP_ENV=$2
    declare EP_FILE=$3
    declare EP_EXPR=$(grep "${EP_PROP}" "${EP_FILE}" | sed "s/[ ]*${EP_PROP}[ ]*=//" )
    export "${EP_ENV}"="${EP_EXPR}"
}

# add to path
if [ -z "${PATH}" ] ; then
        PATH="SSG_HOME/runtime/bin"
else 
	if ! echo $PATH | /bin/egrep -q "(^|:)$SSG_HOME/runtime/bin($|:)" ; then
        	PATH="$PATH:$SSG_HOME/runtime/bin"
	fi
fi

# aliases to start and stop ssg
alias startssg='/etc/rc.d/init.d/ssg start'
alias stopssg='/etc/rc.d/init.d/ssg stop'

system_ram=`grep MemTotal /proc/meminfo |cut -c 15-23`
# Maximum amount of RAM to use
multiplier="2/3"
#
let java_ram="$system_ram*$multiplier"
if [ `expr $java_ram \> 2074412` == 1 ]; then
	# we have more ram than java can use
	# FIXME: when we start running 64 bit JVM
	java_ram=2074412;
 	# CAP at 2 gigs
fi

SSG_JAVA_HOME="/opt/SecureSpan/JDK"

# Setting larger permsize for java 1.6
NODE_OPTS="-Xmx${java_ram}k -XX:MaxPermSize=128M -Xss256k"

SSGSCA=""
if [ -f "/opt/SecureSpan/Appliance/controller/etc/host.properties" ] ; then
    extractProperty "host.sca" SSGSCA "/opt/SecureSpan/Appliance/controller/etc/host.properties"
fi
if [ -z "${SSGSCA}" ] ; then
   SSGSCA="false"
fi

export SSG_JAVA_HOME
export NODE_OPTS
export PATH
export SSGSCA

unset system_ram
unset multiplier
unset java_ram
