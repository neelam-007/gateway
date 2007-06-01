#!/bin/sh

# --------------------------------------------------------------- #
#
# tunetcpstack.sh Version 1.4
#
# Written by Martin Allert, arago AG, Germany
# Contact & Bugfixes: allert@arago.de
#
# This script tunes your TCP/IP stack settings and your
# NIC configurations.
# Most of these settings are recommended for security reasons.
#
# Distribution of this script is only allowed 'as it is'.
#
# Explanation of the used variables is taken from the jass toolkit
# nddconfig. 
# (to SUN: Folks, you might be angry about this, but where else could
# I find a better explanation... I am sorry about that... :)
#
# --------------------------------------------------------------- #

# --------------------------------------------------------------- #
# Some useful definitions                                         #
# --------------------------------------------------------------- #

UREVISION=`uname -r`

BOLD="\033[1m"
NORMAL="\033[m"

# --------------------------------------------------------------- #
# We like Redhat, so we print it like their bootscripts :)        #
# --------------------------------------------------------------- #

fill () {
awk '{leninput=length($($NF)); fill=63-leninput ; for (i=1; i< fill; i++) fillchar=fillchar"." ; printf $($NF) fillchar}'
}

# --------------------------------------------------------------- #
# Defining some formatted output                                  #
# --------------------------------------------------------------- #

setparams () {
VAL=`/usr/sbin/ndd -set $1 $2 $3`
printf "Value of $1 $2 is: " | fill
printf " ${VAL} ($3)\n"
}

case "$1" in
	'start')

# Set the ARP-cache timeout to 1 Minute (60000 ms)
printf "${BOLD}ARP Cache timeout${NORMAL}\n"
setparams /dev/arp arp_cleanup_interval 60000

# Set the specific period of time a specific route is kept
#printf "${BOLD}Decrease route keeping interval${NORMAL}\n"
#setparams /dev/ip ip_ire_flush_interval 60000
#if [ "${UREVSION}" = "5.8" ]
#then
#	setparams /dev/ip ip_ire_arp_interval 60000
#fi

# Disable IP-forwarding
printf "${BOLD}IP Forwarding On/Off${NORMAL}\n"
setparams /dev/ip ip_forwarding 0

# Set default TTL value for IP packets
printf "${BOLD}Decrease default TTL for IP packets${NORMAL}\n"
setparams /dev/ip ip_def_ttl 255

# Deny IP-spoofed packets on multi-homed servers
printf "${BOLD}Deny IP-spoofed packets on multi-homed servers${NORMAL}\n"
setparams /dev/ip ip_strict_dst_multihoming 1

# Disable forwarding of  directed broadcasts
printf "${BOLD}Disable forwarding of directed broadcasts${NORMAL}\n"
setparams /dev/ip ip_forward_directed_broadcasts 0

# Drop source routed packets
printf "${BOLD}Drop source routed packets${NORMAL}\n"
setparams /dev/ip ip_forward_src_routed 0

# Do not respond to broadcast echo requests
printf "${BOLD}Do not respond to broadcast echo requests${NORMAL}\n"
setparams /dev/ip ip_respond_to_echo_broadcast 0

# Do not respond to timestamp requests
printf "${BOLD}Do not respond to timestamp requests${NORMAL}\n"
setparams /dev/ip ip_respond_to_timestamp 0

# Do not respond to timestamp broadcast requests
printf "${BOLD}Do not respond to timestamp broadcast requests${NORMAL}\n"
setparams /dev/ip ip_respond_to_timestamp_broadcast 0

# Do not respond to address mask broadcasts
printf "${BOLD}Do not respond to address mask broadcasts${NORMAL}\n"
setparams /dev/ip ip_respond_to_address_mask_broadcast 0

# Ignore ICMP redirects
printf "${BOLD}Ignore ICMP redirects${NORMAL}\n"
setparams /dev/ip ip_ignore_redirect 0

