#!/bin/bash

# Update EC2 kernel modules autmatically.
modules_file="ec2-modules-`uname -r`-`uname -m`.tgz"
[ -f $modules_file ] && rm -f $modules_file
echo "Attempting kernel modules update from S3"
(wget http://s3.amazonaws.com/ec2-downloads/$modules_file && echo "Retreived $modules_file from S3" || echo "Unable to retreive $modules_file from S3")|logger -s -t "ec2"
(tar xzf $modules_file -C / && depmod -a && echo "Updated kernel modules from S3")|logger -s -t "ec2"

