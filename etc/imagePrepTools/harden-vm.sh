#!/bin/sh
echo "Cleaning up ssh..."
rm -rf ~/.ssh/

echo "Cleaning mysql..."

if [ -e /etc/init.d/mysqld ]; then
    service mysqld stop
else
    service mysql stop
fi

rm -f /var/lib/mysql/ib*
rm -f /var/lib/mysql/*.log

echo "Cleaning logs..."
#chmod the log directory (already done in harden) in case anything new has been created on first boot
chmod o-w /var/log/*
#clean up the log directory now
rm -f /var/log/*/*
find /var/log -maxdepth 1 -type f | grep -v '/btmp$' | xargs rm -f
service ssg stop
rm -f /opt/SecureSpan/Controller/var/logs/*.log
rm -f /opt/SecureSpan/Gateway/node/default/var/logs/*.log
/etc/init.d/rsyslog stop
rm -f /var/log/bash_commands.log


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
echo "the hardening of this vm is now complete. Now run:"
echo ""
echo "export HISTSIZE=0"
echo "rm -f /root/.bash_history"
echo "/usr/bin/vmware-config-tools.pl -d"
echo "rm $0"
echo "poweroff -d"
echo ""
