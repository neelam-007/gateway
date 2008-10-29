#!/bin/sh
echo "Cleaning up ssh..."
rm -rf ~/.ssh/

echo "Cleaning mysql..."
service mysqld stop
rm -f /var/lib/mysql/ib*
rm -f /var/lib/mysql/*.log

echo "Cleaning logs..."
rm -f /var/log/*/*
find /var/log -maxdepth 1 -type f | grep -v '/btmp$' | xargs rm -f

echo "Zeroing disk space..."
# Zero out space only on hardware image, no VMWare
MB_TO_ZERO=$(df -B 1M / | tail -1 | awk '{print $4}')
dd if=/dev/zero of=/tmp/zeros bs=1M count=$MB_TO_ZERO
rm -f /tmp/zeros

echo "Aging accounts..."
# software only - set expire of passwords to now as no sealsys
chage -d 0 root
chage -d 0 ssgconfig

echo "Cleaning network...."
ifdown eth0
rm -f /etc/resolv.conf.*
echo "" > /etc/resolv.conf
echo "" > /etc/security/opasswd

sed -i -e '/HWADDR/d' /etc/sysconfig/network-scripts/ifcfg-eth0
sed -i -e '/HWADDR/d' /etc/sysconfig/network-scripts/ifcfg-eth1

# Remove ssh keys so they are generated on first start
echo "Removing ssh keys..."
rm -f /etc/ssh/ssh_host_*

# below only applies if run interactively
echo "Done, now run:"
echo ""
echo "export HISTSIZE=0"
echo "rm -f /root/.bash_history"
echo "rm $0"
echo "poweroff -d"
echo ""
echo "And enjoy your shiny new VM"
