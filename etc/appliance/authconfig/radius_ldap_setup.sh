#!/bin/bash

# Description:
# This script is used by a .jar file to apply the necessary configurations to make the appliance
# use a Radius and/or LDAP server for SSH and console authentication.
#
# It should be run with one argument only: the configuration file (full_path/filename).
# This configuration file is to be created by a Java interactive wizard used to gather info
# from the user setting up the SSG appliance

# this fucntion is called if the script is ran with --initialconfig parameter
# the purpose is to create the $ORIG_CONF_FILES_DIR in which it will copy the
# configuration files that will be considered original files; it should only
# be called once, during application install time

# Define variables that will not be taken from the radius_ldap.config file:

OPENLDAP_CONF_FILE="/etc/openldap/ldap.conf"
NSS_LDAP_CONF_FILE="/etc/ldap.conf"
NSS_CONF_FILE="/etc/nsswitch.conf"
PAM_RADIUS_CONF_FILE="/etc/pam_radius.conf"
PAM_SSHD_CONF_FILE="/etc/pam.d/sshd"
PAM_LOGIN_FILE="/etc/pam.d/login"
SKEL_DIR="/etc/skel_ssg"
BK_TIME=$(date +"%Y%m%d_%H%M%S")
DATE_TIME=$(date +"%Y-%m-%d %H:%M:%S")
BK_DIR="/opt/SecureSpan/Appliance/config/authconfig/bk_files"
ORIG_CONF_FILES_DIR="/opt/SecureSpan/Appliance/config/authconfig/orig_conf_files"
LOG_FILE="/opt/SecureSpan/Appliance/config/radius_ldap_setup.log"
CFG_FILES="$OPENLDAP_CONF_FILE $NSS_LDAP_CONF_FILE $NSS_CONF_FILE $PAM_RADIUS_CONF_FILE $PAM_SSHD_CONF_FILE /etc/sysconfig/authconfig /etc/pam.d/system-auth-ac"
OWNER_CFG_FILE="layer7"
PERM_CFG_FILE="600"

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

getCurrentConfigType () {
# this is used to determine the current configuration type of the system
# should we use authconfig --test here ?!
# I have to enchance this !!!!

if [ $(grep "ldap" $NSS_CONF_FILE | wc -l) -eq 0 ] && [ $(grep "radius" $PAM_SSHD_CONF_FILE | wc -l) -eq 0 ]; then
	# type is local
	RETVAL=2
elif [ $(grep "ldap" $NSS_CONF_FILE | wc -l) -ge 1 ] && [ $(grep "radius" $PAM_SSHD_CONF_FILE | wc -l) -eq 0 ]; then
	# type is ldap_only
	RETVAL=3
elif [ $(grep "ldap" $NSS_CONF_FILE | wc -l) -eq 0 ] && [ $(grep "radius" $PAM_SSHD_CONF_FILE | wc -l) -ge 1 ]; then
	# type is radius_only
	RETVAL=4
elif [ $(grep "ldap" $NSS_CONF_FILE | wc -l) -ge 1 ] && [ $(grep "radius" $PAM_SSHD_CONF_FILE | wc -l) -ge 1 ]; then
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
# I need to enhance this too!!!!

if [ $# -eq 1 ]; then
	case $1 in
		ldap_only)
			echo "LDAP server IP: $(grep "^URI" $OPENLDAP_CONF_FILE | sed 's|^.*//||' | tr -d '/' | cut -f 1 -d \:)"
			echo "LDAP server Port: $(grep "^URI" $OPENLDAP_CONF_FILE | sed 's|^.*//||' | tr -d '/' | cut -f 2 -d \:)"
			echo "LDAP base: $(grep "^BASE" $OPENLDAP_CONF_FILE | sed 's|^BASE ||')"
			;;
				
		radius_only)
			echo "Radius server IP: $(cat $PAM_RADIUS_CONF_FILE | grep "^[0-9]" | awk '{print $1}')"
			echo "Radius server secret: $(cat $PAM_RADIUS_CONF_FILE | grep "^[0-9]" | awk '{print $2}')"
			echo "Radius timeout: $(cat $PAM_RADIUS_CONF_FILE | grep "^[0-9]" | awk '{print $3}')"
			;;
		
		local)
			echo "System is configured for local (file) authentication only."
			;;

		radius_with_ldap)
			echo "Radius server IP: $(cat $PAM_RADIUS_CONF_FILE | grep "^[0-9]" | awk '{print $1}')"
			echo "Radius server secret: $(cat $PAM_RADIUS_CONF_FILE | grep "^[0-9]" | awk '{print $2}')"
			echo "Radius timeout: $(cat $PAM_RADIUS_CONF_FILE | grep "^[0-9]" | awk '{print $3}')"
			echo "LDAP server IP: $(grep "^URI" $OPENLDAP_CONF_FILE | sed 's|^.*//||' | tr -d '/' | cut -f 1 -d \:)"
			echo "LDAP server Port: $(grep "^URI" $OPENLDAP_CONF_FILE | sed 's|^.*//||' | tr -d '/' | cut -f 2 -d \:)"
			echo "LDAP base: $(grep "^BASE" $OPENLDAP_CONF_FILE | sed 's|^BASE ||')"
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
		toLog "  Info - $BK_DIR does not exists. Creating it..."
		mkdir $BK_DIR
		if [ "X$?" == "X0" ]; then
			toLog "  Info - $BK_DIR not found but successfuly created."
		else
			toLog "  ERROR - $BK_DIR could not be created! Exiting..."
			exit 1
		fi
	fi
	
	checkFileExists $1
	if [ "X$RETVAL" == "X1" ]; then
		toLog "  ERROR - File $1 does not exist! Exiting..."
		exit 1
	else
		local FILE=$1
	fi
	cp --preserve=mode,ownership $FILE $BK_DIR"/"$(basename $FILE)"_bk_"$BK_TIME
	if [ "X$?" == "X0" ]; then
		toLog "  Success - Backup of current $FILE file created."
		RETVAL=0
	else
		toLog "  ERROR - Backup of current $FILE could not be created! Exiting..."
		exit 1
	fi
