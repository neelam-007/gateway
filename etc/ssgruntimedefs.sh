# FILE /etc/profile.d/ssgruntimedefs.sh
# LAYER 7 TECHNOLOGIES
# 07-07-2003, flascelles
#
# $Id$
#
# Defines JAVA_HOME, TOMCAT_HOME, etc
# Edit at deployment time to ensure values are accurate
#

default_java_opts="-Xmx768m -Xss1m"
export SSG_HOME=/ssg
JAVA_HOME=/ssg/j2sdk1.4.2_05
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
	JAVA_OPTS="$JAVA_OPTS -Djini.server.hostname=`cat /ssg/etc/conf/cluster_hostname`"
else
	JAVA_OPTS="$JAVA_OPTS -Djini.server.hostname=`hostname -f`"
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
alias startssg='$TOMCAT_HOME/bin/catalina.sh start -security'
alias stopssg='$TOMCAT_HOME/bin/shutdown.sh'

if [ -e "/opt/oracle" ] ; then
	export ORACLE_HOME=/opt/oracle/product/9.2
	export PATH=$PATH:$ORACLE_HOME/bin
	export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$ORACLE_HOME/lib
	export NLS_LANG=AMERICAN_AMERICA.US7ASCII
fi

