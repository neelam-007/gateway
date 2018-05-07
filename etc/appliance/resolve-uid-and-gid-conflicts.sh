#!/bin/bash

# Description: This script resolves UID and GID conflicts, between the local passwd and group files and a customer's LDAP database,
# by assigning new UIDs and GIDs to the local conflicting accounts

# configurable values
#MIN_NEW_UID="20000"
#MAX_NEW_UID="20010"
#MIN_NEW_GID="30000"
#MAX_NEW_GID="30010"
UID_MAP="ssgconfig:50000,gateway:50001,layer7:50002,ssem:50004"
GID_MAP="ssgconfig:50000,gateway:50001,layer7:50002,ssem:50004,nfast:50003,pkcs11:50006"
PRIMARY_GROUP_MAP="ssgconfig:ssgconfig,gateway:gateway,layer7:layer7,ssem:ssem"
# use either "yes" or "no" for these values
VERBOSE="yes"
PROMPT_BEFORE_EXEC="yes"
# if this is set to yes, command line option processing will not occur and the options above must be set
IGNORE_COMMAND_LINE="no"

# constants
# these should not be changed unless directed to be a Layer 7 employee
OUR_USERS="ssgconfig gateway layer7 nfast ssem"
OUR_GROUPS="ssgconfig gateway layer7 nfast pkcs11"
# no directories with spaces or commas in this list (even escaped ones!)
# doing so will break for loops
DIRS_TO_CHOWN="/opt/SecureSpan /home/ssgconfig /opt/nfast /var/spool/mail"
TIMESTAMP=`date +"%Y-%m-%d-at-%H-%M-%S"`
LOG_FILE="$0-$TIMESTAMP.log"
RUN_FILE="$0-$TIMESTAMP.run.sh"

# functions
toLog () {
	# test if 'date' command is available
	local DATE=$(which date)
	if [ "X$?" == "X0" ]; then
		LOG_TIME=$(date "+"%a" "%b" "%e" "%H:%M:%S" "%Y"")
		# there is no verification that the above syntax is working properly
		# in case there will be changes in the coreutils package that brings
		# the 'date' binary
	else
		echo -e "ERROR ($LINENO) - The 'date' command does not appear to be available."
		exit 1
	fi
	# test if LOG_FILE exists
	if [ ! -f $LOG_FILE ]; then
		# log file does not exist! Creating it...
		echo "$LOG_TIME: Log file created." >> $LOG_FILE;
	fi
	# log the message
	if [ "$VERBOSE" == "yes" ]; then
		echo -e "$LOG_TIME: $@"
	fi
	echo -e "$LOG_TIME: $@" >> $LOG_FILE;
	# end of "toLog" function
}

confirmAction() {
    confirmed=2
    echo -n "${1} [y/n]: "

    while [ "${confirmed}" -eq 2 ]
    do
        read choice
        choice=$(echo "${choice}" | tr "[:upper:]" "[:lower:]")

        case $choice in
            "y")     confirmed=0;;
            "yes")   confirmed=0;;
            "t")     confirmed=0;;
            "true")  confirmed=0;;
            "n")     confirmed=1;;
            "no")    confirmed=1;;
            "f")     confirmed=1;;
            "false") confirmed=1;;
            *) echo -n "That is not a valid choice [y/n]: ";;
        esac
    done

    return ${confirmed}
}

displayUsage() {
	local SCRIPT_FILE_NAME=`basename $0`
	echo -e "usage: $SCRIPT_FILE_NAME <OPTIONS>"
	echo -e ""
	echo -e "where <OPTIONS> is a combination of"
	echo -e "-h | --help\t\tDisplay this message"
	echo -e "-v | --verbose\t\tDisplay logging info"
	echo -e "-y | --yes\t\tAutomatically answer yes to all prompts"
	echo -e "--minuid=<NUMBER>\tMinimum UID value to assign to Layer 7's accounts"
	echo -e "--maxuid=<NUMBER>\tMaximum UID value to assign to Layer 7's accounts"
	echo -e "--mingid=<NUMBER>\tMinimum GID value to assign to Layer 7's groups"
	echo -e "--maxgid=<NUMBER>\tMaximum GID value to assign to Layer 7's groups"
	echo -e "--uidmap=<MAPPING>\tMapping of usernames to new UIDs"
	echo -e "--gidmap=<MAPPING>\tMapping of groupnames to new GIDs"
	echo -e "--pgm=<MAPPING>\t\tMapping of usernames to their primary groupnames"
	echo -e ""
	echo -e "and <NUMBER> is a quoted integer"
	echo -e "and <MAPPING> is a comma separated list of pairs,"
	echo -e "\twhere each pair is associated by a colon"
	echo -e "\tand where order within each pair is important"
	echo -e "\tand no spaces are allowed"
	echo -e "\texample: \"ssgconfig:800,gateway:801,layer7:805\""
}

