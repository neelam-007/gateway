#!/bin/bash

############################################
##        SSG VirualAppliance 5.4.1       ##
##      Radius and/or LDAP integration    ##
##   Version:  1.1                        ##
##   Date: 2011-04-13                     ##
############################################

# Description:
# This script is used by the ???.jar to apply the configurations that are necessary
# to make the appliance use a Radius and/or LDAP server for SSH and console authentication.
# It should be run with one argument only: the configuration file (full_path/filename).
# This configuration file is to be created by a Java interactive wizard gathering info
# from the user setting up the SSG appliance

# TBD:
# should we add a firewall rule to allow access to radius/ldap server?

# The SSG Virtual Appliance 5.4.1 64bit system has the following packages already installed:
# openldap-2.3.43-12.el5_6.7.x86_64.rpm
# nss_ldap-253-37.el5.rpm

# Define variables that will not be taken from the sourced configuration file:

LDAP_CONF_FILE1="/etc/openldap/ldap.conf"
LDAP_CONF_FILE2="/etc/ldap.conf"
LDAP_NSS_FILE="/etc/nsswitch.conf"
PAM_RADIUS_CONF_FILE="/etc/pam_radius.conf"
BK_TIME=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="/opt/SecureSpan/Appliance/config/radius_ldap_setup.log"

# END of variable definition section