# Do not send ICMP redirects
printf "${BOLD}Do not send ICMP redirects${NORMAL}\n"
setparams /dev/ip ip_send_redirects 0

# Do not send ICMP timestamp requests
printf "${BOLD}Do not send ICMP timestamp requests${NORMAL}\n"
setparams /dev/ip ip_respond_to_timestamp 0

# Decrease the tcp time wait interval
if [ ${UREVISION} = "5.6" ]; then
printf "${BOLD}Decrease the tcp time wait interval${NORMAL}\n"
  setparams /dev/tcp tcp_close_wait_interval 5000
else
printf "${BOLD}Decrease the tcp time wait interval${NORMAL}\n"
  setparams /dev/tcp tcp_time_wait_interval 5000
fi

# Lower the smallest anon port
printf "${BOLD}Lower the smallest anon port${NORMAL}\n"
setparams /dev/tcp tcp_smallest_anon_port 1024

# Adjust boundaries of smallest non-priv port
printf "${BOLD}Set smallest non-priv portt${NORMAL}\n"
setparams /dev/tcp tcp_smallest_nonpriv_port 1024
setparams /dev/udp udp_smallest_nonpriv_port 1024

# Speed up the flushing of half-closed connection in state FIN_WAIT_2
printf "${BOLD}Flushing of half-closed connection in state FIN_WAIT_2${NORMAL}\n"
setparams /dev/tcp tcp_fin_wait_2_flush_interval 67500ms

# Increase the receive and transmit window sizes
printf "${BOLD}Increase the receive and transmit window sizes${NORMAL}\n"
setparams /dev/tcp tcp_xmit_hiwat 400000
setparams /dev/tcp tcp_recv_hiwat 400000

# decrease the retransmit interval
printf "${BOLD}Decrease the retransmit interval${NORMAL}\n"
setparams /dev/tcp tcp_rexmit_interval_max 60000ms

# decrease the retransmit interval
printf "${BOLD}Decrease the local DACK interval${NORMAL}\n"
setparams /dev/tcp tcp_local_dack_interval 500



# increase number of half-open connections
printf "${BOLD}Increase number of half-open connections${NORMAL}\n"
setparams /dev/tcp tcp_conn_req_max_q0 8192

# increase number of simultaneous connections
printf "${BOLD}Increase number of simultaneous connections${NORMAL}\n"
setparams /dev/tcp tcp_conn_req_max_q 8192

# Decrease TCP connection abort interval
printf "${BOLD}Decrease TCP connection abort interval${NORMAL}\n"
setparams /dev/tcp tcp_ip_abort_interval 30000

# Decrease TCP Keepalive Interval
printf "${BOLD}Decrease TCP Keepalive Interval${NORMAL}\n"
setparams /dev/tcp tcp_keepalive_interval 60000

# Increasing maximum congestial window size
printf "${BOLD}Increasing maximum congestial window size${NORMAL}\n"
setparams /dev/tcp tcp_slow_start_initial 2

# Turn off returning source routes in source routed packets
if [ "${UREVISION}" = "5.8" ]
then
	printf "${BOLD}Disable returning source routes${NORMAL}\n"
	setparams /dev/tcp tcp_rev_src_routes 0
fi

# Add other high ports to the range of privileged ports
#printf "${BOLD}Add some high port to range of privports${NORMAL}\n"
#setparams /dev/tcp tcp_extra_priv_ports_add 8080
#setparams /dev/udp udp_extra_priv_ports_add 

	;;

	'-h' | '--help')
		cat << EOT | more

===============================================================================
	Explanation of the parameters set in the script
	(not all are set, but there is always an example attached)
===============================================================================


#
# arp_cleanup_interval
#
#  This option determines the period of time the Address Resolution
#  Protocol (ARP) cache maintains entries. ARP attacks may be effective
#  with the default interval. Shortening the timeout interval should
#  reduce the effectiveness of such an attack.
#  The default value is 300000 milliseconds (5 minutes).
#
arp_cleanup_interval=60000

