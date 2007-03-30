#!/bin/bash
start=`pwd`
here=`dirname $0`
cd $here
cd ..
source_cd=`pwd`
cd $start
ssgversion=`rpm -q ssg`
tarariversion=`rpm -q ssg-tarari`
# Check for sanity
fix_mysql () {
        sh ${source_cd}/bin/mycnfupdate.sh
      	if [ ${?} -eq 0 ] ; then
		echo "Succesful mysql config update"
      	else 
		echo "Panic, mysql config update failure"
		return 1;
        fi
}

fix_packages () {
	rpm -Fvh ${source_cd}/data/ssg-3.7b4-1.i386.rpm
      	if [ ${?} -eq 0 ] ; then
		echo "Succesful package update"
      	else 
		echo "Panic, ssg rpm failure"
		return 1;
        fi
	if [ "${tarariversion}" = "ssg-tarari-3.6-5" ] || [ "${tarariversion}" = "ssg-tarari-3.6-4" ]; then
		# found old 3.6-x kit.  replace with 4.2 kit
		rpm -Fvh ${source_cd}/data/ssg-tarari-4.2.2.21-1.i386.rpm
	elif [ "${tarariversion}" = "ssg-tarari-3.7-4" ] || [ "${tarariversion}" = "ssg-tarari-3.7-3" ] ; then
		# found old 3.7-x kit. replace with 4.4 kit
		rpm -Fvh ${source_cd}/data/ssg-tarari-4.4.2.50-1.i386.rpm
	else
		echo "No Previously Installed Tarari, continuing without it"
	fi
      	if [ ${?} -eq 0 ] ; then
		echo "Succesful package update"
      	else 
		echo "Panic, tarari rpm failure"
		return 1;
        fi
}

if [ "${ssgversion}" = "ssg-3.6.5-4" ] || [ "${ssgversion}" = "ssg-3.6.5-5" ]; then
	# We're sane
	echo "Found sane ssg version. Proceeding with update"
	fix_mysql
      	if [ ${?} -eq 0 ] ; then
		echo -n ""
      	else 
		exit 1;
        fi
	fix_packages
      	if [ ${?} -eq 0 ] ; then
		echo -n ""
      	else 
		exit 1;
        fi
else
	echo "This is not a 3.6.5 system, aborting"
	exit 1;
fi


