# FILE /etc/profile.d/ssgruntimedefs.sh
# LAYER 7 TECHNOLOGIES
# 07-07-2003, flascelles
# $Id$
# Defines JAVA_HOME, TOMCAT_HOME, etc
# Edit at deployment time to ensure values are accurate
#

# Under cygwin?.
cygwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
esac

export SSG_HOME=/ssg

# define java home
JAVA_HOME=/ssg/j2sdk1.4.2_04
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

# Set Java system properties
if  [ -e "/ssg/etc/conf/cluster_hostname" ];
then
	export JAVA_OPTS=-Djini.server.hostname=`cat /ssg/etc/conf/cluster_hostname`
else
	export JAVA_OPTS=-Djini.server.hostname=`hostname`
fi

# define tomcat home
TOMCAT_HOME="/ssg/tomcat/"
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
