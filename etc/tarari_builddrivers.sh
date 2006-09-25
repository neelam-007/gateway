#!/bin/sh
# This script, when placed in the top level of the Tarari drivers tree will build the drivers for 
# ALL currently installed devel kernels (kernel-devel packages). It invokes the existing Makefile(s) but overrides the KERNELSOURCE var used therein.
#
# Place this file in /usr/local/Tarari/src/drivers/
#
# USAGE: ./build.sh [clean]
# the default target is the all target. Use clean to clean up the results of a build.
#
# Author: Michael Egery
# Copyright: Layer 7 Technologies.



KERNELSOURCEROOT="/usr/src/kernels"
ALLKERNELS=`ls ${KERNELSOURCEROOT}`

do_clean() {
	for WHICHKERNEL in ${ALLKERNELS}; do
		KERNELSOURCE=${KERNELSOURCEROOT}/${WHICHKERNEL}
		echo "********************************************************"
		echo "cleaning up drivers for ${WHICHKERNEL}"
		echo "********************************************************"
		(make KERNELSOURCE=${KERNELSOURCEROOT}/${WHICHKERNEL} clean)
	done
}

do_make() {
	for WHICHKERNEL in ${ALLKERNELS}; do
		KERNELSOURCE=${KERNELSOURCEROOT}/${WHICHKERNEL}
		
		echo "********************************************************"
		echo "Building drivers for kernel ${WHICHKERNEL}"
		echo "********************************************************"
		(make KERNELSOURCE=${KERNELSOURCEROOT}/${WHICHKERNEL})
		
		echo "********************************************************"
		echo "installing drivers for ${WHICHKERNEL}"
		echo "********************************************************"
		(make KERNELSOURCE=${KERNELSOURCEROOT}/${WHICHKERNEL} install)
		
		echo "********************************************************"
		echo "cleaning up drivers for ${WHICHKERNEL}"
		echo "********************************************************"
		(make KERNELSOURCE=${KERNELSOURCEROOT}/${WHICHKERNEL} clean)
	done
}

case ${1} in
	clean) 
	   do_clean
	   ;;
	*)
	   do_make
           ;;
esac
