#!/bin/bash
# Run this script on the instance to be bundled
# tested with Canonical Ubuntu 9.10 base ami

EBS_DEVICE=${1:-'/dev/sdf'}
EBS_MOUNT_POINT=${3:-'/mnt/ebs'}

mkdir -p ${EBS_MOUNT_POINT}
mkfs.ext3 ${EBS_DEVICE} -L uec-rootfs
mount  ${EBS_DEVICE} ${EBS_MOUNT_POINT}

# make a local working copy
service ssg stop
if [ -e /etc/init.d/mysqld ]; then
    service mysqld stop
else
    service mysql stop
fi
service rsyslogd stop
rsync --stats -av --exclude=/root/.bash_history --exclude=/root/*.sh --exclude=/root/*.rpm --exclude=/root/.ssh/authorized_keys --exclude=/home/*/.ssh/authorized_keys --exclude=/home/*/.bash_history --exclude=/etc/ssh/ssh_host_* --exclude=/etc/ssh/moduli --exclude=/etc/udev/rules.d/*persistent-net.rules --exclude=/var/lib/ec2/* --exclude=/mnt/* --exclude=/proc/* --exclude=/tmp/* / ${EBS_MOUNT_POINT} > /mnt/rsync.log.$$ 2>&1

#clear out log files
cd ${EBS_MOUNT_POINT}/var/log
for i in `ls ./**/*`; do
  echo $i && echo -n> $i
done

echo "" > ./bash_commands.log;
echo "" > ./boot.log;
echo "" > ./btmp
echo "" > ./cron
echo "" > ./dmesg
echo "" > ./maillog
echo "" > ./messages
echo "" > ./secure
echo "" > ./wtmp

rm ./*.1

umount $EBS_MOUNT_POINT