#!/bin/bash

USAGE_MESSAGE="${0} <Build Directory> <Architecture> <Kernel ID> <Ram ID> <Manifest Directory> <Cert location> <Private Key Location> <EC2 Location> <S3 Bucket> <EC2 Region>"

#command line parameters
BUILD_DIR=$1
if [ Z${BUILD_DIR} == Z ]; then
  echo ${USAGE_MESSAGE}  
  exit 1
fi

#command line parameters
ARCH=$2
if [ Z${ARCH} == Z ]; then
  echo ${USAGE_MESSAGE}  
  exit 2
fi

#command line parameters
KERNEL=$3
if [ Z${KERNEL} == Z ]; then
  echo ${USAGE_MESSAGE}  
  exit 3
fi

#command line parameters
RAM=$4
if [ Z${RAM} == Z ]; then
  echo ${USAGE_MESSAGE}  
  exit 4
fi

#command line parameters
MANIFEST=$5
if [ Z${MANIFEST} == Z ]; then
  echo ${USAGE_MESSAGE}  
  exit 5
fi

#command line parameters
CERT=$6
if [ Z${CERT} == Z ]; then
  echo ${USAGE_MESSAGE}  
  exit 6
fi

#command line parameters
PK=$7
if [ Z${PK} == Z ]; then
  echo ${USAGE_MESSAGE}  
  exit 7
fi

#command line parameters
LOCATION=$8
if [ Z${LOCATION} == Z ]; then
  echo ${USAGE_MESSAGE}  
  exit 8
fi

#command line parameters
S3_DIRECTORY=$9
if [ Z${S3_DIRECTORY} == Z ]; then
  echo ${USAGE_MESSAGE}  
  exit 9
fi

#command line parameters
REGION=${10}
if [ Z${REGION} == Z ]; then
  echo ${USAGE_MESSAGE}  
  exit 9
fi

if [ ! -d ${MANIFEST} ]; then
  mkdir ${MANIFEST}
fi

# Checks to ensure proper tools exist
PATH=/usr/bin:$PATH
command -v ec2-upload-bundle >/dev/null 2>&1 || { echo >&2 "${0} requires ec2-upload-bundle but it's not installed.  Aborting."; exit 8;};
command -v ec2-bundle-vol >/dev/null 2>&1 || { echo >&2 "${0} requires ec2-bundle-vol but it's not installed.  Aborting."; exit 8;};
command -v ec2-register >/dev/null 2>&1 || { echo >&2 "${0} requires ec2-register but it's not installed.  Aborting."; exit 8;};

ec2-bundle-vol --generate-fstab -v ${BUILD_DIR} -s 4096 -r ${ARCH} --user 3453-1765-7600 --no-inherit --kernel ${KERNEL} --ramdisk ${RAM} -d ${MANIFEST} -c ${CERT} -k ${PK};
ec2-upload-bundle --retry -m ${MANIFEST}/image.manifest.xml -a AKIAJ3C2GINP622T5DHA -s McXf6m6mYeE8Xo1Jfkl9pTwv9g8M4VbkTVWDpMld --location ${LOCATION} -b ${S3_DIRECTORY};
ec2-register ${S3_DIRECTORY}/image.manifest.xml -C ${CERT} -K ${PK} --region ${REGION} --kernel ${KERNEL} --ramdisk ${RAM};

exit 0;