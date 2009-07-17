#!/bin/sh
#
# Script to automatically configure a slave system
#
# Call with -v for verbose output
#
# Note: You *MUST* configure MASTER, DBUSER, DBPWD and DB below
#
# Note: You *MUST* configure SELECT, LOCK TABLES and RELOAD privilege on the master:
#
# GRANT SELECT, LOCK TABLES, RELOAD ON *.* TO 'DBUSER'@'SLAVE' IDENTIFIED BY 'DBPWD';
#
# Where:
#   SLAVE is the IP address or host name of the slave system where this script is installed
#   DBUSER is the database user configured below
#   DBPWD is the database password configured below
#
# Jay MacDonald - v1 - 20080207
# Jay MacDonald - v2 - 20080214

################################################# Start configurable settings

# Set the defaults up
DBUSER="repluser"
DBPWD="replpass"
ROOT="root"
ROOT_PWD=""

CLONE_DB="no"
DB="ssg"

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
	echo "--> For verbose output run with -v"
	echo ""
fi

################
# Get the settings

echo -n "Enter hostname or IP for the MASTER: "
read -e MASTER

if test -z $MASTER ; then
	echo "You must enter a MASTER"
	clean_up 1
fi

echo -n "Enter replication user: [$DBUSER] "
read -e
if test $REPLY ; then DBUSER=$REPLY ; fi

echo -n "Enter replication password: [$DBPWD] "
read -s -e
if test $REPLY ; then DBPWD=$REPLY ; fi
echo ""

echo -n "Enter MySQL root user: [$ROOT] "
read -e
if test $REPLY ; then ROOT=$REPLY ; fi

echo -n "Enter MySQL root password: [$ROOT_PWD] "
read -s -e
if test $REPLY ; then ROOT_PWD=$REPLY ; fi
echo ""

echo -n "Do you want to clone a database from $MASTER (yes or no)? [no] "
read -e
if test $REPLY ; then CLONE_DB=$REPLY ; fi

if test "$CLONE_DB" != "yes" -a "$CLONE_DB" != "no" ; then
	echo "Must answer 'yes' or 'no' (type whole word)"
	clean_up 1
fi

if test "$CLONE_DB" == "yes" ; then
	echo -n "Enter name of database to clone: [$DB] "
	read -e 
	if test $REPLY ; then DB=$REPLY ; fi
fi

verbose "MASTER = $MASTER"
verbose "DBUSER = $DBUSER"
verbose "DBPWD = $DBPWD"
verbose "ROOT = $ROOT"
verbose "ROOT_PWD = $ROOT_PWD"
verbose "CLONE_DB = $CLONE_DB"
verbose "DB = $DB"

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

################
# Set the master configuration

# Stop the slave
verbose "Stopping slave"
CMD="STOP SLAVE\G"
RESULT=`$SLAVE_MYSQL -e "$CMD\G" 2>/tmp/mb_error.$$`

if test $? -ne 0; then
	mysql_fail "Error stopping slave"
fi

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

# Change the MASTER settings in slave
verbose "Changing MASTER settings"
CMD="CHANGE MASTER TO MASTER_HOST='$MASTER',
	MASTER_USER='$DBUSER',
	MASTER_PASSWORD='$DBPWD',
	MASTER_PORT=3307,
	MASTER_CONNECT_RETRY=100,
	MASTER_LOG_FILE='$File',
	MASTER_LOG_POS=$Position;"

RESULT=`$SLAVE_MYSQL -e "$CMD" 2>/tmp/mb_error.$$`

if test $? -ne 0; then
	mysql_fail "Error changing master settings"
fi

################
# Clone the database if requested

if test "$CLONE_DB" == "yes" ; then

	################
	# Confirm slave is not running on Master to ensure that changes we make here
	# don't blow up there. This assumes we are pulling ourselves into a
	# MASTER-MASTER replication scheme. If it is not a MASTER-MASTER then this
	# will require manual intervention.

	verbose "Confirming slave not running on $MASTER"
	CMD="SHOW SLAVE STATUS\G"
	RESULT=`$MASTER_MYSQL -e "$CMD" 2>/tmp/mb_error.$$`

	if test $? -ne 0 ; then
       		mysql_fail "Error getting master's slave status"
	fi

	# Evaluate the response into variables
	evaluate_result "$RESULT"

	verbose "Slave_IO_Running = $Slave_IO_Running"
	verbose "Slave_SQL_Running = $Slave_SQL_Running"
	verbose "Master_Host = $Master_Host"

	if test $Slave_IO_Running == "Yes" -a $Slave_SQL_Running == "Yes" ; then
		echo "Error: Slave is operating on $MASTER."
		echo "       Please shut it down before running this script to prevent unintended"
		echo "       alteration of master database (mysqladmin stop-slave)"
		clean_up 1
	fi

	################
	# Check if the database already exists and drop if so
	CMD="SHOW DATABASES"
	RESULT=`$SLAVE_MYSQL -e "$CMD" 2>/tmp/mb_error.$$ | grep "^${DB}$"`

	if test $RESULT ; then
		# Database exists, so drop it
		# This is a drastic procedure. Confirm before doing anything...
		echo ""
		echo "W A R N I N G"
		echo "  About to drop the $DB database on localhost"
		echo "  and copy from $MASTER"
		echo ""
		echo -n "Are you sure you want to do this? [N] "
		read -e

		if test -z $REPLY ; then
			REPLY="N"
		fi

		if test $REPLY != "y" -a $REPLY != "Y" -a $REPLY != "yes" ; then
			clean_up 0
		fi


		################
		# Drop the database

		verbose "Dropping database"
		CMD="DROP DATABASE $DB"
		RESULT=`$SLAVE_MYSQL -e "$CMD" 2>/tmp/mb_error.$$`

		if test $? -ne 0 ; then
        		mysql_fail "Error dropping database"
		fi
	fi

	################
	# Create the database

	verbose "Creating database: $DB"
	CMD="CREATE DATABASE $DB"
	RESULT=`$SLAVE_MYSQL -e "$CMD" 2>/tmp/mb_error.$$`

	if test $? -ne 0 ; then
        	mysql_fail "Error creating database"
	fi

	################
	# pipe in the database

	verbose "Copying database from $MASTER"
	mysqldump -h $MASTER -u $DBUSER -p$DBPWD --master-data=1 $DB | $SLAVE_MYSQL $DB
fi

################
# Start the slave

verbose "Starting slave"
CMD="START SLAVE\G"
RESULT=`$SLAVE_MYSQL -e "$CMD\G" 2>/tmp/mb_error.$$`

if test $? -ne 0; then
	mysql_fail "Error starting slave"
fi

# Give the slave a chance to start
sleep 1

################
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
	echo "Slave successfully created"
	echo "Manually confirm that slave is running on $MASTER"
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
