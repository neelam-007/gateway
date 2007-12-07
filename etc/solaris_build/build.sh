#!/bin/sh

#echo Cleaning out previous build stuff.
rm -rf pkgroot
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

grep VERSION= pkginfo
if [ ! $? -eq 0 ] ; then
    echo "\nVERSION=\"$version\"" >> pkginfo
else
    cp pkginfo pkginfo.old
    cat pkginfo.old | sed "s/VERSION=[^ ]*/VERSION=${version}/" > pkginfo
fi

grep PSTAMP= pkginfo
if [ ! $? -eq 0 ] ; then
    echo "\nPSTAMP=\"${STAMP}\"" >> pkginfo
else
    cp pkginfo pkginfo.old
    cat pkginfo.old | sed "s/VERSION=[^ ]*/VERSION=${version}/" > pkginfo
fi

cd ..
echo "cleaning old files"
rm -f ssg.tar
echo "making new ones"
cp ../ssg*.tar.gz ssg.tar.gz
echo Decompressing...
gunzip ssg.tar.gz 

cd pkgroot/
echo "Unpacking standard tarball"
/usr/sfw/bin/gtar -xf ../ssg.tar 
rm ../ssg.tar

#Minor cleanup, and removal of evil spaces....! DIE!
echo "Cleanup"

rm -f ssg/etc/inf/ssg/webadmin/help/securespan\ manager\ help\ system.log
#rmdir ssg/dist
#rm -rf ssg/jdk

echo "Making dir structure"
mkdir -p export/home/gateway
mkdir -p export/home/ssgconfig
mkdir -p ssg/etc/profile.d

echo "moving config and startup"

mv ssg/bin/profile ssg/etc/
mv ssg/bin/java.sh ssg/etc/profile.d/
mv ssg/bin/ssgruntimedefs.sh ssg/etc/profile.d/
mv ssg/bin/ssg-utilities.sh ssg/etc/profile.d/
mv ssg/bin/setopts.sh ssg/etc/profile.d/
mv ssg/bin/jvmoptions ssg/etc/profile.d/

echo "Moving properties to partition Template"

mv ssg/etc/conf/*.properties ssg/etc/conf/partitions/partitiontemplate_/
mv ssg/etc/conf/cluster_hostname-dist ssg/etc/conf/partitions/partitiontemplate_/

# put a java.security in the partition template (so we get one for each partition)
mv ssg/bin/ssg-java.security ssg/etc/conf/partitions/partitiontemplate_/java.security
echo "Cleaning non-solaris scripts"

echo Fixing permissions...

chmod -f 775 ssg/configwizard
chmod -f 664 ssg/configwizard/*
chmod -f 775 ssg/configwizard/*.sh
chmod -f 775 ssg/configwizard/lib
chmod -fR 775 ssg/etc/keys  2&>/dev/null
chmod -Rf 775 ssg/etc/conf
chmod -f 755 ssg/bin/*.sh
chmod -f 755 ssg/bin/*.pl
chmod -Rf 775 ssg/migration
chmod -f 755 ssg/migration/*.sh
chmod -f 755 ssg/etc/profile.d
chmod -f 775 ssg/etc/profile.d/*
chmod -f 775 ssg/etc/*

echo Creating Prototype...

echo "i pkginfo" > ../pkgbuild/Prototype
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
#du -a ./etc | awk '{print $2}' | pkgproto | sed -e "s/$CURRENT_OWNER/root sys/" >> ../pkgbuild/Prototype

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

