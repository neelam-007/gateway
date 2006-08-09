#!/bin/bash
# Network Startup config
#
# chkconfig: - 99 01
# description: Layer7's Secure Span Gateway TCP protection
# processname: tomcat
# pidfile: /var/run/ssg.pid
# config: /ssg/tomcat

# Source function library.
. /etc/rc.d/init.d/functions

# things -- attempting to start while running is a failure, and shutdown
# when not running is also a failure.  So we just do it the way init scripts
# are expected to behave here.
start() {
	echo "Setting wide local port range for more outbound connections"
	echo "1024 65530" > /proc/sys/net/ipv4/ip_local_port_range
	echo "Setting Low latency TCP"
	echo 1 > /proc/sys/net/ipv4/tcp_low_latency
	echo "Turning off timestamps"
	echo 0 > /proc/sys/net/ipv4/tcp_timestamps
	echo "Lowering keepalive time"
	echo 1200 > /proc/sys/net/ipv4/tcp_keepalive_time
	echo 5 > /proc/sys/net/ipv4/tcp_keepalive_intvl
	echo "Lowering FIN timeout"
	echo 10 > /proc/sys/net/ipv4/tcp_fin_timeout
	echo "Turning On Window scaling"
	echo 1 > /proc/sys/net/ipv4/tcp_window_scaling
	echo "Turning On Selective Acknowledgement"
	echo 1 > /proc/sys/net/ipv4/tcp_sack
	echo "Increasing SYN packet Backlog"
	echo 8192 > /proc/sys/net/ipv4/tcp_max_syn_backlog
	echo "Setting higher tcp memory limits"
	echo 8388608 > /proc/sys/net/core/wmem_max
	echo 8388608 > /proc/sys/net/core/rmem_max
	echo "4096 87380 4194304" > /proc/sys/net/ipv4/tcp_rmem
	echo "4096 65536 4194304" > /proc/sys/net/ipv4/tcp_wmem
	echo "4000000 4500000 5000000" > /proc/sys/net/ipv4/tcp_mem
	echo "Turning on TIME_WAIT recyle and reuse"
	echo 1 > /proc/sys/net/ipv4/tcp_tw_recycle
	echo 1 > /proc/sys/net/ipv4/tcp_tw_reuse
	echo "Increasing number of TIME_WAIT buckets"
	echo 360000 > /proc/sys/net/ipv4/tcp_max_tw_buckets
	echo "Turning on syncookie protection from Denial of Service (DOS) attacks"
	echo 1 > /proc/sys/net/ipv4/tcp_syncookies
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

