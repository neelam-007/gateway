#!/bin/bash

############################################
##        BASH Command Line Logging       ##
##           Date: 2011-04-25             ##
############################################

# Description:
# This script is used to enable catching the log messages sent by bash to rsyslog.
#
# Expected behaviour:
# - it only modifies the /etc/rsyslog.conf file
# - it will make a backup before modifying the rsyslog.conf file
# - it checks for existance, ownership, permissions, type and name of the rsyslog config file
# - it writes its own log file recording each action it performs

RSYSLOG_CONF_FILE="/etc/rsyslog.conf"
OWNER_RSYSLOG_CONF_FILE="root"
PERM_RSYSLOG_CONF_FILE="644"

BASH_LOG_FILE="/var/log/bash_commands.log"

BK_TIME=$(date +"%Y%m%d_%H%M%S")
BK_DIR="/opt/SecureSpan/Appliance/config/bash_log/"
LOG_FILE="/opt/SecureSpan/Appliance/config/bash_cl_audit_toggle.log"

# END of variable definition section

# Functions:
toLog () {
# test if 'date' command is available
local DATE=$(which date)
if [ "X$?" == "X0" ]; then
        LOG_TIME=$(date "+"%a" "%b" "%e" "%H:%M:%S" "%Y"")
        # there is no verification that the above syntax is working properly
        # in case there will be changes in the coreutils package that brings
        # the 'date' binary
else
        echo -e "ERROR - The 'date' command does not appear to be available."
        exit 1
fi

# test if LOG_FILE exists
if [ -f $LOG_FILE ]; then
        echo -e $LOG_TIME": "$@ >> $LOG_FILE;
else
        # log file does not exist! Creating it...
        echo "$LOG_TIME: Log file created." >> $LOG_FILE;
fi
# end of "toLog" function

}

checkFileExists () {
local FILE=$1
if [ -e $FILE ]; then
        RETVAL=0
else
        RETVAL=1
fi
# END of "checkFileExists" function
}


