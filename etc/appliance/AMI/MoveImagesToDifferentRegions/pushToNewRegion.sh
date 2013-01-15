#!/bin/bash
#1) Start basic amazon linux image in original region
#2) Attach Snapshot to amazon linux image
#3) ssh into image
#4) mount snapshot and tar ball up contents
#5) Start basic amazon linux image in new region
#6) Attach Snapshot to amazon linux image
#7) scp ssh key into instance
#8) ssh into instance and scp tar ball from original region
#9) unpack tar ball into snapshot
#10) unmount tarball
#11) delete ssh key and tar ball
#12) detach volume
#13) turn volume into snapshot
#14) register snapshot as ami

# Values to Modify between environments
#SCRIPTS TO RUN REMOTELY
GRAB_PACK_SCRIPT="copyFromSnapshotToTarBall.sh"
GRAB_PACK_SCRIPT_LOCATION="/root/AMI_Build_Tools/scripts/${GRAB_PACK_SCRIPT}"
GRAB_UNPACK_SCRIPT="obtainAndUnpackVolume.sh"
GRAB_UNPACK_SCRIPT_LOCATION="/root/AMI_Build_Tools/scripts/${GRAB_UNPACK_SCRIPT}"
SSH_KEY_US_EAST_PATH="/root/AMI_Build_Tools/scripts/security/tacticalbuilduseast.pem"
SSH_KEY_US_EAST="tacticalBuildUSEast"
SSH_KEY_US_EAST_FILE="tacticalbuilduseast.pem"
SSH_KEY_US_WEST_1="tacticalBuildUSWest1"
SSH_KEY_US_WEST_1_PATH="/root/AMI_Build_Tools/scripts/security/tacticalBuildUSWest1.pem"
SSH_KEY_US_WEST_2_PATH="/root/AMI_Build_Tools/scripts/security/tacticalBuildUSWest2.pem"
SSH_KEY_US_WEST_2="tacticalBuildUSWest2"
SSH_KEY_EU="tacticalBuildEUWest1"
SSH_KEY_EU_PATH="/root/AMI_Build_Tools/scripts/security/tacticalBuildEUWest1.pem"
SSH_KEY_AP_SE_1="tacticalBuildAPSE1"
SSH_KEY_AP_SE_1_PATH="/root/AMI_Build_Tools/scripts/security/tacticalBuildAPSE1.pem"
SSH_KEY_AP_SE_2="rodapsoutheast2"
SSH_KEY_AP_SE_2_PATH="/root/AmazonSSHKeys/rodapsoutheast2.pem"
SSH_KEY_AP_NE_1="rodapnortheast1"
SSH_KEY_AP_NE_1_PATH="/root/AmazonSSHKeys/rodapnortheast1.pem"

DATE_PREFIX=`date +%Y%m%d_%H%M_%S`
AMI_NAME_32BIT="ssg_v70_32bit_EBS"
VOLUME_NAME_32BIT="ssg_v70_32bit_EBS"
SNAPSHOT_NAME_32BIT="ssg_v70_32bit_EBS"
AMI_NAME_64BIT="ssg_v70_64bit_EBS"
VOLUME_NAME_64BIT="ssg_v70_64bit_EBS"
SNAPSHOT_NAME_64BIT="ssg_v70_64bit_EBS"
MIGRATION_INSTANCE_NAMES="SSG EBS Migration Instance 64BIT"
SOURCE_MIGRATION_INSTANCE_NAME="SSG EBS Migration Instance Source 64BIT"
SOURCE_MIGRATION_INSTANCE_HOST=""
AMI_TAR_BALL="v70_ebs_ami.tgz"
ARCH_32BIT="i386"
ARCH_64BIT="x86_64"

#US_EAST
SG_US_EAST="Standard SSG Firewall"
SOURCE_SNAP_US_EAST_32BIT="snap-3a0dd775"
SOURCE_SNAP_US_EAST_64BIT="snap-9a3dd3d5"