# you need to pass $@ to this for it to work as expected
getCommandLineOptions() {
	if [ "X$IGNORE_COMMAND_LINE" == "Xyes" ]; then
		return
	fi
	
	# NOTE: This requires GNU getopt
	TEMP=`/usr/bin/getopt --options hvy --longoptions help,verbose,yes,minuid:,maxuid:,mingid:,maxgid:,uidmap:,gidmap:,pgm: --name "$0" -- "$@"`
	
	if [ $? != 0 ]; then
		toLog "ERROR ($LINENO): Couldn't parse command line options"
		displayUsage
		exit 1
	fi
	
	# Note the quotes around `$TEMP': they are essential!
	eval set -- "$TEMP"

	# set the default values
	unset MIN_NEW_UID
	unset MAX_NEW_UID
	unset MIN_NEW_GID
	unset MAX_NEW_GID
	unset UID_MAP
	unset GID_MAP
	unset PRIMARY_GROUP_MAP
	VERBOSE="no"
	PROMPT_BEFORE_EXEC="yes"
	
	while true; do
		case "$1" in
			-h | --help ) displayUsage; exit 1 ;;
			-v | --verbose ) VERBOSE="yes"; shift ;;
			-y | --yes ) PROMPT_BEFORE_EXEC="no"; shift ;;
			--minuid ) MIN_NEW_UID="$2"; shift 2 ;;
			--maxuid ) MAX_NEW_UID="$2"; shift 2 ;;
			--mingid ) MIN_NEW_GID="$2"; shift 2 ;;
			--maxgid ) MAX_NEW_GID="$2"; shift 2 ;;
			--uidmap ) UID_MAP="$2"; shift 2 ;;
			--gidmap ) GID_MAP="$2"; shift 2 ;;
			--pgm | --primarygroupmap ) PRIMARY_GROUP_MAP="$2"; shift 2 ;;
			-- ) shift; break ;;
			* ) break ;;
		esac
	done
}

isSet() {
	if [ "X$1" == "X" ]; then
		toLog "$2"
		exit 1
	fi
}

isAlphaNumericString() {
	isSet "$1" "$2"
	if [[ $1 = *[[:alnum:]]* ]]; then
		return
	fi
	toLog "$2"
	exit 1
}

isNumber() {
	isSet "$1" "$2"
	if [[ $1 = *[[:digit:]]* ]]; then
		return
	fi
	toLog "$2"
	exit 1
}

isYesOrNo() {
	if [ "$1" == "yes" ]; then
		return
	fi
	if [ "$1" == "no" ]; then
		return
	fi
	toLog "$2"
	exit 1
}

checkType() {
	local TYPE=$1
	local VALUE=$2
	local MAP_NAME=$3
	if [ "$TYPE" == "uid" ] || [ "$TYPE" == "gid" ]; then
		isNumber "$VALUE" "ERROR ($LINENO): Value \"$VALUE\" in map $MAP_NAME is not a $TYPE"
	fi
	if [ "$TYPE" == "username" ] || [ "$TYPE" == "groupname" ]; then
		isAlphaNumericString "$VALUE" "ERROR ($LINENO): Value \"$VALUE\" in map $MAP_NAME is not a $TYPE"
	fi
}

checkUserOrGroupExists() {
	local TYPE=$1
	local VALUE=$2
	local MAP_NAME=$3
	if [ "$TYPE" == "username" ]; then
		local OUR_USERS_UID=`getent passwd $VALUE | awk -F ':' '{ print $3 }'`
		if [[ $OUR_USERS_UID -eq '' ]]; then
			toLog "ERROR ($LINENO): User \"$VALUE\" in map $MAP_NAME does not exist"
			exit 1;
		fi
	fi
	if [ "$TYPE" == "groupname" ]; then
		local OUR_GROUPS_GID=`getent group $VALUE | awk -F ':' '{ print $3 }'`
		if [[ $OUR_GROUPS_GID -eq '' ]]; then
			toLog "ERROR ($LINENO): Group \"$VALUE\" in map $MAP_NAME does not exist"
			exit 1;
		fi
	fi
}

