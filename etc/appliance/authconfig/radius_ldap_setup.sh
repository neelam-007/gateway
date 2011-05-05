#!/bin/bash

############################################
##        SSG VirualAppliance 5.4.1       ##
##      Radius and/or LDAP integration    ##
##           Date: 2011-04-13             ##
############################################

# Description:
# This script is used by a .jar file to apply the necessary configurations to make the appliance
# use a Radius and/or LDAP server for SSH and console authentication.
#
# It should be run with one argument only: the configuration file (full_path/filename).
# This configuration file is to be created by a Java interactive wizard gathering info
# from the user setting up the SSG appliance

# TBD:
# should we add a firewall rule to allow access to radius/ldap server?

# The SSG Virtual Appliance 5.4.1 64bit system has the following packages already installed:
# openldap-2.3.43-12.el5_6.7.x86_64.rpm
# nss_ldap-253-37.el5.rpm

# Define variables that will not be taken from the radius_ldap.config file:

LDAP_CONF_FILE1="/etc/openldap/ldap.conf"
LDAP_CONF_FILE2="/etc/ldap.conf"
LDAP_NSS_FILE="/etc/nsswitch.conf"
PAM_RADIUS_CONF_FILE="/etc/pam_radius.conf"
BK_TIME=$(date +"%Y%m%d_%H%M%S")
BK_DIR="/opt/SecureSpan/Appliance/config/authconfig/bk_files"
LOG_FILE="/opt/SecureSpan/Appliance/config/radius_ldap_setup.log"
OWNER_CFG_FILE="layer7"
PERM_CFG_FILE="600"

# END of variable definition section

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
        echo -e "$LOG_TIME: $@" >> "$LOG_FILE";
else
        # log file does not exist! Creating it...
        echo "$LOG_TIME: Log file created." >> "$LOG_FILE";
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


getCurrentConfigType () {
# this is used to determine the current configuration type of the system

if [ $(grep "ldap" /etc/nsswitch.conf | wc -l) -eq 0 ] && [ $(grep "radius" /etc/pam.d/sshd | wc -l) -eq 0 ]; then
        # type is local
	RETVAL=2
elif [ $(grep "ldap" /etc/nsswitch.conf | wc -l) -ge 1 ] && [ $(grep "radius" /etc/pam.d/sshd | wc -l) -eq 0 ]; then
        # type is ldap_only
	RETVAL=3
elif [ $(grep "ldap" /etc/nsswitch.conf | wc -l) -eq 0 ] && [ $(grep "radius" /etc/pam.d/sshd | wc -l) -ge 1 ]; then
        # type is radius_only
	RETVAL=4
elif [ $(grep "ldap" /etc/nsswitch.conf | wc -l) -ge 1 ] && [ $(grep "radius" /etc/pam.d/sshd | wc -l) -ge 1 ]; then
        # type is radius_with_ldap
	RETVAL=5
else
        toLog " ERROR - Current configuration type could not be determined!"
        RETVAL=1
fi

# END of 'getCurrentConfigType' function
}


getCurrentConfigValues () {
# This function will be called if the script is run with '--getcurrentconfig' parameter.
# It will output to stdout the default values of the directives from the configuration files
# relevant to the configuration type detected

if [ $# -eq 1 ]; then
        case $1 in
                ldap_only)
                        echo "LDAP server IP: $(grep "^URI" /etc/openldap/ldap.conf | sed 's|^.*//||' | tr -d '/')"
                        echo "LDAP base: $(grep "^BASE" /etc/openldap/ldap.conf | sed 's|^BASE ||')"
        	        ;;
                
		radius_only)
                        echo "Radius server IP: $(cat /etc/pam_radius.conf | grep "^[0-9]" | awk '{print $1}')"
                        echo "Radius server secret: $(cat /etc/pam_radius.conf | grep "^[0-9]" | awk '{print $2}')"
			echo "Radius timeout: $(cat /etc/pam_radius.conf | grep "^[0-9]" | awk '{print $3}')"
	                ;;
		
		local)
			echo "System is configured for local (file) authentication only."
			;;

		radius_with_ldap)
			echo "Radius server IP: $(cat /etc/pam_radius.conf | grep "^[0-9]" | awk '{print $1}')"
                        echo "Radius server secret: $(cat /etc/pam_radius.conf | grep "^[0-9]" | awk '{print $2}')"
			echo "Radius timeout: $(cat /etc/pam_radius.conf | grep "^[0-9]" | awk '{print $3}')"
			echo "LDAP server IP: $(grep "^URI" /etc/openldap/ldap.conf | sed 's|^.*//||' | tr -d '/')"
                        echo "LDAP base: $(grep "^BASE" /etc/openldap/ldap.conf | sed 's|^BASE ||')"
			;;
		*)
			toLog "  ERROR - Function 'getCurrentConfigValues' called with invalid argument. Exiting..."	
			exit 1
			;;
        esac
else
        toLog "  ERROR - One parameter required for the 'getCurrentConfigValues' function but none or more received. Exiting..."
        exit 1
fi

# END of 'getCurrentConfigValues' function
}


