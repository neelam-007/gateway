#!/bin/bash

rpm -Uvh --force bash-3.2-24.x86_64.rpm &> /dev/null
rpm -ivh rsyslog-3.22.1-3.el5_5.1.x86_64.rpm &> /dev/null
rpm -e sysklogd
sed -i "/\(.*\)\/syslogd.pid\(.*$\)/d" /etc/logrotate.d/syslog
sed -i "s|^SYSLOGD_OPTIONS=\"-m 0\"|SYSLOGD_OPTIONS=\"-c3\"|" /etc/sysconfig/rsyslog
chkconfig rsyslog on
/etc/init.d/rsyslog restart &> /dev/null

