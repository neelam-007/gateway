#/bin/sh
###########################################################################
#
# Startup script for SSG db fail over
#
# chkconfig: - 99 01
# description: dbfaildetect
# Property of Layer 7 Technologies
#
# by Jay Thorne - jthorne@layer7tech.com
###########################################################################

#. /etc/init.d/functions

# programs used
IFCONFIG="/sbin/ifconfig"
GARP="/ssg/bin/garp"
NC="/ssg/bin/nc"
PING="/bin/ping"
# Default netmask. Linux (helpfully) supplies a /8 route on
# 10. networks
NETMASK="255.255.255.0"

DEFAULT_ROUTE=`/sbin/route -n | egrep ^0.0.0.0 | cut -c 17-32 | tr -d [:blank:]`
BACK_END_ROUTE=""

# Sanity Check:
if [ -e $IFCONFIG -a -e $GARP -a  -e $NC -a -e $PING ]; then
	echo -n ""
else
	echo "Some dependencies not satisfied, please check for $IFCONFIG, $GARP, $NC, $PING"
	exit 1
fi

# If network has routes on secure side
# Fill in back end router if there is one
# We test that machine to see if we're the ones cut off from the world
# Not the existant db host

# globals and configuration
PIDFILE=/var/run/dbfaildetect.pid
EMAIL="NEED_A_REAL_EMAIL"

DBHOST="NEED_A_REAL_DB_CLUSTER_ADDRESS"
# cluster IP addresss


if [ -z "$DEFAULT_ROUTE" ] ;  then
     echo "No default route, Exiting"
     exit
fi

INTERFACE="eth0"
# Main interface to the back end / most secure network
INTERFACE_ALIAS="$INTERFACE:0"
# alias interface for the db cluster communication

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
desc() {
tries=`expr $TRIES + 1`
cat <<EOF

This program will detect if a MySQL DB instance is available
at $DBHOST / port $PORT.

If it fails to connect to this DB after $tries tries, this script
will attempt to perform an IP takeover of address $DBHOST
if this machine has ICMP connectivity to its default route.

If this machine has the alias already up, it will then test
if connectivity to the default route has failed and if so
relinquish the cluster address, assuming that the machine
is disconnected from the network at the moment.

IP takeover involves:
  a) creating an alias on $INTERFACE_ALIAS
  b) sending a gratuitous ARP packet to inform other machines
     that the previous arp address is no longer valid


EOF
}

selfmaster() {
    iface=`/sbin/ifconfig | grep $INTERFACE_ALIAS`
    if [ -z "$iface" ] ; then
        # echo "Alias Interface not found"
        return 1
    else
        # echo "Alias interface found"
        return 0
    fi
}
success() {
	rm -f $TRYAGAINFILE
	sleep $SLEEPRETRY
}

failure() {
	echo "Failed get connection"
	if ! [ -f $TRYAGAINFILE ] ; then
		echo "Setting first failure"
		echo
		touch $TRYAGAINFILE
		sleep $RETEST
	else
		echo "Failed Twice. Time for action"
		if  pinggw; then
		    echo "Other node down, network is alive, taking over as master"
		    doiptakeover
		else
		    echo "Network down, not attempting to take over as master"
		fi
		sleep $RETEST
		rm $TRYAGAINFILE
	fi
}

doiptakeover() {
    date
    echo " Taking over IP $DBHOST";
    $IFCONFIG $INTERFACE_ALIAS $DBHOST netmask $NETMASK 
    echo "Sending gratuitous arp"
    $GARP -i $INTERFACE -a $DBHOST
#    echo "echo 'SSG DB Failover' | mail -s 'SSG DB Failover' $EMAIL"
}

takedownalias() {
    date
    echo " Network failure detected: relinquishing cluster IP"
    $IFCONFIG $INTERFACE_ALIAS 99.99.99.99
    # needed to make local machine not be able to reach a formerly
    # configured address - linux bug(ette) workaround
    $IFCONFIG $INTERFACE_ALIAS down
    echo "Sending gratuitous arp"
    $GARP -i $INTERFACE -a $DBHOST
}

pinggw() {
    # 5 packets, 1 second apart, waiting 7 seconds
    # USE back end route if it exists
    if [ -z "$BACK_END_ROUTE" ] ;  then
        $PING -n -c 5 -i 0.4 -q -w 7 $DEFAULT_ROUTE >/dev/null 2>&1
    else
        $PING -I $INTERFACE -n -c 5 -i 0.4 -q -w 7 $BACK_END_ROUTE >/dev/null 2>&1
    fi
    return $?
}

doget() {
    # first, lets see if we think we're cluster master.
    # cluster master is defined as owning the cluster ip and alias.
    if selfmaster; then
        # wait a minute, I'm master.
        # I should see if i'm still alive on the net
        if pinggw; then
            # yay, i'm alive, I'm master, so it looks like I should stay that way.
            success
        else
            # duh, I'm dead to the world , and I think i'm master
            # turn off that alias, so when i'm plugged back in again
            # I won't be stupid and try to own it still
            takedownalias
            # now start the loop again to see if
            # we've come alive again
            success
        fi
    else
        # we're not the master (any more)
    	if echo " " | $NC -w $WAIT -o /tmp/foo $DBHOST $PORT > /tmp/bar;  then
    		success
    	else

    		RETVAL=$?
    		failure $RETVAL
    	fi
    fi
}


loop() {
	while (true)
	do doget	
	done
}


# See how we were called.
case "$1" in
  start)
	echo "Starting $0"
	loop >/var/log/dbfail.log 2>&1 &
	echo $! > $PIDFILE
	;;
  stop)
	echo "Stopping $0"
	kill -9 `cat $PIDFILE`
	;;
  desc)
    desc
    ;;
  *)
	echo "Usage: $0 {start|stop|desc}"
	exit 1
esac

