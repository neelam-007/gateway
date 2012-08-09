#!/bin/bash

# values that will be obtained from autoscalenode.properties
createDatabase="no"
databaseAdminPassword=""
databaseAdminUsername=""
databaseHost=""
databaseName=""
databaseNodePassword=""
databaseNodeUsername=""
databasePort=""
host=""
adminUsername=""
adminPassword=""
clusterHostname="localhost"
clusterPassword=""
hostsecret=""

# Static Values
PID_FILE="sspcd"
PC_SCRIPT="/opt/SecureSpan/Controller/bin/processcontroller.sh"
PC_USER="layer7"
DATE_PREFIX=`date +%Y%m%d_%H%M_%S`
MOUNT_DIR="/mnt/secretvol_${DATE_PREFIX}"
MOUNT_POINT="/dev/sdf"
MODULAR_ASSERTION_FOLDER="modular_assertions"
CUSTOM_ASSERTION_FOLDER="custom_assertions"

NODE_MANAGEMENT_URI='https://localhost:8765/services/nodeManagementApi'
ADMIN_GATEWAY_URI='https://10.190.217.50:8443/configuration'

# Error Situations:

# If the gateway has already been configured, exit the script
if [ -f "/opt/SecureSpan/Gateway/node/default/etc/conf/node.properties" ] ; then
   echo "Gateway has already been configured.  Exiting"|logger -s -t "ec2"
   exit 1
fi

# ssg service should have started by this point.  If it has not, we abort
SSG_STATUS=$(service ssg status)
SSG_RUNNING=$(echo "${SSG_STATUS}" | grep running)
SSG_STOPPED=$(echo "${SSG_STATUS}" | grep stopped)
if [ -z "${SSG_RUNNING}" ] ; then
  echo "SSG has never been started to begin with.  Exiting." |logger -s -t "ec2"
  exit 1 
fi

#---------------------------------#

. /etc/rc.d/init.d/functions

# Mount Snapshot and scrape out values
if [ -d "${MOUNT_DIR}" ] ; then
  umount "${MOUNT_DIR}"
  rm -rf "${MOUNT_DIR}/"
fi

mkdir -m 000 "${MOUNT_DIR}"
mount -t ext3 -o noatime,ro "${MOUNT_POINT}" "${MOUNT_DIR}"


# copy modular assertions and custom assertions into proper locations
cp -f "${MOUNT_DIR}/${MODULAR_ASSERTION_FOLDER}"/*.aar /opt/SecureSpan/Gateway/runtime/modules/assertions/
cp -f "${MOUNT_DIR}/${CUSTOM_ASSERTION_FOLDER}"/*.jar  /opt/SecureSpan/Gateway/runtime/modules/lib/
chown gateway:gateway /opt/SecureSpan/Gateway/runtime/modules/lib/*.jar

# make sure to unmount it
umount "${MOUNT_DIR}"

# Wait for PC to be listening on port 8765
COUNTER=0
END=200

ns=`netstat -tnap | grep 8765`

while [ ! -n "$ns" -a $COUNTER -le $END ]
do
    sleep 20
    ns=`netstat -tnap | grep 8765`
    COUNTER=$(($COUNTER+1))
    echo "SSG is not running yet. Time waited: 20 seconds X ${COUNTER}" |logger -s -t "ec2"
done

# if PC did not start up in time, exit script and delete create node request message
if [ $COUNTER -gt $END ] ; then
  echo "SSG failed to start in time for this script to run.  Failed to create node" |logger -s -t "ec2"
  exit 1
fi

# Obtain the configuration information from the Admin Gateway
CREATE_NODE_REQUEST=`wget --no-check-certificate -qO- $ADMIN_GATEWAY_URI`

if ! `echo "${CREATE_NODE_REQUEST}" | grep -q '.*createNode.*'`; then
  echo "Failed to create the node."|logger -s -t "ec2"
  CREATE_NODE_REQUEST=""
  exit 1
fi

hostsecret=$(sed '/^\#/d' /opt/SecureSpan/Controller/etc/host.properties | grep 'host.secret'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

wget --no-check-certificate -O /tmp/createNodeResponse.xml \
     --header="Content-Type: text/xml; charset=UTF-8" --header="SOAPAction: " \
     --header="Cookie: PC-AUTH=${hostsecret}" \
     --post-data "${CREATE_NODE_REQUEST}" \
     $NODE_MANAGEMENT_URI 2> /tmp/createNodeError.log

# Always delete create node request message
CREATE_NODE_REQUEST=""

if ! grep -q '<soap:Envelope[^>]*><soap:Body><ns2:createNodeResponse[^>]*>.*</ns2:createNodeResponse></soap:Body></soap:Envelope>' /tmp/createNodeResponse.xml; then
  echo "Failed to create the node."|logger -s -t "ec2"
  CREATE_NODE_REQUEST=""
  exit 1
fi

echo "Node joined the cluster"|logger -s -t "ec2"
exit 0