#US_WEST_1
KERNEL_ID_US_WEST_1_64BIT="aki-921444d7"
RAM_ID_US_WEST_1_64BIT="ari-961444d3"
KERNEL_ID_US_WEST_1_32BIT="aki-9e1444db"
RAM_ID_US_WEST_1_32BIT="ari-941444d1"
AMI_ID_US_WEST_1="ami-19f9de5c"
SG_US_WEST_1="SSG"

#US_WEST_2
KERNEL_ID_US_WEST_2_64BIT="aki-8ae26fba"
RAM_ID_US_WEST_2_64BIT="ari-1ce26f2c"
KERNEL_ID_US_WEST_2_32BIT="aki-cae26ffa"
RAM_ID_US_WEST_2_32BIT="ari-d0e26fe0"
AMI_ID_US_WEST_2="ami-2231bf12"
SG_US_WEST_2="SSG"

#EU
KERNEL_ID_EU_64BIT="aki-fe61548a"
RAM_ID_EU_64BIT="ari-f6615482"
KERNEL_ID_EU_32BIT="aki-fc615488"
RAM_ID_EU_32BIT="ari-f4615480"
AMI_ID_EU="ami-937474e7"
SG_EU="SSG"

#ap-southeast-1
KERNEL_ID_AP_SE_1_32BIT="aki-181a644a"
RAM_ID_AP_SE_1_32BIT="ari-161a6444"
KERNEL_ID_AP_SE_1_64BIT="aki-1e1a644c"
RAM_ID_AP_SE_1_64BIT="ari-141a6446"
AMI_ID_AP_SE_1="ami-a2a7e7f0"
SG_AP_SE_1="Standard SSG Firewall"

#ap-southeast-2
KERNEL_ID_AP_SE_2="aki-1e1a644c"
RAM_ID_AP_SE_2="ari-141a6446"
AMI_ID_AP_SE_2="ami-b3990e89"

#ap-northeast-1
KERNEL_ID_AP_NE_1="aki-ea09a2eb"
RAM_ID_AP_NE_1="ari-bc09a2bd"
AMI_ID_AP_NE_1="ami-486cd349"
SG_AP_NE_1="SSG"

function scpOutWithRetries()
{
  CONNECTION_REFUSED="start"
  HOST=$1
  USER=$2
  KEY=$3
  COUNTER=0
  END=$4
  FILES=$5

  while [ -n "${CONNECTION_REFUSED}" -a $COUNTER -le $END ]
  do
    echo "Attempting to connect to ${HOST} to send ${FILES}.  Tries so far: ${COUNTER}"
    sleep 15
    SCPOUTPUT=$(scp -i "${KEY}" -o "StrictHostKeyChecking=no" $FILES $USER@"${HOST}": 2>&1)
    CONNECTION_REFUSED=""
    CONNECTION_REFUSED=$(echo $SCPOUTPUT | grep "Connection refused")
    echo "${SCPOUTPUT}"
    COUNTER=$(($COUNTER+1))
  done
}

function scpInWithRetries()
{
  CONNECTION_REFUSED="start"
  HOST=$1
  USER=$2
  KEY=$3
  COUNTER=0
  END=$4
  FILES=$5

  while [ -n "${CONNECTION_REFUSED}" -a $COUNTER -le $END ]
  do
    echo "Attempting to connect to ${HOST} to obtain ${FILES}.  Tries so far: ${COUNTER}"
    sleep 15
    SCPOUTPUT=$(scp -i "${KEY}" -o "StrictHostKeyChecking=no" $USER@"${HOST}:${FILES}" . 2>&1)
    CONNECTION_REFUSED=""
    CONNECTION_REFUSED=$(echo $SCPOUTPUT | grep "Connection refused")
    echo "${SCPOUTPUT}" 
    COUNTER=$(($COUNTER+1))
  done
}

function sshWithRetries()
{
  CONNECTION_REFUSED="start"
  HOST=$1
  USER=$2
  KEY=$3
  COUNTER=0
  END=$4
  EXTRACOMMANDS=$5

  while [ -n "${CONNECTION_REFUSED}" -a $COUNTER -le $END ]
  do
    echo "Attempting to connect to SSH to ${HOST} to execute commands.  Tries so far: ${COUNTER}"
    sleep 15
    SSHOUTPUT=$(ssh -i "${KEY}" -o "StrictHostKeyChecking=no" $USER@"${HOST}" -t "${EXTRACOMMANDS}" 2>&1)
    CONNECTION_REFUSED=""
    CONNECTION_REFUSED=$(echo $SSHOUTPUT | grep "Connection refused")
    echo "${OUTPUT}"
    COUNTER=$(($COUNTER+1))
  done
}

