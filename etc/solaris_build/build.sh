#!/bin/sh

#echo Cleaning out previous build stuff.
rm -rf pkgroot
#rm -rf pkgbuild
mkdir pkgroot

cd pkgbuild

# pkgadd will run just the checkinstall script as user nobody
# the rest are run as root
chmod 755 *install

# Fix pkginfo to sub in the version
# supply a sane default
# run script as ./build.sh 4.0m1 to set version
version="4.0"
if [ ! -z "$1" ]; then
	version=$1;
fi
echo "Setting package version to $version"
STAMP=`date '+%Y%m%d'`
echo "VERSION=\"$version\"" >> pkginfo
echo "PSTAMP=\"${STAMP}\"" >> pkginfo

cd ..
echo "cleaning old files"
rm -f ssg.tar
rm -f solaris_ssg_bin.tar
echo "making new ones"
cp ../ssg*.tar.gz ssg.tar.gz
sh make_overlay_tarball.sh
echo Decompressing...
gunzip ssg.tar.gz 
gunzip solaris_ssg_bin.tar.gz

cd pkgroot/
echo "Unpacking standard tarball"
/usr/sfw/bin/gtar -xf ../ssg.tar 
rm ../ssg.tar
echo "Unpacking Solaris replacement files"
/usr/sfw/bin/gtar -xf ../solaris_ssg_bin.tar
rm ../solaris_ssg_bin.tar

#Minor cleanup, and removal of evil spaces....! DIE!
echo "Cleanup"

rm -f ssg/etc/inf/ssg/webadmin/help/securespan\ manager\ help\ system.log
rmdir ssg/dist
rm -rf ssg/jdk

echo "Making dir structure"
mkdir -p etc/rc2.d
mkdir -p export/home/gateway
mkdir -p export/home/ssgconfig
mkdir -p etc/init.d
mkdir -p etc/ipf
mkdir -p etc/snmp/conf

echo "moving config and startup"

# Mysql config
mv ssg/bin/my.cnf.ssg etc/my.cnf.ssg
# profile for ssgconfig menu
mv ssg/bin/configuser_bashrc export/home/ssgconfig/.profile
# ssg config run script
mv ssg/bin/ssgconfig.sh ssg/configwizard/
# snmp
mv ssg/bin/snmpd.conf etc/snmp/conf/snmpd.conf.sample
# ipf source file
cp ssg/bin/base-ipf.conf etc/ipf/ipf.conf
mv ssg/bin/base-ipf.conf ssg/etc/
# Tune Stack
mv ssg/bin/tune_solaris_tcp_stack.sh etc/init.d/ssg_tcp_tune.sh
# Database cluster startup daemon
mv ssg/bin/ssg-dbstatus-initd etc/init.d/ssg-dbstatus
# Main ssg process startup
mv ssg/bin/ssg-initd ssg/bin/ssg
# need a separate sh starter script to work around solaris vagarities
mv ssg/bin/ssg-starter etc/init.d/ssg
# apply network config startup/shutdown process
mv ssg/bin/sysconfigscript-initd ssg/bin/ssg-sysconfig
# need a separate sh starter script to work around solaris vagarities
mv ssg/bin/ssg-sysconfig-starter etc/init.d/ssg-sysconfig
mv ssg/bin/systemconfig.sh ssg/sysconfigwizard/
mv ssg/bin/ssg_sys_config.pl ssg/sysconfigwizard/
mv ssg/bin/ssgmigration.sh ssg/migration/
# because of the way the tarball is built, it only include the linux flash file
# so I had to move it into the solaris kit that we copy into /ssg/bin
mv ssg/bin/grandmaster_flash.solaris ssg/migration/cfg/grandmaster_flash

echo "Touching supplementary ruleset."
touch ssg/etc/ssg-ipf.conf

echo "Moving properties to partition Template"

mv ssg/etc/conf/*.properties ssg/etc/conf/partitions/partitiontemplate_/
mv ssg/etc/conf/cluster_hostname-dist ssg/etc/conf/partitions/partitiontemplate_/

# put a java.security in the partition template (so we get one for each partition)
mv ssg/bin/ssg-java.security ssg/etc/conf/partitions/partitiontemplate_/java.security
echo "Cleaning non-solaris scripts"
rm ssg/bin/tcp_tune.sh
rm ssg/bin/my.ini
rm ssg/bin/pkcs11_linux.cfg
rm ssg/bin/tarari-initd

echo Fixing permissions...

chmod -f 755 export/home/ssgconfig/.profile
chmod -f 775 ssg/configwizard
chmod -f 664 ssg/configwizard/*
chmod -f 775 ssg/configwizard/*.sh
chmod -f 775 ssg/configwizard/lib
chmod -fR 775 ssg/etc/keys  2&>/dev/null
chmod -f 775 ssg/sysconfigwizard
chmod -f 664 ssg/sysconfigwizard/*
chmod -f 775 ssg/sysconfigwizard/lib
chmod -f 775 ssg/sysconfigwizard/configfiles
chmod -f 775 ssg/sysconfigwizard/*.sh
chmod -Rf 775 ssg/etc/conf
chmod -f 755 etc/init.d/*
chmod -f 755 ssg/bin/*.sh
chmod -f 755 ssg/bin/*.pl
chmod -Rf 775 ssg/migration
chmod -f 755 ssg/migration/*.sh

echo Creating Prototype...

echo "i request" > ../pkgbuild/Prototype
echo "i pkginfo" >> ../pkgbuild/Prototype
echo "i checkinstall" >> ../pkgbuild/Prototype
echo "i preinstall" >> ../pkgbuild/Prototype 
echo "i postinstall" >> ../pkgbuild/Prototype
echo "i preremove" >> ../pkgbuild/Prototype
echo "i postremove" >> ../pkgbuild/Prototype
echo "i depend" >> ../pkgbuild/Prototype 
echo "i copyright" >> ../pkgbuild/Prototype 

# establish current owner of the dropped files
CURRENT_OWNER=`ls -ld . | awk '{print $3 " " $4}'`
echo "Making Prototype (pkg manifest file)"

# home dirs
du -a ./export/home/gateway | awk '{print $2}' | pkgproto | sed -e "s/$CURRENT_OWNER/gateway gateway/" >> ../pkgbuild/Prototype
du -a ./export/home/ssgconfig | awk '{print $2}' | pkgproto | sed -e "s/$CURRENT_OWNER/ssgconfig gateway/" >> ../pkgbuild/Prototype
### Main /ssg
du -a ./ssg | awk '{print $2}' | pkgproto | sed -e "s/$CURRENT_OWNER/gateway gateway/" >> ../pkgbuild/Prototype
### Startups Note the permissions
du -a ./etc | awk '{print $2}' | pkgproto | sed -e "s/$CURRENT_OWNER/root sys/" >> ../pkgbuild/Prototype

echo Done

echo Making the package... 
#not radically different from cpio
pkgmk -o -r .  -d ../pkgbuild/ -f ../pkgbuild/Prototype >../pkgmk.log 2>&1
echo Done
echo

cd ../pkgbuild/
echo Bundling into a single file
#No prompting which packet if the package is qualified at the end of the line
pkgtrans -s `pwd` ../L7TECHssg.pkg L7TECHssg >> ../pktrans.log 2>&1
echo Done
echo

