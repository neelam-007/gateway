#!/bin/bash
# Network Startup config
#
# chkconfig: 2345 99 01
# description: Layer7's Secure Span Gateway TCP protection
# processname: Gateway.jar
# pidfile: /var/run/ssg.pid
# config: /ssg/etc

# Source function library.
. /etc/rc.d/init.d/functions

# things -- attempting to start while running is a failure, and shutdown
# when not running is also a failure.  So we just do it the way init scripts
# are expected to behave here.
start() {
	echo "Setting wide local port range for more outbound connections"
	echo "1024 65530" > /proc/sys/net/ipv4/ip_local_port_range
	echo "Disable ECN because some systems don't do it right yet"
	echo 0 > /proc/sys/net/ipv4/tcp_ecn
	echo "Setting Low latency TCP"
	echo 1 > /proc/sys/net/ipv4/tcp_low_latency
	echo "Turning off timestamps"
	echo 0 > /proc/sys/net/ipv4/tcp_timestamps
	echo "Lowering keepalive time"
	echo 2400 > /proc/sys/net/ipv4/tcp_keepalive_time
	echo 5 > /proc/sys/net/ipv4/tcp_keepalive_intvl
	echo "Lowering FIN timeout"
	echo 20 > /proc/sys/net/ipv4/tcp_fin_timeout
	echo "Turning On Window scaling"
	echo 1 > /proc/sys/net/ipv4/tcp_window_scaling
	echo "Turning On Selective Acknowledgement"
	echo 1 > /proc/sys/net/ipv4/tcp_sack
	echo "Disable route triangulation" 
	echo 1 > /proc/sys/net/ipv4/conf/all/rp_filter
	echo "Disable Source routing" 
	echo 0 > /proc/sys/net/ipv4/conf/all/accept_source_route
	echo "Disable Ping broadcasts" 
	echo 1 > /proc/sys/net/ipv4/icmp_echo_ignore_broadcasts
	echo "Increasing SYN packet Backlog"
	echo 8192 > /proc/sys/net/ipv4/tcp_max_syn_backlog
	echo "Setting higher tcp memory limits"
	echo 16777216 > /proc/sys/net/core/wmem_max
	echo 16777216 > /proc/sys/net/core/rmem_max
	# This is in PAGES, not bytes
	# I want about 1000 messages, up to 100 kbytes in size in flight
	# at once. This number is fully invented as a straw man.
	# thats 100 Mbytes. This number should use a power of 2
	# So we'll allocate 128Mbytes
	# 128*(1024*1024)/4096=32768
	echo "32768 32768 32768" > /proc/sys/net/ipv4/tcp_mem
	echo "Setting socket sizes for best cpu usage"
	echo "4096 4096 16777216" > /proc/sys/net/ipv4/tcp_rmem
	echo "4096 4096 16777216" > /proc/sys/net/ipv4/tcp_wmem
	echo "Turning on TIME_WAIT recyle and reuse"
	echo 1 > /proc/sys/net/ipv4/tcp_tw_recycle
	echo 1 > /proc/sys/net/ipv4/tcp_tw_reuse
	echo "Increasing number of TIME_WAIT buckets"
	echo 360000 > /proc/sys/net/ipv4/tcp_max_tw_buckets
	echo "Turning on syncookie protection from Denial of Service (DOS) attacks"
	echo 1 > /proc/sys/net/ipv4/tcp_syncookies
	echo "Don't cache thresholds from previous connections"
    	echo 1 > /proc/sys/net/ipv4/tcp_no_metrics_save
    	echo "Increase Network backlogs for Gigabit"
    	echo 2500 > /proc/sys/net/core/netdev_max_backlog
    	echo "Increase maximum connections"
    	echo 10240 > /proc/sys/net/core/somaxconn
	echo "Memory limit for fragment assembly"
        echo 4194304 > /proc/sys/net/ipv4/ipfrag_high_thresh
	echo "Done"
	return 0
}
stop() {
	echo 
}

# See how we were called.
case "$1" in
  start)
	start
	;;
  stop)
	stop
	;;
  restart)
	stop
	start
	;;
esac

exit $RETVAL