function mountAndCopy()
{
  REGION=$1  
  SOURCE_AMI_ID=$2
  SOURCE_SSH_KEY=$3
  SOURCE_SSH_KEY_FILE=$4
  SOURCE_SSH_KEY_PATH=$5
  SECURITY_GROUP_NAME=$6
  SOURCE_SNAPSHOT=$7
  SOURCE_MIGRATION_INSTANCE_NAME=$8
  
  echo "Starting mountAndCopy for ${REGION} from ${SOURCE_SNAPSHOT} to ${SOURCE_AMI_ID} to create ${AMI_TAR_BALL}"
    
  RUN_INSTANCES_OUTPUT=$(ec2-run-instances $SOURCE_AMI_ID -n 1 -k $SOURCE_SSH_KEY -t t1.micro -g  "${SECURITY_GROUP_NAME}" --region $REGION -b "/dev/sdf=${SOURCE_SNAPSHOT}" )
  INSTANCE_ID=$(echo $RUN_INSTANCES_OUTPUT | sed 's/.*INSTANCE\s*\(i-\S*\)\s.*/\1/')
  
  ec2-create-tags $INSTANCE_ID --tag "Name=${SOURCE_MIGRATION_INSTANCE_NAME}" --region $REGION
 
  IS_RUNNING=""
  COUNTER=0
  END=200
  DESCRIBE_OUTPUT=$(ec2-describe-instances $INSTANCE_ID --region $REGION)
  IS_RUNNING=$(echo $DESCRIBE_OUTPUT | grep "running")
  
  while [ -z "${IS_RUNNING}" -a $COUNTER -le $END ]
  do
      sleep 20
      DESCRIBE_OUTPUT=$(ec2-describe-instances $INSTANCE_ID --region $REGION)
      IS_RUNNING=$(echo $DESCRIBE_OUTPUT | grep "running")        
      COUNTER=$(($COUNTER+1))
      echo "Instance is not running yet. Time waited: 20 seconds X ${COUNTER}"
  done
  
  if [ $COUNTER -gt $END ] ; then
    echo "Instance failed to start in time for this script to run."
    exit 1
  fi
  
  echo "Instance ${INSTANCE_ID} started successfully"  
  
  SOURCE_DNS=$(echo $DESCRIBE_OUTPUT | sed 's/\(.*\s\)\(\S*.amazonaws.com\)\(\s.*\)/\2/g')
  
  FILES_TO_UPLOAD="${GRAB_PACK_SCRIPT_LOCATION}"
  
  scpOutWithRetries "${SOURCE_DNS}" "ec2-user" "${SOURCE_SSH_KEY_PATH}" 10 "${FILES_TO_UPLOAD}"   
  sshWithRetries "${SOURCE_DNS}" "ec2-user" "${SOURCE_SSH_KEY_PATH}" 10 "ls -lt; ./${GRAB_PACK_SCRIPT} ${AMI_TAR_BALL} | tee -a ./${SOURCE_DNS}_${DATE_PREFIX}.log"
  scpInWithRetries "${SOURCE_DNS}" "ec2-user" "${SOURCE_SSH_KEY_PATH}" 10 "${SOURCE_DNS}_${DATE_PREFIX}.log"
  sshWithRetries "${SOURCE_DNS}" "ec2-user" "${SOURCE_SSH_KEY_PATH}" 10 "ls -lt"
  
  echo "Tar Ball of the AMI in ${REGION} on host ${SOURCE_DNS}.  Please shut down Instance ${INSTANCE_ID} when you are done with it."
  
  SOURCE_MIGRATION_INSTANCE_HOST="${SOURCE_DNS}"    
}

