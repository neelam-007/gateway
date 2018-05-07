#!/bin/bash

# Update ec2-ami-tools autmatically.
[ -f ec2-ami-tools.noarch.rpm ] && rm -f ec2-ami-tools.noarch.rpm
echo "Attempting ami-utils update from S3"
(wget http://s3.amazonaws.com/ec2-downloads/ec2-ami-tools.noarch.rpm && echo "Retreived ec2-ami-tools from S3" || echo "Unable to retreive ec2-ami-tools from S3")|logger -s -t "ec2"
(rpm -Uvh ec2-ami-tools.noarch.rpm && echo "Updated ec2-ami-tools from S3" || echo "ec2-ami-tools already up to date")|logger -s -t "ec2"
