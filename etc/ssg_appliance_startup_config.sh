#!/bin/sh
cd /etc/init.d/
for x in *; do /sbin/chkconfig $x off; done
cd

# enable only what we want and expect to be on
/sbin/chkconfig network on
/sbin/chkconfig anacron on
/sbin/chkconfig kudzu on
# Smartd is only for ata disks. Scsi on appliance now
# /sbin/chkconfig smartd on
/sbin/chkconfig crond on
/sbin/chkconfig ntpd on
/sbin/chkconfig sshd on
/sbin/chkconfig rhnsd on
/sbin/chkconfig acpid on
/sbin/chkconfig cpuspeed on
/sbin/chkconfig iptables on
/sbin/chkconfig syslog on
# lm_sensors currently hates IBM gear
# /sbin/chkconfig lm_sensors on
/sbin/chkconfig apmd on
/sbin/chkconfig mysql on
/sbin/chkconfig ssg on
/sbin/chkconfig tcp_tune on
/sbin/chkconfig tarari on