#
# ip_forward_directed_broadcasts
#
#  This option determines whether to forward broadcast packets directed
#  to a specific net or subnet, if that net or subnet is directly
#  connected to the machine. If the system is acting as a router, this
#  option can be exploited to generate a great deal of broadcast network
#  traffic. Turning this option off will help prevent broadcast traffic
#  attacks.
#  The default value is 1 (true).
#
ip_forward_directed_broadcasts=0

#
# ip_forward_src_routed 
# ip6_forward_src_routed (Solaris 8 and 9)
#
#  This option determines whether to forward packets that are source
#  routed. These packets define the path the packet should take instead
#  of allowing network routers to define the path.
#  The default value is 1 (true).
#
ip_forward_src_routed=0
ip6_forward_src_routed=0

#
# ip_ignore_redirect
# ip6_ignore_redirect (Solaris 8 and 9)
#
#  This option determines whether to ignore Internet Control Message
#  Protocol (ICMP) packets that define new routes. If the system is
#  acting as a router, an attacker may send redirect messages to alter
#  routing tables as part of sophisticated attack (man in the middle
#  attack) or a simple denial of service.
#  The default value is 0 (false).
#
ip_ignore_redirect=1
ip6_ignore_redirect=1

#
# ip_ire_flush_interval (Solaris 2.5.1, 2.6, and 7)
# ip_ire_arp_interval   (Solaris 8 and 9)
#
#  This option determines the period of time at which a specific route
#  will be kept, even if currently in use. ARP attacks may be effective
#  with the default interval. Shortening the time interval may reduce
#  the effectiveness of attacks.
#  The default interval is 1200000 milliseconds (20 minutes).
#
ip_ire_flush_interval=60000
ip_ire_arp_interval=60000

#
# ip_respond_to_address_mask_broadcast
#
#  This options determines whether to respond to ICMP netmask requests
#  which are typically sent by diskless clients when booting. An
#  attacker may use the netmask information for determining network
#  topology or the broadcast address for the subnet.
#  The default value is 0 (false).
#
ip_respond_to_address_mask_broadcast=0

#
# ip_respond_to_echo_broadcast
# ip6_respond_to_echo_multicast (Solaris 8 and 9)
#
#  This option determines whether to respond to ICMP broadcast echo
#  requests (ping). An attacker may try to create a denial of service
#  attack on subnets by sending many broadcast echo requests to which all
#  systems will respond. This also provides information on systems that
#  are available on the network.
#  The default value is 1 (true).
#
ip_respond_to_echo_broadcast=0
ip6_respond_to_echo_multicast=0

#
# ip_respond_to_timestamp
#
#  This option determines whether to respond to ICMP timestamp requests
#  which some systems use to discover the time on a remote system. An
#  attacker may use the time information to schedule an attack at a
#  period of time when the system may run a cron job (or other time-
#  based event) or otherwise be busy. It may also be possible predict
#  ID or sequence numbers that are based on the time of day for spoofing
#  services.
#  The default value is 1 (true).
#
ip_respond_to_timestamp=0

#
# ip_respond_to_timestamp_broadcast
#
#  This option determines whether to respond to ICMP broadcast timestamp
#  requests which are used to discover the time on all systems in the
#  broadcast range. This option is dangerous for the same reasons as 
#  responding to a single timestamp request. Additionally, an attacker
#  may try to create a denial of service attack by generating many
#  broadcast timestamp requests.
#  The default value is 1 (true).
#
ip_respond_to_timestamp_broadcast=0

#
# ip_send_redirects
# ip6_send_redirects (Solaris 8 and 9)
#
#  This option determines whether to send ICMP redirect messages which
#  can introduce changes into remote system's routing table. It should
#  only be used on systems that act as routers.
#  The default value is 1 (true).
#
ip_send_redirects=0
ip6_send_redirects=0

