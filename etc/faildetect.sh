#/bin/sh

# programs used
IFCONFIG="/sbin/ifconfig"
GARP="/usr/local/bin/garp"
WGET="/usr/bin/wget"
PING="/bin/ping" 
ARP="/sbin/arp"
# globals and configuration

EMAIL="jthorne@layer7tech.com"

THISNODE=192.168.1.105
OTHERNODE=192.168.1.222

HOST="192.168.1.231"
# cluster IP addresss

INTERFACE="eth0:0"
# alias interface

FILE="ssg/wsil"
# file on server to get
# WSIL proves app is alive.

PORT=8080
# and port number

WAIT=1
# timeout for each try

TRIES=1
# retries

RETEST=2
# time to wait for retry

TRYAGAINFILE="/tmp/wget_failed_once"
# litter file

SLEEPRETRY=10
# and finally, how long do we wait to see if its still up
#
# Subroutines
#
success() {
	echo -n "."
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
		echo "Cue soundtrack"
		echo "Testing to see if $HOST is alive"
		if $PING -w 3 -n -q -c 3 $HOST; then
			# damn, webservice is down but the IP is up
			# now what do we do.
			shoottheothernodeinthehead
			sleep 2
			doiptakeover
		else 
			doiptakeover
		fi
		sleep $RETEST
		rm $TRYAGAINFILE
		doget	
		# 
	fi
}

shoottheothernodeinthehead() {
	echo "echo 'SSG software Failed, host still alive' | mail -s 'SSG Failed' $EMAIL" 
	echo "need to tell that other node to stfu"
	echo 'ssh root@$OTHERNODE -c "$IFCONFIG $INTERFACE down"'
}

doiptakeover() {
	echo "Taking over IP $HOST";
	echo "Bringing up interface $INTERFACE";
	$IFCONFIG $INTERFACE $HOST
	echo "Sending gratuitous arp"
	$GARP -i $INTERFACE -a $HOST
	echo "echo 'SSG Failover' | mail -s 'SSG Failover' $EMAIL" 
}

doget() {
	# empty my arp cache, just in case the remote host just went down
	# this would wait for the arp timeout otherwise.
	arpentry=`$ARP -e | grep $HOST | grep -v incomplete`
	if [ "$arpentry" != "" ] ; then
		echo "Clearing arp cache"
		$ARP -d $HOST
	fi

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

