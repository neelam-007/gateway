#!/bin/bash
#########################################################################
# Per-Partition
#

this_is_a_partition=${1}
number_of_partitions=${2}


if [ -z "${this_is_a_partition}" ] ; then
	this_is_a_partition="false"
fi


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


if [ "${this_is_a_partition}" == "true" ] ; then
	if [ ${number_of_partitions} -gt 1 ]; then
	  let java_ram="${system_ram}*${multiplier}/${number_of_partitions}"
	  if [ `expr $java_ram \< 524288 ` == 1 ]; then
	      # Set a floor, prevent OOM situation
		java_ram=524288;
	  fi
	fi
fi

# Setting larger permsize for java 1.6
partition_opts="-Xmx${java_ram}k -XX:MaxPermSize=128M -Xss256k"

export partition_opts
unset system_ram
unset multiplier
unset java_ram

# End Per-Partition
#########################################################################
