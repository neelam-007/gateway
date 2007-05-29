#!/bin/bash

#
# Performs the system level setup of the HSM needed to initialize or restore the keystore
#

SSGBINROOT="/ssg/bin"
EXPECT="/usr/bin/expect"
HSM_SETUP_SCRIPT="${SSGBINROOT}/initialize-hsm.expect"
INITIALIZE_HSM_COMMAND="${EXPECT} ${HSM_SETUP_SCRIPT}"
RESTORE_HSM=""

SCA_CONTROL_FILE="/etc/init.d/sca"
SCA_CONTROL="sudo ${SCA_CONTROL_FILE}"
SCA_DIAG="sudo scadiag"
SCA_DIAG_ZERO_HSM="${SCA_DIAG} -z mca0"
KEYDATA_DIR="/var/opt/sun/sca6000/keydata"

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
    #check to make sure the script is present before doing anything else
    if [ ! -s "$HSM_SETUP_SCRIPT" ] ; then
        echo "Could not find ${HSM_SETUP_SCRIPT}. Cannot initialize the hsm. Exiting."
        exit 1;
    fi

    if [ ! -s "$SCA_CONTROL_FILE" ] ; then
        echo "Could not find ${SCA_CONTROL_FILE}. Cannot initialize the hsm. Exiting."
        exit 1;
    fi

    echo "Starting the SCA."
    (${SCA_CONTROL} start)
    RV=$?
    if [ ${RV} -ne 0 ] ; then
        echo "${SCA_CONTROL} start exited with code ${RV}."
        if [ ${RV} -ne 1 ] ; then
            echo "Failed to start the sca daemon. Cannot initialize the HSM."
            echo "Exiting."
            exit 1;
        fi
    fi

    (${SCA_DIAG_ZERO_HSM})
    RV=$?
    if [ ${RV} -ne 0 ] ; then
        echo "${SCA_DIAG_ZERO_HSM} failed with error code ${RV}. Failed to initialize the HSM. Exiting."
        exit 1;
    fi

    echo "Stopping the SCA."
    (${SCA_CONTROL} stop)
    RV=$?
    if [ ${RV} -ne 0 ] ; then
        echo "${SCA_CONTROL} stop exited with code ${RV}."
        if [ ${RV} -ne 1 ] ; then
            echo "Failed to stop the sca daemon. Cannot initialize the HSM."
            echo "Exiting."
            exit 1;
        fi
    fi

    echo "Emptying the keydata directory"
    rm -rf ${KEYDATA_DIR}/*

    echo "Starting the SCA."
    (${SCA_CONTROL} start)
    RV=$?
    if [ ${RV} -ne 0 ] ; then
        echo "${SCA_CONTROL} start failed with error code ${RV}. Failed to initialize the HSM. Exiting."
        exit 1;
    fi

    (${INITIALIZE_HSM_COMMAND} $password)
    RV=$?
    echo "${INITIALIZE_HSM_COMMAND} exited with code ${RV}"
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
