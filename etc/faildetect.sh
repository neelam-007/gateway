#/bin/sh
# $Id$
# programs used
IFCONFIG="/sbin/ifconfig"
GARP="/usr/local/bin/garp"
WGET="/usr/bin/wget"

# globals and configuration

EMAIL="jthorne@layer7tech.com"

HOST="192.168.1.231"
# cluster IP addresss

INTERFACE="eth0:0"
# alias interface

FILE="index.html"
# file on server to get

PORT=8080
# and port number

WAIT=1
# timeout for each try

TRIES=1
# retries

RETEST=1
# time to wait for retry

TRYAGAINFILE="/tmp/wget_failed_once"
# litter file

SLEEPRETRY=10
# and finally, how long do we wait to see if its still up
#
# Subroutines
#
success() {
	rm $TRYAGAINFILE
	sleep $SLEEPRETRY
	doget
}

failure() {
	echo "Failed get page"
	if ! [ -f $TRYAGAINFILE ] ; then
		echo "Setting first failure"
		echo
		touch $TRYAGAINFILE
		sleep $RETEST
		doget
	else
		echo "Failed Twice. Time for action"
		echo
		doiptakeover
		sleep $RETEST
		rm $TRYAGAINFILE
		doget	
		# 
	fi
}

doiptakeover() {
	# FIXME: currently Defanged
	echo "Taking over IP $HOST";
	echo "$IFCONFIG $INTERFACE $HOST"
	echo "Sending gratuitous arp"
	echo "$GARP $HOST"
	echo "echo 'SSG Failover' | mail -s 'SSG Failover' $EMAIL" 
}

doget() {
	# grab a file from the tomcat using wget
	# quiet, few retries, short timeout, configurable location
	if $WGET  -q -t $TRIES -T $WAIT http://$HOST:$PORT/$FILE ;  then 
		success
	else 
		RETVAL=$?
		failure $RETVAL
	fi
}

# after all those definitions now call the entry point.
doget
