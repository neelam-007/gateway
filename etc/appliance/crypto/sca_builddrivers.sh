#!/bin/bash
#
# This script faciliates building of our SCA Driver rpm.
#
# To see usage information run:
#   ./sca_builddriver.sh
#
# Author: Michael Egery
# Copyright: Layer 7 Technologies.



KERNELSOURCEROOT="/usr/src/kernels"
SCADRVDIR="/opt/sun/sca6000/bin/drv/"
ALLKERNELS=`cat supported_kernels`
RPM_TOP="rpmbuild"
RPM_SPEC="sca6000drv.spec"
RPM_SOURCE="sca6000drv.tgz"

do_usage() {
	cat >&1 <<-EOF
		Usage:
		 	${0} make - Compile for all the supported kernels
		 	${0} clean - Cleanup
	EOF
}

#
# Clean up build files
#
do_clean() {
	patch_makefile
	for WHICHKERNEL in ${ALLKERNELS}; do
		echo "********************************************************"
		echo "cleaning up drivers for ${WHICHKERNEL}"
		echo "********************************************************"
		(KERNEL_VER=${WHICHKERNEL} make clean)
	done
	rm -f ${SCADRVDIR}/*.ko
}

#
# Make the Tarari drivers
#
do_make() {
	patch_makefile
	
	for WHICHKERNEL in ${ALLKERNELS}; do
		echo "********************************************************"
		echo "Building drivers for kernel ${WHICHKERNEL}"
		echo "********************************************************"
		(KERNEL_VER=${WHICHKERNEL} make)
		
		echo "********************************************************"
		echo "installing drivers for ${WHICHKERNEL}"
		echo "********************************************************"
		(KERNEL_VER=${WHICHKERNEL} make install)
		
		echo "********************************************************"
		echo "cleaning up build for ${WHICHKERNEL}"
		echo "********************************************************"
		(KERNEL_VER=${WHICHKERNEL} make clean)
	done
        TAR_OUT=$RPM_SOURCE
	pushd ${PWD}/.. &>/dev/null
	        tar -czvhf "${TAR_OUT}" /etc/init.d/sca* ${SCADRVDIR}/*smp.ko
	popd &>/dev/null

	unpatch_makefile
}

exitfail() {
	echo "${1}"
        exit 1
}

patch_makefile() {
	perl -i -pe 's/^(KERNEL_VER.*)/#$1/' ./driver/Makefile
	perl -i -pe 's/^(KERNEL_VER.*)/#$1/' ./framework/Makefile
}

unpatch_makefile() {
	perl -i -pe 's/^#(KERNEL_VER.*)/$1/' ./driver/Makefile
	perl -i -pe 's/^#(KERNEL_VER.*)/$1/' ./framework/Makefile
}

case ${1} in
	clean) 
	   do_clean
	   ;;
	make)
	   do_make
           ;;
	*)
	   do_usage
	   ;;
esac
