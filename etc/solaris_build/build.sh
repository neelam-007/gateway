#!/bin/sh

#echo Cleaning out previous build stuff.
rm -rf pkgroot
#rm -rf pkgbuild
mkdir pkgroot
mkdir pkgbuild

cd pkgbuild

#echo Getting files...
#scp myke@10.100.104.28:Desktop/L7/whatever... .

#pkgadd will run the scripts as user nobody
chmod 755 *install
cd ..

rm -f ssg.tar
rm -f solaris_ssg_bin.tar

echo Decompressing...
gunzip ssg.tar.gz 
gunzip solaris_ssg_bin.tar.gz

cd pkgroot/
echo "Unpacking standard tarball"
tar -xf ../ssg.tar 
rm ../ssg.tar
echo "Unpacking Solaris replacement files"
tar -xf ../solaris_ssg_bin.tar
rm ../solaris_ssg_bin.tar

mkdir -p etc/rc2.d
mkdir -p export/home/gateway
mkdir -p export/home/ssgconfig
mkdir -p etc/init.d
mkdir -p etc/ipf
mkdir -p etc/snmp/conf
rmdir ssg/dist

mv ssg/bin/my.cnf etc/my.cnf.ssg
mv ssg/bin/configuser_bashrc export/home/ssgconfig/.bashrc
mv ssg/bin/snmpd.conf etc/snmp/conf/snmpd.conf
mv ../ipf.conf etc/ipf/ipf.conf
mv ../tune_solaris_tcp_stack.sh etc/init.d/ssg_tcp_tune.sh
mv ../gateway_DOTprofile export/home/gateway/.profile

# .. Actual, REAL, FUNCTIONAL startup-script moves will go here... next week.

cd etc/rc2.d
ln ../init.d/ssg_tcp_tune.sh S71SSG_tcptune
cd ../../

echo Fixing permissions...
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
chmod -Rf 775 ssg/tomcat/conf
chmod -Rf 775 ssg/jdk/jre/lib/security/
chmod -Rf 775 ssg/migration

echo Creating Prototype...
echo "i pkginfo" > ../pkgbuild/Prototype 
echo "i checkinstall" >> ../pkgbuild/Prototype 
echo "i preinstall" >> ../pkgbuild/Prototype 
echo "i depend" >> ../pkgbuild/Prototype 
echo "i copyright" >> ../pkgbuild/Prototype 

### Export/home stuff!
CURRENT_OWNER=`ls -ld . | awk '{print $3 " " $4}'`

echo "Making Prototype (pkg manifest file)"

du -a ./export/home/gateway | awk '{print $2}' | pkgproto | sed -e "s/$CURRENT_OWNER/gateway gateway/" >> ../pkgbuild/Prototype
du -a ./export/home/ssgconfig | awk '{print $2}' | pkgproto | sed -e "s/$CURRENT_OWNER/ssgconfig gateway/" >> ../pkgbuild/Prototype

### Main /ssg
du -a ./ssg | awk '{print $2}' | pkgproto | sed -e "s/$CURRENT_OWNER/gateway gateway/" >> ../pkgbuild/Prototype

### Startups
du -a ./etc | awk '{print $2}' | pkgproto | sed -e "s/$CURRENT_OWNER/root sys/" >> ../pkgbuild/Prototype

echo Done
echo

echo Making the package... 
#not radically different from cpio
pkgmk -o -r .  -d ../pkgbuild/ -f ../pkgbuild/Prototype     
echo Done
echo

cd ../pkgbuild/

echo Bundling into a single file
#No prompting which packet if the package is qualified at the end of the line
pkgtrans -s `pwd` ./L7TECHssg.pkg L7TECHssg
echo Done
echo

#echo Compressing...
#bzip2 L7TECHssg.pkg
#echo Done

#cd ..
#mv pkgbuild/L7TECHssg.pkg.bz2 

#optionally delete build dirs here.

#move to my snapshotted vm
#cd ..
#scp pkgbuild/L7TECHssg.pkg myke@10.100.104.231:.