else
	toLog "  ERROR - Function 'doBackup' should be called with one parameter only! Exiting..."
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
# configuration files that will be considered original files; it should only
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
			cp --preserve=mode,ownership $F $ORIG_CONF_FILES_DIR"/"$(echo $F | sed -e 's|/|+|g')
			if [ $? -ne 0 ]; then
				toLog "ERROR - File $F does not exist! Exiting..."
				exit 1
			else
				toLog "Info - Original file $F was sucessfully copied to $ORIG_CONF_FILES_DIR directory."
			fi
		fi
	done
fi

# END of 'doBackupOriginalConfig' function
}

doConfigPAMmkHomeDir () {
# This function will be used to configure pam to automatically create home directories for
# any user allowed to login via any service that includes the pam auth rules from system-auth-ac:
LINE=$(grep -n "^session" /etc/pam.d/system-auth-ac | head -1 | cut -d":" -f1)
sed -i '$LINEs|\(^session.*$\)|# Added by $0 on $DATE_TIME:\nsession required pam_mkhomedir.so skel=$SKEL_DIR umask=077\n\1|' /etc/pam.d/system-auth-ac
if [ $? -ne 0 ] || [ "$(grep "skel" /etc/pam.d/system-auth-ac | cut -d" " -f3)" != "pam_mkhomedir.so" ]; then
	RETVAL=1
else
	RETVAL=0
fi

# END of 'doConfigPAMmkHomeDir' function
}

doConfigAUTHhash () {
# This function will be used to toggle the hash algorithm for user's passwords between md5 and sha512
# md5 has been used prior RHEL6; sha512 was by default the hash algoritm used by RHEL6
sed -i "s|^PASSWDALGORITHM.*$|PASSWDALGORITHM=$1|" /etc/sysconfig/authconfig
if [ $? -ne 0 ] || [ "$(grep "^PASSWDALGORITHM" /etc/sysconfig/authconfig | cut -d"=" -f2)" != "sha512" ]; then
	RETVAL=1
else
	RETVAL=0
fi

# END of 'doConfigAUTHhash' function
}

