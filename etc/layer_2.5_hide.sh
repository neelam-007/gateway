echo "0" > /proc/sys/net/ipv4/conf/all/accept_redirects
echo "0" > /proc/sys/net/ipv4/conf/all/accept_source_route
echo "0" > /proc/sys/net/ipv4/conf/all/send_redirects
echo "1" > /proc/sys/net/ipv4/conf/all/rp_filter
echo "1" > /proc/sys/net/ipv4/conf/all/log_martians

echo "0" > /proc/sys/net/ipv4/conf/default/accept_redirects
echo "0" > /proc/sys/net/ipv4/conf/default/accept_source_route
echo "0" > /proc/sys/net/ipv4/conf/default/send_redirects
echo "1" > /proc/sys/net/ipv4/conf/default/rp_filter
echo "1" > /proc/sys/net/ipv4/conf/default/log_martians

echo "0" > /proc/sys/net/ipv4/conf/eth0/accept_redirects
echo "0" > /proc/sys/net/ipv4/conf/eth0/accept_source_route
echo "0" > /proc/sys/net/ipv4/conf/eth0/send_redirects
echo "1" > /proc/sys/net/ipv4/conf/eth0/rp_filter
echo "1" > /proc/sys/net/ipv4/conf/eth0/log_martians

echo "1" > /proc/sys/net/ipv4/icmp_echo_ignore_all
echo "1" > /proc/sys/net/ipv4/icmp_echo_ignore_broadcasts
echo "1" > /proc/sys/net/ipv4/icmp_ignore_bogus_error_responses
echo "1" > /proc/sys/net/ipv4/tcp_syncookies
# stupid to do the following on a shared net
#echo "1" > /proc/sys/net/ipv4/conf/eth0/arp_ignore
#echo "1" > /proc/sys/net/ipv4/conf/all/arp_ignore
#echo "1" > /proc/sys/net/ipv4/conf/default/arp_ignore