checkAndFixMap() {
	local MAP_VALUES=$1
	local MAP_NAME=$2
	# types can be "username", "groupname", "uid", or "gid"
	local LEFT_TYPE=$3
	local RIGHT_TYPE=$4
	
	MAP_VALUES=`echo "$MAP_VALUES" | sed 's/,/ /g'`
	eval "$MAP_NAME=\"$MAP_VALUES\""
	
	ID_LIST=""
	
	for MAP_VALUE in `echo "$MAP_VALUES"`; do
		LEFT_VALUE=`echo $MAP_VALUE | awk -F ':' '{ print $1 }'`
		RIGHT_VALUE=`echo $MAP_VALUE | awk -F ':' '{ print $2 }'`
		
		checkType "$LEFT_TYPE" "$LEFT_VALUE" "$MAP_NAME"
		checkType "$RIGHT_TYPE" "$RIGHT_VALUE" "$MAP_NAME"
		
		if [ "$LEFT_TYPE" == "username" ] || [ "$LEFT_TYPE" == "groupname" ]; then
			checkUserOrGroupExists "$LEFT_TYPE" "$LEFT_VALUE" "$MAP_NAME"
		fi
		if [ "$RIGHT_TYPE" == "username" ] || [ "$RIGHT_TYPE" == "groupname" ]; then
			checkUserOrGroupExists "$RIGHT_TYPE" "$RIGHT_VALUE" "$MAP_NAME"
		fi
		
		ID_LIST="$ID_LIST\n$RIGHT_VALUE"
	done
	
	# make sure that the uids and gids are unique
	if [ "$RIGHT_TYPE" == "uid" ] || [ "$RIGHT_TYPE" == "gid" ]; then
		ORIG_COUNT=`echo -e "$ID_LIST" | wc -l`
		UNIQUE_COUNT=`echo -e "$ID_LIST" | uniq | wc -l`
		if [ "$ORIG_COUNT" -ne "$UNIQUE_COUNT" ]; then
			toLog "ERROR ($LINENO): UIDs or GIDs in map $MAP_NAME are not unique"
			exit 1
		fi
	fi
}

verifyConfigurableValues() {
	toLog "INFO ($LINENO): Checking configurable values"
	
	# are the required variables set
	isSet "$VERBOSE" "ERROR ($LINENO): VERBOSE variable in script is unset"
	isSet "$PROMPT_BEFORE_EXEC" "ERROR ($LINENO): PROMPT_BEFORE_EXEC variable in script is unset"
	
	# do the required variables have valid values
	isYesOrNo "$VERBOSE" "ERROR ($LINENO): VERBOSE variable in script is set to neither \"yes\" nor \"no\""
	isYesOrNo "$PROMPT_BEFORE_EXEC" "ERROR ($LINENO): PROMPT_BEFORE_EXEC variable in script is set to neither \"yes\" nor \"no\""
	
	# are the correct optional variables set
	if [[ -n "$UID_MAP" ]]; then
		if [[ -n "$MIN_NEW_UID" ]]; then
			toLog "ERROR ($LINENO): Either the UID map or min UID and max UID must be set"
			exit 1;
		fi
		if [[ -n "$MAX_NEW_UID" ]]; then
			toLog "ERROR ($LINENO): Either the UID map or min UID and max UID must be set"
			exit 1;
		fi
	else
		if [[ -z "$MIN_NEW_UID" ]]; then
			toLog "ERROR ($LINENO): Either the UID map or min UID and max UID must be set"
			exit 1;
		fi
		if [[ -z "$MAX_NEW_UID" ]]; then
			toLog "ERROR ($LINENO): Either the UID map or min UID and max UID must be set"
			exit 1;
		fi
	fi
	if [[ -n "$GID_MAP" ]]; then
		if [[ -n "$MIN_NEW_GID" ]]; then
			toLog "ERROR ($LINENO): Either the GID map or min GID and max GID must be set"
			exit 1;
		fi
		if [[ -n "$MAX_NEW_GID" ]]; then
			toLog "ERROR ($LINENO): Either the GID map or min GID and max GID must be set"
			exit 1;
		fi
	else
		if [[ -z "$MIN_NEW_GID" ]]; then
			toLog "ERROR ($LINENO): Either the GID map or min GID and max GID must be set"
			exit 1;
		fi
		if [[ -z "$MAX_NEW_GID" ]]; then
			toLog "ERROR ($LINENO): Either the GID map or min GID and max GID must be set"
			exit 1;
		fi
	fi
	# we don't need to check if PRIMARY_GROUP_MAP is set as it's optional to set or not
	
	# do the optional variables have correct values
	if [[ -n "$UID_MAP" ]]; then
		checkAndFixMap "$UID_MAP" "UID_MAP" "username" "uid"
	else
		isNumber "$MIN_NEW_UID" "ERROR ($LINENO): Min UID is not set to a numeric value"
		isNumber "$MAX_NEW_UID" "ERROR ($LINENO): Max UID is not set to a numeric value"
		# are the min values actually less than the max values
		if [ $MIN_NEW_UID -gt $MAX_NEW_UID ]; then
			toLog "ERROR ($LINENO): Min UID is greater than max UID"
			exit 1
		fi
	fi
	if [[ -n "$GID_MAP" ]]; then
		checkAndFixMap "$GID_MAP" "GID_MAP" "groupname" "gid"
	else
		isNumber "$MIN_NEW_GID" "ERROR ($LINENO): Min GID is not set to a numeric value"
		isNumber "$MAX_NEW_GID" "ERROR ($LINENO): Max GID is not set to a numeric value"
		# are the min values actually less than the max values
		if [ $MIN_NEW_GID -gt $MAX_NEW_GID ]; then
			toLog "ERROR ($LINENO): Min GID is greater than max GID"
			exit 1
		fi
	fi
	if [[ -n "$PRIMARY_GROUP_MAP" ]]; then
		checkAndFixMap "$PRIMARY_GROUP_MAP" "PRIMARY_GROUP_MAP" "username" "groupname"
	fi
	toLog "INFO ($LINENO): Done checking configurable values"
}