doLocalAuthSufficient (){
# local authorization is sufficient for local users
LINE2=$(grep -n "^account" /etc/pam.d/system-auth-ac | head -1 | cut -d":" -f1)
sed -i '$LINEs|\(^account.*$\)|\1\n# Added by $0 on $DATE_TIME:\naccount     sufficient    pam_localuser.so\n|' /etc/pam.d/system-auth-ac
if [ $? -ne 0 ] || [ "$(grep "pam_localuser.so" /etc/pam.d/system-auth-ac | awk '{print $3}')" != "pam_localuser.so" ]; then
	RETVAL=1
else
	RETVAL=0
fi
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
	SSHD_CONFIG_CHECK=$(grep "^UsePAM " /etc/ssh/sshd_config | cut -d" " -f2)
	if [ "X$SSHD_CONFIG_CHECK" == "Xyes" ]; then
		toLog "    Success - SSHD is configured to use PAM."
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
	# adding the line for pam_radius with 'sufficient' ensures that
	# SSHD will allow logins if radius server fails; also, the 'retry=2' and
	# 'localifdown' option makes sure that after 2 retries the auth process
	# will fall back to local authentication
	
	# search for the requisite line (added by the hardenning script... but after) and if it's not there, add it:
	if [ $(grep "requisite" $PAM_SSHD_CONF_FILE | wc -l) -eq 1 ]; then
		sed -i "s/\(.*requisite.*$\)/auth\tsufficient\tpam_radius_auth.so conf=\/etc\/pam_radius.conf retry=2 localifdown\n\1/" $PAM_SSHD_CONF_FILE
		if [ $(grep "pam_radius.conf" $PAM_SSHD_CONF_FILE | cut -d"/" -f3 | cut -d" " -f1) != "pam_radius.conf" ]; then
			toLog "    ERROR - Inserting the pam_radius module line in the $PAM_SSHD_CONF_FILE file failed! Exiting..."
			exit 1
		fi
	else
		sed -i "s|\(^#%PAM.*$\)|\1\n# Added by $0 on $DATE_TIME:\nauth\tsufficient\tpam_radius_auth.so conf=\/etc\/pam_radius.conf retry=2 localifdown\n /
		auth\trequisite\tpam_listfile.so item=user sense=allow file=\/etc\/ssh\/ssh_allowed_users onerr=succeed|" $PAM_SSHD_CONF_FILE
		if [ $(grep "pam_radius.conf" $PAM_SSHD_CONF_FILE | cut -d"/" -f3 | cut -d" " -f1) != "pam_radius.conf" ]; then
			toLog "    ERROR - Inserting the pam_radius module line in the $PAM_SSHD_CONF_FILE file failed! Exiting..."
			exit 1
		fi
	fi
	toLog "    Success - Configuration of $PAM_SSHD_CONF_FILE completed."
fi

# Configuration of linux pam radius module:
checkFileExists $PAM_RADIUS_CONF_FILE
if [ "X$RETVAL" == "X1" ]; then
	toLog "    ERROR - File $PAM_RADIUS_CONF_FILE does not exist! Exiting...";
	exit 1
else
	toLog "   Info - FILE $PAM_RADIUS_CONF_FILE CONFIGURATION:"
	sed -i "s|\(^127.0.0.1.*$\)/# Commented by $0 on $DATE_TIME:\n#\1\n|" $PAM_RADIUS_CONF_FILE
	if [ $? -ne 0 ]; then
		toLog "    ERROR - Commenting the default host (127.0.0.1) line in $PAM_RADIUS_CONF_FILE failed! Exiting..."
		exit 1
	else
		sed -i "s|^other-server.*$|# Added by $0 on $DATE_TIME:\n$RADIUS_SRV_HOST\t$RADIUS_SECRET\t$RADIUS_TIMEOUT|" $PAM_RADIUS_CONF_FILE
		if [ $? -ne 0 ]; then
			toLog "    ERROR - Inserting the radius server details line in $PAM_RADIUS_CONF_FILE failed. Exiting..."
			exit 1
		else
			toLog "    Success - Configuration of $PAM_RADIUS_CONF_FILE completed."
		fi
	fi
fi

# END of 'doConfigureRADIUSonly' function
}

