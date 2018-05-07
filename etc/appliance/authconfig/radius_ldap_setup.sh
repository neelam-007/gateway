#!/bin/bash

# Description:
# This script is used by a .jar file to apply the necessary configurations to make the appliance
# use a Radius and/or LDAP server for SSH and console authentication.
#
# It should be run with one argument only: the configuration file (full_path/filename).
# This configuration file is to be created by a Java interactive wizard used to gather info
# from the user setting up the SSG appliance

# this fucntion is called if the script is ran with --backuporiginalconfig parameter
# the purpose is to create the $ORIG_CONF_FILES_DIR in which it will copy the
# configuration files that will be considered original files; it should only
# be called once, during application install time

# Define variables that will not be taken from the radius_ldap_setup.conf file:

LDAPS_CERTS_DIR="/etc/openldap/certs"
OPENLDAP_CONF_FILE="/etc/openldap/ldap.conf"
NSS_LDAP_CONF_FILE="/etc/nslcd.conf"
NSS_CONF_FILE="/etc/nsswitch.conf"
PAM_LDAP_CONF_FILE="/etc/pam_ldap.conf"
PAM_RADIUS_CONF_FILE="/etc/pam_radius.conf"
PAM_SSHD_CONF_FILE="/etc/pam.d/sshd"
PAM_LOGIN_CONF_FILE="/etc/pam.d/login"
SYS_AUTHCONF_FILE="/etc/sysconfig/authconfig"
PAM_SYS_AUTHCONF_FILE="/etc/pam.d/system-auth-ac"
SKEL_DIR="/etc/skel_ssg"
SUDOERS_FILE="/etc/sudoers"
BK_TIME=$(date +"%Y%m%d_%H%M%S")
DATE_TIME=$(date +"%Y-%m-%d %H:%M:%S")
BK_DIR="/opt/SecureSpan/Appliance/config/authconfig/bk_files"
ORIG_CONF_FILES_DIR="/opt/SecureSpan/Appliance/config/authconfig/orig_conf_files"
LOG_FILE="/opt/SecureSpan/Appliance/config/radius_ldap_setup.log"
RADIUS_CFG_FILES="$PAM_RADIUS_CONF_FILE $PAM_SSHD_CONF_FILE $PAM_SYS_AUTHCONF_FILE $PAM_LOGIN_CONF_FILE $SUDOERS_FILE"
LDAP_CFG_FILES="$OPENLDAP_CONF_FILE $NSS_LDAP_CONF_FILE $NSS_CONF_FILE $PAM_LDAP_CONF_FILE $PAM_LOGIN_CONF_FILE $PAM_SSHD_CONF_FILE $SUDOERS_FILE $SYS_AUTHCONF_FILE $PAM_SYS_AUTHCONF_FILE"
RADIUS_WITH_LDAP_CFG_FILES="$OPENLDAP_CONF_FILE $NSS_LDAP_CONF_FILE $NSS_CONF_FILE $PAM_RADIUS_CONF_FILE $PAM_SSHD_CONF_FILE $PAM_LOGIN_CONF_FILE $SUDOERS_FILE $SYS_AUTHCONF_FILE $PAM_SYS_AUTHCONF_FILE"
LOCAL_CFG_FILES="$PAM_SSHD_CONF_FILE $PAM_RADIUS_CONF_FILE $OPENLDAP_CONF_FILE $NSS_LDAP_CONF_FILE $NSS_CONF_FILE $PAM_SYS_AUTHCONF_FILE $SYS_AUTHCONF_FILE"

OWNER_CFG_FILE="layer7"
PERM_CFG_FILE="600"

# END of variables definition section

# FUNCTIONS:

toLog () {
    # this function will be called with one parameter whenever we need to send messages into the log file

    # test if 'date' command is available
    local DATE=$(which date)
    if [ $? -eq 0 ]; then
        LOG_TIME=$(date "+"%a" "%b" "%e" "%H:%M:%S" "%Y"")
        # there is no verification that the above syntax is working properly
        # in case there will be changes in the coreutils package that brings
        # the 'date' binary
    else
        echo -e "ERROR - The 'date' command does not appear to be available."
        ERREX="1"
    fi

    # test if $LOG_FILE exists
    if [ ! -f $LOG_FILE ]; then
        # log file does not exist! Creating it...
        echo "$LOG_TIME: Log file created." >> "$LOG_FILE"
    fi
    echo -e "$LOG_TIME: $@" >> "$LOG_FILE"

    # END of 'toLog' function
}

getCurrentConfigType () {
    # this is used to determine the current configuration type of the system
    # should we use authconfig --test here ? NO!
    if [ "X$(grep "local" /opt/SecureSpan/Appliance/config/authconfig/.auth_current | wc -l)" == "X1" ]; then
        # type is local
        RETVAL=2
    elif [ "X$(grep "ldap_only" /opt/SecureSpan/Appliance/config/authconfig/.auth_current | wc -l)" == "X1" ]; then
        # type is ldap_only
        RETVAL=3
    elif [ "X$(grep "radius_only" /opt/SecureSpan/Appliance/config/authconfig/.auth_current | wc -l)" == "X1" ]; then
        # type is radius_only
        RETVAL=4
    elif  [ "X$(grep "radius_with_ldap" /opt/SecureSpan/Appliance/config/authconfig/.auth_current | wc -l)" == "X1" ]; then
        # type is radius_with_ldap
        RETVAL=5
    else
        toLog " ERROR - Current configuration type could not be determined!"
        RETVAL=1
    fi

    # END of 'getCurrentConfigType' function
}

getCurrentConfigValues () {
    if [ "X$1" == "Xldap_only" ]; then
        # TODO: switch this from referencing /etc/nslcd.conf to /etc/pam_ldap.conf
        if [ "X$(grep "^pam_login_attribute" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" == "XsAMAccountName" ]; then
              echo "LDAP implementation is Microsoft Active Directory."
        fi
        echo "LDAP server IP/Hostname: $(grep "^URI" $OPENLDAP_CONF_FILE | sed 's|^.*//||' | tr -d '/' | cut -f 1 -d \:)"
        echo "LDAP server Port: $(grep "^URI" $OPENLDAP_CONF_FILE | sed 's|^.*//||' | tr -d '/' | cut -f 2 -d \:)"
        echo "LDAP base: $(grep "^BASE" $OPENLDAP_CONF_FILE | sed 's|^BASE ||')"
        if [ "X$(grep "^binddn" $NSS_LDAP_CONF_FILE | wc -l)" == "X1" ] && [ "X$(grep "^bindpw" $NSS_LDAP_CONF_FILE | wc -l)" == "X1" ]; then
            echo "Anonymous bind is disabled:"
            echo "  LDAP bind DN: $(grep "^binddn" $NSS_LDAP_CONF_FILE | awk '{print $2}')"
            echo "  LDAP bind password: $(grep "^bindpw" $NSS_LDAP_CONF_FILE | awk '{print $2}')"
        else
            echo "Anonymous bind is used."
        fi
        if [ "X$(grep "^URI" $OPENLDAP_CONF_FILE | tr -d '/' | cut -f 1 -d \: | awk '{print $2}')" == "Xldaps" ]; then
            echo "LDAP communication is encrypted:"
            echo "  Server/CA certificate file is: $(grep "^tls_cacert" $NSS_LDAP_CONF_FILE | awk '{print $2}')"
            echo "  Client handling of server's certificate: $(grep "^TLS_REQCERT" $NSS_LDAP_CONF_FILE | awk '{print $2}')"
            if [  "X$(grep "^TLS_REQCERT" $NSS_LDAP_CONF_FILE | awk '{print $2}')" == "Xnever" ]; then
                echo "    The client will not request or check the server certificate."
            elif [ "X$(grep "^TLS_REQCERT" $NSS_LDAP_CONF_FILE | awk '{print $2}')" == "Xallow" ]; then
                echo "    The client proceeds if no certificate or a bad certificate is presented."
            elif [ "X$(grep "^TLS_REQCERT" $NSS_LDAP_CONF_FILE | awk '{print $2}')" == "Xtry" ]; then
                echo "    The session is immediately terminated if a bad certificate is presented."
            elif [ "X$(grep "^TLS_REQCERT" $NSS_LDAP_CONF_FILE | awk '{print $2}')" == "Xhard" ] || [ "X$(grep "^TLS_REQCERT" $NSS_LDAP_CONF_FILE | awk '{print $2}')" == "Xdemand" ]; then
                echo "    The session is immediately terminated if no certificate or a bad certificate is presented."
            fi
        else
            echo "LDAP communication is not encrypted."
        fi
        echo "Group name is: $(cat /etc/sudoers | grep "systemconfig.sh" | sed 's/ALL.*$//' | grep -v "^ssgconfig")"
        echo "Group ID is: $(grep "^pam_filter" $NSS_LDAP_CONF_FILE | awk '{print $2}' | cut -d"=" -f2)"
        echo "LDAP object to look for password info is: $(grep "^nss_base_passwd" $NSS_LDAP_CONF_FILE | head -n 1 | awk '{print $2}' | cut -d"?" -f1)"
        echo "LDAP object to look for shadow info is: $(grep "^nss_base_shadow" $NSS_LDAP_CONF_FILE | head -n 1 | awk '{print $2}' | cut -d"?" -f1)"
        echo "LDAP object to look for group info is: $(grep "^nss_base_group" $NSS_LDAP_CONF_FILE | head -n 1 | awk '{print $2}' | cut -d"?" -f1)"
    elif [ "X$1" == "Xradius_only" ]; then
        echo "Radius server IP/Hostname: "$(grep -A1 "^# Added by" $PAM_RADIUS_CONF_FILE | grep -v "^# Added by" | awk '{print $1}')""
        echo "Radius secret: "$(grep -A1 "^# Added by" $PAM_RADIUS_CONF_FILE | grep -v "^# Added by" | awk '{print $2}')""
        echo "Radius timeout: "$(grep -A1 "^# Added by" $PAM_RADIUS_CONF_FILE | grep -v "^# Added by" | awk '{print $3}')""
    elif [ "X$1" == "Xradius_with_ldap" ]; then
        echo "Radius server IP/Hostname: "$(grep -A1 "^# Added by" $PAM_RADIUS_CONF_FILE | grep -v "^# Added by" | awk '{print $1}')""
        echo "Radius secret: "$(grep -A1 "^# Added by" $PAM_RADIUS_CONF_FILE | grep -v "^# Added by" | awk '{print $2}')""
        echo "Radius timeout: "$(grep -A1 "^# Added by" $PAM_RADIUS_CONF_FILE | grep -v "^# Added by" | awk '{print $3}')""
        # TODO: switch this from referencing /etc/nslcd.conf to /etc/pam_ldap.conf
        if [ "X$(grep "^pam_login_attribute" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" == "XsAMAccountName" ]; then
            echo "LDAP implementation is Microsoft Active Directory."
        fi
        echo "LDAP server IP/Hostname: $(grep "^URI" $OPENLDAP_CONF_FILE | sed 's|^.*//||' | tr -d '/' | cut -f 1 -d \:)"
        echo "LDAP server Port: $(grep "^URI" $OPENLDAP_CONF_FILE | sed 's|^.*//||' | tr -d '/' | cut -f 2 -d \:)"
        echo "LDAP base: $(grep "^BASE" $OPENLDAP_CONF_FILE | sed 's|^BASE ||')"
        if [ "X$(grep "^binddn" $NSS_LDAP_CONF_FILE | wc -l)" == "X1" ] && [ "X$(grep "^bindpw" $NSS_LDAP_CONF_FILE | wc -l)" == "X1" ]; then
                echo "Anonymous bind is disabled:"
                echo "  LDAP bind DN: $(grep "^binddn" $NSS_LDAP_CONF_FILE | awk '{print $2}')"
                echo "  LDAP bind password: $(grep "^bindpw" $NSS_LDAP_CONF_FILE | awk '{print $2}')"
        else
                echo "Anonymous bind is used."
        fi
        if [ "X$(grep "^URI" $OPENLDAP_CONF_FILE | tr -d '/' | cut -f 1 -d \: | awk '{print $2}')" == "Xldaps" ]; then
            echo "LDAP communication is encrypted:"
            echo "  Server/CA certificate file is: $(grep "^tls_cacert" $NSS_LDAP_CONF_FILE | awk '{print $2}')"
            echo "  Client handling of server's certificate: $(grep "^TLS_REQCERT" $NSS_LDAP_CONF_FILE | awk '{print $2}')"
            if [  "X$(grep "^TLS_REQCERT" $NSS_LDAP_CONF_FILE | awk '{print $2}')" == "Xnever" ]; then
                echo "    The client will not request or check the server certificate."
            elif [ "X$(grep "^TLS_REQCERT" $NSS_LDAP_CONF_FILE | awk '{print $2}')" == "Xallow" ]; then
                echo "    The client proceeds if no certificate or a bad certificate is presented."
            elif [ "X$(grep "^TLS_REQCERT" $NSS_LDAP_CONF_FILE | awk '{print $2}')" == "Xtry" ]; then
                echo "    The session is immediately terminated if a bad certificate is presented."
            elif [ "X$(grep "^TLS_REQCERT" $NSS_LDAP_CONF_FILE | awk '{print $2}')" == "Xhard" ] || [ "X$(grep "^TLS_REQCERT" $NSS_LDAP_CONF_FILE | awk '{print $2}')" == "Xdemand" ]; then
                echo "    The session is immediately terminated if no certificate or a bad certificate is presented."
            fi
        else
            echo "LDAP communication is not encrypted."
        fi
        echo "Group name is: $(cat /etc/sudoers | grep "systemconfig.sh" | sed 's/ALL.*$//' | grep -v "^ssgconfig")"
        echo "Group ID is: $(grep "^pam_filter" $NSS_LDAP_CONF_FILE | awk '{print $2}' | cut -d"=" -f2)"
        echo "LDAP object to look for password info is: $(grep "^nss_base_passwd" $NSS_LDAP_CONF_FILE | head -n 1 | awk '{print $2}' | cut -d"?" -f1)"
        echo "LDAP object to look for shadow info is: $(grep "^nss_base_shadow" $NSS_LDAP_CONF_FILE | head -n 1 | awk '{print $2}' | cut -d"?" -f1)"
        echo "LDAP object to look for group info is: $(grep "^nss_base_group" $NSS_LDAP_CONF_FILE | head -n 1 | awk '{print $2}' | cut -d"?" -f1)"
    else
        toLog "ERROR - function showCurrentConfigValues was called with a wrong parameter."
        ERREX="1"
    fi

    # END of getCurrentConfigValues
}