createMapsIfNeeded() {
	if [[ -z "$UID_MAP" ]]; then
		local UID_TO_ASSIGN="$MIN_NEW_UID"
		UID_MAP=""
		for OUR_USER in $OUR_USERS; do
			local OUR_USERS_UID=`getent passwd $OUR_USER | awk -F ':' '{ print $3 }'`

			# if our user doesn't exist, skip to the next one
			if [[ $OUR_USERS_UID -eq '' ]]; then
				toLog "DEBUG ($LINENO): $OUR_USER does not exist"
				continue
			fi
			toLog "DEBUG ($LINENO): $OUR_USER has UID $OUR_USERS_UID"
			
			# map the user's UID to a new one in the range
			# first find an unused UID in the range
			local NUM_USERS_WITH_NEW_UID=`getent passwd | awk -F ':' '{ print ":"$3":" }' | grep ":$UID_TO_ASSIGN:" | wc -l`
			toLog "DEBUG ($LINENO): There are $NUM_USERS_WITH_NEW_UID users with the new UID"
			while [ $NUM_USERS_WITH_NEW_UID -gt 0 ]; do
				UID_TO_ASSIGN=$[UID_TO_ASSIGN+1]
				toLog "DEBUG ($LINENO): Considering assigning UID $UID_TO_ASSIGN"
				NUM_USERS_WITH_NEW_UID=`getent passwd | awk -F ':' '{ print ":"$3":" }' | grep ":$UID_TO_ASSIGN:" | wc -l`
				toLog "DEBUG ($LINENO): There are $NUM_USERS_WITH_NEW_UID users with the new UID"
			done
			if [ $UID_TO_ASSIGN -gt $MAX_NEW_UID ]; then
				toLog "ERROR ($LINENO): UID range isn't large enough"
				exit 1;
			fi
			# then add our user's new UID to the map
			toLog "Will assign UID $UID_TO_ASSIGN to user $OUR_USER"
			UID_MAP="$UID_MAP $OUR_USER:$UID_TO_ASSIGN"
			UID_TO_ASSIGN=$[UID_TO_ASSIGN+1]
		done
		
		toLog "DEBUG ($LINENO): UID_MAP = \"$UID_MAP\""
	fi
	
	if [[ -z "$GID_MAP" ]]; then
		# check for conflicting GIDs
		local GID_TO_ASSIGN="$MIN_NEW_GID"
		GID_MAP=""
		for OUR_GROUP in $OUR_GROUPS; do
			local OUR_GROUPS_GID=`getent group $OUR_GROUP | awk -F ':' '{ print $3 }'`

			# if our group doesn't exist, skip to the next one
			if [[ $OUR_GROUPS_GID -eq '' ]]; then
				toLog "INFO ($LINENO): Group $OUR_GROUP does not exist"
				continue
			fi
			toLog "$OUR_GROUP has GID $OUR_GROUPS_GID"
			
			# map the group's GID to a new one in the range
			# first find an unused GID in the range
			local NUM_GROUPS_WITH_NEW_GID=`getent group | awk -F ':' '{ print ":"$3":" }' | grep ":$GID_TO_ASSIGN:" | wc -l`
			toLog "DEBUG ($LINENO): There are $NUM_GROUPS_WITH_NEW_GID groups with the new GID"
			while [ $NUM_GROUPS_WITH_NEW_GID -gt 0 ]; do
				GID_TO_ASSIGN=$[GID_TO_ASSIGN+1]
				toLog "DEBUG ($LINENO): Considering assigning GID $GID_TO_ASSIGN"
				NUM_GROUPS_WITH_NEW_GID=`getent group | awk -F ':' '{ print ":"$3":" }' | grep ":$GID_TO_ASSIGN:" | wc -l`
				toLog "DEBUG ($LINENO): There are $NUM_GROUPS_WITH_NEW_GID groups with the new GID"
			done
			if [ $GID_TO_ASSIGN -gt $MAX_NEW_GID ]; then
				toLog "ERROR ($LINENO): GID range isn't large enough"
				exit 1;
			fi
			# then add our group's new GID to the map
			toLog "Will assign GID $GID_TO_ASSIGN to group $OUR_GROUP"
			GID_MAP="$GID_MAP $OUR_GROUP:$GID_TO_ASSIGN"
			GID_TO_ASSIGN=$[GID_TO_ASSIGN+1]
		done
		
		toLog "DEBUG ($LINENO): GID_MAP = \"$GID_MAP\""
	fi
}

