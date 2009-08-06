# LAYER 7 TECHNOLOGIES
# Defines SSG_JAVA_HOME, etc

# Set to 1 if using 32-bit jvm, to avoid trying to allocate more than 2gb of ram for the jvm
using32bitjvm=0

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

system_ram=`grep MemTotal /proc/meminfo |cut -c 15-23`
# Maximum amount of RAM to use
multiplier="2/3"
#
let java_ram="$system_ram*$multiplier"
if [ $using32bitjvm -ne 0 -a `expr $java_ram \> 2074412` == 1 ]; then
	# we have more ram than java can use
	# FIXME: when we start running 64 bit JVM
	java_ram=2074412;
 	# CAP at 2 gigs
fi

SSG_JAVA_HOME="/opt/SecureSpan/JDK"

# Setting larger permsize for java 1.6
NODE_OPTS="-Xmx${java_ram}k -Xss256k"

export SSG_JAVA_HOME
export NODE_OPTS
export PATH

unset system_ram
unset multiplier
unset java_ram