doBackup () {
    # this function expects to be called with one parameter only identifying the file that should be processed.
    if [ $# -eq 1 ]; then
        local FILE_TO_BACKUP=$1
        # making sure there is a directory to store backup files in:
        if [ ! -d $BK_DIR ]; then
            toLog "  Info - $BK_DIR does not exists. Creating it..."
            mkdir $BK_DIR
            if [ $? -eq 0 ]; then
                toLog "  Info - $BK_DIR not found but successfuly created."
            else
                toLog "  ERROR - $BK_DIR could not be created!"
                RETVAL=1
            fi
        fi

        if [ ! -f "$FILE_TO_BACKUP" ]; then
            toLog "  ERROR - File \"$FILE_TO_BACKUP\" does not exist!"
            RETVAL=1
            return
        fi

        cp --preserve=mode,ownership "$FILE_TO_BACKUP" "$BK_DIR/$(echo $FILE_TO_BACKUP | sed -e 's|/|+|g')_bk_$BK_TIME"
        if [ $? -eq 0 ]; then
            toLog "  Success - Backup of current $FILE_TO_BACKUP file created."
            RETVAL=0
        else
            toLog "  ERROR - Backup of current $FILE_TO_BACKUP could not be created!"
            RETVAL=1
        fi
    else
        toLog "  ERROR - Function 'doBackup' should be called with one parameter only!"
        RETVAL=1
    fi
    # END of 'doBackup' function
}

getOriginalFiles () {
    # this function will replace any current configuration files relevant to the setup
    # type indicated by the only pramater it accepts with the original versions of those files.
    if [ $# -eq 1 ]; then
        # making sure the directory exists and it is readable
        if [ ! -d $ORIG_CONF_FILES_DIR ]; then
                RETVAL=1
        fi
        if [ "X$1" == "Xradius_only" ]; then
            for F in $RADIUS_CFG_FILES; do
                cp --preserve=mode,ownership $ORIG_CONF_FILES_DIR"/"$(echo $F | sed -e 's|/|+|g') "/$(echo $F | sed -e 's|+|/|g')"
                if [ $? -eq 0 ]; then
                    RETVAL=0
                else
                    RETVAL=1
                fi
            done
        elif [ "X$1" == "Xldap_only" ]; then
            for F in $LDAP_CFG_FILES; do
                cp --preserve=mode,ownership $ORIG_CONF_FILES_DIR"/"$(echo $F | sed -e 's|/|+|g') "/$(echo $F | sed -e 's|+|/|g')"
                if [ $? -eq 0 ]; then
                    RETVAL=0
                else
                    RETVAL=1
                fi
            done
        elif [ "X$1" == "Xradius_with_ldap" ]; then
            FILES=$(ls -1 $ORIG_CONF_FILES_DIR)
            for F in $FILES; do
                cp --preserve=mode,ownership $ORIG_CONF_FILES_DIR"/"$F "/$(echo $F | sed -e 's|+|/|g')"
                if [ $? -eq 0 ]; then
                    RETVAL=0
                else
                    RETVAL=1
                fi
            done
        fi
    else
        toLog "    ERROR - Function 'getOriginalFiles' should be called with one parameter only!"
        RETVAL=1
    fi

    # END of 'getOriginalFiles' function
}

doBackupOriginalConfig () {
    # this fucntion is called if the script is ran with --backuporiginalconfig parameter;
    # the purpose is to create the $ORIG_CONF_FILES_DIR in which it will copy the
    # configuration files that will be considered original files; it should only
    # be called once, during application install time

    # making sure there is a directory to store backup files in:
    if [ ! -d $ORIG_CONF_FILES_DIR ]; then
        toLog "    Info - $ORIG_CONF_FILES_DIR does not exists. Creating it..."
        mkdir $ORIG_CONF_FILES_DIR
        if [ $? -eq 0 ]; then
            toLog "Info - $ORIG_CONF_FILES_DIR not found but successfuly created."
        else
            toLog "ERROR - $ORIG_CONF_FILES_DIR could not be created!"
            ERREX="1"
        fi
    fi
    if [ ! "$(ls -A $ORIG_CONF_FILES_DIR)" ]; then

        # get the set of files to backup (with no files listed more than once)
        FILES_TO_BACKUP=`echo "$RADIUS_CFG_FILES $LDAP_CFG_FILES $RADIUS_WITH_LDAP_CFG_FILES $LOCAL_CFG_FILES" | tr ' ' '\n' | sort | uniq | tr '\n' ' '`
        for F in $FILES_TO_BACKUP; do
            if [ ! -f "$F" ]; then
                toLog "ERROR - File \"$F\" does not exist! Cannot backup original config"
                ERREX="1"
            else
                cp --preserve=mode,ownership $F $ORIG_CONF_FILES_DIR"/"$(echo $F | sed -e 's|/|+|g')
                if [ $? -ne 0 ]; then
                    toLog "ERROR - Could not copy file \"$F\" to backup original config"
                    ERREX="1"
                else
                    toLog "Info - Original file \"$F\" was sucessfully copied to $ORIG_CONF_FILES_DIR directory."
                fi
            fi
        done
    fi

    # END of 'doBackupOriginalConfig' function
}

doConfigPAMmkHomeDir () {
    RETVAL=0

    # This function will be used to configure pam to automatically create home directories for
    # any user allowed to login via any service that includes the pam auth rules from system-auth-ac:
    LINE=$(grep -n "^session" $PAM_SYS_AUTHCONF_FILE | head -1 | cut -d":" -f1)
    sed -i ""$LINE"s|\(^session.*$\)|# Added by $0 on $DATE_TIME:\nsession     required     pam_mkhomedir.so skel=$SKEL_DIR umask=077\n\1|" $PAM_SYS_AUTHCONF_FILE
    if [ $? -ne 0 ] || [ "$(grep "skel" $PAM_SYS_AUTHCONF_FILE |  awk '{print $3}')" != "pam_mkhomedir.so" ]; then
        let RETVAL=$RETVAL+1
    fi

    # ssh needs to be configured separately to create home dirs as it doesn't source system-auth-ac
    LINE=$(grep -n "^session" $PAM_SSHD_CONF_FILE | head -1 | cut -d":" -f1)
    sed -i "${LINE}s|\(^session.*$\)|# Added by $0 on $DATE_TIME:\nsession     required     pam_mkhomedir.so skel=$SKEL_DIR umask=077\n\1|" $PAM_SSHD_CONF_FILE
    if [ $? -ne 0 ] || [ "$(grep "skel" $PAM_SSHD_CONF_FILE |  awk '{print $3}')" != "pam_mkhomedir.so" ]; then
        let RETVAL=$RETVAL+1
    fi
    # END of 'doConfigPAMmkHomeDir' function
}

doConfigAUTHhash () {
    # This function will be used to toggle the hash algorithm for user's passwords between md5 and sha512
    # md5 has been used prior RHEL6; sha512 was by default the hash algoritm used by RHEL6
    sed -i "s|^PASSWDALGORITHM.*$|PASSWDALGORITHM=$1|" $SYS_AUTHCONF_FILE
    if [ $? -ne 0 ] || [ "$(grep "^PASSWDALGORITHM" $SYS_AUTHCONF_FILE | cut -d"=" -f2)" != "sha512" ]; then
        RETVAL=1
    else
        RETVAL=0
    fi

    # END of 'doConfigAUTHhash' function
}

doLocalAuthSufficient () {
    # if we already have "account sufficient pam_localuser.so" in the file, we don't need to add it again
    if ( grep '^account[[:space:]]*sufficient[[:space:]]*pam_localuser.so' "$PAM_SYS_AUTHCONF_FILE" > /dev/null ) then
        RETVAL=0
        return
    fi

    # local authorization is sufficient for local users
    LINE2=$(grep -n "^account" $PAM_SYS_AUTHCONF_FILE | head -1 | cut -d":" -f1)
    sed -i ""$LINE2"s|\(^account.*$\)|\1\n# Added by $0 on $DATE_TIME:\naccount     sufficient    pam_localuser.so|" $PAM_SYS_AUTHCONF_FILE
    if [ $? -ne 0 ] || [ "$(grep "pam_localuser.so" $PAM_SYS_AUTHCONF_FILE | awk '{print $3}')" != "pam_localuser.so" ]; then
        RETVAL=1
    else
        RETVAL=0
    fi
}

doUpdateSudoers () {
    # updating the /etc/sudoers file:
    if [ "X$LDAP_GROUP_NAME" != "X" ]; then
        # prepare group name - in case there are spaces we have to escape them:
        SUDOERS_COMP_GROUP_NAME=$(echo "$LDAP_GROUP_NAME" | sed 's| |\\\\ |g')
        doBackup "/etc/sudoers"
        if [ "X$RETVAL" == "X0" ]; then
            sed -i "s|\(^ssgconfig ALL = NOPASSWD: /sbin/service ssem start.*$\)|\1\n\n# Added by $0 on $DATE_TIME:\n\
%$SUDOERS_COMP_GROUP_NAME ALL = NOPASSWD: /sbin/reboot\n\
%$SUDOERS_COMP_GROUP_NAME ALL = (layer7) NOPASSWD: /opt/SecureSpan/Appliance/config/systemconfig.sh\n\
%$SUDOERS_COMP_GROUP_NAME ALL = (layer7) NOPASSWD: /opt/SecureSpan/Appliance/config/scahsmconfig.sh\n\
%$SUDOERS_COMP_GROUP_NAME ALL = (layer7,root) NOPASSWD: /opt/SecureSpan/Appliance/libexec/masterkey-manage.pl\n\
%$SUDOERS_COMP_GROUP_NAME ALL = (layer7) NOPASSWD: /opt/SecureSpan/Appliance/libexec/ncipherconfig.pl\n\
%$SUDOERS_COMP_GROUP_NAME ALL = (layer7) NOPASSWD: /opt/SecureSpan/Appliance/libexec/ssgconfig_launch\n\
%$SUDOERS_COMP_GROUP_NAME ALL = (layer7) NOPASSWD: /opt/SecureSpan/EnterpriseManager/config/emconfig.sh\n\
%$SUDOERS_COMP_GROUP_NAME ALL = (layer7) NOPASSWD: /opt/SecureSpan/Appliance/libexec/patchcli_launch\n\
%$SUDOERS_COMP_GROUP_NAME ALL = NOPASSWD: /sbin/chkconfig ssem on, /sbin/chkconfig ssem off\n\
%$SUDOERS_COMP_GROUP_NAME ALL = NOPASSWD: /opt/SecureSpan/Appliance/libexec/viewlog\n\
%$SUDOERS_COMP_GROUP_NAME ALL = NOPASSWD: /sbin/service ssem start, /sbin/service ssem stop, /sbin/service ssem status\n|" /etc/sudoers
        if [ $? -ne 0 ] || [ $(grep "^%$SUDOERS_COMP_GROUP_NAME" /etc/sudoers | wc -l) -eq 0 ]; then
            toLog "    ERROR - Updating the /etc/sudoers file failed. Restoring the previous version of the /etc/sudoers file."
            rm -rf /etc/sudoers && cp --preserve=mode,ownership $ORIG_CONF_FILES_DIR"/"+etc+sudoers /etc/sudoers
            if [ $? -ne 0 ]; then
                toLog "    ERROR - Restoring the previous version of the /etc/sudoers file failed!"
                RETVAL=1
            else
                toLog "    Success - Restoring the previous version of the /etc/sudoers file completed."
            fi
            RETVAL=1
            fi
        else
            toLog "    ERROR - the /etc/sudoers file could not be backed-up."
            RETVAL=1
        fi
    else
        toLog "    ERROR - The value of 'LDAP_GROUP_NAME' directive is not valid!"
        RETVAL=1
    fi
}


doConfigureRADIUSonly () {
    ## ==========================================
    ##     Configuration for RADIUS Only Auth
    ## ==========================================
    toLog "Info - ===== Starting system configuration for RADIUS only authentication. ====="
    if [ ! -f "/etc/ssh/sshd_config" ]; then
        toLog "  ERROR - File /etc/ssh/sshd_config does not exist!"
        ERREX="1"
    else
        toLog "   Info - FILE etc/ssh/sshd_config VERIFICATION:"
        # Making sure that "UsePAM" directive is set to "yes". The SSG appliance usually has this set to yes.
        SSHD_CONFIG_CHECK=$(grep "^UsePAM " /etc/ssh/sshd_config | cut -d" " -f2)
        if [ "X$SSHD_CONFIG_CHECK" == "Xyes" ]; then
            toLog "    Success - SSHD is configured to use PAM."
        else
            toLog "    ERROR - SSHD is not configured to use PAM!"
            ERREX="1"
        fi
    fi

    # Configuring pam sshd module to use pam_radius:
    if [ ! -f "$PAM_SSHD_CONF_FILE" ]; then
        toLog "  ERROR - File $PAM_SSHD_CONF_FILE does not exist!"
        ERREX="1"
    else
        toLog "   Info - FILE $PAM_SSHD_CONF_FILE CONFIGURATION:"
        # adding the line for pam_radius with 'sufficient' ensures that
        # SSHD will allow logins if radius server fails; also, the 'retry=2' and
        # 'localifdown' option makes sure that after 2 retries the auth process
        # will fall back to local authentication

        # search for the requisite line (added by the hardenning script... but after) and if it's not there, add it:
        if [ "X$(grep "requisite" $PAM_SSHD_CONF_FILE | wc -l)" == "X1" ]; then
            sed -i "s|\(.*requisite.*$\)|# Added by $0 on $DATE_TIME:\nauth\tsufficient\tpam_radius_auth.so conf=$PAM_RADIUS_CONF_FILE retry=2 localifdown\n\1|" $PAM_SSHD_CONF_FILE
            if [ "X$(grep "$PAM_RADIUS_CONF_FILE" $PAM_SSHD_CONF_FILE | awk '{print $4}' | cut -d"=" -f2)" != "X$PAM_RADIUS_CONF_FILE" ]; then
                toLog "    ERROR - Inserting the pam_radius module line in the $PAM_SSHD_CONF_FILE file failed!"
            fi
        else
            sed -i "s|\(^#%PAM.*$\)|\1\n# Added by $0 on $DATE_TIME:\nauth\tsufficient\tpam_radius_auth.so conf=$PAM_RADIUS_CONF_FILE retry=2 localifdown\nauth\trequisite\tpam_listfile.so item=user sense=allow file=\/etc\/ssh\/ssh_allowed_users onerr=succeed|" $PAM_SSHD_CONF_FILE
            if [ "X$(grep "$PAM_RADIUS_CONF_FILE" $PAM_SSHD_CONF_FILE | awk '{print $4}' | cut -d"=" -f2)" != "X$PAM_RADIUS_CONF_FILE" ]; then
                toLog "    ERROR - Inserting the pam_radius module line in the $PAM_SSHD_CONF_FILE file failed!"
            fi
        fi
        toLog "    Success - Configuration of $PAM_SSHD_CONF_FILE completed."
    fi

    # Configuring pam login module to use pam_radius:
    if [ ! -f "$PAM_LOGIN_CONF_FILE" ]; then
        toLog "  ERROR - File $PAM_LOGIN_CONF_FILE does not exist!"
        ERREX="1"
    else
        toLog "   Info - FILE $PAM_LOGIN_CONF_FILE CONFIGURATION:"
        # adding the line for pam_radius with 'sufficient' ensures that
        # login will allow logins if radius server fails; also, the 'retry=2' and
        # 'localifdown' option makes sure that after 2 retries the auth process
        # will fall back to local authentication
        sed -i "s|\(^#%PAM.*$\)|\1\n# Added by $0 on $DATE_TIME:\nauth\tsufficient\tpam_radius_auth.so conf=$PAM_RADIUS_CONF_FILE retry=2 localifdown|" $PAM_LOGIN_CONF_FILE
        if [ "X$(grep "$PAM_RADIUS_CONF_FILE" $PAM_LOGIN_CONF_FILE | awk '{print $4}' | cut -d"=" -f2)" != "X$PAM_RADIUS_CONF_FILE" ]; then
            toLog "    ERROR - Inserting the pam_radius module line in the $PAM_LOGIN_CONF_FILE file failed!"
            ERREX="1"
        else
            toLog "    Success - Configuration of $PAM_LOGIN_CONF_FILE completed."
        fi
    fi

    # Configuration of linux pam radius module:
    if [ ! -f "$PAM_RADIUS_CONF_FILE" ]; then
        toLog "    ERROR - File $PAM_RADIUS_CONF_FILE does not exist!"
        ERREX="1"
    else
        toLog "   Info - FILE $PAM_RADIUS_CONF_FILE CONFIGURATION:"
        sed -i "s|\(^127.0.0.1.*$\)|# Commented by $0 on $DATE_TIME:\n#\1\n|" $PAM_RADIUS_CONF_FILE
        if [ $? -ne 0 ]; then
            toLog "    ERROR - Commenting the default host (127.0.0.1) line in $PAM_RADIUS_CONF_FILE failed!"
            ERREX="1"
        else
            sed -i "s|^other-server.*$|# Added by $0 on $DATE_TIME:\n$RADIUS_SRV_HOST\t$RADIUS_SECRET\t$RADIUS_TIMEOUT|" $PAM_RADIUS_CONF_FILE
            if [ $? -ne 0 ]; then
                toLog "    ERROR - Inserting the radius server details line in $PAM_RADIUS_CONF_FILE failed."
                ERREX="1"
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
    sed -i "s|^USELDAP=.*$|USELDAP=yes|" $SYS_AUTHCONF_FILE
    if [ $? -ne 0 ] || [ "X$(grep "^USELDAP=" $SYS_AUTHCONF_FILE | cut -d"=" -f2)" != "Xyes" ]; then
        toLog "  ERROR - Enabling LDAP in $SYS_AUTHCONF_FILE failed."
        STATUS=1
    else
        toLog "  Success - LDAP has been enabled."
    fi

    if [ ! -f "$OPENLDAP_CONF_FILE" ]; then
        toLog "  ERROR - File $OPENLDAP_CONF_FILE does not exist!"
        STATUS=1
    else
        toLog "   Info - FILE $OPENLDAP_CONF_FILE CONFIGURATION:"
        # BASE field
        if [ "X$LDAP_BASE" != "X" ]; then
            sed -i "/^BASE[[:space:]]*/d" "$OPENLDAP_CONF_FILE"
            if [ $? -ne 0 ]; then
                toLog "    ERROR - Could not remove preexisting BASE lines in $OPENLDAP_CONF_FILE."
                STATUS=1
            fi
            sed -i "s|\(^#BASE.*$\)|\1\n# Added by $0 on $DATE_TIME:\nBASE $LDAP_BASE\n|" $OPENLDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "$(grep "^BASE" $OPENLDAP_CONF_FILE)" != "BASE $LDAP_BASE" ]; then
                toLog "    ERROR - Configuring 'BASE' field in $OPENLDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'BASE' set to $LDAP_BASE in $OPENLDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - The value of LDAP_BASE ($LDAP_BASE) variable in the configuration file is not valid!"
            STATUS=1
        fi
        # URI field
        sed -i "/^URI[[:space:]]*/d" "$OPENLDAP_CONF_FILE"
        if [ $? -ne 0 ]; then
            toLog "    ERROR - Could not remove preexisting URI lines in $OPENLDAP_CONF_FILE."
            STATUS=1
        fi
        sed -i "s|\(^#URI.*$\)|\1\n# Added by $0 on $DATE_TIME:\nURI $LDAP_TYPE://$LDAP_SRV:$LDAP_PORT\n|" $OPENLDAP_CONF_FILE
        if [ $? -ne 0 ] || [ "X$(grep "^URI" $OPENLDAP_CONF_FILE | cut -d" " -f2)" != "X$LDAP_TYPE://$LDAP_SRV:$LDAP_PORT" ]; then
            toLog "    ERROR - Configuring 'URI' field in $OPENLDAP_CONF_FILE failed."
            STATUS=1
        else
            toLog "    Success - 'URI' set to $LDAP_TYPE://$LDAP_SRV:$LDAP_PORT in $OPENLDAP_CONF_FILE."
        fi

        # up to this point whether it is ldap or ldaps is not important
        if [ "X$LDAP_TYPE" == "Xldaps" ]; then
            toLog "   Info - '$LDAP_TYPE' will be configured."
            if [ ! -d "$LDAPS_CERTS_DIR" ]; then
                mkdir "$LDAPS_CERTS_DIR"
                chmod 644 "$LDAPS_CERTS_DIR"
                if [ $? -ne 0 ]; then
                    toLog "    ERROR - creating ldap cert directory"
                    STATUS=1
                fi
            fi
            # cacert url or file
            # Instead of using the TLS_CACERTDIR directive that is discourage, the TLS_CACERT directive
            # will be used to specify the one file that will contain all the necessary CA certificates.
            # For both situations (using URL or a previously copied file as the CA certificate) the file will end up in the /etc/openldap/cacerts directory.
            if [ "X$LDAP_CACERT_URL" != "X" ]; then
                ## replace space with %20
                LDAP_CACERT_URL=$(echo "$LDAP_CACERT_URL" | sed 's/ /%20/g')
                URL_CACERT_FILE=$(echo "$LDAP_CACERT_URL" | sed 's/.*\///')
                ## have to specify the output file, otherwise the output file name may contain spaces
                wget -O "$URL_CACERT_FILE"  --quiet --no-check-certificate --no-clobber --dns-timeout=2 --timeout=2 --waitretry=2 --tries=2 $LDAP_CACERT_URL
                if [ $? -ne 0 ]; then
                    toLog "    ERROR - Retrieving the CA certificate from URL failed!"
                    STATUS=1
                else
                    if [ -s "$URL_CACERT_FILE" ]; then
                        toLog "    Success - CA certificate has been retreived successfuly."
                        mv -f --backup=numbered "$URL_CACERT_FILE" "$LDAPS_CERTS_DIR"
                        if [ $? -ne 0 ]; then
                            toLog "    ERROR - Installing the CA certificate in $LDAPS_CERTS_DIR directory failed!"
                            STATUS=1
                        else
                            toLog "    Success - CA certificate installation completed."
                            CACERT_FILE_NAME=$URL_CACERT_FILE
                        fi
                    else
                        toLog "    ERROR - Retrieving the CA certificate from URL failed!"
                        STATUS=1
                    fi
                fi
            # a file copied via scp on the SSG system will be used:
            elif [ -s "$LDAP_CACERT_FILE" ]; then
                /bin/cp -a --backup=numbered "$LDAP_CACERT_FILE" $LDAPS_CERTS_DIR
                if [ $? -ne 0 ]; then
                    toLog "    ERROR - Copying the CA certificate file failed."
                    STATUS=1
                else
                    toLog "    Success - CA certificate installation completed."
                    CACERT_FILE_NAME=$(basename $LDAP_CACERT_FILE)
                fi
            fi


            if [ "X$CACERT_FILE_NAME" != "X" ]; then
                chmod 644 "$LDAPS_CERTS_DIR/$CACERT_FILE_NAME"
                if [ $? -ne 0 ] ; then
                    toLog "    ERROR - Changing cacert file permission."
                    STATUS=1
                fi
                # tls_cacertfile  in /etc/nslcd.conf
                echo "# Added by $0 on $DATE_TIME:" >> $NSS_LDAP_CONF_FILE
                echo "tls_cacertfile $LDAPS_CERTS_DIR/$CACERT_FILE_NAME" >> $NSS_LDAP_CONF_FILE
                if [ $? -ne 0 ] || [ "X$(grep "^tls_cacertfile " $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "X$LDAPS_CERTS_DIR/$CACERT_FILE_NAME" ]; then
                    toLog "    ERROR - Configuring 'tls_cacertfile' field in $NSS_LDAP_CONF_FILE failed."
                    STATUS=1
                else
                    toLog "    Success - 'tls_cacertfile' set to $LDAPS_CERTS_DIR/$CACERT_FILE_NAME in $NSS_LDAP_CONF_FILE."
                fi
              # TLS_CACERT in /etc/openldap/ldap.conf
              echo "TLS_CACERT $LDAPS_CERTS_DIR/$CACERT_FILE_NAME" >> $OPENLDAP_CONF_FILE
                if [ $? -ne 0 ] || [ "X$(grep "^TLS_CACERT " $OPENLDAP_CONF_FILE | cut -d" " -f2)" != "X$LDAPS_CERTS_DIR/$CACERT_FILE_NAME" ]; then
                    toLog "    ERROR - Configuring 'TLS_CACERT' field in $OPENLDAP_CONF_FILE failed."
                    STATUS=1
                else
                    toLog "    Success - 'TLS_CACERT' set to $LDAPS_CERTS_DIR/$CACERT_FILE_NAME in $OPENLDAP_CONF_FILE."
                fi
            fi



            # TLS_REQCERT in /etc/openldap/ldap.conf
            if [ "X$LDAP_TLS_REQCERT" != "X" ]; then
                echo "# Added by $0 on $DATE_TIME" >> $OPENLDAP_CONF_FILE
                echo "TLS_REQCERT $LDAP_TLS_REQCERT" >> $OPENLDAP_CONF_FILE
                if [ $? -ne 0 ] || [ "X$(grep "^TLS_REQCERT" $OPENLDAP_CONF_FILE | cut -d" " -f2)" != "X$LDAP_TLS_REQCERT" ]; then
                    toLog "    ERROR - Configuring 'TLS_REQCERT' field in $OPENLDAP_CONF_FILE failed."
                    STATUS=1
                else
                    toLog "    Success - 'TLS_REQCERT' set to $LDAP_TLS_REQCERT in $OPENLDAP_CONF_FILE."
                fi
            fi

            # tls_cacert in /etc/ldap.conf
            if [ "X$CACERT_FILE_NAME" != "X" ]; then
                sed -i "/^tls_cacertdir[[:space:]]*/d" "$NSS_LDAP_CONF_FILE"
                if [ $? -ne 0 ]; then
                    toLog "    ERROR - Could not remove preexisting tls_cacertdir lines in $NSS_LDAP_CONF_FILE."
                    STATUS=1
                fi
                sed -i "s|\(^#tls_cacertdir /etc/ssl/certs.*$\)|\1\n# Added by $0 on $DATE_TIME:\ntls_cacertdir $LDAPS_CERTS_DIR\n|" $NSS_LDAP_CONF_FILE
                if [ $? -ne 0 ] || [ "X$(grep "^tls_cacertdir" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "X$LDAPS_CERTS_DIR" ]; then
                    toLog "    ERROR - Configuring 'tls_cacertdir' field in $NSS_LDAP_CONF_FILE failed."
                    STATUS=1
                else
                    toLog "    Success - 'tls_cacertdir' set to $LDAPS_CERTS_DIR in $NSS_LDAP_CONF_FILE."
                fi
            fi

            # TLS_REQCERT in /etc/ldap.conf
            if [ "X$LDAP_TLS_REQCERT" != "X" ]; then
                echo "# Added by $0 on $DATE_TIME" >> $NSS_LDAP_CONF_FILE
                 echo "TLS_REQCERT $LDAP_TLS_REQCERT" >> $NSS_LDAP_CONF_FILE
                 if [ $? -ne 0 ] || [ "X$(grep "^TLS_REQCERT" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "X$LDAP_TLS_REQCERT" ]; then
                    toLog "    ERROR - Configuring 'TLS_REQCERT' field in $NSS_LDAP_CONF_FILE failed."
                    STATUS=1
                 else
                    toLog "    Success - 'TLS_REQCERT' set to $LDAP_TLS_REQCERT in $NSS_LDAP_CONF_FILE."
                 fi
            fi

            # tls cipher suite
            if [ "X$LDAP_TLS_CIPHER_SUITE" != "X" ]; then
                sed -i "s|\(^#tls_ciphers.*$\)|\1\n# Added by $0 on $DATE_TIME:\ntls_ciphers $LDAP_TLS_CIPHER_SUITE\n|" $NSS_LDAP_CONF_FILE
                if [ $? -ne 0 ] || [ "X$(grep "^tls_ciphers" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "X$LDAP_TLS_CIPHER_SUITE" ]; then
                    toLog "    ERROR - Configuring 'tls_ciphers' field in $NSS_LDAP_CONF_FILE failed."
                    STATUS=1
                else
                    toLog "    Success - 'tls_ciphers' set to $LDAP_TLS_CIPHER_SUITE in $NSS_LDAP_CONF_FILE."
                fi
            fi
        elif [ "X$LDAP_TYPE" == "Xldap" ]; then
            # ssl field will be set to no:
            # sed -i "s|\(^# OpenLDAP SSL mechanism.*$\)|\1\n# Added by $0 on $DATE_TIME:\nssl no\n|" $NSS_LDAP_CONF_FILE
            sed -i "/^ssl[[:space:]]*/d" "$NSS_LDAP_CONF_FILE"
            if [ $? -ne 0 ]; then
                toLog "    ERROR - Could not remove preexisting ssl lines in $NSS_LDAP_CONF_FILE."
                STATUS=1
            fi
            echo -e "\n# Added by $0 on $DATE_TIME:\nssl no\n" >> $NSS_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^ssl" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "Xno" ]; then
                toLog "    ERROR - Disabling 'ssl' in $NSS_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - SSL has been disabled in $NSS_LDAP_CONF_FILE file."
            fi
        fi
    fi

    # CONFIGURING NSS LDAP FILE:
    if [ ! -f "$NSS_LDAP_CONF_FILE" ]; then
        toLog "  ERROR - File $NSS_LDAP_CONF_FILE does not exist!"
        STATUS=1
    else
        toLog "   Info - FILE $NSS_LDAP_CONF_FILE CONFIGURATION:"
        # host field
        sed -i "s|\(^host 127.0.0.1\)|# Commented by $0 on $DATE_TIME:\n#\1\n|" $NSS_LDAP_CONF_FILE
        if [ $? -ne 0 ] || [ "X$(grep -i "^host" $NSS_LDAP_CONF_FILE)" != "X" ]; then
                toLog "    ERROR - Disabling default 'host' field in $NSS_LDAP_CONF_FILE failed."
                STATUS=1
        else
                toLog "    Success - 'host' field disabled in $NSS_LDAP_CONF_FILE."
        fi

        # base field
        if [ "X$LDAP_BASE" != "X" ]; then
            sed -i "s|\(^base dc=example,dc=com.*$\)|# Commented by $0 on $DATE_TIME:\n#\1\n# Added by $0 on $DATE_TIME:\nbase $LDAP_BASE\n|" $NSS_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^base" $NSS_LDAP_CONF_FILE)" != "Xbase $LDAP_BASE" ]; then
                toLog "    ERROR - Configuring the 'base' field in $NSS_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'base' field set to $LDAP_BASE in $NSS_LDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - The value of LDAP_BASE ($LDAP_BASE) variable in the configuration file is not valid!"
            STATUS=1
        fi

        # uri field
        if [ "X$LDAP_TYPE" != "X" ] && [ "X$LDAP_SRV" != "X" ] && [ "X$LDAP_PORT" != "X" ]; then
            sed -i '/^uri /d' $NSS_LDAP_CONF_FILE && \
            sed -i "s|\(^#uri ldap:.*$\)|\1\n# Added by $0 on $DATE_TIME:\nuri $LDAP_TYPE://$LDAP_SRV:$LDAP_PORT|" $NSS_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^uri" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "X$LDAP_TYPE://$LDAP_SRV:$LDAP_PORT" ]; then
                toLog "    ERROR - Configuring 'uri' field in $NSS_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'uri' field set to $LDAP_TYPE://$LDAP_SRV:$LDAP_PORT in $NSS_LDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - At least the value of one of LDAP_TYPE, LDAP_SRV or LDAP_PORT variables in the configuration file is not valid!"
            STATUS=1
        fi

    # pam_filter field
#        if [ "X$PAM_FILTER" != "X" ]; then
#                sed -i "s|\(^# Filter to AND with.*$\)|\1\n# Added by $0 on $DATE_TIME:\npam_filter $PAM_FILTER\n|" $NSS_LDAP_CONF_FILE
#                if [ $? -ne 0 ] || [ "X$(grep "^pam_filter" $NSS_LDAP_CONF_FILE)" != "Xpam_filter $PAM_FILTER" ]; then
#                        toLog "    ERROR - Configuring 'pam_filter' field in $NSS_LDAP_CONF_FILE failed."
#                        STATUS=1
#                else
#                        toLog "    Success - 'pam_filter' field set to $PAM_FILTER in $NSS_LDAP_CONF_FILE."
#                fi
#        else
#                toLog "    ERROR - PAM_FILTER cannot be empty!"
#                STATUS=1
#        fi

        # pam_login_attribute field
#        if [ "X$PAM_LOGIN_ATTR" != "X" ]; then
#                if [ "X$AD" == "Xyes" ]; then
#                        toLog "    INFO - Directory Server is an AD."
#                        sed -i "s|\(^# The user ID attribute.*$\)|\1\n# Added by $0 on $DATE_TIME:\npam_login_attribute $PAM_LOGIN_ATTR\n|" $NSS_LDAP_CONF_FILE
#                        if [ $? -ne 0 ] || [ "X$(grep "^pam_login_attribute" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "X$PAM_LOGIN_ATTR" ]; then
#                                toLog "    ERROR - Configuring 'pam_login_attribute' field in $NSS_LDAP_CONF_FILE failed."
#                                STATUS=1
#                        else
#                                toLog "    Success - 'pam_login_attribute' field set to $PAM_LOGIN_ATTR in $NSS_LDAP_CONF_FILE."
#                        fi
#
#           # mappings for AD 2003 and newer with SFU enabled
#                   sed -i "s|\(^pam_filter.*$\)|\1\n\n# Added by $0 on $DATE_TIME:\n\
#nss_map_objectclass posixAccount User\n\
#nss_map_objectclass shadowAccount User\n\
#nss_map_objectclass posixGroup Group\n\
#nss_map_attribute uid sAMAccountName\n\
#nss_map_attribute uniqueMember Member\n\
#nss_map_attribute homeDirectory unixHomeDirectory\n|" $NSS_LDAP_CONF_FILE
#                   if [ $? -ne 0 ] || [ "X$(grep "^nss_map_attribute uid" $NSS_LDAP_CONF_FILE)" != "Xnss_map_attribute uid sAMAccountName" ]; then
#                           toLog "    ERROR - Adding mappings for AD in $NSS_LDAP_CONF_FILE failed."
#                           STATUS=1
#                   else
#                           toLog "    Success - AD mappings configured in $NSS_LDAP_CONF_FILE."
#                   fi
#                elif [ "X$AD" == "Xno" ]; then
#                        toLog "    INFO - Directory Server is not an AD."
#                        sed -i "s|\(^# The user ID attribute.*$\)|\1\n# Added by $0 on $DATE_TIME:\npam_login_attribute $PAM_LOGIN_ATTR\n|" $NSS_LDAP_CONF_FILE
#                        if [ $? -ne 0 ] || [ "X$(grep "^pam_login_attribute" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "X$PAM_LOGIN_ATTR" ]; then
#                                toLog "    ERROR - Configuring 'pam_login_attribute' field in $NSS_LDAP_CONF_FILE failed."
#                                STATUS=1
#                        else
#                                toLog "    Success - 'pam_login_attribute' field set to $PAM_LOGIN_ATTR in $NSS_LDAP_CONF_FILE."
#                        fi
#                else
#                        toLog "    ERROR - AD has an unexpected value."
#                        STATUS=1
#                fi
#        else
#                toLog "    ERROR - PAM_LOGIN_ATTR cannot be empty!"
#                STATUS=1
#        fi
#
    # pam_min_uid field
#        if [ "X$PAM_MIN_UID" != "X" ]; then
#                sed -i "s|\(^#pam_min_uid.*$\)|\1\n# Added by $0 on $DATE_TIME:\npam_min_uid $PAM_MIN_UID|" $NSS_LDAP_CONF_FILE
#                if [ $? -ne 0 ] || [ "X$(grep "^pam_min_uid" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "X$PAM_MIN_UID" ]; then
#                        toLog "    ERROR - Configuring 'pam_min_uid' field in $NSS_LDAP_CONF_FILE failed."
#                        STATUS=1
#                else
#                        toLog "    Success - 'pam_min_uid' field set to $PAM_MIN_UID in $NSS_LDAP_CONF_FILE."
#                fi
#        else
#                toLog "    ERROR - PAM_MIN_UID cannot be empty!"
#                STATUS=1
#        fi

        # pam_max_uid field
#        if [ "X$PAM_MAX_UID" != "X" ]; then
#                sed -i "s|\(^#pam_max_uid.*$\)|\1\n# Added by $0 on $DATE_TIME:\npam_max_uid $PAM_MAX_UID\n|" $NSS_LDAP_CONF_FILE
#                if [ $? -ne 0 ] || [ "X$(grep "^pam_max_uid" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "X$PAM_MAX_UID" ]; then
#                        toLog "    ERROR - Configuring 'pam_max_uid' field in $NSS_LDAP_CONF_FILE failed."
#                        STATUS=1
#                else
#                        toLog "    Success - 'pam_max_uid' field set to $PAM_MAX_UID in $NSS_LDAP_CONF_FILE."
#                fi
#        else
#                toLog "    ERROR - PAM_MAX_UID cannot be empty!"
#                STATUS=1
#        fi


        # timelimit field
        if [ "X$NSS_TIMELIMIT" != "X" ]; then
            sed -i "s/^#timelimit /timelimit /" $NSS_LDAP_CONF_FILE && \
                sed -i "s|^timelimit.*$|# Added by $0 on $DATE_TIME:\ntimelimit $NSS_TIMELIMIT\n|" $NSS_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^timelimit" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "X$NSS_TIMELIMIT" ]; then
                toLog "    ERROR - Configuring 'timelimit' field in $NSS_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'timelimit' field set to $NSS_TIMELIMIT in $NSS_LDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - NSS_TIMELIMIT cannot be empty!"
            STATUS=1
        fi

        # bind_timelimit
        if [ "X$NSS_BIND_TIMELIMIT" != "X" ]; then
            sed -i "s/^#bind_timelimit /bind_timelimit /" $NSS_LDAP_CONF_FILE && \
                sed -i "s|^bind_timelimit.*$|# Added by $0 on $DATE_TIME:\nbind_timelimit $NSS_BIND_TIMELIMIT\n|" $NSS_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^bind_timelimit" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "X$NSS_BIND_TIMELIMIT" ]; then
                toLog "    ERROR - Configuring 'bind_timelimit' field in $NSS_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'bind_timelimit' field set to $NSS_BIND_TIMELIMIT in $NSS_LDAP_CONF_FILE."
            fi
        else
                toLog "    ERROR - NSS_BIND_TIMELIMIT cannot be empty!"
                STATUS=1
        fi

        # idle_timelimit
        if [ "X$NSS_IDLE_TIMELIMIT" != "X" ]; then
            sed -i "s/^#idle_timelimit /idle_timelimit /" $NSS_LDAP_CONF_FILE && \
                sed -i "s|^idle_timelimit.*$|# Added by $0 on $DATE_TIME:\nidle_timelimit $NSS_IDLE_TIMELIMIT\n|" $NSS_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^idle_timelimit" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "X$NSS_IDLE_TIMELIMIT" ]; then
                toLog "    ERROR - Configuring 'idle_timelimit' field in $NSS_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'idle_timelimit' field set to $NSS_IDLE_TIMELIMIT in $NSS_LDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - NSS_IDLE_TIMELIMIT cannot be empty!"
            STATUS=1
        fi


              # bind_policya
# disabled as this is not supported in /etc/nslcd.conf
#       if [ "X$NSS_BIND_POLICY" != "X" ]; then
#                sed -i "s|\(^#bind_policy.*$\)|\1\n# Added by $0 on $DATE_TIME:\nbind_policy $NSS_BIND_POLICY\n|" $NSS_LDAP_CONF_FILE
#                if [ $? -ne 0 ] || [ "X$(grep "^bind_policy" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "X$NSS_BIND_POLICY" ]; then
#                        toLog "    ERROR - Configuring 'bind_policy' field in $NSS_LDAP_CONF_FILE failed."
#                        STATUS=1
#                else
#                        toLog "    Success - 'bind_policy' field set to $NSS_BIND_POLICY in $NSS_LDAP_CONF_FILE."
#                fi
#        else
#                toLog "    ERROR - NSS_BIND_POLICY cannot be empty!"
#                STATUS=1
#        fi

        # nss_base_passwd
#        if [ "X$NSS_BASE_PASSWD" != "X" ]; then
#                sed -i "s|\(^#nss_base_passwd.*$\)|\1\n# Added by $0 on $DATE_TIME:\nnss_base_passwd $NSS_BASE_PASSWD,$LDAP_BASE?one\n|" $NSS_LDAP_CONF_FILE
#                if [ $? -ne 0 ] || [ "X$(grep "^nss_base_passwd" $NSS_LDAP_CONF_FILE | head -n 1)" != "Xnss_base_passwd $NSS_BASE_PASSWD,$LDAP_BASE?one" ]; then
#                        toLog "    ERROR - Configuring 'nss_base_passwd' field in $NSS_LDAP_CONF_FILE failed."
#                        STATUS=1
#                else
#                        toLog "    Success - 'nss_base_passwd' field set to $NSS_BASE_PASSWD in $NSS_LDAP_CONF_FILE."
#                fi
#        else
#                toLog "    ERROR - NSS_BASE_PASSWD cannot be empty!"
#                STATUS=1
#        fi


        # base passwd in /etc/nslcd.conf
        if [ "X$NSS_BASE_PASSWD" != "X" ]; then
            sed -i "s|\(^#base[[:space:]]*passwd.*$\)|\1\n# Added by $0 on $DATE_TIME:\nbase passwd $NSS_BASE_PASSWD,$LDAP_BASE\n|" $NSS_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^base passwd" $NSS_LDAP_CONF_FILE | head -n 1)" != "Xbase passwd $NSS_BASE_PASSWD,$LDAP_BASE" ]; then
                toLog "    ERROR - Configuring 'base passwd' field in $NSS_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'base passwd' field set to $NSS_BASE_PASSWD in $NSS_LDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - NSS_BASE_PASSWD cannot be empty!"
            STATUS=1
        fi


        # nss_base_group
#        if [ "X$NSS_BASE_GROUP" != "X" ]; then
#                sed -i "s|\(^#nss_base_group.*$\)|\1\n# Added by $0 on $DATE_TIME:\nnss_base_group $NSS_BASE_GROUP,$LDAP_BASE?one\n|" $NSS_LDAP_CONF_FILE
#                if [ $? -ne 0 ] || [ "X$(grep "^nss_base_group" $NSS_LDAP_CONF_FILE | head -n 1)" != "Xnss_base_group $NSS_BASE_GROUP,$LDAP_BASE?one" ]; then
#                        toLog "    ERROR - Configuring 'nss_base_group' field in $NSS_LDAP_CONF_FILE failed."
#                        STATUS=1
#                else
#                        toLog "    Success - 'nss_base_group' field set to $NSS_BASE_GROUP in $NSS_LDAP_CONF_FILE."
#                fi
#        else
#                toLog "    ERROR - NSS_BASE_GROUP cannot be empty!"
#                STATUS=1
#        fi


        # base group in /etc/nslcd.conf
        if [ "X$NSS_BASE_GROUP" != "X" ]; then
            sed -i "s|\(^#base[[:space:]]*group.*$\)|\1\n# Added by $0 on $DATE_TIME:\nbase group $NSS_BASE_GROUP,$LDAP_BASE\n|" $NSS_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^base group" $NSS_LDAP_CONF_FILE | head -n 1)" != "Xbase group $NSS_BASE_GROUP,$LDAP_BASE" ]; then
                toLog "    ERROR - Configuring 'base group' field in $NSS_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'base group' field set to $NSS_BASE_GROUP in $NSS_LDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - NSS_BASE_GROUP cannot be empty!"
            STATUS=1
        fi

        # nss_base_shadow
#        if [ "X$NSS_BASE_SHADOW" != "X" ]; then
#                sed -i "s|\(^#nss_base_shadow.*$\)|\1\n# Added by $0 on $DATE_TIME:\nnss_base_shadow $NSS_BASE_SHADOW,$LDAP_BASE?one\n|" $NSS_LDAP_CONF_FILE
#                if [ $? -ne 0 ] || [ "X$(grep "^nss_base_shadow" $NSS_LDAP_CONF_FILE)" != "Xnss_base_shadow $NSS_BASE_SHADOW,$LDAP_BASE?one" ]; then
#                        toLog "    ERROR - Configuring 'nss_base_shadow' field in $NSS_LDAP_CONF_FILE failed."
#                        STATUS=1
#                else
#                        toLog "    Success - 'nss_base_shadow' field set to $NSS_BASE_SHADOW in $NSS_LDAP_CONF_FILE."
#                fi
#        else
#                toLog "    ERROR - NSS_BASE_SHADOW cannot be empty!"
#                STATUS=1
#        fi


        # base shadow in /etc/nslcd.conf
        if [ "X$NSS_BASE_SHADOW" != "X" ]; then
            sed -i "s|\(^#base[[:space:]]*shadow.*$\)|\1\n# Added by $0 on $DATE_TIME:\nbase shadow $NSS_BASE_SHADOW,$LDAP_BASE\n|" $NSS_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^base shadow" $NSS_LDAP_CONF_FILE | head -n 1)" != "Xbase shadow $NSS_BASE_SHADOW,$LDAP_BASE" ]; then
                toLog "    ERROR - Configuring 'base shadow' field in $NSS_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'base shadow' field set to $NSS_BASE_SHADOW in $NSS_LDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - NSS_BASE_GROUP cannot be empty!"
            STATUS=1
        fi

        # default scope
        sed -i "s|\(^#scope sub.*$\)|\1\n# Added by $0 on $DATE_TIME:\nscope sub|" $NSS_LDAP_CONF_FILE
        if [ $? -ne 0 ] || [ "X$(grep "^scope sub" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "Xsub" ]; then
            toLog "    ERROR - Configuring 'scope' field in $NSS_LDAP_CONF_FILE failed."
            STATUS=1
        else
            toLog "    Success - 'scope' field set to sub in $NSS_LDAP_CONF_FILE."
        fi

        # If anonymous bind to ldap is disabled then credentials are expected:
        if [ "X$LDAP_ANONYM" == "Xno" ]; then
            # Enable LDAP auth on the system
            sed -i "s|^USELDAPAUTH.*$|USELDAPAUTH=yes|" $SYS_AUTHCONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^USELDAPAUTH" $SYS_AUTHCONF_FILE | cut -d"=" -f2)" != "Xyes" ]; then
                toLog "    ERROR - Enabling LDAP AUTH in $SYS_AUTHCONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - LDAP AUTH has been enabled."
            fi

            # binddn field
            if [ "X$LDAP_BINDDN" != "X" ]; then
                sed -i "s|\(^#binddn.*$\)|\1\n# Added by $0 on $DATE_TIME:\nbinddn $LDAP_BINDDN\n|" $NSS_LDAP_CONF_FILE
                if [ $? -ne 0 ] || [ "X$(grep "^binddn" $NSS_LDAP_CONF_FILE)" != "Xbinddn $LDAP_BINDDN" ]; then
                    toLog "    ERROR - Configuring 'binddn' field in $NSS_LDAP_CONF_FILE failed."
                    STATUS=1
                else
                    toLog "    Success - 'binddn' field set to $LDAP_BINDDN in $NSS_LDAP_CONF_FILE file."
                fi
            else
                toLog "    ERROR - The value of 'binddn' directive for $NSS_LDAP_CONF_FILE is not valid!"
                STATUS=1
            fi

            # bindpw field
            # password field can be empty so we will no check for the content of the variable
            sed -i "s|\(^#bindpw.*$\)|\1\n# Added by $0 on $DATE_TIME:\nbindpw $LDAP_BIND_PASSWD\n|" $NSS_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^bindpw" $NSS_LDAP_CONF_FILE | cut -d" " -f2)" != "X$LDAP_BIND_PASSWD" ]; then
                toLog "    ERROR - Configuring 'bindpw' field in $NSS_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'bindpw' field set to $LDAP_BIND_PASSWD in $NSS_LDAP_CONF_FILE file."
            fi
        else
            # Disable LDAP auth on the system
            sed -i "s|^USELDAPAUTH.*$|USELDAPAUTH=no|" $SYS_AUTHCONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^USELDAPAUTH" $SYS_AUTHCONF_FILE | cut -d"=" -f2)" != "Xno" ]; then
                toLog "    ERROR - Disabling LDAP AUTH in $SYS_AUTHCONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - LDAP AUTH has been disabled."
            fi
        fi
        toLog "   Success - Configuration of $NSS_LDAP_CONF_FILE completed."
    fi

    # restart the nslcd daemon (nsswitch's LDAP daemon)
    /etc/init.d/nslcd restart > /dev/null
    if [ $? -ne 0 ]; then
        toLog "   ERROR - Could not restart the nslcd daemon"
        STATUS=1
    else
        toLog "   Info - Successfully restarted the nslcd daemon"
    fi

    # Configuring the $NSS_CONF_FILE file
    if [ ! -f "$NSS_CONF_FILE" ]; then
        toLog "  ERROR - File $NSS_CONF_FILE does not exist!"
        STATUS=1
    else
        toLog "   Info - FILE $NSS_CONF_FILE CONFIGURATION:"
        # passwd field
        sed -i "s|^passwd:\(.*$\)|# Commented by $0 on $DATE_TIME:\n#passwd:\1\n# Added by $0 on $DATE_TIME:\npasswd: files ldap\n|" $NSS_CONF_FILE
        if [ $? -ne 0 ] || [ "X$(grep "^passwd" $NSS_CONF_FILE | cut -d" " -f3)" != "Xldap" ]; then
            toLog "    ERROR - Configuration of 'passwd' field in $NSS_CONF_FILE failed."
            STATUS=1
        else
            toLog "    Success - 'passwd' field in $NSS_CONF_FILE configured."
        fi

        # shadow field
        sed -i "s|^shadow:\(.*$\)|# Commented by $0 on $DATE_TIME:\n#shadow:\1\n# Added by $0 on $DATE_TIME:\nshadow: files ldap\n|" $NSS_CONF_FILE
        if [ $? -ne 0 ] || [ "X$(grep "^shadow" $NSS_CONF_FILE | cut -d" " -f3)" != "Xldap" ]; then
            toLog "    ERROR - Configuration of 'shadow:' field in $NSS_CONF_FILE failed."
            STATUS=1
        else
            toLog "    Success - 'shadow' field in $NSS_CONF_FILE configured."
        fi

        # group field
        sed -i "s|^group:\(.*$\)|# Commented by $0 on $DATE_TIME:\n#group:\1\n# Added by $0 on $DATE_TIME:\ngroup: files ldap\n|" $NSS_CONF_FILE
        if [ $? -ne 0 ] || [ "X$(grep "^group" $NSS_CONF_FILE | cut -d" " -f3)" != "Xldap" ]; then
            toLog "    ERROR - Configuration of 'group:' field in $NSS_CONF_FILE failed."
            STATUS=1
        else
            toLog "    Success - 'group' field in $NSS_CONF_FILE configured."
        fi

        # if this point was reached, then:
        toLog "   Success - Configuration of $NSS_CONF_FILE completed."
    fi

    # CONFIGURING PAM:
    if [ ! -f "$PAM_LDAP_CONF_FILE" ]; then
        toLog "  ERROR - File $PAM_LDAP_CONF_FILE does not exist!"
        STATUS=1
    else
        toLog "   Info - FILE $PAM_LDAP_CONF_FILE CONFIGURATION:"
        # host field
        sed -i "s|\(^host 127.0.0.1\)|# Commented by $0 on $DATE_TIME:\n#\1\n|" $PAM_LDAP_CONF_FILE
        if [ $? -ne 0 ] || [ "X$(grep -i "^host" $PAM_LDAP_CONF_FILE)" != "X" ]; then
            toLog "    ERROR - Disabling default 'host' field in $PAM_LDAP_CONF_FILE failed."
            STATUS=1
        else
            toLog "    Success - 'host' field disabled in $PAM_LDAP_CONF_FILE."
        fi

        # base field
        if [ "X$LDAP_BASE" != "X" ]; then
            sed -i "s|\(^base dc=example,dc=com.*$\)|# Commented by $0 on $DATE_TIME:\n#\1\n# Added by $0 on $DATE_TIME:\nbase $LDAP_BASE\n|" $PAM_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^base" $PAM_LDAP_CONF_FILE)" != "Xbase $LDAP_BASE" ]; then
                toLog "    ERROR - Configuring the 'base' field in $PAM_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'base' field set to $LDAP_BASE in $PAM_LDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - The value of LDAP_BASE ($LDAP_BASE) variable in the configuration file is not valid!"
            STATUS=1
        fi

        # uri field
        if [ "X$LDAP_TYPE" != "X" ] && [ "X$LDAP_SRV" != "X" ] && [ "X$LDAP_PORT" != "X" ]; then
            sed -i "/^uri[[:space:]]*/d" "$PAM_LDAP_CONF_FILE"
            if [ $? -ne 0 ]; then
                toLog "    ERROR - Could not remove preexisting URI lines in $PAM_LDAP_CONF_FILE."
                STATUS=1
            fi
            sed -i "s|\(^#uri ldap:.*$\)|\1\n# Added by $0 on $DATE_TIME:\nuri $LDAP_TYPE://$LDAP_SRV:$LDAP_PORT\n|" $PAM_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^uri" $PAM_LDAP_CONF_FILE | cut -d" " -f2)" != "X$LDAP_TYPE://$LDAP_SRV:$LDAP_PORT" ]; then
                toLog "    ERROR - Configuring 'uri' field in $PAM_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'uri' field set to $LDAP_TYPE://$LDAP_SRV:$LDAP_PORT in $PAM_LDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - At least the value of one of LDAP_TYPE, LDAP_SRV or LDAP_PORT variables in the configuration file is not valid!"
            STATUS=1
        fi

        # pam_filter field
        if [ "X$PAM_FILTER" != "X" ]; then
            sed -i "s|\(^# Filter to AND with.*$\)|\1\n# Added by $0 on $DATE_TIME:\npam_filter $PAM_FILTER\n|" $PAM_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^pam_filter" $PAM_LDAP_CONF_FILE)" != "Xpam_filter $PAM_FILTER" ]; then
                toLog "    ERROR - Configuring 'pam_filter' field in $PAM_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'pam_filter' field set to $PAM_FILTER in $PAM_LDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - PAM_FILTER cannot be empty!"
            STATUS=1
        fi

        # pam_login_attribute field
        if [ "X$PAM_LOGIN_ATTR" != "X" ]; then
            if [ "X$AD" == "Xyes" ]; then
                toLog "    INFO - Directory Server is an AD."
                sed -i "s|\(^# The user ID attribute.*$\)|\1\n# Added by $0 on $DATE_TIME:\npam_login_attribute $PAM_LOGIN_ATTR\n|" $PAM_LDAP_CONF_FILE
                if [ $? -ne 0 ] || [ "X$(grep "^pam_login_attribute" $PAM_LDAP_CONF_FILE | cut -d" " -f2)" != "X$PAM_LOGIN_ATTR" ]; then
                    toLog "    ERROR - Configuring 'pam_login_attribute' field in $PAM_LDAP_CONF_FILE failed."
                    STATUS=1
                else
                    toLog "    Success - 'pam_login_attribute' field set to $PAM_LOGIN_ATTR in $PAM_LDAP_CONF_FILE."
                fi

                # mappings for AD 2003 and newer with SFU enabled
                sed -i "s|\(^pam_filter.*$\)|\1\n\n# Added by $0 on $DATE_TIME:\n\
nss_map_objectclass posixAccount User\n\
nss_map_objectclass shadowAccount User\n\
nss_map_objectclass posixGroup Group\n\
nss_map_attribute uid sAMAccountName\n\
nss_map_attribute uniqueMember Member\n\
nss_map_attribute homeDirectory unixHomeDirectory\n|" $PAM_LDAP_CONF_FILE
                if [ $? -ne 0 ] || [ "X$(grep "^nss_map_attribute uid" $PAM_LDAP_CONF_FILE)" != "Xnss_map_attribute uid sAMAccountName" ]; then
                    toLog "    ERROR - Adding mappings for AD in $PAM_LDAP_CONF_FILE failed."
                    STATUS=1
                else
                    toLog "    Success - AD mappings configured in $PAM_LDAP_CONF_FILE."
                fi
            elif [ "X$AD" == "Xno" ]; then
                toLog "    INFO - Directory Server is not an AD."
                sed -i "s|\(^# The user ID attribute.*$\)|\1\n# Added by $0 on $DATE_TIME:\npam_login_attribute $PAM_LOGIN_ATTR\n|" $PAM_LDAP_CONF_FILE
                if [ $? -ne 0 ] || [ "X$(grep "^pam_login_attribute" $PAM_LDAP_CONF_FILE | cut -d" " -f2)" != "X$PAM_LOGIN_ATTR" ]; then
                    toLog "    ERROR - Configuring 'pam_login_attribute' field in $PAM_LDAP_CONF_FILE failed."
                    STATUS=1
                else
                    toLog "    Success - 'pam_login_attribute' field set to $PAM_LOGIN_ATTR in $PAM_LDAP_CONF_FILE."
                fi
            else
                toLog "    ERROR - AD has an unexpected value."
                STATUS=1
            fi
        else
            toLog "    ERROR - PAM_LOGIN_ATTR cannot be empty!"
            STATUS=1
        fi

        # pam_min_uid field
        if [ "X$PAM_MIN_UID" != "X" ]; then
            sed -i "s|\(^#pam_min_uid.*$\)|\1\n# Added by $0 on $DATE_TIME:\npam_min_uid $PAM_MIN_UID|" $PAM_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^pam_min_uid" $PAM_LDAP_CONF_FILE | cut -d" " -f2)" != "X$PAM_MIN_UID" ]; then
                toLog "    ERROR - Configuring 'pam_min_uid' field in $PAM_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'pam_min_uid' field set to $PAM_MIN_UID in $PAM_LDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - PAM_MIN_UID cannot be empty!"
            STATUS=1
        fi

        # pam_max_uid field
        if [ "X$PAM_MAX_UID" != "X" ]; then
            sed -i "s|\(^#pam_max_uid.*$\)|\1\n# Added by $0 on $DATE_TIME:\npam_max_uid $PAM_MAX_UID\n|" $PAM_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^pam_max_uid" $PAM_LDAP_CONF_FILE | cut -d" " -f2)" != "X$PAM_MAX_UID" ]; then
                toLog "    ERROR - Configuring 'pam_max_uid' field in $PAM_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'pam_max_uid' field set to $PAM_MAX_UID in $PAM_LDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - PAM_MAX_UID cannot be empty!"
            STATUS=1
        fi

        # timelimit field
        if [ "X$NSS_TIMELIMIT" != "X" ]; then
            sed -i "s/^#timelimit /timelimit /" $PAM_LDAP_CONF_FILE && \
            sed -i "s|^timelimit.*$|# Added by $0 on $DATE_TIME:\ntimelimit $NSS_TIMELIMIT\n|" $PAM_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^timelimit" $PAM_LDAP_CONF_FILE | cut -d" " -f2)" != "X$NSS_TIMELIMIT" ]; then
                toLog "    ERROR - Configuring 'timelimit' field in $PAM_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'timelimit' field set to $NSS_TIMELIMIT in $PAM_LDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - NSS_TIMELIMIT cannot be empty!"
            STATUS=1
        fi

        # bind_timelimit
        if [ "X$NSS_BIND_TIMELIMIT" != "X" ]; then
            sed -i "s/^#bind_timelimit /bind_timelimit /" $PAM_LDAP_CONF_FILE && \
                sed -i "s|^bind_timelimit.*$|# Added by $0 on $DATE_TIME:\nbind_timelimit $NSS_BIND_TIMELIMIT\n|" $PAM_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^bind_timelimit" $PAM_LDAP_CONF_FILE | cut -d" " -f2)" != "X$NSS_BIND_TIMELIMIT" ]; then
                toLog "    ERROR - Configuring 'bind_timelimit' field in $PAM_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'bind_timelimit' field set to $NSS_BIND_TIMELIMIT in $PAM_LDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - NSS_BIND_TIMELIMIT cannot be empty!"
            STATUS=1
        fi

        # idle_timelimit
        if [ "X$NSS_IDLE_TIMELIMIT" != "X" ]; then
            sed -i "s/^#idle_timelimit /idle_timelimit /" $PAM_LDAP_CONF_FILE && \
                sed -i "s|^idle_timelimit.*$|# Added by $0 on $DATE_TIME:\nidle_timelimit $NSS_IDLE_TIMELIMIT\n|" $PAM_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^idle_timelimit" $PAM_LDAP_CONF_FILE | cut -d" " -f2)" != "X$NSS_IDLE_TIMELIMIT" ]; then
                toLog "    ERROR - Configuring 'idle_timelimit' field in $PAM_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'idle_timelimit' field set to $NSS_IDLE_TIMELIMIT in $PAM_LDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - NSS_IDLE_TIMELIMIT cannot be empty!"
            STATUS=1
        fi

        # bind_policy
        if [ "X$NSS_BIND_POLICY" != "X" ]; then
            sed -i "s|\(^#bind_policy.*$\)|\1\n# Added by $0 on $DATE_TIME:\nbind_policy $NSS_BIND_POLICY\n|" $PAM_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^bind_policy" $PAM_LDAP_CONF_FILE | cut -d" " -f2)" != "X$NSS_BIND_POLICY" ]; then
                toLog "    ERROR - Configuring 'bind_policy' field in $PAM_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'bind_policy' field set to $NSS_BIND_POLICY in $PAM_LDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - NSS_BIND_POLICY cannot be empty!"
            STATUS=1
        fi

        # nss_base_passwd
        if [ "X$NSS_BASE_PASSWD" != "X" ]; then
            sed -i "s|\(^#nss_base_passwd.*$\)|\1\n# Added by $0 on $DATE_TIME:\nnss_base_passwd $NSS_BASE_PASSWD,$LDAP_BASE?one\n|" $PAM_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^nss_base_passwd" $PAM_LDAP_CONF_FILE | head -n 1)" != "Xnss_base_passwd $NSS_BASE_PASSWD,$LDAP_BASE?one" ]; then
                toLog "    ERROR - Configuring 'nss_base_passwd' field in $PAM_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'nss_base_passwd' field set to $NSS_BASE_PASSWD in $PAM_LDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - NSS_BASE_PASSWD cannot be empty!"
            STATUS=1
        fi

        # nss_base_group
        if [ "X$NSS_BASE_GROUP" != "X" ]; then
            sed -i "s|\(^#nss_base_group.*$\)|\1\n# Added by $0 on $DATE_TIME:\nnss_base_group $NSS_BASE_GROUP,$LDAP_BASE?one\n|" $PAM_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^nss_base_group" $PAM_LDAP_CONF_FILE | head -n 1)" != "Xnss_base_group $NSS_BASE_GROUP,$LDAP_BASE?one" ]; then
                toLog "    ERROR - Configuring 'nss_base_group' field in $PAM_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'nss_base_group' field set to $NSS_BASE_GROUP in $PAM_LDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - NSS_BASE_GROUP cannot be empty!"
            STATUS=1
        fi

        # nss_base_shadow
        if [ "X$NSS_BASE_SHADOW" != "X" ]; then
            sed -i "s|\(^#nss_base_shadow.*$\)|\1\n# Added by $0 on $DATE_TIME:\nnss_base_shadow $NSS_BASE_SHADOW,$LDAP_BASE?one\n|" $PAM_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^nss_base_shadow" $PAM_LDAP_CONF_FILE)" != "Xnss_base_shadow $NSS_BASE_SHADOW,$LDAP_BASE?one" ]; then
                toLog "    ERROR - Configuring 'nss_base_shadow' field in $PAM_LDAP_CONF_FILE failed."
                STATUS=1
            else
                toLog "    Success - 'nss_base_shadow' field set to $NSS_BASE_SHADOW in $PAM_LDAP_CONF_FILE."
            fi
        else
            toLog "    ERROR - NSS_BASE_SHADOW cannot be empty!"
            STATUS=1
        fi

        # default scope
        sed -i "s|\(^#scope sub.*$\)|\1\n# Added by $0 on $DATE_TIME:\nscope sub|" $PAM_LDAP_CONF_FILE
        if [ $? -ne 0 ] || [ "X$(grep "^scope sub" $PAM_LDAP_CONF_FILE | cut -d" " -f2)" != "Xsub" ]; then
            toLog "    ERROR - Configuring 'scope' field in $PAM_LDAP_CONF_FILE failed."
            STATUS=1
        else
            toLog "    Success - 'scope' field set to sub in $PAM_LDAP_CONF_FILE."
        fi

        # If anonymous bind to ldap is disabled then credentials are expected:
        if [ "X$LDAP_ANONYM" == "Xno" ]; then
            # Enable LDAP auth on the system
            sed -i "s|^USELDAPAUTH.*$|USELDAPAUTH=yes|" $SYS_AUTHCONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^USELDAPAUTH" $SYS_AUTHCONF_FILE | cut -d"=" -f2)" != "Xyes" ]; then
                    toLog "    ERROR - Enabling LDAP AUTH in $SYS_AUTHCONF_FILE failed."
                    STATUS=1
            else
                    toLog "    Success - LDAP AUTH has been enabled."
            fi

            # binddn field
            if [ "X$LDAP_BINDDN" != "X" ]; then
                    sed -i "s|\(^#binddn.*$\)|\1\n# Added by $0 on $DATE_TIME:\nbinddn $LDAP_BINDDN\n|" $PAM_LDAP_CONF_FILE
                    if [ $? -ne 0 ] || [ "X$(grep "^binddn" $PAM_LDAP_CONF_FILE)" != "Xbinddn $LDAP_BINDDN" ]; then
                            toLog "    ERROR - Configuring 'binddn' field in $PAM_LDAP_CONF_FILE failed."
                            STATUS=1
                    else
                            toLog "    Success - 'binddn' field set to $LDAP_BINDDN in $PAM_LDAP_CONF_FILE file."
                    fi
            else
                    toLog "    ERROR - The value of 'binddn' directive for $PAM_LDAP_CONF_FILE is not valid!"
                    STATUS=1
            fi

            # bindpw field
            # password field can be empty so we will no check for the content of the variable
            sed -i "s|\(^#bindpw.*$\)|\1\n# Added by $0 on $DATE_TIME:\nbindpw $LDAP_BIND_PASSWD\n|" $PAM_LDAP_CONF_FILE
            if [ $? -ne 0 ] || [ "X$(grep "^bindpw" $PAM_LDAP_CONF_FILE | cut -d" " -f2)" != "X$LDAP_BIND_PASSWD" ]; then
              toLog "    ERROR - Configuring 'bindpw' field in $PAM_LDAP_CONF_FILE failed."
              STATUS=1
            else
              toLog "    Success - 'bindpw' field set to $LDAP_BIND_PASSWD in $PAM_LDAP_CONF_FILE file."
            fi
        else
                # Disable LDAP auth on the system
                sed -i "s|^USELDAPAUTH.*$|USELDAPAUTH=no|" $SYS_AUTHCONF_FILE
                if [ $? -ne 0 ] || [ "X$(grep "^USELDAPAUTH" $SYS_AUTHCONF_FILE | cut -d"=" -f2)" != "Xno" ]; then
                        toLog "    ERROR - Disabling LDAP AUTH in $SYS_AUTHCONF_FILE failed."
                        STATUS=1
                else
                        toLog "    Success - LDAP AUTH has been disabled."
                fi
        fi
        toLog "   Success - Configuration of $PAM_LDAP_CONF_FILE completed."
    fi

    # sshd
    # if radius is already configured than we don't need to configure this file for ldap:
    if [ "X$(grep "$PAM_RADIUS_CONF_FILE" $PAM_SSHD_CONF_FILE | awk '{print $4}' | cut -d"=" -f2)" == "X$PAM_RADIUS_CONF_FILE" ]; then
        toLog "   Info - Looks like PAM is already configured to use Radius for SSH authentication. No need to configure PAM for LDAP authentication."
    else
        sed -i "s|\(^#%PAM-1.0.*$\)|\1\n#Added by $0 on $DATE_TIME:\nauth sufficient pam_ldap.so|" $PAM_SSHD_CONF_FILE
        if [ $? -ne 0 ] || [ "X$(grep "^auth" $PAM_SSHD_CONF_FILE | head -n 1 | cut -d" " -f3)" != "Xpam_ldap.so" ]; then
            toLog "   ERROR - Configuration of $PAM_SSHD_CONF_FILE to use pam_ldap.so library failed."
            STATUS=1
        else
            toLog "   Success - $PAM_SSHD_CONF_FILE configured to use pam_ldap.so library."
        fi
    fi

    # login
    # if radius is already configured than we don't need to configure this file for ldap:
    if [ "X$(grep "$PAM_RADIUS_CONF_FILE" $PAM_LOGIN_CONF_FILE | awk '{print $4}' | cut -d"=" -f2)" == "X$PAM_RADIUS_CONF_FILE" ]; then
        toLog "   Info - Looks like PAM is already configured to use Radius for console authentication. No need to configure PAM for LDAP authentication."
    else
        sed -i "s|\(.*pam_securetty.so.*$\)|\1\n#Added by $0 on $DATE_TIME:\nauth sufficient pam_ldap.so|" $PAM_LOGIN_CONF_FILE
        if [ $? -ne 0 ] || [ "X$(grep "^auth" $PAM_LOGIN_CONF_FILE | head -n 2 | tail -n 1 | cut -d" " -f3)" != "Xpam_ldap.so" ]; then
            toLog "   ERROR - Configuration of $PAM_LOGIN_CONF_FILE to use pam_ldap.so library failed."
            STATUS=1
        else
            toLog "   Success - $PAM_LOGIN_CONF_FILE configured to use pam_ldap.so library."
        fi
    fi

    # END of 'doConfigureLDAPonly' function
}

