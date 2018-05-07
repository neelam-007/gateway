#!/bin/sh
cd /etc/init.d/
for x in *; do /sbin/chkconfig $x off; done
cd

# enable only what we want and expect to be on
/sbin/chkconfig network on
/sbin/chkconfig anacron on
/sbin/chkconfig kudzu on
/sbin/chkconfig irqbalance on
# Smartd is only for ata disks. Scsi on appliance now
# /sbin/chkconfig smartd on
/sbin/chkconfig crond on
/sbin/chkconfig ntpd on
/sbin/chkconfig sshd on
/sbin/chkconfig rhnsd on
/sbin/chkconfig acpid on
/sbin/chkconfig cpuspeed on
/sbin/chkconfig iptables on
/sbin/chkconfig ip6tables on
/sbin/chkconfig rsyslog on
/sbin/chkconfig apmd on
/sbin/chkconfig mysqld on
/sbin/chkconfig mysql on

# Distro and msyql.com version
/sbin/chkconfig ssg on
/sbin/chkconfig ssg-dbstatus on
/sbin/chkconfig ssgsysconfig on
/sbin/chkconfig tcp_tune on
/sbin/chkconfig tarari on

