#!/bin/bash
# Run this script on the instance used to migrate a snapshot from one Environment to the environment this script is running in.

SSH_KEY=$1
SSH_USER=$2
SSH_HOST=$3
REMOTE_FILE=$4

EBS_DEVICE="/dev/sdf"
EBS_MOUNT_POINT="/mnt/ebs"

sudo mkdir -p ${EBS_MOUNT_POINT}
sudo mkfs.ext3 ${EBS_DEVICE} -L uec-rootfs
sudo mount  ${EBS_DEVICE} ${EBS_MOUNT_POINT}

sudo scp -i "${SSH_KEY}" -o StrictHostKeyChecking=no "${SSH_USER}@${SSH_HOST}:${REMOTE_FILE}" "${EBS_MOUNT_POINT}"
cd "${EBS_MOUNT_POINT}"
sudo rm -rf ./lost+found/ 
sudo tar -xvf "${REMOTE_FILE}"
sudo rm -rf "${REMOTE_FILE}"
cd ~    

df -h
sudo umount ${EBS_MOUNT_POINT}

echo "Script successfully completed."

exit 0 