#/bin/sh
# programs used
IFCONFIG="/sbin/ifconfig"
GARP="/usr/local/bin/garp"
WGET="/usr/bin/wget"
NC=/usr/bin/nc
# globals and configuration

EMAIL="jthorne@layer7tech.com"

HOST="10.0.0.25"
# cluster IP addresss

INTERFACE="eth0:0"
# alias interface

# file on server to get

PORT=3306
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
	rm -f $TRYAGAINFILE
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
	$IFCONFIG $INTERFACE $HOST
	echo "Sending gratuitous arp"
	$GARP -i eth0 -a $HOST
	$GARP -i eth0 -a $HOST
	echo "echo 'SSG Failover' | mail -s 'SSG Failover' $EMAIL" 
}

doget() {
	# grab a file from the tomcat using wget
	# quiet, few retries, short timeout, configurable location
	#if $WGET  -q -t $TRIES -T $WAIT http://$HOST:$PORT/$FILE ;  then 
	if echo " " | $NC -w $WAIT -o /tmp/foo $HOST $PORT > /tmp/bar;  then 
		success
	else 
		RETVAL=$?
		failure $RETVAL
	fi
}

# after all those definitions now call the entry point.
doget
