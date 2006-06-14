#!/bin/sh
# /etc/profile.d/ssgruntimedefs.sh
# LAYER 7 TECHNOLOGIES
# Defines JAVA_HOME, TOMCAT_HOME, etc
# and tunes the JVM for whatever is installed

release=`cat /etc/redhat-release`
# historical compatibility
# stuff 

if [ "$release" = "Red Hat Linux release 8.0 (Psyche)" ]; then
	old_rel=1;
fi

if [ "$release" = "Red Hat Linux release 9 (Shrike)" ]; then
	old_rel=1;
fi

unset release

if [ "$old_rel" = 1 ]; then
	export LD_ASSUME_KERNEL="2.2.5"
fi
unset old_rel

# The meat
cpucount=`grep "cpu MHz" /proc/cpuinfo  |wc -l| tr -d \ `

let cpucount="$cpucount*1"; # sanitize

# non-portable to non-english meminfo of which I don't know of any
# at the moment
system_ram=`grep MemTotal /proc/meminfo |cut -c 15-23`

multiplier="2/3"

let java_ram="$system_ram*$multiplier" 
if [ `expr $java_ram \> 2274412` == 1 ]; then
	# we have more ram than java can use
	# FIXME: when we start running 64 bit JVM
	java_ram=2274412;
fi
let maxnewsize="$java_ram*75/100"

default_java_opts="-Djavax.xml.transform.TransformerFactory=org.apache.xalan.processor.TransformerFactoryImpl"
default_java_opts="$default_java_opts -Dfile.encoding=UTF-8 -Dsun.net.inetaddr.ttl=30 -Dnetworkaddress.cache.ttl=30"
default_java_opts="$default_java_opts -server -Djava.awt.headless=true -XX:CompileThreshold=1500"
default_java_opts="$default_java_opts -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger"
default_java_opts="$default_java_opts -Xmx${java_ram}k -Xms${java_ram}k -Xmn${maxnewsize}k -Xss192k "

unset system_ram
unset multiplier
unset java_ram
unset maxnewsize


if [ -e /ssg/etc/conf/JVM ]; then
	# this means that if /ssg/etc/conf/JVM exists
	JAVA_HOME=`cat /ssg/etc/conf/JVM`
	# we use its contents to override the supplied JDK
else
	JAVA_HOME="/ssg/jdk"
fi

let gcthreads="$cpucount-1"

if [ `expr $JAVA_HOME : ".*1\.4.*"` != 0 ]; then 
	# default_java_opts="$default_java_opts -XX:+UseParNewGC -XX:ParallelGCThreads=$cpucount "
	default_java_opts="$default_java_opts -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=90"
	default_java_opts="$default_java_opts -XX:SurvivorRatio=128 -XX:MaxTenuringThreshold=0"
elif [ `expr $JAVA_HOME : ".*1\.5\.0_06*"` != 0 ]; then
	# fujitsu did a benchmark using this tuning: 
	# -Xbatch -Xss256K -Xms360G -Xmx360G -Xmn300G -XX:+UseISM -XX:+AggressiveHeap -XX:+UseParallelOldGC 
	# This will likely have big latency times but the best performance
	default_java_opts="$default_java_opts -Xbatch -XX:+AggressiveHeap XX:+UseParallelOldGC"
	# Only if 1.5.0_06 
	# use the jvm file above to set this
	# Sun tried this:
	# -Xbatch -XX:+AggressiveHeap -Xss256k -Xmx176g -Xms176g -Xmn133g -XX:+UseBiasedLocking -classpath
else 
	# Next two lines are consistent low latency at the expense of 40% performance
	# default_java_opts="$default_java_opts -XX:ParallelGCThreads=$gcthreads -XX:+UseConcMarkSweepGC"
	# default_java_opts="$default_java_opts -XX:+UseParNewGC -XX:SurvivorRatio=8 -XX:TargetSurvivorRatio=90 -XX:MaxTenuringThreshold=15"
	# default options include batch: This may be commented out: it pauses all threads during GC
	default_java_opts="$default_java_opts -Xbatch" 
	# uncomment ONE of these gc options
	# Standard
	default_java_opts="$default_java_opts -XX:+UseParallelGC " 
	# or comment them all out to let the JVM decide 
	# throughput collector with an added "maximum pause time
	#default_java_opts="$default_java_opts -XX:+UseParallelGC -XX:MaxGCPauseMillis=1000" 
	# throughput collector "new"
	#default_java_opts="$default_java_opts -XX+UseParNewGC" 
	# concurrent low pause collector: Showed low performance
	#default_java_opts="$default_java_opts -XX+UseConcMarkSweepGC"
fi
unset gcthreads
unset cpucount

ulimit -s 2048

SSG_HOME=/ssg
TOMCAT_HOME=/ssg/tomcat
CATALINA_PID=/ssg/etc/conf/ssg.pid

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

# May have to restore something like this for OS other than linux
# if $cygwin; then
#    mac=''
#else
#    mac=`/sbin/ifconfig eth0 |awk '/HWaddr/ {print $5}'`
#fi
#
#if [ ! -z $mac ]; then
#        default_java_opts="$default_java_opts -Dcom.l7tech.cluster.macAddress=$mac"
#fi

# aliases to start and stop ssg
alias startssg='/etc/rc.d/init.d/ssg start'
alias stopssg='/etc/rc.d/init.d/ssg stop'

JAVA_OPTS=$default_java_opts;
unset default_java_opts

export SSG_HOME
export JAVA_HOME
export TOMCAT_HOME
export JAVA_OPTS
export CATALINA_PID

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

unset cygwin;

if [ -e  /usr/local/Tarari ]; then
	export TARARIROOT=/usr/local/Tarari
	export PATH=$TARARIROOT/bin:$PATH
	export LD_LIBRARY_PATH=$TARARIROOT/lib:$LD_LIBRARY_PATH
	export KERNELSOURCE=/usr/src/linux
	export JAVA_OPTS="-Dcom.l7tech.common.xml.tarari.enable=true $JAVA_OPTS"
fi


if [ -e "/opt/oracle" ] ; then
	export ORACLE_HOME=/opt/oracle/product/9.2
	export PATH=$PATH:$ORACLE_HOME/bin
	export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$ORACLE_HOME/lib
	export NLS_LANG=AMERICAN_AMERICA.US7ASCII
fi

