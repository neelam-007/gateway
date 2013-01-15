#!/bin/bash
# Run this script on the instance to be bundled
# tested with Canonical Ubuntu 9.10 base ami

EBS_DEVICE="/dev/sdf"
EBS_MOUNT_POINT="/mnt/ebs"
TAR_BALL=$1

sudo mkdir -p ${EBS_MOUNT_POINT}
sudo mount ${EBS_DEVICE} ${EBS_MOUNT_POINT}
cd /mnt/ebs/
sudo tar -czvf "/home/ec2-user/${TAR_BALL}" . 
cd ~
sudo umount $EBS_MOUNT_POINT
echo "Script successfully completed."
exit 0