doBackup () {
# this function expects to be called with two parameters only identifying the type of backup
# and the file that should be processed.
# type of backup can be:
# 'orig' - copies the specified original file to backup diretory; these files will be used in
#          the roll back operation performed by 'doRestore' function
# 'current' - copies the specified current configuration file to backup directory before
#          changing it.

if [ $# -eq 2 ]; then
	# making sure there is a directory to store backup files in:
	if [ ! -d $BK_DIR ]; then
		toLog "  Info - $BK_DIR does not exists. Creating it..."
		mkdir $BK_DIR
		if [ "X$?" == "X0" ]; then
			toLog "  Info - $BK_DIR not found but successfuly created."
		else
			toLog "  ERROR - $BK_DIR could not be created! Exiting..."
			exit 1
		fi
	fi
	
	checkFileExists $2
        if [ "X$RETVAL" == "X1" ]; then
        	toLog "  ERROR - File $2 does not exist! Exiting..."
                exit 1
        else
                local FILE=$2
        fi

	case $1 in
	orig)
		cp --preserve=mode,ownership "$FILE" "$BK_DIR/$(echo $FILE | sed -e 's/\//-/g')_orig_$BK_TIME"
		if [ "X$?" == "X0" ]; then
                	toLog "  Success - Backup of ORIGINAL $FILE file created."
			RETVAL=0
		else
                	toLog "  ERROR - Backup of ORIGINAL $FILE could not be created! Exiting..."
	                exit 1

	        fi
		;;

	current)	
		cp --preserve=mode,ownership "$FILE" "$BK_DIR/$FILE_bk_$BK_TIME"
                if [ "X$?" == "X0" ]; then
                        toLog "  Success - Backup of current $FILE file created."
			RETVAL=0
                else
                        toLog "  ERROR - Backup of current $FILE could not be created! Exiting..."
                        exit 1
                fi
		;;

	*)
		toLog "  Function 'doBackup' called with invalid parameter! Exiting..."
		exit 1
		;;
	esac
else
	toLog "  ERROR - Function 'doBackup' should be called with two parameters only! Exiting..."
	exit 1
fi

# END of 'doBackup' function
}


doRestore () {
# this function expects to be called with one parameter only identifying the file
# that should be processed.

FILES=$(ls -1 $BK_DIR | grep "orig")
for F in $FILES; do
	cp --preserve=mode,ownership $BK_DIR"/"$F "/$(echo $F | sed -e 's/-/\//g' | sed -e "s/_orig.*$//")"
	if [ "X$?" == "X0" ]; then
		toLog "Success - Restoration of $F successful."
		RETVAL=0
	else
		toLog "Warning - Restoration of $F could not be completed!"
		RETVAL=1
	fi
done

# END of 'doRestore' function
}


