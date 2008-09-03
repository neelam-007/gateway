# LAYER 7 TECHNOLOGIES
# Defines JAVA_HOME, etc

ulimit -s 2048

# add to path
if [ -z "${PATH}" ] ; then
        PATH="$JAVA_HOME/bin:SSG_HOME/bin"
else 
	if ! echo $PATH | /bin/egrep -q "(^|:)$JAVA_HOME/bin($|:)" ; then
        	PATH="$PATH:$JAVA_HOME/bin"
	fi

	if ! echo $PATH | /bin/egrep -q "(^|:)$SSG_HOME/bin($|:)" ; then
        	PATH="$PATH:$SSG_HOME/bin"
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

# Setting larger permsize for java 1.6
NODE_OPTS="-Xmx${java_ram}k -XX:MaxPermSize=128M -Xss256k"

export NODE_OPTS
export PATH

unset system_ram
unset multiplier
unset java_ram
