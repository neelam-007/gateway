#!/bin/bash

USER_TO_LOCK=$1
LOG_FILE="/opt/SecureSpan/Appliance/config/radius_ldap_setup.log"
DATE_TIME=$(date +"%Y-%m-%d %H:%M:%S")

# END of variables definition section

# FUNCTIONS:

toLog () {
# this function will be called with one parameter whenever we need to send messages into the log file

# test if 'date' command is available
local DATE=$(which date)
if [ "X$?" == "X0" ]; then
        LOG_TIME=$(date "+"%a" "%b" "%e" "%H:%M:%S" "%Y"")
        # there is no verification that the above syntax is working properly
        # in case there will be changes in the coreutils package that brings
        # the 'date' binary
else
        echo -e "ERROR - The 'date' command does not appear to be available. Exiting..."
        exit 1
fi

# test if $LOG_FILE exists
if [ -f $LOG_FILE ]; then
        echo -e "$LOG_TIME: $@" >> "$LOG_FILE"
else
        # log file does not exist! Creating it...
        echo "$LOG_TIME: Log file created." >> "$LOG_FILE"
fi
# END of 'toLog' function
}


passwd -l $USER_TO_LOCK > /dev/null
if [ "X$?" == "X0" ]; then
        toLog "Info - The $USER_TO_LOCK was lcoked; it will still be available to root."
else
        toLog "ERROR - The $USER_TO_LOCK could not be locked!"
        exit 1
fi

# END of script