# END of FUNCTIONS section
#############################################


#############################################
# script BODY section

# the default auth type is local:
if [ ! -f /opt/SecureSpan/Appliance/config/authconfig/.auth_current ]; then
    echo "local" > /opt/SecureSpan/Appliance/config/authconfig/.auth_current
fi

if [ $# -eq 1 ]; then
    if [ "X$1" == "X--getcurrentconfig" ]; then
        # find out what type of auth is the current one:
        getCurrentConfigType
        if [ "X$RETVAL" != "X1" ]; then
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
                    toLog "ERROR - Function 'getCurrentConfigType' returned invalid value!"
                ERREX="1"
                    ;;
            esac
        else
            toLog "ERROR - Determining the current config type failed!"
            ERREX="1"
        fi
    elif [ "X$1" == "X--backuporiginalconfig" ]; then
        # create the repository of original configuration files:
        doBackupOriginalConfig
    else
        toLog "ERROR - The script was called with an invalid parameter."
        ERREX="1"
    fi
elif [ $# -eq 2 ]; then
    if [ "X$1" == "X--configfile" ]; then
        SCRIPT_CONFIG_FILE=$2
        if [ ! -f "$SCRIPT_CONFIG_FILE" ]; then
            toLog "Info - File $SCRIPT_CONFIG_FILE does not exist. This may not be an error!"
            # the script will NOT exit with error because it is called each time the appliance
            # boots but the appliance may have not been configured for radius authentication
        else
            toLog "======= NEW RUN ======="
            source $SCRIPT_CONFIG_FILE
            if [ $? -eq 0 ]; then
                toLog "Info - Configuration file ($(basename $SCRIPT_CONFIG_FILE)) successfuly sourced."
                case "$CFG_TYPE" in
                    ldap_only)
                       # make backups of each file that will be changed/replaced:
                        toLog "Info - ===== Starting backup for LDAP current configuration files. ====="
                        for FILE_TO_BACKUP in `echo "$LDAP_CFG_FILES"`; do
                            doBackup "$FILE_TO_BACKUP"
                        done

                        # bring back the original files
                        toLog "Info - ===== Restoring original configuration files. ====="
                        getOriginalFiles ldap_only
                        if [ "X$RETVAL" != "X0" ]; then
                            toLog "  ERROR - Restoring original files failed."
                            ERREX="1"
                        else
                            toLog "  Success - original configuration files restored."
                            toLog "Info - Starting configuration for LDAP only."
                            doConfigureLDAPonly
                            if [ "X$STATUS" == "X1" ]; then
                                toLog "  ERROR - Configuration of LDAP only authentication failed!"
                                ERREX="1"
                            else
                                toLog "  Success - System configuration for LDAP only authentication completed."
                                echo "ldap_only" > /opt/SecureSpan/Appliance/config/authconfig/.auth_current
                                # Configuration of pam to create home directories at sucessful login:
                                doConfigPAMmkHomeDir
                                if [ "X$RETVAL" == "X0" ]; then
                                    toLog "  Success - Home directories will be automatically created at first successful login."
                                else
                                    toLog "  ERROR - Configuration of PAM to create home directories failed!"
                                    ERREX="1"
                                fi
                                # Make sure that local auth is sufficient for local users:
                                toLog "Info - make local auth sufficient so that local users can still have access in case something goes wrong."
                                doLocalAuthSufficient
                                if [ "X$RETVAL" == "X1" ]; then
                                   toLog "  ERROR - Configuring $PAM_SYS_AUTHCONF_FILE to consider local auth sufficient for local users failed!"
                                   ERREX="1"
                                else
                                   toLog "  Success - The system was configured to consider local auth to be sufficient for local users."
                                fi
                                toLog "Info - update /etc/sudoers."
                                doUpdateSudoers
                                if [ "X$RETVAL" == "X1" ]; then
                                    toLog "  ERROR - Updating /etc/sudoers file failed!"
                                    ERREX="1"
                                else
                                    toLog "  Success - /etc/sudoers updated successfully."
                                fi
                            fi
                        fi
                    ;;

                    radius_only)
                        # make backups of each file that will be changed/replaced:
                        toLog "Info - ===== Starting backup for Radius current configuration files. ====="
                        for FILE_TO_BACKUP in `echo "$RADIUS_CFG_FILES"`; do
                            doBackup "$FILE_TO_BACKUP"
                        done
                        # bring back the original files
                        toLog "Info - ===== Restoring original configuration files. ====="
                        getOriginalFiles radius_only
                        if [ "X$RETVAL" != "X0" ]; then
                            toLog "  ERROR - Restoring original files failed."
                            ERREX="1"
                        else
                            toLog "  Success - original configuration files restored."
                            toLog "Info - Starting configuration for Radius only."
                            doConfigureRADIUSonly
                            if [ "X$RETVAL" == "X0" ]; then
                                toLog " Success - System configuration for Radius only authentication completed."
                                echo "radius_only" > /opt/SecureSpan/Appliance/config/authconfig/.auth_current
                            else
                                toLog " ERROR - System configuration for Radius only authentication failed!"
                                ERREX="1"
                            fi
                            # Configuration of pam to create home directories at sucessful login:
                            doConfigPAMmkHomeDir
                            if [ "X$RETVAL" == "X1" ]; then
                                toLog "ERROR - Configuration of PAM to create home directories failed!"
                                ERREX="1"
                            else
                                toLog "Success - Home directories will be automatically created at first successful login."
                                toLog "Info - /etc/skel_ssg is the directory holding the skeleton files for new users."
                            fi
                            # Make sure that local auth is sufficient for local users:
                            toLog "Info - make local auth sufficient so that local users can still have access in case something goes wrong."
                            doLocalAuthSufficient
                            if [ "X$RETVAL" == "X1" ]; then
                                toLog "  ERROR - Configuring $PAM_SYS_AUTHCONF_FILE to consider local auth sufficient for local users failed!"
                                ERREX="1"
                            else
                                toLog "  Success - The system was configured to consider local auth to be sufficient for local users."
                            fi
                        fi
                    ;;

                    radius_with_ldap)
                        # make backups of each file that will be changed/replaced:
                        toLog "Info - ===== Starting backup for Radius and LDAP current configuration files. ====="
                        for FILE_TO_BACKUP in `echo $RADIUS_WITH_LDAP_CFG_FILES`; do
                          doBackup "$FILE_TO_BACKUP"
                        done

                        # bring back the original files
                        toLog "Info - ===== Restoring original configuration files. ====="
                        getOriginalFiles radius_with_ldap
                        if [ "X$RETVAL" != "X0" ]; then
                            toLog "  ERROR - Restoring original files failed."
                            ERREX="1"
                        else
                            toLog "  Success - original configuration files restored."
                            toLog "Info - Starting configuration for Radius with LDAP."
                            doConfigureRADIUSonly
                            if [ "X$RETVAL" == "X0" ]; then
                                doConfigureLDAPonly
                                if [ "X$STATUS" == "X1" ]; then
                                    toLog "  ERROR - System configuration for Radius with LDAP authentication failed!"
                                    ERREX="1"
                                else
                                    toLog "  Success - System configuration for Radius with LDAP authentication completed."
                                    echo "radius_with_ldap" > /opt/SecureSpan/Appliance/config/authconfig/.auth_current
                                fi
                            fi

                            toLog "Info - update /etc/sudoers."
                            doUpdateSudoers
                            if [ "X$RETVAL" == "X1" ]; then
                                toLog "  ERROR - Updating /etc/sudoers file failed!"
                                ERREX="1"
                            else
                                toLog "  Success - /etc/sudoers updated successfully."
                            fi

                            # Configuration of pam to create home directories at sucessful login:
                            doConfigPAMmkHomeDir
                            if [ "X$RETVAL" == "X1" ]; then
                                toLog "ERROR - Configuration of PAM to create home directories failed!"
                                ERREX="1"
                            else
                                toLog "Success - Home directories will be automatically created at first successful login."
                            fi
                            # Make sure that local auth is sufficient for local users:
                            toLog "Info - make local auth sufficient so that local users can still have access in case something goes wrong."
                            doLocalAuthSufficient
                            if [ "X$RETVAL" == "X1" ]; then
                                toLog "  ERROR - Configuring $PAM_SYS_AUTHCONF_FILE to consider local auth sufficient for local users failed!"
                                ERREX="1"
                            else
                                toLog "  Success - The system was configured to consider local auth to be sufficient for local users."
                            fi
                        fi
                    ;;

                    local|file)
                        getCurrentConfigType
                        if [ "X$RETVAL" == "X2" ]; then
                            CUR_CFG_TYPE="local"
                            toLog "Info - ===== Starting system configuration for local authentication. ====="
                            toLog "  Info - System is already configured for local authentication. Nothing will be changed."
                        else
                            # make backups of each file that will be replaced:
                            toLog "Info - ===== Starting system reconfiguration for local authentication. ====="
                            for FILE_TO_BACKUP in `echo "$LOCAL_CFG_FILES"`; do
                                doBackup "$FILE_TO_BACKUP"
                            done
                            # bring back the original files
                            toLog "Info - ===== Restoring original configuration files. ====="
                            getOriginalFiles radius_with_ldap
                            if [ "X$RETVAL" == "X0" ]; then
                                toLog "  Success - System re-configuration for local authentication completed."
                                echo "local" > /opt/SecureSpan/Appliance/config/authconfig/.auth_current
                            else
                                toLog "  ERROR - System re-configuration for local authentication failed!"
                                ERREX="1"
                            fi
                            # Make sure that local auth is sufficient for local users:
                            toLog "Info - make local auth sufficient so that local users can still have access in case something goes wrong."
                            doLocalAuthSufficient
                            if [ "X$RETVAL" == "X1" ]; then
                                toLog "  ERROR - Configuring $PAM_SYS_AUTHCONF_FILE to consider local auth sufficient for local users failed!"
                                ERREX="1"
                            else
                                toLog "  Success - The system was configured to consider local auth to be sufficient for local users."
                            fi
                        fi
                    ;;

                    *)
                    toLog "ERROR - Configuration type read from configuration file is not valid!"
                    ERREX="1"
                esac
                toLog "Info - backup the config file:"
                doBackup $SCRIPT_CONFIG_FILE
                if [ $RETVAL -eq 0 ]; then
                    toLog "  Success - Configuration file was successfuly backed up."
                else
                     toLog "  ERROR - configuration file was NOT backed up."
                fi
            else
                toLog "ERROR - Sourcing of configuration file failed!"
                ERREX="1"
            fi
        fi
    else
        toLog "ERROR - First parameter received is invalid!"
    ERREX="1"
    fi
