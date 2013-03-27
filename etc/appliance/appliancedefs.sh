# LAYER 7 TECHNOLOGIES
# Defines SSG_JAVA_HOME, etc

# Set to 1 if using 32-bit jvm, to avoid trying to allocate more than 2gb of ram for the jvm
usebigmemory=1
jvmarch=x86_64

ulimit -s 2048

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

system_ram=`grep MemTotal /proc/meminfo | awk '{print $2}'`
# Maximum amount of RAM to use
multiplier="2/3"
#
let java_ram="$system_ram*$multiplier"
#if we have a lot of ram, we need to check whether we are enabled to use it or not.
if [ `expr $java_ram \> 2074412` == 1 ] ; then
    if [ $usebigmemory -eq 1 ]; then
	    # we have more ram than java can use
	    # FIXME: when we start running 64 bit JVM
	    java_ram=2074412;
 	    # CAP at 2 gigs
    else
        java_ram=4194304;
    fi
fi

SSG_JAVA_HOME="/opt/SecureSpan/JDK"

# Setting larger permsize for java 1.6
NODE_OPTS="-Xmx${java_ram}k -Xss256k -XX:+UseParallelOldGC"
[ "${jvmarch}" == "x86_64" ] && NODE_OPTS="$NODE_OPTS -XX:+UseCompressedOops"

# Reserve ports 7001 and 7100 on the appliance
NODE_OPTS="$NODE_OPTS -Dcom.l7tech.server.transport.reservedPorts=7001,7100"

export SSG_JAVA_HOME
export NODE_OPTS
export PATH

unset system_ram
unset multiplier
unset java_ram