function migrateAndDeploy()
{
  REGION=$1  
  SOURCE_INSTANCE=$2
  SOURCE_SSH_KEY_FILE=$3
  SOURCE_SSH_KEY_PATH=$4
  DESTINATION_AMI_ID=$5
  DESTINATION_SSH_KEY=$6
  DESTINATION_SSH_KEY_PATH=$7
  KERNEL_ID=$8
  RAM_ID=$9
  ARCH=${10}
  SECURITY_GROUP_NAME=${11}
  AMI_NAME=${12}
  VOLUME_NAME=${13}
  SNAPSHOT_NAME=${14}
  
  echo "Starting migrateAndDeploy for ${REGION} from ${SOURCE_INSTANCE} to ${DESTINATION_AMI_ID} to register an AMI with ${KERNEL_ID} ${RAM_ID} ${ARCH} ${SECURITY_GROUP_NAME}"
    
  RUN_INSTANCES_OUTPUT=$(ec2-run-instances $DESTINATION_AMI_ID -n 1 -k $DESTINATION_SSH_KEY -t t1.micro -g  "${SECURITY_GROUP_NAME}" --region $REGION -b "/dev/sdf=:50" )
  INSTANCE_ID=$(echo $RUN_INSTANCES_OUTPUT | sed 's/.*INSTANCE\s*\(i-\S*\)\s.*/\1/')
  
  if [ -z "${INSTANCE_ID}" ] ; then
    echo "Instance failed to start. Exiting" 
    exit 1
  fi
  
  ec2-create-tags $INSTANCE_ID --tag "Name=${MIGRATION_INSTANCE_NAMES}" --region $REGION
 
  IS_RUNNING=""
  COUNTER=0
  END=200
  DESCRIBE_OUTPUT=$(ec2-describe-instances $INSTANCE_ID --region $REGION)
  IS_RUNNING=$(echo $DESCRIBE_OUTPUT | grep "running")
  
  while [ -z "${IS_RUNNING}" -a $COUNTER -le $END ]
  do
      sleep 20
      DESCRIBE_OUTPUT=$(ec2-describe-instances $INSTANCE_ID --region $REGION)
      IS_RUNNING=$(echo $DESCRIBE_OUTPUT | grep "running")        
      COUNTER=$(($COUNTER+1))
      echo "Instance is not running yet. Time waited: 20 seconds X ${COUNTER}"
  done
  
  if [ $COUNTER -gt $END ] ; then
    echo "Instance failed to start in time for this script to run."
    exit 1
  fi
  
  echo "Instance ${INSTANCE_ID} started successfully"
  
  IS_STATUS_OK=""
  COUNTER2=0
  END2=200
  DESCRIBE_STATUS_OUTPUT=$(ec2-describe-instance-status $INSTANCE_ID --region $REGION)
  IS_STATUS_OK=$(echo $DESCRIBE_STATUS_OUTPUT | grep "SYSTEMSTATUS reachability passed INSTANCESTATUS reachability passed")
  
  while [ -z "${IS_STATUS_OK}" -a $COUNTER2 -le $END2 ]
  do
      sleep 10
      COUNTER2=$(($COUNTER2+1))
      echo "Instance has not passed status checks. Time waited: 10 seconds X ${COUNTER2}"
      DESCRIBE_STATUS_OUTPUT=$(ec2-describe-instance-status $INSTANCE_ID --region $REGION)
      IS_STATUS_OK=$(echo $DESCRIBE_STATUS_OUTPUT | grep "SYSTEMSTATUS reachability passed INSTANCESTATUS reachability passed")
  done
  
  if [ $COUNTER2 -gt $END2 ] ; then
    echo "Instance failed to pass status checks."
    exit 1
  fi
  
  echo "Instance ${INSTANCE_ID} passed Status Checks"
  
  PUBLIC_DNS=$(echo $DESCRIBE_OUTPUT | sed 's/\(.*\s\)\(\S*.amazonaws.com\)\(\s.*\)/\2/g')
  
  FILES_TO_UPLOAD="${GRAB_UNPACK_SCRIPT_LOCATION} ${SOURCE_SSH_KEY_PATH}"
  
  scpOutWithRetries "${PUBLIC_DNS}" "ec2-user" "${DESTINATION_SSH_KEY_PATH}" 10 "${FILES_TO_UPLOAD}"   
  sshWithRetries "${PUBLIC_DNS}" "ec2-user" "${DESTINATION_SSH_KEY_PATH}" 10 "ls -lt; ./${GRAB_UNPACK_SCRIPT} ${SOURCE_SSH_KEY_FILE} ec2-user ${SOURCE_INSTANCE} v70_ebs_ami.tgz 2>&1 | tee -a ./${PUBLIC_DNS}_${DATE_PREFIX}.log"
  scpInWithRetries "${PUBLIC_DNS}" "ec2-user" "${DESTINATION_SSH_KEY_PATH}" 10 "${PUBLIC_DNS}_${DATE_PREFIX}.log"
  sshWithRetries "${PUBLIC_DNS}" "ec2-user" "${DESTINATION_SSH_KEY_PATH}" 10 "ls -lt; sudo rm -rf ~/*; ls -lt"
  
  VOL_ID=$(echo $DESCRIBE_OUTPUT | sed 's/\(.*\/dev\/sdf.*\)\(vol-\S*\)\(\s.*\)/\2/g')
  DETACH_OUTPUT=$(ec2-detach-volume $VOL_ID --instance $INSTANCE_ID --region $REGION)
  
  IS_VOLUME_DETACHED=""
  COUNTERV=0
  ENDV=40
  DESCRIBE_VOLUME_OUTPUT=$(ec2-describe-volumes $VOL_ID --region $REGION)
  IS_VOLUME_DETACHED=$(echo $DESCRIBE_STATUS_OUTPUT | grep "available")
  
  while [ -z "${IS_VOLUME_DETACHED}" -a $COUNTERV -le $ENDV ]
  do
      sleep 10
      COUNTERV=$(($COUNTERV+1))
      echo "Volume Has Not Detached. Time waited: 10 seconds X ${COUNTERV}"
      DESCRIBE_VOLUME_OUTPUT=$(ec2-describe-volumes $VOL_ID --region $REGION)
      IS_VOLUME_DETACHED=$(echo $DESCRIBE_VOLUME_OUTPUT | grep "available")
  done
  
  if [ $COUNTERV -gt $ENDV ] ; then
    echo "Volume failed to detach"
    exit 1
  fi
  
  echo "Volume ${VOL_ID} detached"
  
  ec2-create-tags $VOL_ID --tag "Name=${VOLUME_NAME}" --region $REGION
  
  CREATE_SNAPSHOT_OUTPUT=$(ec2-create-snapshot $VOL_ID -d "${VOLUME_NAME}" --region $REGION)
  SNAP_ID=$(echo $CREATE_SNAPSHOT_OUTPUT | sed 's/\(.*\s\)\(snap-\S*\)\(\s.*\)/\2/g')
   
  IS_SNAPSHOT_CREATED=""
  COUNTERV1=0
  ENDV1=100
  DESCRIBE_SNAPSHOT_OUTPUT=$(ec2-describe-snapshots $SNAP_ID --region $REGION)
  IS_SNAPSHOT_CREATED=$(echo $DESCRIBE_SNAPSHOT_OUTPUT | grep "completed")
  
  while [ -z "${IS_SNAPSHOT_CREATED}" -a $COUNTERV1 -le $ENDV1 ]
  do
      sleep 10
      COUNTERV1=$(($COUNTERV1+1))
      echo "Snapshot has not been created. Time waited: 10 seconds X ${COUNTERV1}"
      DESCRIBE_SNAPSHOT_OUTPUT=$(ec2-describe-snapshots $SNAP_ID --region $REGION)
      IS_SNAPSHOT_CREATED=$(echo $DESCRIBE_SNAPSHOT_OUTPUT | grep "completed")
  done
  
  if [ $COUNTERV1 -gt $ENDV1 ] ; then
    echo "Snapshot failed to create"
    exit 1
  fi
  
  echo "Snapshot ${SNAP_ID} created"
  
  ec2-create-tags $SNAP_ID --tag "Name=${SNAPSHOT_NAME}" --region $REGION
  
  REGISTER_ID=$(ec2-register -s $SNAP_ID -name "${AMI_NAME}" --region $REGION --kernel $KERNEL_ID --ramdisk $RAM_ID -a $ARCH -d "${AMI_NAME}")
  AMI_ID=$(echo $REGISTER_ID | sed 's/\(.*IMAGE\s*\)\(ami-\S*\)\(.*\)/\2/g')
  
  if [ -z "${AMI_ID}" ] ; then
    echo "AMI failed to be created"
    exit 1
  fi
  
  ec2-create-tags $AMI_ID --tag "Name=${AMI_NAME}" --region $REGION
  
  echo "AMI with ID ${AMI_ID} and name ${AMI_NAME} has been created in ${REGION}.  Please shut down Instance ${INSTANCE_ID}" 
}