doConfigureRADIUSonly () {
## ==========================================
##     Configuration for RADIUS Only Auth
## ==========================================
toLog "Info - ===== Starting system configuration for RADIUS only authentication. ====="
toLog " Info - Applying all necessary radius only authentication specific configurations:"
checkFileExists /etc/ssh/sshd_config
if [ "X$RETVAL" == "X1" ]; then
	toLog "  ERROR - File /etc/ssh/sshd_config does not exist! Exiting..."
        exit 1
else
	toLog "   Info - FILE etc/ssh/sshd_config VERIFICATION:"
        # Making sure that "UsePAM" directive is set to "yes". The SSG appliance usually has this set to yes.
        SSHD_CONFIG_CHECK=$(grep "^UsePAM " /etc/ssh/sshd_config | awk '{print $2}')
        if [ "X$SSHD_CONFIG_CHECK" == "Xyes" ]; then
        	toLog "    Info - SSHD is configured to use PAM."
        else
                toLog "    ERROR - SSHD is not configured to use PAM! Exiting..."
                # as SSH service on any SSG appliance must be configured to use PAM this means that
                # something is wrong with the system so we should exit at this point
        	exit 1
        fi
fi

# Configuring pam sshd module to use pam_radius module:
checkFileExists /etc/pam.d/sshd
if [ "X$RETVAL" == "X1" ]; then
	toLog "  ERROR - File /etc/pam.d/sshd does not exist! Exiting..."
        exit 1
else
	toLog "   Info - FILE /etc/pam.d/sshd CONFIGURATION:"
        CMD1=$(grep -v "^#" /etc/pam.d/sshd | head -1 | grep "radius" | wc -l)
        if [ "X$CMD1" == "X1" ]; then
	        toLog "    Warning - /etc/pam.d/sshd seems to be already configured."
		# Reconfiguration:
		# making a backup copy of the current config file, just in case
		doBackup current /etc/pam.d/sshd
		# removing the line containing 'radius' from /etc/pam.d/sshd file
	        sed -i "/radius/d" /etc/pam.d/sshd
		if [ $? -ne 0 ]; then
			toLog "    ERROR - Removing the line containing 'radius' from /etc/pam.d/sshd file."
			exit 1
		fi
        
		# inserting the pam_radius module line as first line in /etc/pam.d/sshd
		sed -i "s/\(.*requisite.*$\)/auth\tsufficient\tpam_radius_auth.so conf=\/etc\/pam_radius.conf retry=2 localifdown\n\1/" /etc/pam.d/sshd
		if [ $? -ne 0 ]; then
			toLog "    ERROR - Inserting the pam_radius module line in /etc/pam.d/sshd file. Exiting..."
			exit 1
		fi
	        toLog "    Success - Reconfiguration of /etc/pam.d/sshd completed."
        else
		# First time configuration:
		# making a backup copy of the original config, just in case
                doBackup orig /etc/pam.d/sshd
                # adding the line for pam_radius with 'sufficient' ensures that
                # SSHD will allow logins if radius server fails; also, the 'retry=2' and
                # 'localifdown' option makes sure that after 2 retries the auth process
                # will fall back to local authentication
                sed -i "s/\(.*requisite.*$\)/auth\tsufficient\tpam_radius_auth.so conf=\/etc\/pam_radius.conf retry=2 localifdown\n\1/" /etc/pam.d/sshd
		if [ $? -ne 0 ]; then
			toLog "    ERROR - Inserting the pam_radius module line in /etc/pam.d/sshd file! Exiting..."
			exit 1
		fi
                toLog "    Success - Configuration of /etc/pam.d/sshd completed."
		RETVAL=0
	fi
fi

# Configuration of linux pam radius module:
checkFileExists $PAM_RADIUS_CONF_FILE
if [ "X$RETVAL" == "X1" ]; then
	toLog "    ERROR - File $PAM_RADIUS_CONF_FILE does not exist! Exiting...";
        exit 1;
else
	toLog "   Info - FILE $PAM_RADIUS_CONF_FILE CONFIGURATION:"
	CMD2=$(grep -v "^#\|^$\|secret" $PAM_RADIUS_CONF_FILE | wc -l)
        if [ "X$CMD2" == "X1" ]; then
        	toLog "    Warning - $PAM_RADIUS_CONF_FILE seems to be already configured."
		# Reconfiguration:
		# making a backup copy of the current config file, just in case
		doBackup current $PAM_RADIUS_CONF_FILE
		sed -i "s/^other-server.*$/$RADIUS_SRV_IP\t$RADIUS_SECRET\t$RADIUS_TIMEOUT/" $PAM_RADIUS_CONF_FILE
		toLog "    Success - Reconfiguration of $PAM_RADIUS_CONF_FILE completed."
	else
		# Fisrt time configuration:
		# making a backup copy of the original config, just in case
                doBackup orig $PAM_RADIUS_CONF_FILE
                sed -i "s/\(^127.0.0.1.*$\)/###\1/" $PAM_RADIUS_CONF_FILE
		if [ $? -ne 0 ]; then
			toLog "    ERROR - Commenting the default host (127.0.0.1) line line in $PAM_RADIUS_CONF_FILE"
			exit 1;
		fi
                sed -i "s/^other-server.*$/$RADIUS_SRV_IP\t$RADIUS_SECRET\t$RADIUS_TIMEOUT/" $PAM_RADIUS_CONF_FILE
		if [ $? -ne 0 ]; then
			toLog "    ERROR - Inserting the radius server details line in $PAM_RADIUS_CONF_FILE"
			exit 1;
		fi
                toLog "    Success - Configuration of $PAM_RADIUS_CONF_FILE completed."
		RETVAL=0
	fi
fi

# Disabling LDAP specific configurations, if any:
toLog " Info - Removing any LDAP specific configurations if any:"
checkFileExists $LDAP_CONF_FILE1
if [ "X$RETVAL" == "X1" ]; then
        toLog "  ERROR - File $LDAP_CONF_FILE1 does not exist! Exiting..."
        exit 1
else
        toLog "   Info - FILE $LDAP_CONF_FILE1 RECONFIGRATION:"
	sed -i "/^URI ldap:/d" $LDAP_CONF_FILE1
	sed -i "/^BASE /d" $LDAP_CONF_FILE1
	toLog "    Success - Reconfiguration of $LDAP_CONF_FILE1 completed."
	RETVAL=0
fi

checkFileExists $LDAP_CONF_FILE2
if [ "X$RETVAL" == "X1" ]; then
        toLog "    ERROR - File $LDAP_CONF_FILE2 does not exist! Exiting..."
        exit 1
else
        toLog "   Info - FILE $LDAP_CONF_FILE2 RECONFIGRATION:"
	sed -i "/^uri ldap:/d" $LDAP_CONF_FILE2
        sed -i "/^ssl no/d" $LDAP_CONF_FILE2
        sed -i "/^tls_cacertdir /d" $LDAP_CONF_FILE2
        sed -i "/^pam_password /d" $LDAP_CONF_FILE2
	toLog "    Success - Reconfiguration of $LDAP_CONF_FILE2 completed."
	RETVAL=0
fi

checkFileExists $LDAP_NSS_FILE
if [ "X$RETVAL" == "X1" ]; then
	toLog "  ERROR - File $LDAP_NSS_FILE does not exist! Exiting..."
        exit 1
else
	toLog "   Info - FILE $LDAP_NSS_FILE RECONFIGURATION:"
	if [ $(grep -i "^passwd:.*ldap" $LDAP_NSS_FILE | wc -l) -eq 1 ] && \
           [ $(grep -i "^shadow:.*ldap" $LDAP_NSS_FILE | wc -l) -eq 1 ] && \
           [ $(grep -i "^group:.*ldap" $LDAP_NSS_FILE | wc -l) -eq 1 ]; then
		toLog "    Warning - $LDAP_NSS_FILE seems to be configured for ldap authentication. Reconfiguring."
                # making a backup copy of the original config, just in case
                doBackup current $LDAP_NSS_FILE
                sed -i "s/^passwd:.*$/passwd: files/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "ERROR - Removing 'ldap' from 'passwd:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/^shadow:.*$/shadow: files/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "ERROR - Removing 'ldap' from 'shadow:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/^group:.*$/group: files/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "ERROR - Removing 'ldap' from 'group:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/^netgroup:.*$/netgroup: files/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "ERROR - Removing 'ldap' from 'netgroup:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/^automount:.*$/automount: files/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "ERROR - Removing 'ldap' from 'automount:' field in $LDAP_NSS_FILE"; exit 1; fi
                toLog "    Success - Reconfiguration of $LDAP_NSS_FILE completed."
		RETVAL=0
	fi
fi
	
# END of 'doConfigureRADIUSonly' function
}