createRunFile() {
	toLog "INFO ($LINENO): Creating run file"

	echo -e '#!/bin/bash' > $RUN_FILE
	chmod 700 "$RUN_FILE"

	# get status of our services
	# (1 is running, 0 is not)
	local DBSTATUS_RUNNING=`service ssg-dbstatus status | grep running | wc -l`
	local SSEM_RUNNING=`service ssem status | grep running | wc -l`
	local SSG_RUNNING=`service ssg status | grep running | wc -l`

	# shutdown our services
	if [ $SSEM_RUNNING -eq 1 ]; then
		toLog 'INFO ($LINENO): Will call service ssem stop'
		echo "service ssem stop" >> $RUN_FILE
	fi
	if [ $DBSTATUS_RUNNING -eq 1 ]; then
		toLog 'INFO ($LINENO): Will call service ssg-dbstatus stop'
		echo "service ssg-dbstatus stop" >> $RUN_FILE
	fi
	if [ $SSG_RUNNING -eq 1 ]; then
		toLog 'INFO ($LINENO): Will call service ssg stop'
		echo "service ssg stop" >> $RUN_FILE
	fi

	createOwnershipLog before-change
	
	# change UIDs
	for UID_MAP_ENTRY in $UID_MAP; do
		local OUR_USER=`echo $UID_MAP_ENTRY | awk -F ':' '{ print $1 }'`
		local UID_TO_ASSIGN=`echo $UID_MAP_ENTRY | awk -F ':' '{ print $2 }'`
		local PREVIOUS_UID=`getent passwd $OUR_USER | awk -F ':' '{ print $3 }'`
		toLog "INFO ($LINENO): Will call usermod --uid $UID_TO_ASSIGN $OUR_USER"
		echo "usermod --uid $UID_TO_ASSIGN $OUR_USER" >> $RUN_FILE
		# chown files that our user lost access to when we gave them a new UID
		for DIR_TO_CHOWN in $DIRS_TO_CHOWN; do
			if [ -d "$DIR_TO_CHOWN" ]; then
				toLog "INFO ($LINENO): Will call chown -R --from=$PREVIOUS_UID $OUR_USER $DIR_TO_CHOWN"
				echo "chown -R --from=$PREVIOUS_UID $OUR_USER $DIR_TO_CHOWN" >> $RUN_FILE
			fi
		done
		#toLog "INFO ($LINENO): Will call chown --from=$PREVIOUS_UID $OUR_USER /var/spool/mail/$OUR_USER"
		#echo "chown --from=$PREVIOUS_UID $OUR_USER /var/spool/mail/$OUR_USER" >> $RUN_FILE
	done

	# change GIDs
	for GID_MAP_ENTRY in $GID_MAP; do
		local OUR_GROUP=`echo $GID_MAP_ENTRY | awk -F ':' '{ print $1 }'`
		local GID_TO_ASSIGN=`echo $GID_MAP_ENTRY | awk -F ':' '{ print $2 }'`
		local PREVIOUS_GID=`getent group $OUR_GROUP | awk -F ':' '{ print $3 }'`
		toLog "INFO ($LINENO): Will call groupmod -g $GID_TO_ASSIGN $OUR_GROUP"
		echo "groupmod -g $GID_TO_ASSIGN $OUR_GROUP" >> $RUN_FILE
		# chown files that our group lost access to when we gave them a new GID
		for DIR_TO_CHOWN in $DIRS_TO_CHOWN; do
			if [ -d "$DIR_TO_CHOWN" ]; then
				toLog "INFO ($LINENO): Will call chown -R --from=:$PREVIOUS_GID :$OUR_GROUP $DIR_TO_CHOWN"
				echo "chown -R --from=:$PREVIOUS_GID :$OUR_GROUP $DIR_TO_CHOWN" >> $RUN_FILE
			fi
		done
	done
	
	# assign new primary groups to users
	for PGM_ENTRY in $PRIMARY_GROUP_MAP; do
		local USER_TO_UPDATE=`echo $PGM_ENTRY | awk -F ':' '{ print $1 }'`
		local NEW_GROUP=`echo $PGM_ENTRY | awk -F ':' '{ print $2 }'`
		toLog "INFO ($LINENO): Will call usermod --gid $NEW_GROUP $USER_TO_UPDATE"
		echo "usermod --gid $NEW_GROUP $USER_TO_UPDATE" >> $RUN_FILE
	done

	createOwnershipLog after-change
	
	# startup our services
	if [ $SSG_RUNNING -eq 1 ]; then
		toLog 'Will call service ssg start'
		echo "service ssg start" >> $RUN_FILE
	fi
	if [ $DBSTATUS_RUNNING -eq 1 ]; then
		toLog 'Will call service ssg-dbstatus start'
		echo "service ssg-dbstatus start" >> $RUN_FILE
	fi	
	if [ $SSEM_RUNNING -eq 1 ]; then
		toLog 'Will call service ssem start'
		echo "service ssem start" >> $RUN_FILE
	fi
	toLog "INFO ($LINENO): Done creating run file"
}