else
    toLog "ERROR - One or two parameters expected but none or more received."
 ERREX="1"
fi

toLog "Info - delete the config file:"
rm -rf "$2"
if [ $? -eq 0 ]; then
    toLog "  Success - Configuration file was successfuly deleted."
else
    toLog "  ERROR - Configuration file was NOT successfuly deleted!"
fi

if [ "X$ERREX" == "X1" ]; then
    if [ "X$CFG_TYPE" == "Xradius_only" ]; then
        toLog "INFO - An error was detected."
        toLog "Reverting to initial original config - local auth only..."
        getOriginalFiles radius_only
        if [ "X$RETVAL" != "X0" ]; then
            toLog "  ERROR - Going back to local auth failed!"
        else
            toLog "  Success - Going back to local auth was successful."
            echo "local" > /opt/SecureSpan/Appliance/config/authconfig/.auth_current
        fi
    fi

    if [ "X$CFG_TYPE" == "Xldap_only" ]; then
        toLog "INFO - An error was detected."
        toLog "Reverting to initial original config - local auth only..."
        getOriginalFiles ldap_only
        if [ "X$RETVAL" != "X0" ]; then
            toLog "  ERROR - Going back to local auth failed!"
        else
            toLog "  Success - Going back to local auth was successful."
            echo "local" > /opt/SecureSpan/Appliance/config/authconfig/.auth_current
        fi
    fi

    if [ "X$CFG_TYPE" == "Xradius_with_ldap" ] || [ "X$CFG_TYPE" == "Xlocal" ] || [ "X$CFG_TYPE" == "Xfile" ]; then
        toLog "INFO - An error was detected."
        toLog "Reverting to initial original config - local auth only..."
        getOriginalFiles radius_with_ldap
        if [ "X$RETVAL" != "X0" ]; then
            toLog "  ERROR - Going back to local auth failed!"
        else
            toLog "  Success - Going back to local auth was successful."
            echo "local" > /opt/SecureSpan/Appliance/config/authconfig/.auth_current
        fi
    fi
    exit 1
else
    toLog "======= ALL DONE ======="
    exit 0
fi

# END of BODY section
#############################################

# END of script