doConfigureLDAPonly () {
## ==========================================
##     Configuration for LDAP Only Auth
## ==========================================

# there are 2 configuration files; confusing at first, but:
# - /etc/ldap.conf - not part of OpenLDAP; used by LDAP PAM module
#                    and Nameservice switch libraries for authentication
#                    or name service resolution; installed by nss_ldap package
# - /etc/openldap/ldap.conf - part of/installed by OpenLDAP package; it is
#                             sufficient to querry a LDAP server
#
# We will configure both of them although authconfig-tui would only modify
# the /etc/ldap.conf file (and the /etc/nsswitch.conf)
toLog "Info - ===== Starting system configuration for LDAP only authentication. ====="
toLog " Info - Applying all necessary ldap only authentication specific configurations:"

checkFileExists $LDAP_CONF_FILE1
if [ "X$RETVAL" == "X1" ]; then
	toLog "  ERROR - File $LDAP_CONF_FILE1 does not exist! Exiting..."
	exit 1
else
	toLog "   Info - FILE $LDAP_CONF_FILE1 CONFIGURATION:"
        # determine if this file is already configured
        if [ $(cat $LDAP_CONF_FILE1 | grep -i "^URI ldap://" | wc -l) -eq 1 ] && [ $(cat $LDAP_CONF_FILE1 | grep -i "^BASE" | wc -l) -eq 1 ]; then
		toLog "    Warning - $LDAP_CONF_FILE1 seems to be already configured."
                # Reconfiguration:
                doBackup current $LDAP_CONF_FILE1

                sed -i "s/\(^URI ldap:\/\/\).*$/\1$LDAP_SRV_IP:$LDAP_SRV_PORT\//" $LDAP_CONF_FILE1
                if [ $? -ne 0 ]; then
                	toLog "    ERROR - Replacing 'URI' field value in $LDAP_CONF_FILE1"
                        exit 1
                fi
                
		sed -i "s/\(^BASE \).*$/\1$LDAP_BASE/" $LDAP_CONF_FILE1
                if [ $? -ne 0 ]; then
			toLog "    ERROR - Replacing 'BASE' field value in $LDAP_CONF_FILE1"
			exit 1
		fi
		toLog "    Success - Reconfiguration of $LDAP_CONF_FILE1 completed."
	else
		# First time configuration:
		# making a backup copy of the original config, just in case
		doBackup orig $LDAP_CONF_FILE1
		# adding the necessary lines to the config file
		echo "URI ldap://$LDAP_SRV_IP:$LDAP_SRV_PORT/" >> $LDAP_CONF_FILE1
		if [ $? -ne 0 ]; then
			toLog "    ERROR - Adding 'URI' field value in $LDAP_CONF_FILE1"
			exit 1
		fi

		echo "BASE "$LDAP_BASE"" >> $LDAP_CONF_FILE1
		if [ $? -ne 0 ]; then
			toLog "    ERROR - Adding 'BASE' field value in $LDAP_CONF_FILE1"
			exit 1
        	fi
		toLog "    Success - Configuration of $LDAP_CONF_FILE1 completed."
	fi
fi

checkFileExists $LDAP_CONF_FILE2
if [ "X$RETVAL" == "X1" ]; then
	toLog "  ERROR - File $LDAP_CONF_FILE2 does not exist! Exiting..."
	exit 1
else
	toLog "   Info - FILE $LDAP_CONF_FILE2 CONFIGURATION:"
        # determine if this file is already configured
        if [ $(grep -i "^uri ldap://" $LDAP_CONF_FILE2 | wc -l) -eq 1 ] && \
           [ $(grep -i "^base" $LDAP_CONF_FILE2 | wc -l) -eq 1 ] && \
           [ $(grep -i "^host" $LDAP_CONF_FILE2 | wc -l) -eq 0 ]; then
        	toLog "    Warning - $LDAP_CONF_FILE2 seems to be already configured."
                # Reconfiguration:
                doBackup current $LDAP_CONF_FILE2
                sed -i "s/\(^base \).*$/\1$LDAP_BASE/" $LDAP_CONF_FILE2; if [ $? -ne 0 ]; then toLog "    ERROR - Replacing 'base' field value in $LDAP_CONF_FILE2"; exit 1; fi
                sed -i "s/\(^uri ldap:\/\/\).*$/\1$LDAP_SRV_IP:$LDAP_SRV_PORT\//" $LDAP_CONF_FILE2; if [ $? -ne 0 ]; then toLog "    ERROR - Replacing 'uri' field value in $LDAP_CONF_FILE2"; exit 1; fi
                toLog "    Success - Reconfiguration of $LDAP_CONF_FILE2 completed."
        else
                # First time configuration:
                # making a backup copy of the original config, just in case
                doBackup orig $LDAP_CONF_FILE2
                # disabling the default host directive
                sed -i 's/^host 127.0.0.1/###host 127.0.0.1/' $LDAP_CONF_FILE2; if [ $? -ne 0 ]; then toLog "    ERROR - Disabling default 'host' field in $LDAP_CONF_FILE2"; exit 1; fi
                sed -i 's/^base dc=example,dc=com/###base dc=example,dc=com/' $LDAP_CONF_FILE2; if [ $? -ne 0 ]; then toLog "    ERROR - Disabling default 'base' field in $LDAP_CONF_FILE2"; exit 1; fi
                # adding the custom line (the logic in the regex expression below):
                # - match the above modified line
                # - referencing the match string that need to be reused
                # - referencing all other characters up to the end of line
                # - replace the matched strings with themselves AND
                # - add a new line with the desired content (an active "base" directive)
                sed -i "s/\(^###base dc=example,dc=com\)\(.*\)/\1\2\nbase $LDAP_BASE/" $LDAP_CONF_FILE2; if [ $? -ne 0 ]; then toLog "    ERROR - Adding 'base' field in $LDAP_CONF_FILE2"; exit 1; fi
                # adding the necessary lines to the config (this would be accomplished manually or
                # with the authconfig-tui command)
                echo "uri ldap://$LDAP_SRV_IP:$LDAP_SRV_PORT/" >> $LDAP_CONF_FILE2
                echo "ssl no" >> $LDAP_CONF_FILE2
                echo "tls_cacertdir /etc/openldap/cacerts" >> $LDAP_CONF_FILE2
                echo "pam_password md5" >> $LDAP_CONF_FILE2
                toLog "    Success - Configuration of $LDAP_CONF_FILE2 completed."
        fi
fi

# Configuring the OS to use ldap for auth - modifying $LDAP_NSS_FILE file
checkFileExists $LDAP_NSS_FILE
if [ "X$RETVAL" == "X1" ]; then
	toLog "  ERROR - File $LDAP_NSS_FILE does not exist! Exiting..."
        exit 1
else
	toLog "   Info - FILE $LDAP_NSS_FILE CONFIGURATION:"
	if [ $(grep -i "^passwd:.*ldap" $LDAP_NSS_FILE | wc -l) -eq 1 ] && \
           [ $(grep -i "^shadow:.*ldap" $LDAP_NSS_FILE | wc -l) -eq 1 ] && \
           [ $(grep -i "^group:.*ldap" $LDAP_NSS_FILE | wc -l) -eq 1 ]; then
        	# no reconfiguration needed
                toLog "  Warning - $LDAP_NSS_FILE seems to be already configured."
        else
                # First time configuration:
                # making a backup copy of the original config, just in case
                doBackup orig $LDAP_NSS_FILE
                sed -i "s/^passwd:\(.*$\)/###passwd:\1/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Disabling default 'passwd:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/\(^###passwd:.*$\)/\1\npasswd: files ldap/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Adding 'ldap' to 'passwd:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/^shadow:\(.*$\)/###shadow:\1/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Disabling default 'shadow:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/\(^###shadow:.*$\)/\1\nshadow: files ldap/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Adding 'ldap' to 'shadow:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/^group:\(.*$\)/###group:\1/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Disabling default 'group:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/\(^###group:.*$\)/\1\ngroup: files ldap/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Adding 'ldap' to 'group:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/^netgroup:\(.*$\)/###netgroup:\1/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Disabling default 'netgroup:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/\(^###netgroup:.*$\)/\1\nnetgroup: files ldap/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Adding 'ldap' to 'netgroup:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/^automount:\(.*$\)/###automount:\1/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Disabling default 'automount:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/\(^###automount:.*$\)/\1\nautomount: files ldap/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Adding 'ldap' to 'automount:' field in $LDAP_NSS_FILE"; exit 1; fi
                toLog "    Success - Configuration of $LDAP_NSS_FILE completed."
                RETVAL=0
        fi
fi

# Disabling Radius specific configurations, if any:
toLog " Info - Removing any Radius specific configurations if any:"
checkFileExists /etc/pam.d/sshd
if [ "X$RETVAL" == "X1" ]; then
        toLog "  ERROR - File /etc/pam.d/sshd does not exist! Exiting...";
        exit 1;
else
	toLog "   Info - FILE /etc/pam.d/sshd RECONFIGURATION:"
	sed -i "/pam_radius_auth.so/d" /etc/pam.d/sshd
        if [ $? -ne 0 ]; then
        	toLog "    ERROR - Removing the pam_radius module line from /etc/pam.d/sshd failed. Exiting..."
                exit 1;
	else
                toLog "    Success - Reconfiguration of /etc/pam.d/sshd completed."
        	RETVAL=0
	fi
fi

checkFileExists /etc/pam_radius.conf
if [ "X$RETVAL" == "X1" ]; then
        toLog "  ERROR - File /etc/pam_radius.conf does not exist! Exiting...";
        exit 1;
else
	toLog "   Info - FILE /etc/pam_radius.conf RECONFIGRATION:"
	sed -r -i "s/^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}/other-server/" $PAM_RADIUS_CONF_FILE
	if [ $? -ne 0 ]; then
                toLog "    ERROR - Reconfiguring Radius server line in /etc/pam_radius.conf failed. Exiting..."
                exit 1;
        else
                toLog "    Success - Reconfiguration of /etc/pam_radius.conf completed."
                RETVAL=0
        fi
fi

# END of 'doConfigureLDAPonly' function
}