#
# ip_strict_dst_multihoming
# ip6_strict_dst_multihoming (Solaris 8 and 9)
#
#  This option determines whether to enable strict destination
#  multihoming. If this is set to 1 and ip_forwarding is set to 0, then
#  a packet sent to an interface from which it did not arrive will be
#  dropped. This setting prevents an attacker from passing packets across
#  a machine with multiple interfaces that is not acting a router.
#  The default value is 0 (false).
#
#  NOTE: Strict destination multihoming may prevent SunCluster 2.x
#  systems from operating as intended.  This script will NOT enable
#  strict destination multihoming if SunCluster 2.x software is installed.
#
ip_strict_dst_multihoming=1
ip6_strict_dst_multihoming=1

#
# ip_def_ttl
#
#  This option sets the default time to live (TTL) value for IP packets.
#  Normally, this should not be altered from the default value.
#  Changing it to a different value may fool some OS "fingerprinting"
#  tools such as queso or nmap.
#  The default value is 255.
#
ip_def_ttl=255

#
# tcp_conn_req_max_q0
# 
#  This option sets the size of the queue containing unestablished
#  connections. This queue is part of a protection mechanism against
#  SYN flood attacks. The queue size default is adequate for most
#  systems but should be increased for busy servers.
#  The default value is 1024.
#
tcp_conn_req_max_q0=4096

#
# tcp_conn_req_max_q
#
#  This option sets the maximum number fully established connections.
#  Increasing the size of this queue provides some limited protection
#  against resource consumption attacks. The queue size default is
#  adequate for most systems but should be increased for busy servers.
#  The default value is 128.
#
tcp_conn_req_max_q=1024

#
# tcp_rev_src_routes (Solaris 8 and 9)
#
#  This option determines whether the specified route in a source
#  routed packet will be used in returned packets.  TCP source routed
#  packets may be used in spoofing attacks, so the reverse route should 
#  not be used.
#  The default value is 0 (false).
#
tcp_rev_src_routes=0

#
# Adding specific privileged ports (Solaris 2.6, 7, 8, and 9)
#
#  These options define additional TCP and UDP privileged ports outside
#  of the 1-1023 range.  Any program that attempts to bind the ports
#  listed here must run as root.  This prevents normal users from
#  starting server processes on specific ports.  Multiple ports can be
#  specifed by quoting and separating them with spaces.
#
#  Defaults values:
#	tcp_extra_priv_ports: 2049 (nfsd) 4045 (lockd)
#	udp_extra_priv_ports: 2049 (nfsd) 4045 (lockd)
#
tcp_extra_priv_ports_add="6112"
udp_extra_priv_ports_add=""

#
# Ephemeral port range adjustment
#
#  These options define the upper and lower bounds on ephemeral ports.
#  Ephemeral (means short-lived) ports are used when establishing
#  outbound network connections.
#
#  Defaults values:
#	tcp_smallest_anon_port=32768
#	tcp_largest_anon_port=65535
#	udp_smallest_anon_port=32768
#	udp_largest_anon_port=65535
#
tcp_smallest_anon_port=32768
tcp_largest_anon_port=65535
udp_smallest_anon_port=32768
udp_largest_anon_port=65535

#
# Nonprivileged port range adjustment
#
#  These options define the start of nonprivileged TCP and UDP ports. 
#  The nonprivileged port range normally starts at 1024.  Any program
#  that attempts to bind a nonprivileged port does not have to run as
#  root.
# 
#  Defaults values:
#	tcp_smallest_nonpriv_port=1024
#	udp_smallest_nonpriv_port=1024
#
tcp_smallest_nonpriv_port=1024
udp_smallest_nonpriv_port=1024
EOT
	;;

	*)
		echo "Usage: $0 {start | -h | --help}"
	;;
esac