promptBeforeRunFileExec() {
	toLog "INFO ($LINENO): The following commands will be run:"
	if [ "$VERBOSE" == "yes" ]; then
		cat "$RUN_FILE" 2>&1 | tee -a "$LOG_FILE"
	else
		cat "$RUN_FILE" 2>&1 >> "$LOG_FILE"
	fi
	if [ "$PROMPT_BEFORE_EXEC" == "no" ]; then
		toLog "DEBUG ($LINENO): Skipping prompt for run file exec"
		return
	fi
	confirmAction 'Proceed with running commands?'
	if [ "$confirmed" -eq "1" ]; then
		toLog "INFO ($LINENO): User has selected to not exec the run file"
		exit 1
	fi
}

execRunFile() {
	toLog "INFO ($LINENO): Executing run file. Output follows:"
	if [ "$VERBOSE" == "yes" ]; then
		exec "$RUN_FILE" 2>&1 | tee -a "$LOG_FILE"
	else
		exec "$RUN_FILE" 2>&1 >> "$LOG_FILE"
	fi
	
	toLog "INFO ($LINENO): Run file finished execution"
}

createOwnershipLog() {
	OWNERSHIP_LOG_FILE="$0-$TIMESTAMP-ownership-$1.log"
	touch "$OWNERSHIP_LOG_FILE"
	unset DIR_TO_CHOWN
	
	for DIR_TO_CHOWN in $DIRS_TO_CHOWN; do
		if [[ -d "$DIR_TO_CHOWN" ]]; then
			echo "find \"$DIR_TO_CHOWN\" -printf \"%u:%g:%p\n\" | sort >> \"$OWNERSHIP_LOG_FILE\"" >> $RUN_FILE
		fi
	done
}

verifySameOwnership() {
	local LOG1="$0-$TIMESTAMP-ownership-before-change.log"
	local LOG2="$0-$TIMESTAMP-ownership-after-change.log"
	diff "$LOG1" "$LOG2" > /dev/null
	if [ $? != 0 ]; then
		toLog "ERROR ($LINENO): File ownership was not preserved"
	else
		toLog "INFO ($LINENO): File ownership has been preserved"
	fi
}

# main code
getCommandLineOptions $@
verifyConfigurableValues
createMapsIfNeeded
createRunFile
promptBeforeRunFileExec
execRunFile
verifySameOwnership
toLog "INFO ($LINENO): Done"


