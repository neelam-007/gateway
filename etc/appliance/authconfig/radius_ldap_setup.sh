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
PAM_SSHD_CONF_FILE="/etc/pam.d/sshd"
BK_TIME=$(date +"%Y%m%d_%H%M%S")
BK_DIR="/opt/SecureSpan/Appliance/config/authconfig/bk_files"
ORIG_CONF_FILES_DIR="/opt/SecureSpan/Appliance/config/authconfig/orig_conf_files"
LOG_FILE="/opt/SecureSpan/Appliance/config/radius_ldap_setup.log"
CFG_FILES="$LDAP_CONF_FILE1 $LDAP_CONF_FILE2 $LDAP_NSS_FILE $PAM_RADIUS_CONF_FILE $PAM_SSHD_CONF_FILE"
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


getCurrentConfigType () {
# this is used to determine the current configuration type of the system

if [ $(grep "ldap" $LDAP_NSS_FILE | wc -l) -eq 0 ] && [ $(grep "radius" $PAM_SSHD_CONF_FILE | wc -l) -eq 0 ]; then
	# type is local
	RETVAL=2
elif [ $(grep "ldap" $LDAP_NSS_FILE | wc -l) -ge 1 ] && [ $(grep "radius" $PAM_SSHD_CONF_FILE | wc -l) -eq 0 ]; then
	# type is ldap_only
	RETVAL=3
elif [ $(grep "ldap" $LDAP_NSS_FILE | wc -l) -eq 0 ] && [ $(grep "radius" $PAM_SSHD_CONF_FILE | wc -l) -ge 1 ]; then
	# type is radius_only
	RETVAL=4
elif [ $(grep "ldap" $LDAP_NSS_FILE | wc -l) -ge 1 ] && [ $(grep "radius" $PAM_SSHD_CONF_FILE | wc -l) -ge 1 ]; then
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
			echo "LDAP server IP: $(grep "^URI" $LDAP_CONF_FILE1 | sed 's|^.*//||' | tr -d '/' | cut -f 1 -d \:)"
			echo "LDAP server Port: $(grep "^URI" $LDAP_CONF_FILE1 | sed 's|^.*//||' | tr -d '/' | cut -f 2 -d \:)"
			echo "LDAP base: $(grep "^BASE" $LDAP_CONF_FILE1 | sed 's|^BASE ||')"
			;;
				
		radius_only)
			echo "Radius server IP: $(cat $PAM_RADIUS_CONF_FILE | grep "^[0-9]" | awk '{print $1}')"
			echo "Radius server secret: $(cat $PAM_RADIUS_CONF_FILE | grep "^[0-9]" | awk '{print $2}')"
			echo "Radius timeout: $(cat $PAM_RADIUS_CONF_FILE | grep "^[0-9]" | awk '{print $3}')"
			;;
		
		local|file)
			echo "System is configured for local (file) authentication only."
			;;

		radius_with_ldap)
			echo "Radius server IP: $(cat $PAM_RADIUS_CONF_FILE | grep "^[0-9]" | awk '{print $1}')"
			echo "Radius server secret: $(cat $PAM_RADIUS_CONF_FILE | grep "^[0-9]" | awk '{print $2}')"
			echo "Radius timeout: $(cat $PAM_RADIUS_CONF_FILE | grep "^[0-9]" | awk '{print $3}')"
			echo "LDAP server IP: $(grep "^URI" $LDAP_CONF_FILE1 | sed 's|^.*//||' | tr -d '/' | cut -f 1 -d \:)"
			echo "LDAP server Port: $(grep "^URI" $LDAP_CONF_FILE1 | sed 's|^.*//||' | tr -d '/' | cut -f 2 -d \:)"
			echo "LDAP base: $(grep "^BASE" $LDAP_CONF_FILE1 | sed 's|^BASE ||')"
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
# this function expects to be called with one parameter only identifying the file that should be processed.

