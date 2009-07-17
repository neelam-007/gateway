#!/bin/sh
#
# Script to restart replication from current point on master
#
# Call with -v for verbose output
#
# Note: You *MUST* configure REPLICATION CLIENT privilege on the master:
#
# GRANT REPLICATION CLIENT ON *.* TO 'DBUSER'@'SLAVE' IDENTIFIED BY 'DBPWD';
#
# Where:
#   SLAVE is the IP address or host name of the slave system where
#     this script is installed
#   DBUSER is the database user configured below
#   DBPWD is the database password configured below
#
# That is easiest to do with the add_slave_user.sh script
#
# Jay MacDonald - v1 - 20080201
# Jay MacDonald - v2 - 20080214

################################################# Start configurable settings

# Set up the defaults
MASTER="SET ME"
DBUSER="repluser"
DBPWD="replpass"
ROOT="root"
ROOT_PWD=""

################################################# End configurable settings

clean_up() {
	rm -f /tmp/mb_*.$$
        exit $1
}

verbose() {
	if test $VERBOSE == yes  ; then
		echo "--> $1"
	fi
}

mysql_fail() {
	cat <<-EOM

		==> $1

		Message: `cat /tmp/mb_error.$$`

		Refer to http://dev.mysql.com/doc/refman/5.0/en/error-messages-client.html
		for more information.

	EOM
	clean_up 1
}

evaluate_result() {
	eval `echo "$1" | \
		sed 's/^  *//' | \
		sed 's/: \(.*\)/="\1"/' | \
		grep -v '=""$' | \
		grep '='`
}

# Check if -v set
if test "$1" == "-v" ; then
	VERBOSE="yes"
else
	VERBOSE="no"
fi

################
# Get the settings

echo -n "Enter hostname or IP for the MASTER: [$MASTER] "
read -e
if test $REPLY ; then MASTER=$REPLY ; fi

echo -n "Enter replication user: [$DBUSER] "
read -e
if test $REPLY ; then DBUSER=$REPLY ; fi

echo -n "Enter replication password: [$DBPWD] "
read -e
if test $REPLY ; then DBPWD=$REPLY ; fi

echo -n "Enter MySQL root user: [$ROOT] "
read -e
if test $REPLY ; then ROOT=$REPLY ; fi

echo -n "Enter MySQL root password: [$ROOT_PWD] "
read -e
if test $REPLY ; then ROOT_PWD=$REPLY ; fi

# Massage the user and password args for mysql on slave.
if test $ROOT_PWD ; then ROOT_PWD="-p$ROOT_PWD" ; fi
if test $ROOT ; then ROOT="-u$ROOT" ; fi

# Set commands for slave and master mysql calls
SLAVE_MYSQL="mysql $ROOT $ROOT_PWD"
MASTER_MYSQL="mysql -h$MASTER -u$DBUSER -p$DBPWD"

# Initialize values
File=""
Position=0
Slave_IO_Running="No"
Slave_SQL_Running="No"

# Query the MASTER STATUS from the master database
CMD="SHOW MASTER STATUS\G"
RESULT=`$MASTER_MYSQL -e "$CMD" 2>/tmp/mb_error.$$`

if test $? -ne 0 ; then
	mysql_fail "Error getting master status"
fi

# Get the useful information from SHOW MASTER STATUS\G and convert to variables
evaluate_result "$RESULT"

verbose "File = $File"
verbose "Position = $Position"

# Set the master settings, confirm, start slave, confirm
if test -n "$File" -a $Position -gt 0 ; then

	# Stop the slave
	verbose "Stopping slave"
	CMD="STOP SLAVE\G"
	RESULT=`$SLAVE_MYSQL -e "$CMD\G" 2>/tmp/mb_error.$$`

	if test $? -ne 0; then
		mysql_fail "Error stopping slave"
	fi

	# Change the MASTER settings in slave
	verbose "Changing MASTER settings"
	CMD="CHANGE MASTER TO MASTER_HOST='$MASTER',
		MASTER_USER='$DBUSER',
		MASTER_PASSWORD='$DBPWD',
		MASTER_PORT=3307,
		MASTER_CONNECT_RETRY=10,
		MASTER_LOG_FILE='$File',
		MASTER_LOG_POS=$Position;"

	RESULT=`$SLAVE_MYSQL -e "$CMD" 2>/tmp/mb_error.$$`

	if test $? -ne 0; then
		mysql_fail "Error changing master settings"
	fi

	# Start the slave
	verbose "Starting slave"
	CMD="START SLAVE\G"
	RESULT=`$SLAVE_MYSQL -e "$CMD\G" 2>/tmp/mb_error.$$`

	if test $? -ne 0; then
		mysql_fail "Error starting slave"
	fi

	# Give the slave a chance to start
	sleep 1

	# Confirm the slave settings on slave
	verbose "Confirming slave startup"
	CMD="SHOW SLAVE STATUS\G"
	RESULT=`$SLAVE_MYSQL -e "$CMD\G" 2>/tmp/mb_error.$$`

	if test $? -ne 0; then
		mysql_fail "Error querying slave status"
	fi

	evaluate_result "$RESULT"

	verbose "Slave_IO_Running = $Slave_IO_Running"
	verbose "Slave_SQL_Running = $Slave_SQL_Running"

	if test $Slave_IO_Running == "Yes" -a $Slave_SQL_Running == "Yes" ; then
		echo "Slave successfully started"
		clean_up 0
	else
		cat <<-EOM

			==> Error: Slave not started

			Confirm your settings in $0

			Result of SLAVE STATUS:

			$RESULT

		EOM

		clean_up 1
	fi
else
	cat <<-EOM

		==> Error: File or Position not set

	EOM
	clean_up 1
fi
