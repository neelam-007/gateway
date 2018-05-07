#!/bin/bash

#global variables
BUILD_DIR=builddir
MANIFEST_DIR=manifest
USAGE_MESSAGE='Usage: EC2InstanceStoreCreation.sh <build directory> <buildzip> <xen ec2 tgz file>'
RCLOCAL=/root/AMI_Build_Tools/rc.local
GET_CREDS=/root/AMI_Build_Tools/get-credentials.sh

#command line parameters
BUILD_DIR=$1
if [ Z${BUILD_DIR} == Z ]; then
  echo ${USAGE_MESSAGE}  
  exit 1
fi

BUILD_ZIP=$2
if [ Z${BUILD_ZIP} == Z ]; then
  echo ${USAGE_MESSAGE} 
  exit 1
fi

EC2_XEN=$3
if [ Z${EC2_XEN} == Z ]; then
  echo ${USAGE_MESSAGE} 
  exit 1
fi

if [ ! -r ${RCLOCAL} ]; then
  echo "${RCLOCAL} needs to exist in the same folder as ${0}"
  exit 2
fi

if [ ! -r ${GET_CREDS} ]; then
  echo "${GET_CREDS} needs to exist in the same folder as ${0}"
  exit 3
fi

if [ -d ${BUILD_DIR} -o -f ${BUILD_DIR} ]; then
  echo "${BUILD_DIR} already exists in this folder.  Please pick another name"
  exit 4
fi

if [ ! -r ${BUILD_ZIP} ]; then
  echo "${BUILD_ZIP} is not readable.  Plese provide a proper file."
  exit 5
fi

if [ ! -r ${EC2_XEN} ]; then
  echo "${EC2_XEN} is not readable.  Plese provide a proper file."
  exit 6
fi

# setup initial filesystem
mkdir -p ${BUILD_DIR}
cd ${BUILD_DIR}
mkdir -p lost+found
chmod 700 lost+found
mkdir proc
mkdir sys
mkdir tmp
chmod 1777 tmp
tar xzf ${BUILD_ZIP}

#setup Amazon specific files
tar xzf ${EC2_XEN}  
cp ${RCLOCAL} ./etc/rc.d/
cp ${GET_CREDS} ./usr/local/sbin/
chmod u+x ./usr/local/sbin/`basename "${GET_CREDS}"`

# modify build as chroot
mount /proc ./proc -t proc
cat > ./EC2InstanceStoreCreation_chroot.sh <<EOF
#!/bin/bash

echo "hwcap 0 nosegneg" > /etc/ld.so.conf.d/xen.conf
ldconfig -v

echo >/etc/sysconfig/network-scripts/ifcfg-eth0
echo DEVICE=eth0 >> /etc/sysconfig/network-scripts/ifcfg-eth0
echo ONBOOT=yes >> /etc/sysconfig/network-scripts/ifcfg-eth0
echo BOOTPROTO=dhcp >> /etc/sysconfig/network-scripts/ifcfg-eth0

sed -i -e 's/s0:2345:respawn:\/sbin\/agetty -L 9600 ttyS0 vt100/#s0:2345:respawn:\/sbin\/agetty -L 9600 ttyS0 vt100/' "/etc/inittab"
sed -i -e 's/s1:2345:respawn:\/sbin\/agetty -L 9600 ttyS1 vt100/#s1:2345:respawn:\/sbin\/agetty -L 9600 ttyS1 vt100/' "/etc/inittab"

chkconfig kudzu off

chage -E -1 -I -1 -m 0 -M -1 root
chage -E -1 -I -1 -m 0 -M -1 ssgconfig

sed -i -e 's/PermitRootLogin no/PermitRootLogin yes/' "/etc/ssh/sshd_config"
sed -i -e 's/#RSAAuthentication yes/RSAAuthentication yes/' "/etc/ssh/sshd_config"
sed -i -e 's/PasswordAuthentication yes/PasswordAuthentication no/' "/etc/ssh/sshd_config"

echo 'root' >> /etc/ssh/ssh_allowed_users

if [ ! -e ./lib/modules/*-xenU-ec2*/modules.dep ] ; then
    depmod -a `basename ./lib/modules/*-xenU-ec2*/`;
fi

touch /root/firstrun
chage -d `date +%F` -m 0 -M -1 ssgconfig

umount /proc
exit
EOF

chmod 700 ./EC2InstanceStoreCreation_chroot.sh
chroot ${BUILD_DIR}

exit 0