# Functions:
toLog () {
# test if 'date' command is available
local DATE=$(which date)
if [ "X$?" == "X0" ]; then
        LOG_TIME=$(date "+"%a" "%b" "%e" "%H:%M:%S" "%Y"")
		# we make no verification that the above syntax is working properly
		# in case there will be changes in the coreutils package that brings
		# the date binary
else
        echo -e "ERROR - The 'date' command does not appear to be available."
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

checkFile_exists () {
local FILE=$1
if [ ! -e $FILE ]; then
        toLog "ERROR - File $FILE does not exist!";
        RETVAL=1
fi
# end of "checkFile_exists" function
}

doConfigure () {

# Configuring ldap client
# there are 2 configuration files; confusing at first, but:
# - /etc/ldap.conf - not part of OpenLDAP; used by LDAP PAM module
#                    and Nameservice switch libraries for authentication
#                    or name service resolution; installed by nss_ldap package
# - /etc/openldap/ldap.conf - part of/installed by OpenLDAP package; it is
#                             sufficient to querry a LDAP server
#
# We will configure both of them although authconfig-tui would only modify
# the /etc/ldap.conf file (and the /etc/nsswitch.conf)

## ==========================================
##     Configuration for LDAP Only Auth
## ==========================================
if [ "X$1" == "Xldap" ]; then
        checkFile_exists $LDAP_CONF_FILE1
        if [ "X$RETVAL" == "X1" ]; then
                # the file does not exits so we should exit with error
                exit 1;
        else
                # determine if this file is already configured
                CHECK1=$(cat $LDAP_CONF_FILE1 | grep -i "^uri ldap://" | wc -l)
                if [ "X$CHECK1" != "X0" ]; then
                        toLog "Warning - $LDAP_CONF_FILE1 seems to be already configured."
                else
                        # making a backup copy of the original config, just in case
                        cp -a $LDAP_CONF_FILE1 $LDAP_CONF_FILE1"_orig_"$BK_TIME
                        toLog "Info - "$LDAP_CONF_FILE1"_orig_"$BK_TIME" created."
                        # adding the necessary lines to the config file
                        echo "URI ldap://$LDAP_SRV_IP/" >> $LDAP_CONF_FILE1
                        echo "BASE "$LDAP_BASE"" >> $LDAP_CONF_FILE1
                        toLog "Success - Configuration of $LDAP_CONF_FILE1 completed."
                fi
        fi

        checkFile_exists $LDAP_CONF_FILE2
        if [ "X$RETVAL" == "X1" ]; then
                # the file does not exits so we should exit with error
                exit 1;
		else
                # determine if this file is already configured
                CHECK2=$(cat $LDAP_CONF_FILE2 | grep -i "^uri ldap://" | wc -l)
                if [ "X$CHECK2" != "X0" ]; then
                        toLog "Warning - $LDAP_CONF_FILE2 seems to be already configured."
                else
                        # making a backup copy of the original config, just in case
                        cp -a $LDAP_CONF_FILE2 $LDAP_CONF_FILE2"_orig_"$BK_TIME
                        toLog "Info - "$LDAP_CONF_FILE2"_orig_"$BK_TIME" created."
                        # commenting out the default directive
                        sed -i 's/^host 127.0.0.1/###host 127.0.0.1/' $LDAP_CONF_FILE2
                        # replacing the default active directive with a commented one
                        sed -i 's/^base dc=example,dc=com/###base dc=example,dc=com/' $LDAP_CONF_FILE2
                        # adding the custom line: match the above modified line, referencing the match string
                        # and the rest of the characters up to the end of line and replace them with themselves
                        # but adding a new line with the desired content
                        sed -i "s/\(^###base dc=example,dc=com\)\(.*\)/\1\2\nbase $LDAP_BASE/" $LDAP_CONF_FILE2
                        # adding the necessary lines to the config (this would be accomplished manually or
                        # with the authconfig-tui command)
                        echo "uri ldap://$LDAP_SRV_IP/" >> $LDAP_CONF_FILE2
                        echo "ssl no" >> $LDAP_CONF_FILE2
                        echo "tls_cacertdir /etc/openldap/cacerts" >> $LDAP_CONF_FILE2
                        echo "pam_password md5" >> $LDAP_CONF_FILE2
                        toLog "Success - Configuration of $LDAP_CONF_FILE2 completed."
                fi
        fi

        # Configuring the OS to use ldap for auth - modifying $LDAP_NSS_FILE file
        checkFile_exists $LDAP_NSS_FILE
        if [ "X$RETVAL" == "X1" ]; then
                # the file does not exits so we should exit with error
                exit 1;
        else
                CHECK3=$(grep "ldap" $LDAP_NSS_FILE | wc -l)
                if [ "X$CHECK3" != "X0" ]; then
                        toLog "Warning - $LDAP_NSS_FILE seems to be already configured."
                else
                        # making a backup copy of the original config, just in case
                        cp -a $LDAP_NSS_FILE $LDAP_NSS_FILE"_orig_"$BK_TIME
                        toLog "Info - "$LDAP_NSS_FILE"_orig_"$BK_TIME" created."
                        sed -i "s/^passwd:\(.*$\)/###passwd:\1/" $LDAP_NSS_FILE
                        sed -i "s/\(^###passwd:.*$\)/\1\npasswd: files ldap/" $LDAP_NSS_FILE
                        sed -i "s/^shadow:\(.*$\)/###shadow:\1/" $LDAP_NSS_FILE
                        sed -i "s/\(^###shadow:.*$\)/\1\nshadow: files ldap/" $LDAP_NSS_FILE
                        sed -i "s/^group:\(.*$\)/###group:\1/" $LDAP_NSS_FILE
                        sed -i "s/\(^###group:.*$\)/\1\ngroup: files ldap/" $LDAP_NSS_FILE
                        sed -i "s/^netgroup:\(.*$\)/###netgroup:\1/" $LDAP_NSS_FILE
                        sed -i "s/\(^###netgroup:.*$\)/\1\nnetgroup: files ldap/" $LDAP_NSS_FILE
                        sed -i "s/^automount:\(.*$\)/###automount:\1/" $LDAP_NSS_FILE
                        sed -i "s/\(^###automount:.*$\)/\1\nautomount: files ldap/" $LDAP_NSS_FILE
                        toLog "Success - Configuration of $LDAP_NSS_FILE completed."
                fi
        fi
## ==========================================
##     Configuration for RADIUS Only Auth
## ==========================================
elif [ "X$1" == "Xradius" ]; then
        checkFile_exists /etc/ssh/sshd_config
        if [ "X$RETVAL" == "X1" ]; then
                # the file does not exits so we should exit with error
                exit 1;
        else
                # Making sure that "UsePAM" directive is set to "yes". The SSG appliance usually sets this to yes.
                SSHD_CONFIG_CHECK=$(grep "^UsePAM " /etc/ssh/sshd_config | awk '{print $2}')
                if [ "X$SSHD_CONFIG_CHECK" == "Xyes" ]; then
                        toLog "Info - SSHD is configured to use PAM."
                else
                        toLog "ERROR - SSHD is not configured to use PAM! Exiting..."
                        # as SSH service on any SSG appliance must be configured to use PAM this means that
                        # something is wrong with the system so we should exit at this point
                        exit 1
                fi
        fi

        # Configuring pam sshd module to use pam_radius module:
        checkFile_exists /etc/pam.d/sshd
        if [ "X$RETVAL" == "X1" ]; then
                # the file does not exits so we should exit with error
                exit 1;
        else
                CMD1=$(grep -v "^#" /etc/pam.d/sshd | head -1 | grep "radius" | wc -l)
                if [ "X$CMD1" == "X1" ]; then
                        toLog "Warning - /etc/pam.d/sshd seems to be already configured."
                else
                        cp -a /etc/pam.d/sshd /etc/pam.d/sshd_orig_"$BK_TIME"
                        toLog "Info - /etc/pam.d/sshd_orig_$BK_TIME backup file created."
                        sed -i "s/\(.*requisite.*$\)/auth\tsufficient\tpam_radius_auth.so conf=\/etc\/pam_radius.conf retry=2 localifdown\n\1/" /etc/pam.d/sshd
                        toLog "Success - Configuration of /etc/pam.d/sshd completed."
                # adding the line for pam_radius with 'sufficient' ensures that
				# SSHD will allow logins if radius server fails; also, the 'retry=2' and 
				# 'localifdown' option makes sure that after 2 retries the auth process
				# will fall back to local authentication
                fi
        fi

        # Configuration of linux pam radius module:
        checkFile_exists $PAM_RADIUS_CONF_FILE
        if [ "X$RETVAL" == "X1" ]; then
                # the file does not exits so we should exit with error
                exit 1;
        else
                CMD2=$(grep -v "^#\|^$\|secret" $PAM_RADIUS_CONF_FILE | wc -l)
                if [ "X$CMD2" == "X1" ]; then
                        toLog "Warning - $PAM_RADIUS_CONF_FILE seems to be already configured."
                else
                        cp -a $PAM_RADIUS_CONF_FILE $PAM_RADIUS_CONF_FILE"_orig_"$BK_TIME
                        toLog "Info - /etc/$PAM_RADIUS_CONF_FILE_orig_$BK_TIME backup file created."
                        sed -i "s/\(^127.0.0.1.*$\)/###\1/" $PAM_RADIUS_CONF_FILE
                        sed -i "s/^other-server.*$/$RADIUS_SRV_IP\t$SECRET\t$TIMEOUT/" $PAM_RADIUS_CONF_FILE
                        toLog "Success - Configuration of $PAM_RADIUS_CONF_FILE completed."
                fi
        fi
else
        # we did not received neighter "ldap" nor "radius" as parameter
        toLog "ERROR - The received parameter is not valid; it must be 'ldap' or 'radius'!"
        exit 1
fi

# end of "doConfigure" function
}

doRollback () {
echo "Rolling back...."

}

# END of functions section

# script BODY

CFG_FILE=$(basename $1)
if [ "X$CFG_FILE" == "Xradius_ldap_setup.conf" ]; then
        source $1
else
        toLog "ERROR - The argument provided was not correct; \"radius_ldap_setup.conf\" filename expected!"
fi

case "$CFG_TYPE" in
        ldap_only)
                doConfigure ldap
                ;;

        radius_only)
                doConfigure radius
                ;;

        radius_and_ldap)
                doConfigure ldap
                doConfigure radius
                ;;
		file)
				doRollback
				;;
        *)
                toLog "ERROR - Not a valid configuration type!"
                exit 1;
esac

# END of script