doConfigureRADIUSwithLDAP () {
## ============================================
##     Configuration for RADIUS with LDAP Auth
## ============================================
toLog "Info - ===== Starting system configuration for RADIUS with LDAP authentication. ====="
toLog " Info - Applying all necessary Radius specific configurations:"
checkFileExists /etc/ssh/sshd_config
if [ "X$RETVAL" == "X1" ]; then
        toLog "  ERROR - File /etc/ssh/sshd_config does not exist! Exiting..."
        exit 1
else
        toLog "Info - FILE etc/ssh/sshd_config VERIFICATION:"
        # Making sure that "UsePAM" directive is set to "yes". The SSG appliance usually has this set to yes.
        SSHD_CONFIG_CHECK=$(grep "^UsePAM " /etc/ssh/sshd_config | awk '{print $2}')
        if [ "X$SSHD_CONFIG_CHECK" == "Xyes" ]; then
                toLog "  Info - SSHD is configured to use PAM."
        else
                toLog "  ERROR - SSHD is not configured to use PAM! Exiting..."
                # as SSH service on any SSG appliance must be configured to use PAM this means that
                # something is wrong with the system so we should exit at this point
                exit 1
        fi
fi

# Configuring pam sshd module to use pam_radius module:
checkFileExists /etc/pam.d/sshd
if [ "X$RETVAL" == "X1" ]; then
        toLog "  ERROR - File /etc/pam.d/sshd does not exist! Exiting...";
        exit 1;
else
        toLog "   Info - FILE /etc/pam.d/sshd CONFIGURATION:"
        CMD1=$(grep -v "^#" /etc/pam.d/sshd | head -1 | grep "radius" | wc -l)
        if [ "X$CMD1" == "X1" ]; then
                toLog "    Warning - /etc/pam.d/sshd seems to be already configured."
                # Reconfiguration:
                # making a backup copy of the current config file, just in case
                doBackup current /etc/pam.d/sshd
                # removing the line containing 'radius' from /etc/pam.d/sshd file
                sed -i "/radius/d" /etc/pam.d/sshd
                if [ $? -ne 0 ]; then
                        toLog "    ERROR - Removing the line containing 'radius' from /etc/pam.d/sshd file."
                        exit 1
                fi

                # inserting the pam_radius module line as first line in /etc/pam.d/sshd
                sed -i "s/\(.*requisite.*$\)/auth\tsufficient\tpam_radius_auth.so conf=\/etc\/pam_radius.conf retry=2 localifdown\n\1/" /etc/pam.d/sshd
                if [ $? -ne 0 ]; then
                        toLog "    ERROR - Inserting the pam_radius module line in /etc/pam.d/sshd"
                        exit 1;
                fi
                toLog "    Success - Reconfiguration of /etc/pam.d/sshd completed."
		RETVAL=0
        else
                # First time configuration:
                # making a backup copy of the original config, just in case
                doBackup orig /etc/pam.d/sshd
                # adding the line for pam_radius with 'sufficient' ensures that
                # SSHD will allow logins if radius server fails; also, the 'retry=2' and
                # 'localifdown' option makes sure that after 2 retries the auth process
                # will fall back to local authentication
                sed -i "s/\(.*requisite.*$\)/auth\tsufficient\tpam_radius_auth.so conf=\/etc\/pam_radius.conf retry=2 localifdown\n\1/" /etc/pam.d/sshd
                if [ $? -ne 0 ]; then
                        toLog "    ERROR - Inserting the pam_radius module line in /etc/pam.d/sshd"
                        exit 1;
                fi
                toLog "    Success - Configuration of /etc/pam.d/sshd completed."
                RETVAL=0
        fi
fi

# Configuration of linux pam radius module:
checkFileExists $PAM_RADIUS_CONF_FILE
if [ "X$RETVAL" == "X1" ]; then
        toLog "  ERROR - File $PAM_RADIUS_CONF_FILE does not exist! Exiting..."
        exit 1
else
        toLog "   Info - FILE $PAM_RADIUS_CONF_FILE CONFIGURATION:"
        CMD2=$(grep -v "^#\|^$\|secret" $PAM_RADIUS_CONF_FILE | wc -l)
        if [ "X$CMD2" == "X1" ]; then
                toLog "     Warning - $PAM_RADIUS_CONF_FILE seems to be already configured."
                # Reconfiguration:
                # making a backup copy of the current config file, just in case
                doBackup current $PAM_RADIUS_CONF_FILE
                toLog "  replace the line.....----bla bla"
                toLog "    Success - Reconfiguration of $PAM_RADIUS_CONF_FILE completed."
		RETVAL=0
        else
                # Fisrt time configuration:
                # making a backup copy of the original config, just in case
                doBackup orig $PAM_RADIUS_CONF_FILE
                sed -i "s/\(^127.0.0.1.*$\)/###\1/" $PAM_RADIUS_CONF_FILE
                if [ $? -ne 0 ]; then
                        toLog "    ERROR - Commenting the default host (127.0.0.1) line line in $PAM_RADIUS_CONF_FILE"
                        exit 1;
                fi
                sed -i "s/^other-server.*$/$RADIUS_SRV_IP\t$RADIUS_SECRET\t$RADIUS_TIMEOUT/" $PAM_RADIUS_CONF_FILE
                if [ $? -ne 0 ]; then
                        toLog "    ERROR - Inserting the radius server details line in $PAM_RADIUS_CONF_FILE"
                        exit 1;
		else
                	toLog "    Success - Configuration of $PAM_RADIUS_CONF_FILE completed."
			RETVAL=0
		fi
        fi
fi
# ----------------------------------------------------------------------
toLog " Info - Applying all necessary LDAP specific configurations:"

checkFileExists $LDAP_CONF_FILE1
if [ "X$RETVAL" == "X1" ]; then
        toLog "  ERROR - File $LDAP_CONF_FILE1 does not exist! Exiting..."
        exit 1
else
        toLog "   Info - FILE $LDAP_CONF_FILE1 CONFIGURATION:"
        # determine if this file is already configured
        if [ $(cat $LDAP_CONF_FILE1 | grep -i "^URI ldap://" | wc -l) -eq 1 ] && [ $(cat $LDAP_CONF_FILE1 | grep -i "^BASE" | wc -l) -eq 1 ]; then
                toLog "    Warning - $LDAP_CONF_FILE1 seems to be already configured."
                # Reconfiguration:
                doBackup current $LDAP_CONF_FILE1

                sed -i "s/\(^URI ldap:\/\/\).*$/\1$LDAP_SRV_IP:$LDAP_SRV_PORT\//" $LDAP_CONF_FILE1
                if [ $? -ne 0 ]; then
                        toLog "    ERROR - Replacing 'URI' field value in $LDAP_CONF_FILE1"
                        exit 1
                fi

                sed -i "s/\(^BASE \).*$/\1$LDAP_BASE/" $LDAP_CONF_FILE1
                if [ $? -ne 0 ]; then
                        toLog "    ERROR - Replacing 'BASE' field value in $LDAP_CONF_FILE1"
                        exit 1
                fi
                toLog "    Success - Reconfiguration of $LDAP_CONF_FILE1 completed."
		RETVAL=0
        else
                # First time configuration:
                # making a backup copy of the original config, just in case
                doBackup orig $LDAP_CONF_FILE1
                # adding the necessary lines to the config file
                echo "URI ldap://$LDAP_SRV_IP:$LDAP_SRV_PORT/" >> $LDAP_CONF_FILE1
                if [ $? -ne 0 ]; then
                        toLog "    ERROR - Adding 'URI' field value in $LDAP_CONF_FILE1"
                        exit 1
                fi

                echo "BASE "$LDAP_BASE"" >> $LDAP_CONF_FILE1
                if [ $? -ne 0 ]; then
                        toLog "    ERROR - Adding 'BASE' field value in $LDAP_CONF_FILE1"
                        exit 1
                fi
                toLog "    Success - Configuration of $LDAP_CONF_FILE1 completed."
		RETVAL=0
        fi
fi

checkFileExists $LDAP_CONF_FILE2
if [ "X$RETVAL" == "X1" ]; then
        toLog "  ERROR - File $LDAP_CONF_FILE2 does not exist! Exiting..."
        exit 1
else
        toLog "   Info - FILE $LDAP_CONF_FILE2 CONFIGURATION:"
        # determine if this file is already configured
        if [ $(grep -i "^uri ldap://" $LDAP_CONF_FILE2 | wc -l) -eq 1 ] && \
           [ $(grep -i "^base" $LDAP_CONF_FILE2 | wc -l) -eq 1 ] && \
           [ $(grep -i "^host" $LDAP_CONF_FILE2 | wc -l) -eq 0 ]; then
                toLog "    Warning - $LDAP_CONF_FILE2 seems to be already configured."
                # Reconfiguration:
                doBackup current $LDAP_CONF_FILE2
                sed -i "s/\(^base \).*$/\1$LDAP_BASE/" $LDAP_CONF_FILE2; if [ $? -ne 0 ]; then toLog "    ERROR - Replacing 'base' field value in $LDAP_CONF_FILE2"; exit 1; fi
                sed -i "s/\(^uri ldap:\/\/\).*$/\1$LDAP_SRV_IP:$LDAP_SRV_PORT\//" $LDAP_CONF_FILE2; if [ $? -ne 0 ]; then toLog "    ERROR - Replacing 'uri' field value in $LDAP_CONF_FILE2"; exit 1; fi
                toLog "    Success - Reconfiguration of $LDAP_CONF_FILE2 completed."
		RETVAL=0
        else
                # First time configuration:
                # making a backup copy of the original config, just in case
                doBackup orig $LDAP_CONF_FILE2
                # disabling the default host directive
                sed -i 's/^host 127.0.0.1/###host 127.0.0.1/' $LDAP_CONF_FILE2; if [ $? -ne 0 ]; then toLog "    ERROR - Disabling default 'host' field in $LDAP_CONF_FILE2"; exit 1; fi
                sed -i 's/^base dc=example,dc=com/###base dc=example,dc=com/' $LDAP_CONF_FILE2; if [ $? -ne 0 ]; then toLog "    ERROR - Disabling default 'base' field in $LDAP_CONF_FILE2"; exit 1; fi
                # adding the custom line (the logic in the regex expression below):
                # - match the above modified line
                # - referencing the match string that need to be reused
                # - referencing all other characters up to the end of line
                # - replace the matched strings with themselves AND
                # - add a new line with the desired content (an active "base" directive)
                sed -i "s/\(^###base dc=example,dc=com\)\(.*\)/\1\2\nbase $LDAP_BASE/" $LDAP_CONF_FILE2; if [ $? -ne 0 ]; then toLog "    ERROR - Adding 'base' field in $LDAP_CONF_FILE2"; exit 1; fi
                # adding the necessary lines to the config (this would be accomplished manually or
                # with the authconfig-tui command)
                echo "uri ldap://$LDAP_SRV_IP:$LDAP_SRV_PORT/" >> $LDAP_CONF_FILE2
                echo "ssl no" >> $LDAP_CONF_FILE2
                echo "tls_cacertdir /etc/openldap/cacerts" >> $LDAP_CONF_FILE2
                echo "pam_password md5" >> $LDAP_CONF_FILE2
                toLog "    Success - Configuration of $LDAP_CONF_FILE2 completed."
		RETVAL=0
        fi
fi

# Configuring the OS to use ldap for auth - modifying $LDAP_NSS_FILE file
checkFileExists $LDAP_NSS_FILE
if [ "X$RETVAL" == "X1" ]; then
        toLog "  ERROR - File $LDAP_NSS_FILE does not exist! Exiting..."
        exit 1
else
        toLog "   Info - FILE $LDAP_NSS_FILE CONFIGURATION:"
        if [ $(grep -i "^passwd:.*ldap" $LDAP_NSS_FILE | wc -l) -eq 1 ] && \
           [ $(grep -i "^shadow:.*ldap" $LDAP_NSS_FILE | wc -l) -eq 1 ] && \
           [ $(grep -i "^group:.*ldap" $LDAP_NSS_FILE | wc -l) -eq 1 ]; then
                # no reconfiguration needed
                toLog "    Warning - $LDAP_NSS_FILE seems to be already configured. No changes will be made."
        else
                # First time configuration:
                # making a backup copy of the original config, just in case
                doBackup orig $LDAP_NSS_FILE
                sed -i "s/^passwd:\(.*$\)/###passwd:\1/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Disabling default 'passwd:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/\(^###passwd:.*$\)/\1\npasswd: files ldap/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Adding 'ldap' to 'passwd:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/^shadow:\(.*$\)/###shadow:\1/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Disabling default 'shadow:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/\(^###shadow:.*$\)/\1\nshadow: files ldap/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Adding 'ldap' to 'shadow:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/^group:\(.*$\)/###group:\1/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Disabling default 'group:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/\(^###group:.*$\)/\1\ngroup: files ldap/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Adding 'ldap' to 'group:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/^netgroup:\(.*$\)/###netgroup:\1/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Disabling default 'netgroup:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/\(^###netgroup:.*$\)/\1\nnetgroup: files ldap/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Adding 'ldap' to 'netgroup:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/^automount:\(.*$\)/###automount:\1/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Disabling default 'automount:' field in $LDAP_NSS_FILE"; exit 1; fi
                sed -i "s/\(^###automount:.*$\)/\1\nautomount: files ldap/" $LDAP_NSS_FILE; if [ $? -ne 0 ]; then toLog "    ERROR - Adding 'ldap' to 'automount:' field in $LDAP_NSS_FILE"; exit 1; fi
                toLog "    Success - Configuration of $LDAP_NSS_FILE completed."
                RETVAL=0
        fi
fi

# END of 'doConfigureLDAPandRADIUS' function
}