if [ $# -eq 1 ]; then
	# making sure there is a directory to store backup files in:
	if [ ! -d $BK_DIR ]; then
		toLog "    Info - $BK_DIR does not exists. Creating it..."
		mkdir $BK_DIR
		if [ "X$?" == "X0" ]; then
			toLog "    Info - $BK_DIR not found but successfuly created."
		else
			toLog "    ERROR - $BK_DIR could not be created! Exiting..."
			exit 1
		fi
	fi
	
	checkFileExists $1
	if [ "X$RETVAL" == "X1" ]; then
		toLog "    ERROR - File $1 does not exist! Exiting..."
		exit 1
	else
		local FILE=$1
	fi
	cp --preserve=mode,ownership $FILE $BK_DIR"/"$(basename $FILE)"_bk_"$BK_TIME
	if [ "X$?" == "X0" ]; then
		toLog "    Success - Backup of current $FILE file created."
		RETVAL=0
	else
		toLog "    ERROR - Backup of current $FILE could not be created! Exiting..."
		exit 1
	fi
else
	toLog "    ERROR - Function 'doBackup' should be called with one parameter only! Exiting..."
	exit 1
fi

# END of 'doBackup' function
}


getOriginalFiles () {
# this function will replace any current configuration file relevant to this setup with the original version.
if [ $# -eq 0 ]; then
	# making sure the directory exists and it is readable
	if [ ! -d $ORIG_CONF_FILES_DIR ]; then
		toLog "    Info - $ORIG_CONF_FILES_DIR does not exists. Exiting..."
		exit 1
	fi
	FILES=$(ls -1 $ORIG_CONF_FILES_DIR)
	for F in $FILES; do
		cp --preserve=mode,ownership $ORIG_CONF_FILES_DIR"/"$F "/$(echo $F | sed -e 's/-/\//g')"
		if [ "X$?" == "X0" ]; then
			toLog "Success - Restoration of $F successful."
			RETVAL=0
		else
			toLog "Warning - Restoration of $F could not be completed!"
			RETVAL=1
		fi
	done
else
	toLog "    ERROR - Function 'getOriginalFiles' should be called with one parameter only! Exiting..."
	exit 1
fi

# END of 'getOriginalFiles' function
}

doBackupOriginalConfig () {
# this fucntion is called if the script is ran with --initialconfig parameter
# the purpose is to create the $ORIG_CONF_FILES_DIR in which it will copy the
# configuration files that will be considerd original files; it should only
# be called once, during application install time

# making sure there is a directory to store backup files in:
if [ ! -d $ORIG_CONF_FILES_DIR ]; then
	toLog "    Info - $ORIG_CONF_FILES_DIR does not exists. Creating it..."
	mkdir $ORIG_CONF_FILES_DIR
	if [ "X$?" == "X0" ]; then
		toLog "Info - $ORIG_CONF_FILES_DIR not found but successfuly created."
	else
		toLog "ERROR - $ORIG_CONF_FILES_DIR could not be created! Exiting..."
		exit 1
	fi
fi
if [ $(ls -1A $ORIG_CONF_FILES_DIR | wc -l) -eq 0 ]; then
	for F in $CFG_FILES; do
		checkFileExists $F
		if [ "X$RETVAL" == "X1" ]; then
			toLog "ERROR - File $F does not exist! Exiting..."
			exit 1
		else
			cp --preserve=mode,ownership $F $ORIG_CONF_FILES_DIR"/"$(echo $F | sed -e 's|/|-|g')
			if [ $? -ne 0 ]; then
				toLog "ERROR - File $F does not exist! Exiting..."
				exit 1
			else
				toLog "Info - Original file $F was sucessfully copied to $ORIG_CONF_FILES_DIR directory."
			fi
		fi
	done
fi

# END of 'doInitialConfig' function
}


doConfigureRADIUSonly () {
## ==========================================
##     Configuration for RADIUS Only Auth
## ==========================================
toLog "Info - ===== Starting system configuration for RADIUS only authentication. ====="
checkFileExists /etc/ssh/sshd_config
if [ "X$RETVAL" == "X1" ]; then
	toLog "  ERROR - File /etc/ssh/sshd_config does not exist! Exiting..."
	exit 1
else
	toLog "   Info - FILE etc/ssh/sshd_config VERIFICATION:"
	# Making sure that "UsePAM" directive is set to "yes". The SSG appliance usually has this set to yes.
	SSHD_CONFIG_CHECK=$(grep "^UsePAM " /etc/ssh/sshd_config | awk '{print $2}')
	if [ "X$SSHD_CONFIG_CHECK" == "Xyes" ]; then
		toLog "    Success - SSHD is configured to use PAM."
		RETVAL=0
	else
		toLog "    ERROR - SSHD is not configured to use PAM! Exiting..."
		# as SSH service on any SSG appliance must be configured to use PAM this means that
		# something is wrong with the system so we should exit at this point
		exit 1
	fi
fi

# Configuring pam sshd module to use pam_radius module:
checkFileExists $PAM_SSHD_CONF_FILE
if [ "X$RETVAL" == "X1" ]; then
	toLog "  ERROR - File $PAM_SSHD_CONF_FILE does not exist! Exiting..."
	exit 1
else
	toLog "   Info - FILE $PAM_SSHD_CONF_FILE CONFIGURATION:"
	# making a backup copy of the current file
	doBackup $PAM_SSHD_CONF_FILE
	# adding the line for pam_radius with 'sufficient' ensures that
	# SSHD will allow logins if radius server fails; also, the 'retry=2' and
	# 'localifdown' option makes sure that after 2 retries the auth process
	# will fall back to local authentication
	sed -i "s/\(.*requisite.*$\)/auth\tsufficient\tpam_radius_auth.so conf=\/etc\/pam_radius.conf retry=2 localifdown\n\1/" $PAM_SSHD_CONF_FILE
	if [ $? -ne 0 ]; then
		toLog "    ERROR - Inserting the pam_radius module line in the $PAM_SSHD_CONF_FILE file failed! Exiting..."
		exit 1
	else
		toLog "    Success - Configuration of $PAM_SSHD_CONF_FILE completed."
		RETVAL=0
	fi
fi


# Configuration of linux pam radius module:
checkFileExists $PAM_RADIUS_CONF_FILE
if [ "X$RETVAL" == "X1" ]; then
	toLog "    ERROR - File $PAM_RADIUS_CONF_FILE does not exist! Exiting...";
	exit 1
else
	toLog "   Info - FILE $PAM_RADIUS_CONF_FILE CONFIGURATION:"
	# making a backup copy of the current file
	doBackup $PAM_RADIUS_CONF_FILE
	sed -i "s/\(^127.0.0.1.*$\)/###\1/" $PAM_RADIUS_CONF_FILE
	if [ $? -ne 0 ]; then
		toLog "    ERROR - Commenting the default host (127.0.0.1) line in $PAM_RADIUS_CONF_FILE failed! Exiting..."
		exit 1
	else
		sed -i "s/^other-server.*$/$RADIUS_SRV_IP\t$RADIUS_SECRET\t$RADIUS_TIMEOUT/" $PAM_RADIUS_CONF_FILE
		if [ $? -ne 0 ]; then
			toLog "    ERROR - Inserting the radius server details line in $PAM_RADIUS_CONF_FILE failed. Exiting..."
			exit 1
		else
			toLog "    Success - Configuration of $PAM_RADIUS_CONF_FILE completed."
			RETVAL=0
		fi
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
	# making a backup copy of the current file
	doBackup $LDAP_CONF_FILE1
	# adding the necessary lines to the config file
	echo "URI ldap://$LDAP_SRV_IP:$LDAP_SRV_PORT/" >> $LDAP_CONF_FILE1
	if [ $? -ne 0 ]; then
		toLog "    ERROR - Adding 'URI' field value in $LDAP_CONF_FILE1 failed. Exiting..."
		exit 1
	else
		echo "BASE "$LDAP_BASE"" >> $LDAP_CONF_FILE1
		if [ $? -ne 0 ]; then
			toLog "    ERROR - Adding 'BASE' field value in $LDAP_CONF_FILE1 failed. Exiting..."
			exit 1
		else
			toLog "    Success - Configuration of $LDAP_CONF_FILE1 completed."
		fi
	fi
fi

checkFileExists $LDAP_CONF_FILE2
if [ "X$RETVAL" == "X1" ]; then
	toLog "  ERROR - File $LDAP_CONF_FILE2 does not exist! Exiting..."
	exit 1
else
	toLog "   Info - FILE $LDAP_CONF_FILE2 CONFIGURATION:"
	# making a backup copy of the current file
	doBackup $LDAP_CONF_FILE2
	# disabling the default host directive
	sed -i 's/^host 127.0.0.1/###host 127.0.0.1/' $LDAP_CONF_FILE2
	if [ $? -ne 0 ]; then
		toLog "    ERROR - Disabling default 'host' field in $LDAP_CONF_FILE2 failed. Exiting..."
		exit 1
	else
		sed -i 's/^base dc=example,dc=com/###base dc=example,dc=com/' $LDAP_CONF_FILE2
		if [ $? -ne 0 ]; then
			toLog "    ERROR - Disabling default 'base' field in $LDAP_CONF_FILE2 failed. Exiting..."
			exit 1
		else
			# adding the custom line (the logic in the regex expression below):
			# - match the above modified line
			# - referencing the match string that need to be reused
			# - referencing all other characters up to the end of line
			# - replace the matched strings with themselves AND
			# - add a new line with the desired content (an active "base" directive)
			sed -i "s/\(^###base dc=example,dc=com\)\(.*\)/\1\2\nbase $LDAP_BASE/" $LDAP_CONF_FILE2
			if [ $? -ne 0 ]; then
				toLog "    ERROR - Adding 'base' field in $LDAP_CONF_FILE2 failed. Exiting..."
				exit 1
			else
				# adding the necessary lines to the config
				echo "uri ldap://$LDAP_SRV_IP:$LDAP_SRV_PORT/" >> $LDAP_CONF_FILE2
				echo "ssl no" >> $LDAP_CONF_FILE2
				echo "tls_cacertdir /etc/openldap/cacerts" >> $LDAP_CONF_FILE2
				echo "pam_password md5" >> $LDAP_CONF_FILE2
				toLog "    Success - Configuration of $LDAP_CONF_FILE2 completed."
			fi
		fi
	fi
fi

# Configuring the OS to use ldap for auth - modifying $LDAP_NSS_FILE file
checkFileExists $LDAP_NSS_FILE
if [ "X$RETVAL" == "X1" ]; then
	toLog "  ERROR - File $LDAP_NSS_FILE does not exist! Exiting..."
	exit 1
else
	toLog "   Info - FILE $LDAP_NSS_FILE CONFIGURATION:"
	# making a backup copy of the current file
	doBackup $LDAP_NSS_FILE
	
	sed -i "s/^passwd:\(.*$\)/###passwd:\1/" $LDAP_NSS_FILE
	if [ $? -ne 0 ]; then
		toLog "    ERROR - Disabling default 'passwd:' field in $LDAP_NSS_FILE failed. Exiting..."
		exit 1
	fi
	
	sed -i "s/\(^###passwd:.*$\)/\1\npasswd: files ldap/" $LDAP_NSS_FILE
	if [ $? -ne 0 ]; then
		toLog "    ERROR - Adding 'ldap' to 'passwd:' field in $LDAP_NSS_FILE failed. Exiting..."
		exit 1
	fi
	
	sed -i "s/^shadow:\(.*$\)/###shadow:\1/" $LDAP_NSS_FILE
	if [ $? -ne 0 ]; then
		toLog "    ERROR - Disabling default 'shadow:' field in $LDAP_NSS_FILE failed. Exiting..."
		exit 1
	fi
	
	sed -i "s/\(^###shadow:.*$\)/\1\nshadow: files ldap/" $LDAP_NSS_FILE
	if [ $? -ne 0 ]; then
		toLog "    ERROR - Adding 'ldap' to 'shadow:' field in $LDAP_NSS_FILE failed. Exiting..."
		exit 1
	fi
	
	sed -i "s/^group:\(.*$\)/###group:\1/" $LDAP_NSS_FILE
	if [ $? -ne 0 ]; then
		toLog "    ERROR - Disabling default 'group:' field in $LDAP_NSS_FILE failed. Exiting..."
		exit 1
	fi
	
	sed -i "s/\(^###group:.*$\)/\1\ngroup: files ldap/" $LDAP_NSS_FILE
	if [ $? -ne 0 ]; then
		toLog "    ERROR - Adding 'ldap' to 'group:' field in $LDAP_NSS_FILE failed. Exiting..."
		exit 1
	fi
	
	sed -i "s/^netgroup:\(.*$\)/###netgroup:\1/" $LDAP_NSS_FILE
	if [ $? -ne 0 ]; then
		toLog "    ERROR - Disabling default 'netgroup:' field in $LDAP_NSS_FILE failed. Exiting..."
		exit 1
	fi
	
	sed -i "s/\(^###netgroup:.*$\)/\1\nnetgroup: files ldap/" $LDAP_NSS_FILE
	if [ $? -ne 0 ]; then
		toLog "    ERROR - Adding 'ldap' to 'netgroup:' field in $LDAP_NSS_FILE failed. Exiting..."
		exit 1
	fi
	
	sed -i "s/^automount:\(.*$\)/###automount:\1/" $LDAP_NSS_FILE
	if [ $? -ne 0 ]; then
		toLog "    ERROR - Disabling default 'automount:' field in $LDAP_NSS_FILE failed. Exiting..."
		exit 1
	fi
	
	sed -i "s/\(^###automount:.*$\)/\1\nautomount: files ldap/" $LDAP_NSS_FILE
	if [ $? -ne 0 ]; then
		toLog "    ERROR - Adding 'ldap' to 'automount:' field in $LDAP_NSS_FILE failed. Exiting..."
		exit 1
	fi
	# if this point was reached the above actions were successful
	toLog "    Success - Configuration of $LDAP_NSS_FILE completed."
	RETVAL=0
fi

# END of 'doConfigureLDAPonly' function
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
			toLog "ERROR - Determining the current config type failed! Exiting..."
			exit 1
		fi
	elif [ "X$1" == "X--backuporiginalconfig" ]; then
		# create the repository of original configuration files:
		doBackupOriginalConfig
	else
		toLog "Error - The script was called with an invalid parameter. Exiting..."
		exit 1
	fi
elif [ $# -eq 2 ]; then
	if [ "X$1" == "X--configfile" ]; then
		checkFileExists $2
		if [ "X$RETVAL" == "X1" ]; then
			toLog "Info - File $2 does not exist. This may not be an error!"
			# the script will NOT exit with error because it is called each time the appliance
			# boots but the appliance may have not been configured for radius authentication
		else
			toLog "Info - Verifying the configuration file."
			# Start checking the config file received as second parameter:
			# check ownership
			#if [ "X$(stat -c %U $2)" != "X$OWNER_CFG_FILE" ]; then
			#	toLog "ERROR - $(basename $2) file is not owned by $OWNER_CFG_FILE! Exiting..."
			#	exit 1
			#fi
			# check permissions
			#if [ "X$(stat -c %a $2)" != "X$PERM_CFG_FILE" ]; then
			#	toLog "ERROR - $(basename $2) file does not have $PERM_CFG_FILE permissions! Exiting..."
			#	exit 1
			#fi
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
						getOriginalFiles
						doConfigureLDAPonly
						if [ $RETVAL -eq 0 ]; then
							toLog "Success - System configuration for LDAP only authentication completed."
						else
							toLog "ERROR - Configuration of LDAP only authentication failed! Exiting..."
							exit 1
						fi
						;;

					radius_only)
						getOriginalFiles
						doConfigureRADIUSonly
						if [ "$RETVAL" -eq "0" ]; then
							toLog "Success - System configuration for Radius only authentication completed."
						else
							toLog "ERROR - System configuration for Radius only authentication failed! Exiting..."
							exit 1
						fi
						;;

					radius_with_ldap)
						getOriginalFiles
						doConfigureRADIUSonly
						if [ $RETVAL -eq 0 ]; then
							doConfigureLDAPonly
							if [ $RETVAL -eq 0 ]; then
								toLog "Success - System configuration for Radius with LDAP authentication completed."
							else
								toLog "ERROR - System configuration for Radius with LDAP authentication failed! Exiting..."
								exit 1
							fi
						fi
						;;

					local|file)
						getCurrentConfigType
						if [ $RETVAL -eq 2 ]; then
							CUR_CFG_TYPE="local"
							toLog "Info - ===== Starting system configuration for local authentication. ====="
							toLog "  Info - System is already configured for local authentication. Nothing will be changed."
						else
							# bring back the original files
							toLog "Info - ===== Starting system reconfiguration for local authentication. ====="
							getOriginalFiles
							if [ $RETVAL -eq 0 ]; then
								toLog "Success - System re-configuration for local authentication completed."
							else
								toLog "ERROR - System re-configuration for local authentication failed. Exiting..."
								exit 1
							fi
						fi
						;;

					*)
						toLog "ERROR - Configuration type read from configuration file is not valid! Exiting..."
						exit 1
				esac
				# deleting the config file:
				rm -rf "$2"
				if [ "X$?" == "X0" ]; then
					toLog "Info - Configuration file ($(basename $2)) was successfuly deleted."
				else
					toLog "ERROR - Configuration file ($(basename $2)) was NOT successfuly deleted!"
				fi
			else
				toLog "ERROR - Sourcing of configuration file ($(basename $2)) failed! Exiting..."
				exit 1
			fi
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
