# FILE /etc/profile.d/ssgruntimedefs.sh
# LAYER 7 TECHNOLOGIES
# 07-07-2003, flascelles
# $Id$
# Defines JAVA_HOME, TOMCAT_HOME, etc
# Edit at deployment time to ensure values are accurate
#

export SSG_HOME=/ssg

# define java home
JAVA_HOME=/usr/java/j2sdk1.4.1
export JAVA_HOME

# add to path
PATH=$PATH:$JAVA_HOME/bin:$SSG_HOME/bin
export PATH

# Set Java system properties
export JAVA_OPTS=-Djini.server.hostname=`hostname`

# define tomcat home
TOMCAT_HOME=/usr/java/tomcat-4.1.27-l7p2
export TOMCAT_HOME

# aliases to start and stop ssg
alias startssg='$TOMCAT_HOME/bin/catalina.sh start -security'
alias stopssg='$TOMCAT_HOME/bin/shutdown.sh'