# END of FUNCTIONS section
#############################################



#############################################
# script BODY section

if [ $# -eq 1 ]; then
        if [ "X$1" == "X--getcurrentconfig" ]; then
                # find out what type of auth is the current one:
                getCurrentConfigType
                if [ $RETVAL -ne 1 ]; then
			case $RETVAL in
				2)
                                	toLog "Info - Checking the current configuration type: found 'local'."
					echo 'CFG_TYPE="local"'
                                	getCurrentConfigValues local
					;;
				3)
                                	toLog "Info - Checking the current configuration type: found 'ldap_only'."
					echo 'CFG_TYPE="ldap_only"'
	                                getCurrentConfigValues ldap_only
					;;
				4)
	                                toLog "Info - Checking the current configuration type: found 'radius_only'."
					echo 'CFG_TYPE="radius_only"'
                	                getCurrentConfigValues radius_only
					;;
				5)
					toLog "Info - Checking the current configuration type: found 'radius_with_ldap'."
                                        echo 'CFG_TYPE="radius_with_ldap"'
                                        getCurrentConfigValues radius_with_ldap
                                        ;;
				*)
					toLog "ERROR - Function 'getCurrentConfigType' returned invalid value! Exiting..."
					exit 1
					;;
			esac
                else
                        toLog "ERROR - Determining the current config type failed!"
                fi
	else
		toLog "Error - The script was called with an invalid parameter. Exiting..."
		exit 1
	fi
