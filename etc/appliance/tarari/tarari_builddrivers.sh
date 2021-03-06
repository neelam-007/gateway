#!/bin/bash
#
# This script faciliates building of Tarari rpms.
#
# To see usage information run:
#   ./tarari_builddrivers.sh
#
# Author: Michael Egery
# Copyright: Layer 7 Technologies.


KERNELSOURCEROOT="/usr/src/kernels"
ALLKERNELS=`ls ${KERNELSOURCEROOT}`
INSTALL_BASE=tarari-installs
RPM_TOP="rpmbuild"
RPM_SPEC="tarari.spec"
RPM_SOURCES_ALL="tarari.tar.gz"
RPM_SOURCES="tarari-kernel-drivers.tar.gz tarari.tar.gz"
# 64 bit!
RPM_SERVER="http://rhel5/rhel5/"


do_usage() {
	cat >&1 <<-EOF
		Usage:
		 	${0} fetch             - Fetch kernel sources
		 	${0} unpack <filename> - Unpack a Tarari distibution file
		 	${0} make              - Compile for each kernel
		 	${0} rpm               - Build the RPM
		 	${0} all               - fetch, make and rpm
		 	${0} clean             - Cleanup
			${0} patchmultiread    - Patch the 5.1.1.125.s drivers to disable multiread. THIS IS REQUIRED.
	EOF
}