#mountAndCopy "us-east-1" "ami-1a249873" "${SSH_KEY_US_EAST}" "${SSH_KEY_US_EAST_FILE}" "${SSH_KEY_US_EAST_PATH}" "${SG_US_EAST}" "${SOURCE_SNAP_US_EAST_64BIT}" "${SOURCE_MIGRATION_INSTANCE_NAME}" 
SOURCE_MIGRATION_INSTANCE_HOST="ec2-23-20-110-191.compute-1.amazonaws.com"

if [ -z "${SOURCE_MIGRATION_INSTANCE_HOST}" ] ; then
  echo "Source was not successfully exported."
  exit 1
fi

# Please uncomment whichever build you would like to do.
# US-WEST-1 (California) 32 BIT
#migrateAndDeploy "us-west-1" "${SOURCE_MIGRATION_INSTANCE_HOST}" "${SSH_KEY_US_EAST_FILE}" "${SSH_KEY_US_EAST_PATH}" "${AMI_ID_US_WEST_1}" "${SSH_KEY_US_WEST_1}" "${SSH_KEY_US_WEST_1_PATH}" "${KERNEL_ID_US_WEST_1_32BIT}" "${RAM_ID_US_WEST_1_32BIT}" "${ARCH_32BIT}" "${SG_US_WEST_1}" "${AMI_NAME_32BIT}" "${VOLUME_NAME_32BIT}" "${SNAPSHOT_NAME_32BIT}"

