#!/bin/bash
#
# Run instance user-data if it looks like a script.
#
# Only retrieves and runs the user-data script once per instance.  If
# you want the user-data script to run again (e.g., on the next boot)
# then add this command in the user-data script:
#   rm -f /var/ec2/runUserData.*
#
# History:
#
#
prog=$(basename $0)
logger="logger -t ec2-userdata"
curl="curl --retry 3 --silent --show-error --fail"
instance_data_url=http://169.254.169.254/latest

# Wait until meta-data is available.
perl -MIO::Socket::INET -e '
 until(new IO::Socket::INET("169.254.169.254:80")){print"Waiting for meta-data...\n";sleep 1}
' | $logger

# Exit if we have already run on this instance (e.g., previous boot).
ami_id=$($curl $instance_data_url/meta-data/ami-id)
been_run_file=/var/ec2/$prog.$ami_id
mkdir -p $(dirname $been_run_file)
if [ -f $been_run_file ]; then
  $logger < $been_run_file
  exit
fi

# Retrieve the instance user-data and run it if it looks like a script
user_data_file=$(mktemp -t ec2.user-data.XXXXX)
chmod 700 "${user_data_file}"
$logger "Retrieving user-data"
$curl -o $user_data_file $instance_data_url/user-data 2>&1 | $logger

if [ ! -s $user_data_file ]; then
  $logger "No user-data available"
  echo "user-data was not available" > $been_run_file
elif head -1 $user_data_file | egrep -v '^#!'; then
  $logger "Skipping user-data as it does not begin with #!"
  echo "user-data did not begin with #!" > $been_run_file
else
  $logger "Running user-data"
  echo "user-data has already been run on this instance" > $been_run_file
  $user_data_file 2>&1 | logger -t "user-data"
  $logger "user-data exit code: $?"
fi
rm -f $user_data_file
