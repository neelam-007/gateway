# FILE /etc/profile.d/ssgruntimedefs.sh
# LAYER 7 TECHNOLOGIES
# 07-07-2003, flascelles
#
# $Id$
#
# Defines JAVA_HOME, TOMCAT_HOME, etc
# Edit at deployment time to ensure values are accurate
#
# This is an attempt at self tuning
# 
echo "Experimental ssgruntime setup"
release=`cat /etc/redhat-release`

if [ "$release" = "Red Hat Linux release 8.0 (Psyche)" ]; then
	old_rel=1;
fi

if [ "$release" = "Red Hat Linux release 9 (Shrike)" ]; then
	old_rel=1;
fi

cpucount=`cat /proc/cpuinfo  |grep "cpu MHz" |wc -l| tr -d \ `

let cpucount="$cpucount*1"; # sanitize

system_ram=`cat /proc/meminfo |grep MemTotal |cut -c 15-23`

multiplier="3/5"

let java_ram="$system_ram*$multiplier" 

default_java_opts="-Xms${java_ram}k -Xmx${java_ram}k -Xss256k -server "

let maxnewsize="$java_ram/2"

default_java_opts="$default_java_opts -XX:NewSize=${maxnewsize}k -XX:MaxNewSize=${maxnewsize}k "


if [ -e /ssg/etc/conf/JVM ]; then
	JAVA_HOME=`cat /ssg/etc/conf/JVM`
	if [ $cpucount = 1 ]; then
		echo -n ""
		# single cpu
	else
		# java 1.5 doesn't seem to want the other options
		default_java_opts="$default_java_opts -XX:+UseParNewGC -XX:ParallelGCThreads=$cpucount "
	fi
else
	# default is 1.4.2
	export JAVA_HOME="/ssg/j2sdk1.4.2_05"
	if [ $old_rel = 1 ]; then 
		export LD_ASSUME_KERNEL="2.2.5"
	fi
	if [ $cpucount = 1 ]; then
		echo -n ""
		# single cpu
	else
		default_java_opts="$default_java_opts -XX:+UseParNewGC -XX:ParallelGCThreads=$cpucount "
		default_java_opts="$default_java_opts -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=90"
		default_java_opts="$default_java_opts -XX:SurvivorRatio=128 -XX:MaxTenuringThreshold=0"
	fi
fi

ulimit -s 2048

export SSG_HOME=/ssg
TOMCAT_HOME=/ssg/tomcat/

# Under cygwin?.
cygwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
esac

# define java home
if $cygwin; then
    JAVA_HOME=`cygpath --path --unix "$JAVA_HOME"`
fi

# add to path
PATH=$PATH:$JAVA_HOME/bin:$SSG_HOME/bin

#add to the ld path (shared native libraries)
if $cygwin; then
    export PATH=$PATH:$SSG_HOME/lib
else
    export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$SSG_HOME/lib
fi
export PATH


if [ -z "$JAVA_OPTS" ]; then
    JAVA_OPTS=$default_java_opts
else
    JAVA_OPTS="$JAVA_OPTS $default_java_opts"
fi
# Set Java system properties
if  [ -e "/ssg/etc/conf/cluster_hostname" ];
then
        JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true -Djava.rmi.server.hostname=`cat /ssg/etc/conf/cluster_hostname`"
else
        JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true -Djava.rmi.server.hostname=`hostname -f`"
fi

if $cygwin; then
    mac=''
else
    mac=`/sbin/ifconfig eth0 |awk '/HWaddr/ {print $5}'`
fi

if [ ! -z $mac ]; then
        JAVA_OPTS="$JAVA_OPTS -Dcom.l7tech.cluster.macAddress=$mac"
fi

unset default_java_opts
export JAVA_OPTS

# define tomcat home
if $cygwin; then
    TOMCAT_HOME=`cygpath --path --unix "$TOMCAT_HOME"`
fi

if $cygwin; then
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
    TOMCAT_HOME=`cygpath --path --windows "$TOMCAT_HOME"`
fi
export JAVA_HOME
export TOMCAT_HOME

# aliases to start and stop ssg
alias startssg='/etc/rc.d/init.d/ssg start'
alias stopssg='/etc/rc.d/init.d/ssg stop'

if [ -e "/opt/oracle" ] ; then
	export ORACLE_HOME=/opt/oracle/product/9.2
	export PATH=$PATH:$ORACLE_HOME/bin
	export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$ORACLE_HOME/lib
	export NLS_LANG=AMERICAN_AMERICA.US7ASCII
fi

