# FILE /etc/profile.d/ssgruntimedefs.sh
# LAYER 7 TECHNOLOGIES
# 07-07-2003, flascelles
#
# Defines JAVA_HOME, TOMCAT_HOME, etc
# Edit at deployment time to ensure values are accurate
#

# define java home
JAVA_HOME=/usr/java/j2sdk1.4.1
export JAVA_HOME

# add to path
PATH=$PATH:$JAVA_HOME/bin
export PATH

# define tomcat home
TOMCAT_HOME=/usr/java/jakarta-tomcat-4.1.24
export TOMCAT_HOME

# aliases to start and stop ssg
alias startssg='$TOMCAT_HOME/bin/startup.sh'
alias stopssg='$TOMCAT_HOME/bin/shutdown.sh'
