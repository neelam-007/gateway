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
if [ -f build.version ] ; then
    version="$(<build.version)"
else
    version="5.0"
fi
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

echo "Making dir structure"
mkdir -p export/home/gateway
mkdir -p export/home/layer7

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
du -a ./export/home/layer7  | awk '{print $2}' | pkgproto | sed -e "s/$CURRENT_OWNER/layer7 layer7/"   >> ../pkgbuild/Prototype
# main gateway software
du -a ./opt/SecureSpan/Gateway | grep -v "node/default/var" | awk '{print $2}' | pkgproto | sed -e "s/$CURRENT_OWNER/layer7 layer7/"   >> ../pkgbuild/Prototype
du -a ./opt/SecureSpan/Controller | grep -v "etc" | grep -v "var/logs" | grep -v "var/patches" | grep -v "var/run" | awk '{print $2}' | pkgproto | sed -e "s/$CURRENT_OWNER/root root/"   >> ../pkgbuild/Prototype
du -a ./opt/SecureSpan/Controller/etc | awk '{print $2}' | pkgproto | sed -e "s/$CURRENT_OWNER/layer7 gateway/"   >> ../pkgbuild/Prototype
du -a ./opt/SecureSpan/Controller/var/logs | awk '{print $2}' | pkgproto | sed -e "s/$CURRENT_OWNER/layer7 gateway/"   >> ../pkgbuild/Prototype
du -a ./opt/SecureSpan/Controller/var/run | awk '{print $2}' | pkgproto | sed -e "s/$CURRENT_OWNER/layer7 gateway/"   >> ../pkgbuild/Prototype
du -a ./opt/SecureSpan/Controller/var/patches | awk '{print $2}' | pkgproto | sed -e "s/$CURRENT_OWNER/layer7 gateway/"   >> ../pkgbuild/Prototype
chmod 775 ./opt/SecureSpan/Gateway/node/default/var
du -a ./opt/SecureSpan/Gateway/node/default/var | awk '{print $2}' | pkgproto | sed -e "s/$CURRENT_OWNER/gateway gateway/" >> ../pkgbuild/Prototype

echo Done

echo Making the package... 
#not radically different from cpio
pkgmk -o -r .  -d ../pkgbuild/ -f ../pkgbuild/Prototype >../pkgmk.log 2>&1
echo Done
echo

cd ../pkgbuild/
echo Bundling into a single file
#No prompting which packet if the package is qualified at the end of the line
pkgtrans -s `pwd` ../L7TECHssg-$version.pkg L7TECHssg >> ../pktrans.log 2>&1
echo Done
echo

