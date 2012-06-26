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
AUTOSCALE_PROPERTIES="autoscalenode.properties"
MODULAR_ASSERTION_FOLDER="modular_assertions"
CUSTOM_ASSERTION_FOLDER="custom_assertions"

NODE_MANAGEMENT_URI='https://localhost:8765/services/nodeManagementApi'

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
#daemon --check "${PID_FILE}" "${PC_SCRIPT}" "${PC_USER}" "/var/run/${PID_FILE}.pid"
#RETVAL=$?
#if [ $RETVAL -ne 0 ]; then
#  echo 'Not configuring, since the Process Controller is not running.'
#  exit 1
#fi

# Mount Snapshot and scrape out values
if [ -d "${MOUNT_DIR}" ] ; then
  umount "${MOUNT_DIR}"
  rm -rf "${MOUNT_DIR}/"
fi

mkdir -m 000 "${MOUNT_DIR}"
mount -t ext3 -o noatime,ro "${MOUNT_POINT}" "${MOUNT_DIR}"

# if the autoscale node.properties file can not be found on the mounted volume, unmount it and exit the script
if [ ! -r "${MOUNT_DIR}/${AUTOSCALE_PROPERTIES}" ] ; then
  echo "Can not obtain autoscale information.  Exiting"|logger -s -t "ec2"
  umount "${MOUNT_DIR}"
  exit 1
fi 

# copy modular assertions and custom assertions into proper locations
cp -f "${MOUNT_DIR}/${MODULAR_ASSERTION_FOLDER}"/*.aar /opt/SecureSpan/Gateway/runtime/modules/assertions/
cp -f "${MOUNT_DIR}/${CUSTOM_ASSERTION_FOLDER}"/*.jar  /opt/SecureSpan/Gateway/runtime/modules/lib/
chown gateway:gateway /opt/SecureSpan/Gateway/runtime/modules/lib/*.jar

databaseAdminPassword=$(sed '/^\#/d' "${MOUNT_DIR}/${AUTOSCALE_PROPERTIES}" | grep 'node.db.config.admin.pass'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
databaseAdminUsername=$(sed '/^\#/d' "${MOUNT_DIR}/${AUTOSCALE_PROPERTIES}" | grep 'node.db.config.admin.user'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
databaseHost=$(sed '/^\#/d' "${MOUNT_DIR}/${AUTOSCALE_PROPERTIES}" | grep 'node.db.config.main.host'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
databaseName=$(sed '/^\#/d' "${MOUNT_DIR}/${AUTOSCALE_PROPERTIES}" | grep 'node.db.config.main.name'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
databaseNodePassword=$(sed '/^\#/d' "${MOUNT_DIR}/${AUTOSCALE_PROPERTIES}" | grep 'node.db.config.main.pass'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
databaseNodeUsername=$(sed '/^\#/d' "${MOUNT_DIR}/${AUTOSCALE_PROPERTIES}" | grep 'node.db.config.main.user'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
databasePort=$(sed '/^\#/d' "${MOUNT_DIR}/${AUTOSCALE_PROPERTIES}" | grep 'node.db.config.main.port'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
clusterPassword=$(sed '/^\#/d' "${MOUNT_DIR}/${AUTOSCALE_PROPERTIES}" | grep 'node.cluster.pass'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

# make sure to unmount it
umount "${MOUNT_DIR}"

# if any of the values above are blank, exit the script
if [ -z "${databaseAdminPassword}" -o -z "${databaseAdminUsername}" -o -z "${databaseHost}" -o -z "${databaseName}" -o -z "${databaseNodePassword}" -o -z "${databaseNodeUsername}" -o -z "${databasePort}" -o -z "${clusterPassword}" ]
then
    echo "Missing Configuration Information.  Exiting"|logger -s -t "ec2"
    exit 1
fi

# All configuration information is available so create Node Request.
cat > /tmp/createNodeRequest.xml <<EOF
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Header/>
  <soap:Body>
    <ns1:createNode xmlns:ns1="http://node.api.management.server.l7tech.com/">
      <arg0 xmlns:ns2="http://node.api.management.server.l7tech.com/">
        <name>default</name>
        <clusterHostname>${clusterHostname}</clusterHostname>
        <clusterPassphrase>${clusterPassword}</clusterPassphrase>
        <databases>
          <database>
            <clusterType>STANDALONE</clusterType>
            <databaseAdminPassword>${databaseAdminPassword}</databaseAdminPassword>
            <databaseAdminUsername>${databaseNodeUsername}</databaseAdminUsername>
            <host>${databaseHost}</host>
            <name>${databaseName}</name>
            <nodePassword>${databaseNodePassword}</nodePassword>
            <nodeUsername>${databaseNodeUsername}</nodeUsername>
            <port>${databasePort}</port>
            <type>NODE_ALL</type>
            <vendor>MYSQL</vendor>
          </database>
        </databases>
        <enabled>true</enabled>
      </arg0>
    </ns1:createNode>
  </soap:Body>
</soap:Envelope>
EOF

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
  rm -rf /tmp/createNodeRequest.xml
  exit 1
fi

hostsecret=$(sed '/^\#/d' /opt/SecureSpan/Controller/etc/host.properties | grep 'host.secret'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')

wget --no-check-certificate -O /tmp/createNodeResponse.xml \
     --header="Content-Type: text/xml; charset=UTF-8" --header="SOAPAction: " \
     --header="Cookie: PC-AUTH=${hostsecret}" \
     --post-file=/tmp/createNodeRequest.xml \
     $NODE_MANAGEMENT_URI 2> /tmp/createNodeError.log

# Always delete create node request message
rm -rf /tmp/createNodeRequest.xml

if ! grep -q '<soap:Envelope[^>]*><soap:Body><ns2:createNodeResponse[^>]*>.*</ns2:createNodeResponse></soap:Body></soap:Envelope>' /tmp/createNodeResponse.xml; then
  echo "Failed to create the node."|logger -s -t "ec2"
  exit 1
fi

echo "Node joined the cluster"|logger -s -t "ec2"
exit 0

