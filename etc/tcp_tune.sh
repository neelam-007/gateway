#!/bin/sh
echo "Setting Low latency TCP"
echo 1> /proc/sys/net/ipv4/tcp_low_latency
echo "Lowering keepalive time"
echo 1800 >/proc/sys/net/ipv4/tcp_keepalive_time
echo 5 > /proc/sys/net/ipv4/tcp_keepalive_intvl
echo "Lowering FIN timeout"
echo 10 > /proc/sys/net/ipv4/tcp_fin_timeout
echo "Turning off timestamps"
echo 0 > /proc/sys/net/ipv4/tcp_timestamps
echo "Turning On Window scaling"
echo 1 > /proc/sys/net/ipv4/tcp_window_scaling
echo 1 > /proc/sys/net/ipv4/tcp_sack
echo "Setting higher tcp memory limits"
echo 8388608 > /proc/sys/net/core/wmem_max
echo 8388608 > /proc/sys/net/core/rmem_max
echo "4096 87380 4194304" > /proc/sys/net/ipv4/tcp_rmem
echo "4096 65536 4194304" > /proc/sys/net/ipv4/tcp_wmem
echo "Turning on TIME_WAIT recyle and reuse"
echo 1> /proc/sys/net/ipv4/tcp_tw_recycle
echo 1> /proc/sys/net/ipv4/tcp_tw_reuse

