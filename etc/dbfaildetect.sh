#/bin/sh
###########################################################################
# This program will detect if a MySQL DB cluster on IP address X.X.X.X is up.
# If it fails to connect to this DB cluster after Y tries, this script 
# will attempt to perform an IP takeover of address X.X.X.X
# IP takeover involves:
#   a) an ifconfig alias on a certain interface
#   b) a gratuitous ARP call
# 
#
#
# Property of Layer 7 Technologies
#
# by Jay Thorne - jthorne@layer7tech.com
###########################################################################

# programs used
IFCONFIG="/sbin/ifconfig"
GARP="/usr/local/bin/garp"
WGET="/usr/bin/wget"
NC="/usr/bin/nc"
# globals and configuration

EMAIL="support@layer7tech.com"

DBHOST="10.7.7.77"
# cluster IP addresss

INTERFACE="eth0"
INTERFACE_ALIAS="$INTERFACE:MYSQLDB"
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
	echo "Taking over IP $DBHOST";
	$IFCONFIG $INTERFACE_ALIAS $DBHOST
	echo "Sending gratuitous arp"
	$GARP -i $INTERFACE -a $DBHOST
	echo "echo 'SSG Failover' | mail -s 'SSG Failover' $EMAIL" 
}

doget() {
	# grab a file from the tomcat using wget
	# quiet, few retries, short timeout, configurable location
	#if $WGET  -q -t $TRIES -T $WAIT http://$DBHOST:$PORT/$FILE ;  then 
	if echo " " | $NC -w $WAIT -o /tmp/foo $DBHOST $PORT > /tmp/bar;  then 
		success
	else 
		RETVAL=$?
		failure $RETVAL
	fi
}

# after all those definitions now call the entry point.
doget