elif [ $# -eq 2 ]; then
        if [ "X$1" == "X--configfile" ]; then
		# Start checking the config file received as second parameter:
                # check ownership
                if [ "X$(stat -c %U $2)" != "X$OWNER_CFG_FILE" ]; then
                        toLog "ERROR - $(basename $2) file is not owned by $OWNER_CFG_FILE! Exiting..."
                        exit 1
                fi
                # check permissions
                if [ "X$(stat -c %a $2)" != "X$PERM_CFG_FILE" ]; then
                        toLog "ERROR - $(basename $2) file does not have $PERM_CFG_FILE permissions! Exiting..."
                        exit 1
                fi
                # check file type
                if [ "X$(file -b $2)" != "XASCII text" ]; then
                        toLog "ERROR - $(basename $2) file is not a text file! Exiting..."
                        exit 1
                fi
                # check file name
                if [ "X$(basename $2)" != "Xradius_ldap_setup.conf" ]; then
                        toLog "ERROR - The argument provided was not correct; \"radius_ldap_setup.conf\" filename expected! Exiting..."
                        exit 1
                fi
                # if this point was reached all above conditions were passed so the configuration file can be sourced:
                source $2
                if [ "X$?" == "X0" ]; then
                        toLog "Info - Configuration file ($(basename $2)) successfuly sourced."
                        case "$CFG_TYPE" in
                                ldap_only)
                                        doConfigureLDAPonly
                                        if [ $RETVAL -eq 0 ]; then
                                                toLog "Success - System configuration for LDAP only authentication completed."
                                        else
                                                toLog "ERROR - Configuration of LDAP only authentication failed!"
                                        fi
                                        ;;

                                radius_only)
                                        doConfigureRADIUSonly
                                        if [ "$RETVAL" -eq "0" ]; then
                                                toLog "Success - System configuration for Radius only authentication completed."
                                        else
                                                toLog "ERROR - System configuration for Radius only authentication failed!"
                                        fi
                                        ;;

                                radius_with_ldap)
                                        doConfigureRADIUSwithLDAP
                                        if [ $RETVAL -eq 0 ]; then
                                                toLog "Success - System configuration for Radius with LDAP authentication completed."
                                        else
                                                toLog "ERROR - System configuration for Radius with LDAP authentication failed!"
                                        fi
                                        ;;

                                local)
                                        getCurrentConfigType
					if [ $RETVAL -eq 2 ]; then
						CUR_CFG_TYPE="local"
						toLog "Info - ===== Starting system configuration for local authentication. ====="
						toLog "  Info - System is already configured for local authentication. Nothing will be changed."
					else
						# rollback to local configuration using the original backup files
						toLog "Info - ===== Starting system reconfiguration for local authentication. ====="
						doRestore
						if [ $RETVAL -eq 0 ]; then
							toLog "Success - System re-configuration for local authentication completed."
						else
							toLog "ERROR - System re-configuration for local authentication failed."
						fi
					fi
                                        ;;

                                *)
                                        toLog "ERROR - Configuration type read from configuration file is not valid! Exiting..."
                                        exit 1
                        esac
                        # deleting the config file:
                        #rm -rf "$2"
                        if [ "X$?" == "X0" ]; then
                                toLog "Info - Configuration file ($(basename $2)) was successfuly deleted."
                        else
                                toLog "ERROR - Configuration file ($(basename $2)) was NOT successfuly deleted!"
                        fi
                else
                        toLog "ERROR - Sourcing of configuration file ($(basename $2)) failed! Exiting..."
			exit 1
                fi
        else
                toLog "ERROR - First parameter received is invalid! Exiting..."
                exit 1
        fi
else
        toLog "ERROR - One or two parameters expected but none or more received. Exiting..."
        exit 1
fi

# END of script
