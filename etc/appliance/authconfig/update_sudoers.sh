#!/bin/bash

# This script will be used with one parameter. This parameter will be the name of the group configured in LDAP
# as the group to which the users should belog to in order to have access to the SSG system.


LOG_FILE="/opt/SecureSpan/Appliance/config/update_sudoers.log"
DATE_TIME=$(date +"%Y-%m-%d %H:%M:%S")
GROUP_NAME=$1

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

checkFileExists () {
local FILE=$1
if [ ! -e $FILE ]; then
        RETVAL=1
fi
# END of 'checkFileExists' function
}


sed -i "s|\(^ssgconfig ALL = NOPASSWD: /sbin/service ssem start.*$\)|\1\n\n# Added by $0 on $DATE_TIME:\n\
%$GROUP_NAME ALL = NOPASSWD: /sbin/reboot\n\
%$GROUP_NAME ALL = (layer7) NOPASSWD: /opt/SecureSpan/Appliance/config/systemconfig.sh\n\
%$GROUP_NAME ALL = (layer7) NOPASSWD: /opt/SecureSpan/Appliance/config/scahsmconfig.sh\n\
%$GROUP_NAME ALL = (layer7,root) NOPASSWD: /opt/SecureSpan/Appliance/libexec/masterkey-manage.pl\n\
%$GROUP_NAME ALL = (layer7) NOPASSWD: /opt/SecureSpan/Appliance/libexec/ncipherconfig.pl\n\
%$GROUP_NAME ALL = (layer7) NOPASSWD: /opt/SecureSpan/Appliance/libexec/ssgconfig_launch\n\
%$GROUP_NAME ALL = (layer7) NOPASSWD: /opt/SecureSpan/EnterpriseManager/config/emconfig.sh\n\
%$GROUP_NAME ALL = (layer7) NOPASSWD: /opt/SecureSpan/Appliance/libexec/patchcli_launch\n\
%$GROUP_NAME ALL = NOPASSWD: /sbin/chkconfig ssem on, /sbin/chkconfig ssem off\n\
%$GROUP_NAME ALL = NOPASSWD: /sbin/service ssem start, /sbin/service ssem stop, /sbin/service ssem status\n|" /etc/sudoers

if [ "X$?" == "X0" ]; then
        toLog "Info - The sudoers file was successfuly configured."
else
        toLog "ERROR - The sudoers file was NOT successfuly configured!"
fi

# END of script