doBackup () {
# making sure there is a directory to store backup files in:
if [ ! -d $BK_DIR ]; then
        toLog "Info - $BK_DIR does not exists. Creating it..."
        mkdir -p $BK_DIR
        if [ "X$?" == "X0" ]; then
                toLog "Success - $BK_DIR created."
                RETVAL=0
        else
                toLog "ERROR - $BK_DIR could not be created. Exiting..."
                RETVAL=1
        fi
fi

if [ $# -eq 1 ]; then
        # copy the file to the backup directory and append '_bk_' and the timestamp
        cp --preserve=mode,ownership $1 $BK_DIR"/"$(basename $1)"_bk_"$BK_TIME
        if [ $? -eq 0 ]; then
                toLog "Success - Backup for $1 successfuly created."
                RETVAL=0
        else
                toLog "ERROR - Backup for $1 failed."
                RETVAL=1
        fi
else
        toLog "ERROR - Function 'doBackup' expects one parameter only but none or more received."
        RETVAL=1
fi
# END of doBackup function
}


doEnable () {
# backup for the current configuration file that's going to be changed:
doBackup $RSYSLOG_CONF_FILE
if [ $RETVAL -eq 1 ]; then
        toLog "ERROR - Backup of $RSYSLOG_CONF_FILE failed. Exiting..."
else
        # BASH LINE
        # check if it's already configured
        if [ "x$(grep 'isequal, "bash"' $RSYSLOG_CONF_FILE | cut -d" " -f3 | cut -d\" -f2)" = "xbash" ]; then
                toLog "Info - Logging of bash command line messages seems already enabled. Reconfiguring..."
                # removing line:
                sed -i '/\(.*\)isequal, "bash"\(.*$\)/d' $RSYSLOG_CONF_FILE
                if [ $? -eq 0 ] && [ "x$(grep 'isequal, "bash"' $RSYSLOG_CONF_FILE | cut -d" " -f3 | cut -d\" -f2)" = "x" ]; then
                        toLog "  Success - Removing existing bash line to $RSYSLOG_CONF_FILE completed!"
                else
                        toLog "  ERROR - Removing existing bash line from $RSYSLOG_CONF_FILE failed. Exiting..."
                        exit 1
                fi
                # adding new line:
                echo ":programname, isequal, \"bash\" $BASH_LOG_FILE" >> $RSYSLOG_CONF_FILE
                if [ $? -eq 0 ] && [ "x$(grep 'isequal, "bash"' $RSYSLOG_CONF_FILE | cut -d" " -f3 | cut -d\" -f2)" = "xbash" ]; then
                        toLog "  Success - Adding new bash line to $RSYSLOG_CONF_FILE completed!"
                else
                        toLog "  ERROR - Adding new bash line to $RSYSLOG_CONF_FILE failed! Exiting..."
                        exit 1
                fi
        else
                # adding new line:
                echo ":programname, isequal, \"bash\" $BASH_LOG_FILE" >> $RSYSLOG_CONF_FILE
                if [ $? -eq 0 ] && [ "x$(grep 'isequal, "bash"' $RSYSLOG_CONF_FILE | cut -d" " -f3 | cut -d\" -f2)" = "xbash" ]; then
                        toLog "  Success - Adding bash line to $RSYSLOG_CONF_FILE completed!"
                else
                        toLog "  ERROR - Adding bash line to $RSYSLOG_CONF_FILE failed! Exiting..."
                        exit 1
                fi
        fi
        ######################################################

        # SUDO LINE
        # check if it's already configured
        if [ "x$(grep 'isequal, "sudo"' $RSYSLOG_CONF_FILE | cut -d" " -f3 | cut -d\" -f2)" = "xsudo" ]; then
                toLog "Info - Logging of sudo command line messages seems already enabled. Reconfiguring..."
                # removing line:
                sed -i '/\(.*\)isequal, "sudo"\(.*$\)/d' $RSYSLOG_CONF_FILE
                if [ $? -eq 0 ] && [ "x$(grep 'isequal, "sudo"' $RSYSLOG_CONF_FILE | cut -d" " -f3 | cut -d\" -f2)" = "x" ]; then
                        toLog "  Success - Removing existing sudo line to $RSYSLOG_CONF_FILE completed!"
                else
                        toLog "  ERROR - Removing existing sudo line from $RSYSLOG_CONF_FILE failed. Exiting..."
                        exit 1
                fi
                # adding new line:
                echo ":programname, isequal, \"sudo\" $BASH_LOG_FILE" >> $RSYSLOG_CONF_FILE
                if [ $? -eq 0 ] && [ "x$(grep 'isequal, "sudo"' $RSYSLOG_CONF_FILE | cut -d" " -f3 | cut -d\" -f2)" = "xsudo" ]; then
                        toLog "  Success - Adding new sudo line to $RSYSLOG_CONF_FILE completed!"
                else
                        toLog "  ERROR - Adding new sudo line to $RSYSLOG_CONF_FILE failed! Exiting..."
                        exit 1
                fi
        else
                # adding new line:
                echo ":programname, isequal, \"sudo\" $BASH_LOG_FILE" >> $RSYSLOG_CONF_FILE
                if [ $? -eq 0 ] && [ "x$(grep 'isequal, "sudo"' $RSYSLOG_CONF_FILE | cut -d" " -f3 | cut -d\" -f2)" = "xsudo" ]; then
                        toLog "  Success - Adding sudo line to $RSYSLOG_CONF_FILE completed!"
                else
                        toLog "  ERROR - Adding sudo line to $RSYSLOG_CONF_FILE failed! Exiting..."
                        exit 1
                fi
        fi
        ######################################################

        # SU LINE
        # check if it's already configured
        if [ "x$(grep 'isequal, "su"' $RSYSLOG_CONF_FILE | cut -d" " -f3 | cut -d\" -f2)" = "xsu" ]; then
                toLog "Info - Logging of su command line messages seems already enabled. Reconfiguring..."
                # removing line:
                sed -i '/\(.*\)isequal, "su"\(.*$\)/d' $RSYSLOG_CONF_FILE
                if [ $? -eq 0 ] && [ "x$(grep 'isequal, "su"' $RSYSLOG_CONF_FILE | cut -d" " -f3 | cut -d\" -f2)" = "x" ]; then
                        toLog "  Success - Removing existing su line to $RSYSLOG_CONF_FILE completed!"
                else
                        toLog "  ERROR - Removing existing su line from $RSYSLOG_CONF_FILE failed. Exiting..."
                        exit 1
                fi
                # adding new line:
                echo ":programname, isequal, \"su\" $BASH_LOG_FILE" >> $RSYSLOG_CONF_FILE
                if [ $? -eq 0 ] && [ "x$(grep 'isequal, "su"' $RSYSLOG_CONF_FILE | cut -d" " -f3 | cut -d\" -f2)" = "xsu" ]; then
                        toLog "  Success - Adding new su line to $RSYSLOG_CONF_FILE completed!"
                else
                        toLog "  ERROR - Adding new su line to $RSYSLOG_CONF_FILE failed! Exiting..."
                        exit 1
                fi
        else
                # adding new line:
                echo ":programname, isequal, \"su\" $BASH_LOG_FILE" >> $RSYSLOG_CONF_FILE
                if [ $? -eq 0 ] && [ "x$(grep 'isequal, "su"' $RSYSLOG_CONF_FILE | cut -d" " -f3 | cut -d\" -f2)" = "xsu" ]; then
                        toLog "  Success - Adding su line to $RSYSLOG_CONF_FILE completed!"
                else
                        toLog "  ERROR - Adding su line to $RSYSLOG_CONF_FILE failed! Exiting..."
                        exit 1
                fi
        fi
        ######################################################
        # if this point was reached we can assume there was no error (it would have exited by now otherwise)
        RETVAL=0
fi
# END of 'doEnable' function
}

### END of Functions section

# Script BODY

checkFileExists $RSYSLOG_CONF_FILE
if [ $RETVAL -eq 0 ]; then
        # check ownership
        if [ "X$(stat -c %U $RSYSLOG_CONF_FILE)" != "X$OWNER_RSYSLOG_CONF_FILE" ]; then
                        toLog "ERROR - $(basename $RSYSLOG_CONF_FILE) file is not owned by $OWNER_RSYSLOG_CONF_FILE! Exiting..."
                        exit 1
        fi
        # check permissions
        if [ $(stat -c %a $RSYSLOG_CONF_FILE) -gt $PERM_RSYSLOG_CONF_FILE ]; then
                        toLog "ERROR - $(basename $RSYSLOG_CONF_FILE) file does not have $PERM_RSYSLOG_CONF_FILE permissions! Exiting..."
                        exit 1
        fi
        # check file type
        if [ "X$(file -b $RSYSLOG_CONF_FILE)" != "XASCII English text" ]; then
                        toLog "ERROR - $(basename $RSYSLOG_CONF_FILE) file is not a text file! Exiting..."
                        exit 1
        fi
        # check file name
        if [ "X$(basename $RSYSLOG_CONF_FILE)" != "Xrsyslog.conf" ]; then
                        toLog "ERROR - The rsyslog configuration file name ($(basename $RSYSLOG_CONF_FILE)) seems to be wrong! Exiting..."
                        exit 1
        fi
        # put here the check syntax verification for rsyslog 3.x
        $(which rsyslogd) -c3 -f $RSYSLOG_CONF_FILE -N 1 &> /dev/null
        if [ $? -ne 0 ]; then
                toLog "ERROR - The rsyslog configuration file ($RSYSLOG_CONF_FILE) syntax is not valid. Exiting..."
                exit 1
        fi

        # if this point was reached the rsyslog configuration file should be valid.
        doEnable
        if [ $RETVAL -eq 0 ]; then
                toLog "Success - Bash command line logging sucessfully enabled."
                /etc/init.d/rsyslog reload &> /dev/null
                if [ $? -eq 0 ]; then
                        toLog "Success - Rsyslog successfuly reloaded."
                else
                        toLog "ERROR - Reloading rsyslog failed!"
                fi
        else
                toLog "ERROR - Enabling bash command line logging failed!"
        fi
else
        toLog "ERROR - File $RSYSLOG_CONF_FILE does not exists! Exiting..."
        exit 1
fi

# END of bash_cl_audit_toggle.sh script

