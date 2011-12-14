#!/bin/bash
# Run this script on the instance to be bundled
# tested with Canonical Ubuntu 9.10 base ami

EBS_DEVICE=${1:-'/dev/sdf'}
IMAGE_DIR=${2:-'/mnt/tmp'}
EBS_MOUNT_POINT=${3:-'/mnt/ebs'}

mkdir -p ${EBS_MOUNT_POINT}
mkfs.ext3 ${EBS_DEVICE} -L uec-rootfs
mount  ${EBS_DEVICE} ${EBS_MOUNT_POINT}

# make a local working copy
mkdir -p ${IMAGE_DIR}
service ssg stop
service mysql stop
service rsyslogd stop
rsync --stats -av --exclude=/root/.bash_history --exclude=/root/*.sh --exclude=/root/*.rpm --exclude=/root/.ssh/authorized_keys --exclude=/home/*/.ssh/authorized_keys --exclude=/home/*/.bash_history --exclude=/etc/ssh/ssh_host_* --exclude=/etc/ssh/moduli --exclude=/etc/udev/rules.d/*persistent-net.rules --exclude=/var/lib/ec2/* --exclude=/mnt/* --exclude=/proc/* --exclude=/tmp/* / ${IMAGE_DIR} > /mnt/rsync.log.$$ 2>&1

#clear out log files
cd $IMAGE_DIR/var/log
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

cd $IMAGE_DIR
tar -cSf - -C ./ . | tar xvf - -C $EBS_MOUNT_POINT
#NOTE, You could rsync / directly to EBS_MOUNT_POINT, but this tar trickery saves some space in the snapshot

umount $EBS_MOUNT_POINT