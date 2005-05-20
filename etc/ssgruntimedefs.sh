# FILE /etc/profile.d/ssgruntimedefs.sh
# LAYER 7 TECHNOLOGIES
# 07-07-2003, flascelles
#
#
# Defines JAVA_HOME, TOMCAT_HOME, etc
# Edit at deployment time to ensure values are accurate
#
# This is an attempt at self tuning
# 
release=`cat /etc/redhat-release`

if [ "$release" = "Red Hat Linux release 8.0 (Psyche)" ]; then
	old_rel=1;
fi

if [ "$release" = "Red Hat Linux release 9 (Shrike)" ]; then
	old_rel=1;
fi
# options from the sun specjbb2000 run
#Disks            2 x 73GB Internal    | Command Line     java -d64 -Xbatch   
#                 ULTRA320 SCSI        |                  -Xmn8g -Xms12g      
#Other H/W                             |                  -Xmx12g             
#                                      |                  -XX:+AggressiveHeap 
#                                      |                  -Xss256k   
#

cpucount=`cat /proc/cpuinfo  |grep "cpu MHz" |wc -l| tr -d \ `

let cpucount="$cpucount*1"; # sanitize

system_ram=`cat /proc/meminfo |grep MemTotal |cut -c 15-23`

multiplier="2/3"
let java_ram="$system_ram*$multiplier" 
let maxnewsize="$java_ram/2"

default_java_opts="-Xms${java_ram}k -Xmx${java_ram}k -Xss256k -server -Djava.awt.headless=true -XX:CompileThreshold=1500 "
default_java_opts="$default_java_opts -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger "
default_java_opts="$default_java_opts -XX:NewSize=${maxnewsize}k -XX:MaxNewSize=${maxnewsize}k "


if [ -e /ssg/etc/conf/JVM ]; then
	JAVA_HOME=`cat /ssg/etc/conf/JVM`
else
	JAVA_HOME="/ssg/jdk1.5.0_02"
fi

if [ $cpucount = 1 ]; then
	# single cpu
	default_java_opts="$default_java_opts -XX:+DisableExplicitGC "
else
	if [ `expr $JAVA_HOME : ".*1\.4.*"` != 0 ]; then 
		default_java_opts="$default_java_opts -XX:+UseParNewGC -XX:ParallelGCThreads=$cpucount "
		default_java_opts="$default_java_opts -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=90"
		default_java_opts="$default_java_opts -XX:SurvivorRatio=128 -XX:MaxTenuringThreshold=0"
	else 
		default_java_opts="$default_java_opts -XX:+DisableExplicitGC -XX:+UseParallelGC"
	fi
fi

ulimit -s 2048

SSG_HOME=/ssg
TOMCAT_HOME=/ssg/tomcat/

# Under cygwin?.
cygwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
esac

# add to path
PATH=$PATH:$JAVA_HOME/bin:$SSG_HOME/bin
# Set Java system properties
if  [ -e "/ssg/etc/conf/cluster_hostname" ];
then
        default_java_opts="$default_java_opts -Djava.rmi.server.hostname=`cat /ssg/etc/conf/cluster_hostname`"
else
        default_java_opts="$default_java_opts -Djava.rmi.server.hostname=`hostname -f`"
fi

if $cygwin; then
    mac=''
else
    mac=`/sbin/ifconfig eth0 |awk '/HWaddr/ {print $5}'`
fi

if [ ! -z $mac ]; then
        default_java_opts="$default_java_opts -Dcom.l7tech.cluster.macAddress=$mac"
fi

# aliases to start and stop ssg
alias startssg='/etc/rc.d/init.d/ssg start'
alias stopssg='/etc/rc.d/init.d/ssg stop'


#if [ -z "$JAVA_OPTS" ]; then
    # IF java opts are empty, use these we just built
    #, otherwise don't replace
    JAVA_OPTS=$default_java_opts;
#fi

export SSG_HOME
export JAVA_HOME
export TOMCAT_HOME
export JAVA_OPTS

# define tomcat home
if $cygwin; then
    export PATH=$PATH:$SSG_HOME/lib
    SSG_HOME=`cygpath --path --unix "$SSG_HOME"`
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
    TOMCAT_HOME=`cygpath --path --windows "$TOMCAT_HOME"`
else
    export PATH
    #add to the ld path (shared native libraries)
    export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$SSG_HOME/lib
fi

if [ -e "/opt/oracle" ] ; then
	export ORACLE_HOME=/opt/oracle/product/9.2
	export PATH=$PATH:$ORACLE_HOME/bin
	export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$ORACLE_HOME/lib
	export NLS_LANG=AMERICAN_AMERICA.US7ASCII
fi

if [ "$old_rel" = 1 ]; then 
	export LD_ASSUME_KERNEL="2.2.5"
fi