doConfigureLDAPonly () {
## ==========================================
##     Configuration for LDAP Only Auth
## ==========================================
# there are 2 configuration files:
# - /etc/ldap.conf - is not part of OpenLDAP; is used by LDAP PAM module and
#                    Nameservice switch libraries for authentication or name 
#                    service resolution; is installed by nss_ldap package
# - /etc/openldap/ldap.conf - part of/installed by OpenLDAP package; it is
#                             sufficient to querry an LDAP server
# For authentication purposes it is enough to configure only /etc/ldap.conf file
# (and, of course, the /etc/nsswitch.conf). However, command line tools made available
# by openldap package(s) expect /etc/openldap/ldap.conf file to have the necessary
# details about communication with an openLDAP server
toLog "Info - ===== Starting system configuration for LDAP only authentication. ====="
toLog " Info - Applying all necessary ldap only authentication specific configurations:"

# Enable LDAP auth on the system
sed -i "s|^USELDAP.*$|USELDAP=yes|" /etc/sysconfig/authconfig
if [ $? -ne 0 ] || [ "X$(grep "^USELDAP" /etc/sysconfig/authconfig | cut -d"=" -f2)" != "Xyes" ]; then
	toLog "    ERROR - Enabling LDAP in /etc/sysconfig/authconfig failed. Exiting..."
	exit 1
else
	toLog "    Success - LDAP has been enabled."
fi

checkFileExists $OPENLDAP_CONF_FILE
if [ "X$RETVAL" == "X1" ]; then
	toLog "  ERROR - File $OPENLDAP_CONF_FILE does not exist! Exiting..."
	exit 1
else
	toLog "   Info - FILE $OPENLDAP_CONF_FILE CONFIGURATION:"
	# BASE field
	if [ "X$LDAP_BASE" != "X" ];then
		sed -i "s|\(^#BASE.*$\)|\1\n# Added by $0 on $DATE_TIME:\nBASE $LDAP_BASE\n|" $OPENLDAP_CONF_FILE
		if [ $? -ne 0 ] || [ "$(grep "^BASE" $OPENLDAP_CONF_FILE | cut -d" " -f2)" != "$LDAP_BASE" ]; then
			toLog "    ERROR - Configuring 'BASE' field in $OPENLDAP_CONF_FILE failed. Exiting..."
			exit 1
		else
			toLog "    Success - 'BASE' set to $LDAP_BASE in $OPENLDAP_CONF_FILE."
		fi
	else
		toLog "    ERROR - The value of LDAP_BASE ($LDAP_BASE) variable in the configuration file is not valid! Exiting..."
		exit 1
	if
	# URI field
	sed -i "s|\(^#URI.*$\)|\1\n# Added by $0 on $DATE_TIME:\n$LDAP_TYPE://$LDAP_SRV:$LDAP_PORT\n|" $OPENLDAP_CONF_FILE
	if [ $? -ne 0 ] || [ "$(grep "^URI" $OPENLDAP_CONF_FILE | cut -d" " -f2)" != "$LDAP_TYPE://$LDAP_SRV:LDAP_PORT" ]; then
		toLog "    ERROR - Configuring 'URI' field in $OPENLDAP_CONF_FILE failed. Exiting..."
		exit 1
	else
		toLog "    Success - 'URI' set to $LDAP_TYPE://$LDAP_SRV:$LDAP_PORT in $OPENLDAP_CONF_FILE."
	fi
	
	if [ "X$LDAP_TYPE" == "Xldaps" ] && [ "X$ADVANCED_TLS_CONF" == "Xyes" ]; then
		# tls_reqcert
		if [ "X$LDAP_TLS_REQCERT" == "Xnever" ] || [ "X$LDAP_TLS_REQCERT" == "Xallow" ] || [ "X$LDAP_TLS_REQCERT" == "Xtry" ] || \
			[ "X$LDAP_TLS_REQCERT" == "Xdemand" ] || [ "X$LDAP_TLS_REQCERT" == "Xhard" ]; then
			echo -e "TLS_REQCERT $LDAP_TLS_REQCERT\n" >> $OPENLDAP_CONF_FILE
			if [ $? -ne 0 ] || [ "$(grep "^TLS_REQCERT" $OPENLDAP_CONF_FILE | cut -d" " -f2)" != "$LDAP_TLS_REQCERT" ]; then
				toLog "    ERROR - Configuring 'TLS_REQCERT' field in $OPENLDAP_CONF_FILE failed. Exiting..."
				exit 1
			else
				toLog "    Success - 'TLS_REQCERT' set to $LDAP_TLS_REQCERT in $OPENLDAP_CONF_FILE."
			fi
		else
			toLog "    ERROR - The value of TLS_REQCERT ($LDAP_TLS_REQCERT) directive for $OPENLDAP_CONF_FILE is not valid! Exiting..."
			exit 1
		fi
		
		# tls crlcheck
		if [ "X$LDAP_TLS_CRLCHECK" == "Xnone" ] || [ "X$LDAP_TLS_CRLCHECK" == "Xpeer" ] || [ "X$LDAP_TLS_CRLCHECK" == "Xall" ]; then
			echo -e "TLS_CRLCHECK $LDAP_TLS_CRLCHECK\n" >> $OPENLDAP_CONF_FILE
			if [ $? -ne 0 ] || [ "$(grep "^TLS_CRLCHECK" $OPENLDAP_CONF_FILE | cut -d" " -f2)" != "$LDAP_TLS_CRLCHECK" ]; then
				toLog "    ERROR - Configuring 'TLS_CRLCHECK' field in $OPENLDAP_CONF_FILE failed. Exiting..."
				exit 1
			else
				toLog "    Success - 'TLS_CRLCHECK' set to $LDAP_TLS_CRLCHECK in $OPENLDAP_CONF_FILE."
			fi
		else
			toLog "    ERROR - The value of TLS_CRLCHECK ($LDAP_TLS_CRLCHECK) directive for $OPENLDAP_CONF_FILE is not valid! Exiting..."
			exit 1
		fi
	fi
	# If this point was reached, then:
	toLog "   Success - Configuration of $OPENLDAP_CONF_FILE completed."
fi


# CONFIGURING NSS LDAP FILE:
checkFileExists $NSS_LDAP_CONF_FILE
if [ "X$RETVAL" == "X1" ]; then
	toLog "  ERROR - File $NSS_LDAP_CONF_FILE does not exist! Exiting..."
	exit 1
else
	toLog "   Info - FILE $NSS_LDAP_CONF_FILE CONFIGURATION:"
	# host field
	sed -i "s|\(^host 127.0.0.1\)|# Commented by $0 on $DATE_TIME:\n#\1\n|" $NSS_LDAP_CONF_FILE
	if [ $? -ne 0 ] || [ "$(grep -i "^host" $NSS_LDAP_CONF_FILE)" != "" ]; then
		toLog "    ERROR - Disabling default 'host' field in $NSS_LDAP_CONF_FILE failed. Exiting..."
		exit 1
	else
		toLog "    Success - 'host' field disabled in $NSS_LDAP_CONF_FILE."
	fi

	# base field
	if [ "X$LDAP_BASE" != "X" ];then
		sed -i "s|\(^base dc=example,dc=com.*$\)|# Commented by 0$ on $DATE_TIME:\n#\1\n \
		# Added by $0 on $DATE_TIME:\nbase $LDAP_BASE\n|" $NSS_LDAP_CONF_FILE
		if [ $? -ne 0 ] || [ "$(grep "^base" $NSS_LDAP_CONF_FILE | cut -d" " -f 2)" != "$LDAP_BASE" ]; then
			toLog "    ERROR - Configuring the 'base' field in $NSS_LDAP_CONF_FILE failed. Exiting..."
			exit 1
		else
			toLog "    Success - 'base' field set to $LDAP_BASE in $NSS_LDAP_CONF_FILE."
		fi
	else
		toLog "    ERROR - The value of LDAP_BASE ($LDAP_BASE) variable in the configuration file is not valid! Exiting..."
		exit 1
	fi
	
	# uri field
	if [ "X$LDAP_TYPE" != "X" ] && [ "X$LDAP_SRV" != "X" ] && [ "X$LDAP_PORT" != "X" ]; then
		sed -i "s|\(^#uri ldapi.*$\)|\1\n# Added by $0 on $DATE_TIME:\nuri $LDAP_TYPE://$LDAP_SRV:$LDAP_PORT\n|" $NSS_LDAP_CONF_FILE
		if [ $? -ne 0 ] || [ "$(grep "^uri" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "$LDAP_TYPE://$LDAP_SRV:$LDAP_PORT" ]; then
			toLog "    ERROR - Configuring 'uri' field in $NSS_LDAP_CONF_FILE failed. Exiting..."
			exit 1
		else
			toLog "    Success - 'uri' field set to $LDAP_TYPE://$LDAP_SRV:$LDAP_PORT in $NSS_LDAP_CONF_FILE."
		fi
	else
		toLog "    ERROR - At least the value of one of LDAP_TYPE, LDAP_SRV or LDAP_PORT variables in the configuration file is not valid! Exiting..."
		exit 1
	fi
	
	# Decide if LDAP or LDAPS (defined by $LDAP_TYPE):
	if [ "X$LDAP_TYPE" == "Xldaps" ]; then
		toLog "    INFO - '$LDAP_TYPE' will be configured."
		# ssl field is set to start_tls
		sed -i "s|\(^#ssl on.*$\)|\1\n# Added by $0 on $DATE_TIME:\nssl start_tls\n|" $NSS_LDAP_CONF_FILE
		if [ $? -ne 0 ] || [ "$(grep "^ssl" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "start_tls" ]; then
			toLog "    ERROR - Configuring 'ssl' field in $NSS_LDAP_CONF_FILE failed. Exiting..."
			exit 1
		else
			toLog "    Success - 'ssl' field set to start_tls in $NSS_LDAP_CONF_FILE."
		fi
		
		# cacert url or file
		# Instead of using the TLS_CACERT directive that will accept one file that contains all CA certificates to be recognized (concatenated),
		# the TLS_CACERTDIR directive will be used to specify the directory that will contain multiple CA certificates in different/separeted files:
		# For both situations (using URL or a previously copied file as the CA certificate) the file will end up in the /etc/openldap/cacerts dir.
		if [ "X$LDAP_CACERT_URL" != "X" ]; then
			CACERT_FILE_NAME=$(echo "$LDAP_CACERT_URL" | sed 's/.*\///')
			pushd /etc/openldap/cacerts/
			wget --quiet --no-check-certificate --no-clobber --dns-timeout=2 --timeout=2 --waitretry=2 --tries=2 $LDAP_CACERT_URL
			if [ $? -ne 0 ] || [ "basename $(ls -1 /etc/openldap/cacerts/ | grep "$CACERT_FILE_NAME")" != "$CACERT_FILE_NAME" ] || [ ! -s "$CACERT_FILE_NAME" ]; then
				toLog "    ERROR - Retriving the CA certificate from URL failed or certificate file is empty. Exiting..."
				exit 1
			fi
			popd
		else
			# a file copied via scp to the ssg system will be used:
			unalias cp; cp -a --backup=numbered $LDAP_CACERT_FILE /etc/openldap/cacerts/
			if [ $? -ne 0 ] || [ ! -s "$LDAP_CACERT_FILE" ]; then
				toLog "    ERROR - Copying the CA certificate file failed or the certificate file is empty. Exiting..."
				exit 1
			fi
			# If this point was reached the certificate file has been retrieved sucessfully and it is not empty.
			# Basic verification to make sure the file is a certificate:
			if [ "$(openssl verify /etc/openldap/cacerts/$CACERT_FILE_NAME 2>&1 | grep "^unable")" == "unable to load certificate" ]; then
				toLog "    ERROR - The CA certificate retrieved does not seem to be a certificate! Exiting..."
				exit 1
			else
				toLog "    Success - CA certificate has been retreived successfuly."
			fi
		fi
		
		# client tls auth (mutual authentication)
		if [ "X$CLT_TLS_AUTH" == "Xyes" ]; then
			# tls cert
			sed -i "s|\(^#tls_cert.*$\)|\1# Added by $0 on $DATE_TIME:\ntls_cert $LDAP_TLS_CERT\n|" $NSS_LDAP_CONF_FILE
			if [ $? -ne 0 ] || [ "$(grep "^tls_cert" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "$LDAP_TLS_CERT" ]; then
				toLog "    ERROR - Configuring 'tls_cert' field in $NSS_LDAP_CONF_FILE failed. Exiting..."
				exit 1
			else
				toLog "    Success - 'tls_cert' set to $LDAP_TLS_CERT in $NSS_LDAP_CONF_FILE."
			fi
			
			# tls key
			sed -i "s|\(^#tls_key.*$\)|\1# Added by $0 on $DATE_TIME:\ntls_key $LDAP_TLS_KEY\n|" $NSS_LDAP_CONF_FILE
			if [ $? -ne 0 ] || [ "$(grep "^tls_key" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "$LDAP_TLS_KEY" ]; then
				toLog "    ERROR - Configuring 'tls_key' field in $NSS_LDAP_CONF_FILE failed. Exiting..."
				exit 1
			else
				toLog "    Success - 'tls_key' set to $LDAP_TLS_KEY in $NSS_LDAP_CONF_FILE."
			fi
		fi

		# advanced tls
		if [ "X$ADVANCED_TLS_CONF" == "Xyes" ]; then
			# tls cipher suite
			sed -i "s|\(^#tls_ciphers.*$\)|\1\n# Added by $0 on $DATE_TIME:\ntls_ciphers $LDAP_TLS_CIPHER_SUITE\n|" $NSS_LDAP_CONF_FILE
			if [ $? -ne 0 ] || [ "$(grep "^tls_ciphers" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "$LDAP_TLS_CIPHER_SUITE" ]; then
				toLog "    ERROR - Configuring 'tls_ciphers' field in $NSS_LDAP_CONF_FILE failed. Exiting..."
				exit 1
			else
				toLog "    Success - 'tls_ciphers' set to $LDAP_TLS_CIPHER_SUITE in $NSS_LDAP_CONF_FILE."
			fi
			
			# tls_checkpeer
			if [ "X$LDAP_TLS_CHECKPEER" == "Xyes" ] || [ "X$LDAP_TLS_CHECKPEER" == "xno" ]; then
				sed -i "s|\(^#tls_checkpeer.*$\)|\1\n# Added by $0 on $DATE_TIME:\ntls_checkpeer $LDAP_TLS_CHECKPEER\n|" $NSS_LDAP_CONF_FILE
				if [ $? -ne 0 ] || [ "$(grep "^tls_checkpeer" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "$LDAP_TLS_CHECKPEER" ]; then
					toLog "    ERROR - Configuring 'tls_checkpeer' field in $NSS_LDAP_CONF_FILE failed. Exiting..."
					exit 1
				else
					toLog "    Success - 'tls_checkpeer' set to $LDAP_TLS_CHECKPEER in $NSS_LDAP_CONF_FILE."
				fi
			else
				toLog "    ERROR - The value of tls_checkpeer ($LDAP_TLS_CHECKPEER) directive for $NSS_LDAP_CONF_FILE is not valid! Exiting..."
				exit 1
			fi
		fi
	elif [ "X$LDAP_TYPE" == "Xldap" ]; then
		# ssl field will be set to no:
		sed -i "s|\(^#ssl on.*$\)|\1\n# Added by $0 on $DATE_TIME:\nssl no\n|" $NSS_LDAP_CONF_FILE
		if [ $? -ne 0 ] || [ "$(grep "^ssl" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "no" ]; then
			toLog "    ERROR - Disabling 'ssl' in $NSS_LDAP_CONF_FILE failed. Exiting..."
			exit 1
		else
			toLog "    Success - SSL has been disabled in $NSS_LDAP_CONF_FILE file."
		fi
	else
		toLog "    ERROR - The value of LDAP_TYPE ($LDAP_TYPE) variable in the configuration file is not valid! Exiting..."
		exit 1
	fi

	# If anonymous bind to ldap is disabled then credentials are expected:
	if [ "X$LDAP_ANONYM" == "Xno" ]; then
		# Enable LDAP auth on the system
		sed -i "s|^USELDAPAUTH.*$|USELDAPAUTH=yes|" /etc/sysconfig/authconfig
		if [ $? -ne 0 ] || [ "X$(grep "^USELDAPAUTH" /etc/sysconfig/authconfig | cut -d"=" -f2)" != "Xyes" ]; then
			toLog "    ERROR - Enabling LDAP AUTH in /etc/sysconfig/authconfig failed. Exiting..."
			exit 1
		else
			toLog "    Success - LDAP AUTH has been enabled."
		fi
	
		# binddn field
		if [ "X$LDAP_BINDDN" != "X" ]; then
			sed -i "s|\(^#binddn.*$\)|\1\n# Added by $0 on $DATE_TIME:\nbinddn $LDAP_BINDDN\n|" $NSS_LDAP_CONF_FILE
			if [ $? -ne 0 ] || [ "$(grep "^bindn" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "$LDAP_BINDDN" ]; then
				toLog "    ERROR - Configuring 'binddn' field in $NSS_LDAP_CONF_FILE failed. Exiting..."
				exit 1
			else
				toLog "    Success - 'binddn' field set to $LDAP_BINDDN in $NSS_LDAP_CONF_FILE file."
			fi
		else
			toLog "    ERROR - The value of 'binddn' directive for $NSS_LDAP_CONF_FILE is not valid! Exiting..."
			exit 1
		fi
		
		# bindpw field
		if [ "X$LDAP_BIND_PASSWD" != "X" ]; then
			sed -i "s|\(^#bindpw.*$\)|\1\n# Added by $0 on $DATE_TIME:\nbindpw $LDAP_BIND_PASSWD\n|" $NSS_LDAP_CONF_FILE
			if [ $? -ne 0 ] || [ "$(grep "^bindpw" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "$LDAP_BIND_PASSWD" ]; then
				toLog "    ERROR - Configuring 'bindpw' field in $NSS_LDAP_CONF_FILE failed. Exiting..."
				exit 1
			else
				toLog "    Success - 'bindpw' field set to $LDAP_BIND_PASSWD in $NSS_LDAP_CONF_FILE file."
			fi
		else
			toLog "    ERROR - The value of 'bindpw' directive for $NSS_LDAP_CONF_FILE is not valid! Exiting..."
			exit 1
		fi
	else
		# Disable LDAP auth on the system
		sed -i "s|^USELDAPAUTH.*$|USELDAPAUTH=no|" /etc/sysconfig/authconfig
		if [ $? -ne 0 ] || [ "X$(grep "^USELDAPAUTH" /etc/sysconfig/authconfig | cut -d"=" -f2)" != "Xno" ]; then
			toLog "    ERROR - Disabling LDAP AUTH in /etc/sysconfig/authconfig failed. Exiting..."
			exit 1
		else
			toLog "    Success - LDAP AUTH has been disabled."
		fi
	fi			
else		
	toLog "    Success - Configuration of $NSS_LDAP_CONF_FILE completed."
	
fi

# Configuring the $NSS_CONF_FILE file
checkFileExists $NSS_CONF_FILE
if [ "X$RETVAL" == "X1" ]; then
	toLog "  ERROR - File $NSS_CONF_FILE does not exist! Exiting..."
	exit 1
else
	toLog "   Info - FILE $NSS_CONF_FILE CONFIGURATION:"
	# passwd field
	sed -i "s|^passwd:\(.*$\)|# Commented by $0 on $DATE_TIME:\n#passwd:\1\n# Added by $0 on $DATE_TIME:\npasswd: files ldap\n|" $NSS_CONF_FILE
	if [ $? -ne 0 ] || [ "X$(grep "^passwd" file | cut -d" " -f3)" != "Xldap" ]; then
		toLog "    ERROR - Configuration of 'passwd' field in $NSS_CONF_FILE failed. Exiting..."
		exit 1
	else
		toLog "    Success - 'passwd' field in $NSS_CONF_FILE configured."
	fi
	
	# shadow field
	sed -i "s|^shadow:\(.*$\)|# Commented by $0 on $DATE_TIME:\n#shadow:\1\n# Added by $0 on $DATE_TIME:\nshadow: files ldap\n|" $NSS_CONF_FILE
	if [ $? -ne 0 ] || [ "X$(grep "^shadow" file | cut -d" " -f3)" != "Xldap" ]; then
		toLog "    ERROR - Configuration of 'shadow:' field in $NSS_CONF_FILE failed. Exiting..."
		exit 1
	else
		toLog "    Success - 'shadow' field in $NSS_CONF_FILE configured."
	fi
	
	# group field
	sed -i "s|^group:\(.*$\)|# Commented by $0 on $DATE_TIME:\n#group:\1\n# Added by $0 on $DATE_TIME:\ngroup: files ldap\n|" $NSS_CONF_FILE
	if [ $? -ne 0 ] || [ "X$(grep "^group" file | cut -d" " -f3)" != "Xldap" ]; then
		toLog "    ERROR - Configuration of 'group:' field in $NSS_CONF_FILE failed. Exiting..."
		exit 1
	else
		toLog "    Success - 'group' field in $NSS_CONF_FILE configured."
	fi
			
	# if this point was reached, then:
	toLog "    Success - Configuration of $NSS_CONF_FILE completed."
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
		toLog "ERROR - The script was called with an invalid parameter. Exiting..."
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
			#if [ "X$(file -b $2)" != "XASCII text" ]; then
			#	toLog "ERROR - $(basename $2) file is not a text file! Exiting..."
			#	exit 1
			#fi
			# check file name
			#if [ "X$(basename $2)" != "Xradius_ldap_setup.conf" ]; then
			#	toLog "ERROR - The argument provided was not correct; \"radius_ldap_setup.conf\" filename expected! Exiting..."
			#	exit 1
			#fi
			# if this point was reached all above conditions were passed so the configuration file can be sourced:
			source $2
			if [ "X$?" == "X0" ]; then
				toLog "Info - Configuration file ($(basename $2)) successfuly sourced."
				case "$CFG_TYPE" in
					ldap_only)
						# make backups of each file that will be changed/replaced:
						toLog "Info - ===== Starting backup for LDAP current configuration files. ====="
						doBackup $OPENLDAP_CONF_FILE
						doBackup $NSS_LDAP_CONF_FILE
						doBackup $NSS_CONF_FILE
						doBackup /etc/pam.d/system-auth-ac
						doBackup /etc/sysconfig/authconfig
						# bring back the original files
						toLog "Info - ===== Restoring original configuration files. ====="
						getOriginalFiles
						if [ $RETVAL -ne 0 ]; then
                            toLog "  ERROR - Restoring original files failed. Exiting..."
                            exit 1
                        else
							doConfigureLDAPonly
							if [ $RETVAL -eq 0 ]; then
								toLog "  Success - System configuration for LDAP only authentication completed."
							else
								toLog "  ERROR - Configuration of LDAP only authentication failed! Exiting..."
								exit 1
							fi
							# Configuration of pam to create home directories at sucessful login:
							doConfigPAMmkHomeDir
							if [ "X$RETVAL" == "X1" ]; then
								toLog "ERROR - Configuration of PAM to create home directories failed! Exiting..."
								exit 1
							else
								toLog "Success - Home directories will be automatically created at first successful login."
							fi
						fi
						;;

					radius_only)
						# make backups of each file that will be changed/replaced:
						toLog "Info - ===== Starting backup for Radius current configuration files. ====="
						doBackup $PAM_SSHD_CONF_FILE
						doBackup $PAM_RADIUS_CONF_FILE
						# bring back the original files
						toLog "Info - ===== Restoring original configuration files. ====="
						getOriginalFiles
						if [ $RETVAL -ne 0 ]; then
							toLog "  ERROR - Restoring original files failed. Exiting..."
                            exit 1
                        else
							doConfigureRADIUSonly
							if [ "$RETVAL" -eq "0" ]; then
								toLog "  Success - System configuration for Radius only authentication completed."
							else
								toLog "  ERROR - System configuration for Radius only authentication failed! Exiting..."
								exit 1
							fi
							# Configuration of pam to create home directories at sucessful login:
							doConfigPAMmkHomeDir
							if [ "X$RETVAL" == "X1" ]; then
								toLog "ERROR - Configuration of PAM to create home directories failed! Exiting..."
								exit 1
							else
								toLog "Success - Home directories will be automatically created at first successful login."
							fi
						fi
						;;

					radius_with_ldap)
						# make backups of each file that will be changed/replaced:
						toLog "Info - ===== Starting backup for Radius current configuration files. ====="
						doBackup $PAM_SSHD_CONF_FILE
						doBackup $PAM_RADIUS_CONF_FILE
						toLog "Info - ===== Starting backup for LDAP current configuration files. ====="
						doBackup $OPENLDAP_CONF_FILE
						doBackup $NSS_LDAP_CONF_FILE
						doBackup $NSS_CONF_FILE
						doBackup /etc/pam.d/system-auth-ac
						doBackup /etc/sysconfig/authconfig
						# bring back the original files
						toLog "Info - ===== Restoring original configuration files. ====="
						getOriginalFiles
						if [ $RETVAL -ne 0 ]; then
							toLog "  ERROR - Restoring original files failed. Exiting..."
							exit 1
						else
							doConfigureRADIUSonly
							if [ $RETVAL -eq 0 ]; then
								doConfigureLDAPonly
								if [ $RETVAL -eq 0 ]; then
									toLog "  Success - System configuration for Radius with LDAP authentication completed."
								else
									toLog "  ERROR - System configuration for Radius with LDAP authentication failed! Exiting..."
									exit 1
								fi
							fi
							# Configuration of pam to create home directories at sucessful login:
							doConfigPAMmkHomeDir
							if [ "X$RETVAL" == "X1" ]; then
								toLog "ERROR - Configuration of PAM to create home directories failed! Exiting..."
								exit 1
							else
								toLog "Success - Home directories will be automatically created at first successful login."
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
							# make backups of each file that will be replaced:
							toLog "Info - ===== Starting system reconfiguration for local authentication. ====="
							doBackup $PAM_SSHD_CONF_FILE
							doBackup $PAM_RADIUS_CONF_FILE
							doBackup $OPENLDAP_CONF_FILE
							doBackup $NSS_LDAP_CONF_FILE
							doBackup $NSS_CONF_FILE
							doBackup /etc/pam.d/system-auth-ac
							doBackup /etc/sysconfig/authconfig
							# bring back the original files
							toLog "Info - ===== Restoring original configuration files. ====="
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
				
				# Make sure that local auth is sufficient for local users:
				doLocalAuthSufficient
				if [ "X$RETVAL" == "X1" ]; then
					toLog "  ERROR - Configuring /etc/pam.d/system-auth-ac to consider local auth sufficient for local users failed! Exiting..."
					exit 1
				else
					toLog "   Success - The system was configured to consider local auth to be sufficient for local users."
				fi
				
				# Deleting the config file:
				echo "rm -rf "$2""
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

#################
# Do we want to change the current hash algorithm for user's password?
# Configuration of authentication services to use SHA-512 or MD5 as hash algorithm:
#doConfigAUTHhash $PASS_HASH_ALGO
#if [ "X$RETVAL" == "X1" ]; then
#	toLog "ERROR - Switching to $PASS_HASH_ALGO as hash algorithm for user's passwords has failed! Exiting..."
#	exit 1
#else
#	toLog "Success - $PASS_HASH_ALGO was set as default hash algorithm for user's passwords."
#fi
#################


# END of script