#
# Clean up build files
#
do_clean() {
	for WHICHKERNEL in ${ALLKERNELS}; do
		KERNELSOURCE=${KERNELSOURCEROOT}/${WHICHKERNEL}
		echo "********************************************************"
		echo "cleaning up drivers for ${WHICHKERNEL}"
		echo "********************************************************"
		(make KERNELSOURCE=${KERNELSOURCEROOT}/${WHICHKERNEL} clean)
		rm -f ${TARARIROOT}/drivers/*.ko
	done
}

#
# Make the Tarari drivers
#
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
		#ver=`echo ${WHICHKERNEL} | awk -F '-' '{print $1"-"$2$3}'`
		ver=`echo ${WHICHKERNEL} | awk -F '-' '{print $1"-"$2}'`
		mv ${TARARIROOT}/drivers/cpp_base.ko ${TARARIROOT}/drivers/cpp_base-${ver}.ko
		
		echo "********************************************************"
		echo "cleaning up drivers for ${WHICHKERNEL}"
		echo "********************************************************"
		(make KERNELSOURCE=${KERNELSOURCEROOT}/${WHICHKERNEL} clean)
	done
	popd &>/dev/null
        TAR_OUT="${PWD}/tarari-kernel-drivers.tar.gz"
        [ ! -e "${TAR_OUT}" ] || rm "${TAR_OUT}"
	pushd "${TARARIROOT}/../../.." &>/dev/null
	        tar -czf "${TAR_OUT}" usr/local/Tarari
	popd &>/dev/null
}

#
# Fetch kernel rpms to the current directory
#
do_getkernelsources() {
	RPMS=$(wget -O - ${RPM_SERVER} 2>/dev/null | awk -F'"' '{print $6}' | grep kernel-smp-devel)
	# Fetch the rpms
	for RPM_FILE in ${RPMS}; do
		echo "Fetching RPM: ${RPM_FILE}"
		[ -e "${RPM_FILE}" ] && rm -f "${RPM_FILE}"
		wget "${RPM_SERVER}${RPM_FILE}" 2>/dev/null
	done
	echo "RPMS available for install (rpm -i --oldpackage ...)"
}

#
# Unpack a Tarari software distribution
#
do_unpack() {
	[ ! -z "${1}" ] || exitfail "Missing tarari drop file name (e.g. xbi_4.4.2.21-i686.tgz)"

	# Unpack to temp directory
	INSTALL_SOURCE="${PWD}/${1}"
	pushd /tmp &>/dev/null
	tar xzf "${INSTALL_SOURCE}"
	[ -d "${1%.tgz}" ] || exitfail "Unpacking tarari source did not create expected directory: ${1%.tgz}"
	popd &>/dev/null

	# Find .dist files and unpack them to our install dir
	mkdir -p "${INSTALL_BASE}/${1%.tgz}/usr/local/Tarari"
	[ ${?} -eq 0 ] || exitfail "Error creating install directory ${INSTALL_BASE}"

	pushd "${INSTALL_BASE}/${1%.tgz}/usr/local/Tarari" &>/dev/null
	INSTALL_DISTS=$(find "/tmp/${1%.tgz}" -name '*.dist')
	for DIST_FILE in ${INSTALL_DISTS}; do
		echo "Extracting files from: ${DIST_FILE}"
		cat "${DIST_FILE}" | gunzip | tr "\001-\0377\000" "\000-\0377" | tar xf -
	done
	popd &>/dev/null

	rm -rf "/tmp/${1%.tgz}"

	echo "Tarari install to ${INSTALL_BASE}/${1%.tgz}/usr/local/Tarari"
	echo "TARARIROOT=${PWD}/${INSTALL_BASE}/${1%.tgz}/usr/local/Tarari"
}

#
# Package the already built Tarari software into an rpm
#
do_rpm() {
	popd
	# Create rpm build directories and copy in source files
	mkdir -p "${RPM_TOP}/BUILD" "${RPM_TOP}/RPMS" "${RPM_TOP}/SOURCES" "${RPM_TOP}/SPECS" "${RPM_TOP}/SRPMS"
	[ -f "${RPM_SPEC}" ] || exitfail "Missing spec file: ${RPM_SPEC}"
	cp -f "${RPM_SPEC}" "${RPM_TOP}/SPECS"
	for FILENAME in ${RPM_SOURCES} ; do
		[ -f "${FILENAME}" ] || exitfail "Missing source file: ${FILENAME}"
		cp -f "${FILENAME}" "${RPM_TOP}/SOURCES"
	done
	# Create rpm build configuration files, we need an rc to reference our macros file
	# NOTE: the setting of %_use_internal_dependency_generator and %__find_requires is 
	#       a work around that allows us to generate dependencies correctly even though
	#       Tarari does not mark (some of) their shared libraries as executable.
	cat > "${RPM_TOP}/rpm.rc" <<-EOF
		macrofiles:     /usr/lib/rpm/macros:/usr/lib/rpm/%{_target}/macros:/etc/rpm/macros.*:/etc/rpm/macros:/etc/rpm/%{_target}/macros:~/.rpmmacros:${PWD}/${RPM_TOP}/rpm.macros
	EOF
	cat > "${RPM_TOP}/rpm.macros" <<-EOF
		%_topdir ${PWD}/${RPM_TOP}
                %_use_internal_dependency_generator     0
		%__find_requires /usr/lib/rpm/find-requires ldd
	EOF
        # Build the rpm
	rpmbuild -bb --rcfile /usr/lib/rpm/rpmrc:/usr/lib/rpm/redhat/rpmrc:${PWD}/${RPM_TOP}/rpm.rc tarari.spec
	if [ ${?} -eq 0 ] ; then
		echo "RPM build successful:"
		find "${RPM_TOP}/RPMS" -type f
	fi
}

#
# Install kernel rpms and build the Tarari rpm
#
do_all() {
	# Fail fast if files are missing
	[ -f "${RPM_SPEC}" ] || exitfail "Missing spec file: ${RPM_SPEC}"
	for FILENAME in ${RPM_SOURCES_ALL} ; do
	[ -f "${FILENAME}" ] || exitfail "Missing source file: ${FILENAME}"
	done
	RPMS=$(wget -O - ${RPM_SERVER} 2>/dev/null | awk -F'"' '{print $6}' | grep kernel-smp-devel)
	# Install the rpms
	for RPM_FILE in ${RPMS}; do
		echo "Installing RPM: ${RPM_FILE}"
		rpm -i --oldpackage "${RPM_SERVER}${RPM_FILE}"
	done
	do_make
	do_rpm
}

exitfail() {
	echo "${1}"
        exit 1
}


check_flags() {
	WRONGFLAGS=`grep "#LSI_FLAGS += -D__DISABLE_MULTI_READ__" "${TARARIROOT}/src/drivers/cpp_base/linux/Makefile"`
	if [ -n "${WRONGFLAGS}" ] ; then
		echo ""
		echo "************************************************"
		exitfail "you are building the 5.1.1.125s (astoria) drivers without disabling multi-read which is needed. Run \"${0} patchmultiread\" to patch the source (uncomments #LSI_FLAGS += -D__DISABLE_MULTI_READ__ in ${TARARIROOT}/src/drivers/cpp_base/linux/Makefile)"
	fi
}

check_root() {
	if [ ! -z "${TARARIROOT}" ]; then
        	if [ -d "${TARARIROOT}/src/drivers" ] ; then
			echo "The correct directory exists under the TARARIROOT (src/drivers)"	
			pushd "${TARARIROOT}/src/drivers"
	        else
			exitfail "Tarari source directory does not exist (${TARARIROOT}/src/drivers)"	
	        fi
	elif [ -d ${PWD}/${INSTALL_BASE}/*/usr/local/Tarari/src/drivers ] ; then
		echo "the correct directory exists"	
		export TARARIROOT=$(ls -d ${PWD}/${INSTALL_BASE}/*/usr/local/Tarari)
		pushd ${PWD}/${INSTALL_BASE}/*/usr/local/Tarari/src/drivers
	else
		exitfail "the tarari source directory does not exist"	
	fi

	echo "TARARIROOT=${TARARIROOT}"
	[ -d "${TARARIROOT}" ] || exitfail "TARARIROOT does not exist [${TARARIROOT}]"
}

patch_5_1_1_125s() {
	sed -i -e 's/#LSI_FLAGS += -D__DISABLE_MULTI_READ__/LSI_FLAGS += -D__DISABLE_MULTI_READ__/g' $TARARIROOT/src/drivers/cpp_base/linux/Makefile
}

case ${1} in
	clean) 
	   do_clean
	   ;;
	rpm)
       check_root
       check_flags
	   do_rpm
	   ;;
	patchmultiread)
	   check_root
	   patch_5_1_1_125s
	   ;;
	fetch) 
	   do_getkernelsources
	   ;;
	unpack) 
	   do_unpack "${2}"
	   ;;
	make)
       check_root
	   check_flags
	   do_make
           ;;
	all)
       check_root
       check_flags
	   do_all
           ;;
	*)
	   do_usage
	   ;;
esac
