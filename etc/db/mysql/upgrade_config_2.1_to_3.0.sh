#!/bin/sh

dumpfilename="/tmp/dumpfile.sql.$$"

mysqldump -u root -d --opt -F -K ssg >$dumpfilename

echo "Dumping db to $dumpfilename"

service mysql stop

mv /etc/my.cnf /etc/my.cnf.old

echo "Replacing config"

cp /ssg/bin/my.cnf /etc/my.cnf

echo "Cleaning previous files"
rm -rf /var/lib/mysql/ib* /var/lib/ssg


echo "restarting db"

service mysql start

echo "Wait for db to create new files"

sleep 240;

# wait for the new files to create

echo "Create replacement ssg db"

mysqladmin create ssg

echo "Re-create ssg as previously set"

mysql -u root ssg < $dumpfile

echo "Database is converted"