# US-WEST-1 (California) 64 BIT
#migrateAndDeploy "us-west-1" "${SOURCE_MIGRATION_INSTANCE_HOST}" "${SSH_KEY_US_EAST_FILE}" "${SSH_KEY_US_EAST_PATH}" "${AMI_ID_US_WEST_1}" "${SSH_KEY_US_WEST_1}" "${SSH_KEY_US_WEST_1_PATH}" "${KERNEL_ID_US_WEST_1_64BIT}" "${RAM_ID_US_WEST_1_64BIT}" "${ARCH_64BIT}" "${SG_US_WEST_1}" "${AMI_NAME_64BIT}" "${VOLUME_NAME_64BIT}" "${SNAPSHOT_NAME_32BIT}"


# US-WEST-2 (Oregon) 32 BIT
#migrateAndDeploy "us-west-2" "${SOURCE_MIGRATION_INSTANCE_HOST}" "${SSH_KEY_US_EAST_FILE}" "${SSH_KEY_US_EAST_PATH}" "${AMI_ID_US_WEST_2}" "${SSH_KEY_US_WEST_2}" "${SSH_KEY_US_WEST_2_PATH}" "${KERNEL_ID_US_WEST_2_32BIT}" "${RAM_ID_US_WEST_2_32BIT}" "${ARCH_32BIT}" "${SG_US_WEST_2}" "${AMI_NAME_32BIT}" "${VOLUME_NAME_32BIT}" "${SNAPSHOT_NAME_32BIT}"

# US-WEST-2 (Oregon) 64 BIT
#migrateAndDeploy "us-west-2" "${SOURCE_MIGRATION_INSTANCE_HOST}" "${SSH_KEY_US_EAST_FILE}" "${SSH_KEY_US_EAST_PATH}" "${AMI_ID_US_WEST_2}" "${SSH_KEY_US_WEST_2}" "${SSH_KEY_US_WEST_2_PATH}" "${KERNEL_ID_US_WEST_2_64BIT}" "${RAM_ID_US_WEST_2_64BIT}" "${ARCH_64BIT}" "${SG_US_WEST_2}" "${AMI_NAME_64BIT}" "${VOLUME_NAME_64BIT}" "${SNAPSHOT_NAME_64BIT}"


