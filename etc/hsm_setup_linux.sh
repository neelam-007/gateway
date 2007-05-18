#!/bin/bash

#
# Performs the system level setup of the HSM needed to initialize or restore the keystore
#

SSGBINROOT="/ssg/bin"
EXPECT="/usr/bin/expect"
INITIALIZE_HSM="${EXPECT} ${SSGBINROOT}/initialize-hsm.expect"
RESTORE_HSM=""

what_to_do=${1}
password=${2}

usage() {
    echo
    echo "Usage: hsm_setup.sh [init|restore|usage] hsmpassword"
    echo "  init - initialize the hsm. The HSM will be cleared and a new keystore will be created."
    echo "  restore - copy key data from a backup made with the same master key"
    echo "  usage - print these usage instructions"
    echo "  password - the password for the hsm"
    echo
    exit 1;
}

if [ -z "${what_to_do}" ] ; then
    echo
    echo "Please specify a command."
    usage
fi

do_hsm_init() {
    echo "Starting the SCA."
    /etc/init.d/sca start

    scadiag -z mca0

    echo "Stopping the SCA."
    /etc/init.d/sca stop
    
    echo "Emptying the keydata directory"
    rm -rf /var/opt/sun/sca6000/keydata/*

    echo "Starting the SCA."
    /etc/init.d/sca start

    (${INITIALIZE_HSM})
}

case "${what_to_do}" in
  init)
    do_hsm_init;
    exit $?;
	;;
  restore)
	echo "no restore script yet"
	exit "$?"
	;;
  usage)
    usage;
    ;;
  *)
    echo "unknown argument '${what_to_do}'"
    usage;
    ;;
esac