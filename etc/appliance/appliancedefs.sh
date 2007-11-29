# LAYER 7 TECHNOLOGIES
# Defines JAVA_HOME, etc

ulimit -s 2048

# add to path
PATH=$PATH:$JAVA_HOME/bin:$SSG_HOME/bin


# aliases to start and stop ssg
alias startssg='/etc/rc.d/init.d/ssg start'
alias stopssg='/etc/rc.d/init.d/ssg stop'

ALL_PARTITIONS=`ls ${SSG_HOME}/etc/conf/partitions/ | grep -v template_`
PARTITION_COUNT=`echo ${ALL_PARTITIONS} | wc -w`

system_ram=`grep MemTotal /proc/meminfo |cut -c 15-23`
# Maximum amount of RAM for _SINGLE_ partition
multiplier="2/3"
#
let java_ram="$system_ram*$multiplier"
if [ `expr $java_ram \> 2074412` == 1 ]; then
	# we have more ram than java can use
	# FIXME: when we start running 64 bit JVM
	java_ram=2074412;
	# CAP at 2 gigs
fi

if [ ${PARTITION_COUNT} -gt 1 ]; then
  let java_ram="${system_ram}*${multiplier}/${PARTITION_COUNT}"
  if [ `expr $java_ram \< 524288 ` == 1 ]; then
      # Set a floor, prevent OOM situation
    java_ram=524288;
  fi
fi

# Setting larger permsize for java 1.6
PARTITION_OPTS="-Xmx${java_ram}k -XX:MaxPermSize=128M -Xss256k"

# End Per-Partition
#########################################################################


LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$SSG_HOME/lib

export LD_LIBRARY_PATH
export PARTITION_OPTS
export PATH

unset system_ram
unset multiplier
unset java_ram
