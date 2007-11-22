#!/bin/bash
# LAYER 7 TECHNOLOGIES
# Defines JAVA_HOME, etc

cd `dirname $0`
pushd .. > /dev/null
SSG_HOME=`pwd`
popd > /dev/null

ulimit -s 2048

# add to path
PATH=$PATH:$JAVA_HOME/bin:$SSG_HOME/bin

# Set rmi hostname for cluster correctness

# aliases to start and stop ssg
alias startssg='/etc/rc.d/init.d/ssg start'
alias stopssg='/etc/rc.d/init.d/ssg stop'

if [ -e  /usr/local/Tarari ]; then
	export TARARIROOT=/usr/local/Tarari
	export PATH=$TARARIROOT/bin:$PATH
	export LD_LIBRARY_PATH=$TARARIROOT/lib:$LD_LIBRARY_PATH
fi

#!/bin/bash

ALL_PARTITIONS=`ls ${SSG_HOME}/etc/conf/partitions/ | grep -v template_`
PARTITION_COUNT=`echo ${ALL_PARTITIONS} | wc -w`
number_of_partitions=${PARTITION_COUNT}

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

if [ ${number_of_partitions} -gt 1 ]; then
  let java_ram="${system_ram}*${multiplier}/${number_of_partitions}"
  if [ `expr $java_ram \< 524288 ` == 1 ]; then
      # Set a floor, prevent OOM situation
    java_ram=524288;
  fi
fi

# Setting larger permsize for java 1.6
PARTITION_OPTS="-Xmx${java_ram}k -XX:MaxPermSize=128M -Xss256k"

unset system_ram
unset multiplier
unset java_ram

export PARTITION_OPTS

# End Per-Partition
#########################################################################

export PATH
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$SSG_HOME/lib