# EU-WEST-1 32 BIT
#migrateAndDeploy "eu-west-1" "${SOURCE_MIGRATION_INSTANCE_HOST}" "${SSH_KEY_US_EAST_FILE}" "${SSH_KEY_US_EAST_PATH}" "${AMI_ID_EU}" "${SSH_KEY_EU}" "${SSH_KEY_EU_PATH}" "${KERNEL_ID_EU_32BIT}" "${RAM_ID_EU_32BIT}" "${ARCH_32BIT}" "${SG_EU}" "${AMI_NAME_32BIT}" "${VOLUME_NAME_32BIT}" "${SNAPSHOT_NAME_32BIT}"

# EU-WEST-1 64 BIT
migrateAndDeploy "eu-west-1" "${SOURCE_MIGRATION_INSTANCE_HOST}" "${SSH_KEY_US_EAST_FILE}" "${SSH_KEY_US_EAST_PATH}" "${AMI_ID_EU}" "${SSH_KEY_EU}" "${SSH_KEY_EU_PATH}" "${KERNEL_ID_EU_64BIT}" "${RAM_ID_EU_64BIT}" "${ARCH_64BIT}" "${SG_EU}" "${AMI_NAME_64BIT}" "${VOLUME_NAME_64BIT}" "${SNAPSHOT_NAME_64BIT}"


# AP-SOUTHEAST-1 32 BIT (Singapore)
#migrateAndDeploy "ap-southeast-1" "${SOURCE_MIGRATION_INSTANCE_HOST}" "${SSH_KEY_US_EAST_FILE}" "${SSH_KEY_US_EAST_PATH}" "${AMI_ID_AP_SE_1}" "${SSH_KEY_AP_SE_1}" "${SSH_KEY_AP_SE_1_PATH}" "${KERNEL_ID_AP_SE_1_32BIT}" "${RAM_ID_AP_SE_1_32BIT}" "${ARCH_32BIT}" "${SG_AP_SE_1}" "${AMI_NAME_32BIT}" "${VOLUME_NAME_32BIT}" "${SNAPSHOT_NAME_32BIT}"

# AP-SOUTHEAST-1 64 BIT (Singapore)
migrateAndDeploy "ap-southeast-1" "${SOURCE_MIGRATION_INSTANCE_HOST}" "${SSH_KEY_US_EAST_FILE}" "${SSH_KEY_US_EAST_PATH}" "${AMI_ID_AP_SE_1}" "${SSH_KEY_AP_SE_1}" "${SSH_KEY_AP_SE_1_PATH}" "${KERNEL_ID_AP_SE_1_64BIT}" "${RAM_ID_AP_SE_1_64BIT}" "${ARCH_64BIT}" "${SG_AP_SE_1}" "${AMI_NAME_64BIT}" "${VOLUME_NAME_64BIT}" "${SNAPSHOT_NAME_64BIT}"


#migrateAndDeploy "ap-southeast-2" "ec2-23-20-113-129.compute-1.amazonaws.com" "${SSH_KEY_US_EAST_FILE}" "${SSH_KEY_US_EAST_PATH}" "${AMI_ID_AP_SE_2}" "${SSH_KEY_AP_SE_2}" "${SSH_KEY_AP_SE_2_PATH}" "${KERNEL_ID_AP_SE_2}" "${RAM_ID_AP_SE_2}" "x86_64" "SSG"
#migrateAndDeploy "ap-northeast-1" "ec2-23-20-113-129.compute-1.amazonaws.com" "${SSH_KEY_US_EAST_FILE}" "${SSH_KEY_US_EAST_PATH}" "${AMI_ID_AP_NE_1}" "${SSH_KEY_AP_NE_1}" "${SSH_KEY_AP_NE_1_PATH}" "${KERNEL_ID_AP_NE_1}" "${RAM_ID_AP_NE_1}" "x86_64" "${SG_AP_NE_1}"

exit 0
