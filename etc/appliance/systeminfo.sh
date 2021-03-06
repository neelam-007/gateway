#!/bin/bash -u
#
# Copyright (C) 2007 Layer 7 Technologies Inc.
#
# File: systeminfo.sh
# Purpose: This script is run by SSG Ping service to collect additional system
#          information. Text produced is appended as preformatted text to the
#          response HTML.
. /opt/SecureSpan/Appliance/libexec/envclean
alias echo='/bin/echo'
MYSQL_OPTS="-u root"
if [ -r "/root/.my.cnf" ] ; then
  MYSQL_OPTS="--defaults-extra-file=/root/.my.cnf ${MYSQL_OPTS}"
fi

echo "********************************************************************************"
echo "****************************** Kernel Information ******************************"
echo "********************************************************************************"
/bin/uname -an
echo
echo

echo "********************************************************************************"
echo "******************** Kernel Boot Parameters (/proc/cmdline) ********************"
echo "********************************************************************************"
/bin/cat /proc/cmdline
echo
echo

echo "********************************************************************************"
echo "********************************* Drive Space **********************************"
echo "********************************************************************************"
/bin/df -h
echo
echo

echo "********************************************************************************"
echo "********************************** Free Space **********************************"
echo "********************************************************************************"
/usr/bin/free
echo
echo

echo "********************************************************************************"
echo "********************** Memory Information (/proc/meminfo) **********************"
echo "********************************************************************************"
/bin/cat /proc/meminfo
echo
echo

echo "********************************************************************************"
echo "********************************* Process List *********************************"
echo "********************************************************************************"
/bin/ps aufxwwww
echo
echo

echo "********************************************************************************"
echo "************************** Kernel and SSG RPM Packages *************************"
echo "********************************************************************************"
/bin/rpm -qa | egrep '^(kernel|ssg)-' | sort
echo
echo

echo "********************************************************************************"
echo "***************************** Tarari (cpp_manager) *****************************"
echo "********************************************************************************"
if [ -e /usr/local/Tarari/bin/cpp_manager ]; then
   LD_LIBRARY_PATH="/usr/local/Tarari/lib" /usr/local/Tarari/bin/cpp_manager -s
else
   echo cpp_manager not found
fi
echo
echo

echo "********************************************************************************"
echo "****************************** MySQL daemon status *****************************"
echo "********************************************************************************"
# MySQL-cluster no longer calls is mysqld so deal with the two options
if [ -e /etc/init.d/mysqld ]; then
    /sbin/service mysqld status
else
    /sbin/service mysql status
fi
echo
echo

echo "********************************************************************************"
echo "********************************* MySQL status *********************************"
echo "********************************************************************************"
/usr/bin/mysql ${MYSQL_OPTS} -e "SHOW STATUS"
echo
echo

echo "********************************************************************************"
echo "****************************** MySQL slave status ******************************"
echo "********************************************************************************"
/usr/bin/mysql ${MYSQL_OPTS} -e "SHOW SLAVE STATUS\G"
echo
echo
