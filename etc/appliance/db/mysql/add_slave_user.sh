#!/bin/sh
#
# Script to add slave user to database
#
# Call with user, host and password at command line
#
# e.g. add_slave_user.sh repluser ssg1.l7tech.com replpass
#

################################################# Start configurable settings

# Set the defaults up
DBUSER="repluser"
DBPWD="replpass"
ROOT="root"
ROOT_PWD=""

################################################# End configurable settings

###############################################
# Clean up temporary files end exit with status

clean_up() {
	rm -f /tmp/mb_*.$$
	exit $1
}

##################################################
# Message to display whenever a MySQL error occurs

mysql_fail() {
	cat <<-EOM

		==> $1

		Message: `cat /tmp/mb_error.$$`

		Refer to http://dev.mysql.com/doc/refman/5.0/en/error-messages-client.html
		for more information.

	EOM
	clean_up 1
}

##########################################################
# Confirm that MySQL is properly configured to be a master

confirm_master() {
	echo ""
	echo "Checking configuration of running MySQL..."

	CMD="SHOW VARIABLES LIKE 'log_%' ; SHOW VARIABLES LIKE 'server_id'"
	RESULT=`$MYSQL -e "$CMD" 2>/tmp/mb_error.$$ | grep -v 'Variable_name'`

	if test $? -ne 0 ; then
		mysql_fail "Error getting variables"
	fi

	# Evaluate the response into variables
	evaluate_result "$RESULT"

	if test "$server_id" == "0" \
		-o "$log_bin" == "OFF" \
		-o "$log_slave_update" == "OFF" ; then

		cat <<-EOM
			==> MySQL is not configured for replication

		    	server_id = $server_id
		    	log_bin = $log_bin
		    	log_slave_updates = $log_slave_updates

		EOM
	
		return 1
	else
		return 0
	fi
}

#################################################
# Configure MySQL to be a master node and restart

configure_my_cnf() {
	while test -z $DB_NODE_ID ; do
		echo -n "Is this the Primary (1) or Secondary (2) database node? "
		read -e DB_NODE_ID
		case $DB_NODE_ID in
			1 )	echo -n "  --> Setting as Primary DB node... "
				;;

			2 )	echo -n "  --> Setting as Secondary DB node... "
				;;

        		* )	echo "Please enter 1 or 2"
				;;
		esac
	done

	sed -in -e "s/^#\(server-id=$DB_NODE_ID\)/\1/g" \
		-e 's/^#log-bin/log-bin/g' \
		-e 's/^#log-slave-update/log-slave-update/g' /etc/my.cnf

	if test $? -ne 0 ; then
		echo "Error updating /etc/my.cnf"
		clean_up 1
	else
		echo "OK"
	fi

	echo "Restarting the database"
	service mysqld restart
}

######################################################
# Evaluate the result from a MySQL call into variables

evaluate_result() {
	eval `echo "$1" | \
		sed 's/\t\(.*\)/=\1/'`
}

#################
# Check if -v set

if test "$1" == "-v" ; then
	VERBOSE="yes"
else
	VERBOSE="no"
	echo "--> For verbose output run with -v"
	echo ""
fi

##################
# Get the settings

echo
echo "Gathering information for SLAVE user"
while test -z $SLAVE ; do
	echo -n "Enter hostname or IP for the SLAVE: "
	read -e SLAVE

	if test -z $SLAVE ; then
        	echo "You must enter a SLAVE"
	fi
done

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

# Massage the user and password args for mysql
if test $ROOT_PWD ; then ROOT_PWD="-p$ROOT_PWD" ; fi
if test $ROOT ; then ROOT="-u$ROOT" ; fi

# Set commands for mysql call
MYSQL="mysql $ROOT $ROOT_PWD"

############################
# Confirm we can be a MASTER

confirm_master

while test $? -ne 0 ; do
	configure_my_cnf
	sleep 1
	confirm_master
done

echo "MySQL appears to be properly configured with server_id=$server_id"
echo -n "Do you want to continue? [Y] "
read -e

if test -z $REPLY ; then
	REPLY="Y"
fi

if test $REPLY != "y" -a $REPLY != "Y" -a $REPLY != "yes" ; then
	clean_up 0
fi


#############################
# Grant the slave permissions

echo "Granting slave permissions to $DBUSER@$SLAVE"

# Run it from a temp file to get around bash expansion
echo "GRANT REPLICATION SLAVE, REPLICATION CLIENT, SELECT, LOCK TABLES, RELOAD ON *.* TO '$DBUSER'@'$SLAVE' IDENTIFIED BY '$DBPWD';" > /tmp/mb_cmd.$$

RESULT=`$MYSQL < /tmp/mb_cmd.$$ 2>/tmp/mb_error.$$`

if test $? -ne 0 ; then
        mysql_fail "Error granting permissions"
fi

echo "Done."

clean_up 0
