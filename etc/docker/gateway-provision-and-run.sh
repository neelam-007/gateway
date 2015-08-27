#!/bin/bash

SYSLOG_TAG='apim-provisioning'

USERDATA_DIR='/userdata'
USERDATA_SHUTDOWN_FILE="$USERDATA_DIR/shutdown"

GATEWAY_DIR='/opt/SecureSpan/Gateway'
GATEWAY_BOOTSTRAP_DIR="$GATEWAY_DIR/node/default/etc/bootstrap"
GATEWAY_LICENSE_DIR="$GATEWAY_BOOTSTRAP_DIR/license"
GATEWAY_LICENSE_FILE="$GATEWAY_LICENSE_DIR/license.xml"
GATEWAY_SERVICES_DIR="$GATEWAY_BOOTSTRAP_DIR/services"

APPLIANCE_DIR='/opt/SecureSpan/Appliance'
SSGCONFIG_LAUNCH_PATH="$APPLIANCE_DIR/libexec/ssgconfig_launch"

function log() {
	local MESSAGE_PRIORITY="$1"
	local MESSAGE="$2"

	logger -s -t "$SYSLOG_TAG" -p "$MESSAGE_PRIORITY" "$MESSAGE"
}

function logError() {
	log user.error "ERROR: $1"
}

function logInfo() {
	log user.warning "WARNING: $1"
}

function logInfo() {
	log user.info "INFO: $1"
}

function logErrorAndExit() {
	logError "$1"
	exit 1
}

function doWaitForServiceStartUp() {
        SERVICE_NAME="$1"
        SERVICE_PORT="$2"
        COUNTER=0
        END=120

        logInfo "waiting for the $SERVICE_NAME to start up"

        ns=`netstat -tnap | grep "$SERVICE_PORT" | grep LISTEN`
        while [ ! -n "$ns" -a $COUNTER -le $END ]; do
                logInfo "$SERVICE_NAME is not running yet."
                sleep 5
                ns=`netstat -tnap | grep "$SERVICE_PORT" | grep LISTEN`
                COUNTER=$(($COUNTER+1))
                if [ $COUNTER -gt $END ]; then
                        logErrorAndExit "giving up waiting for the $SERVICE_NAME to start up."
                fi
        done
}

function doWaitForSSGStartUp() {
        doWaitForServiceStartUp "gateway" "8443"
}

function doWaitForProcessControllerStartUp() {
        doWaitForServiceStartUp "process controller" "8765"
}


# create a bootstap directory if we need to
if [ -d "$GATEWAY_BOOTSTRAP_DIR" ]; then
	logInfo "found gateway bootstrap dir at \"$GATEWAY_BOOTSTRAP_DIR\". Continuing."
else
	logInfo "no gateway bootstrap dir found at \"$GATEWAY_BOOTSTRAP_DIR\". Creating it."
	mkdir "$GATEWAY_BOOTSTRAP_DIR" || logErrorAndExit "could not create directory \"$GATEWAY_BOOTSTRAP_DIR\""
	chmod 755 "$GATEWAY_BOOTSTRAP_DIR" || logErrorAndExit "could not chmod directory \"$GATEWAY_BOOTSTRAP_DIR\""
fi

# copy in the license file from the environment
if [ "$SSG_LICENSE" == "" ]; then
	logErrorAndExit "no license set via SSG_LICENSE in the environment. Exiting."
else
	logInfo "license found. Writing to disk."
	mkdir "$GATEWAY_LICENSE_DIR" || logErrorAndExit "could not create directory \"$GATEWAY_LICENSE_DIR\""
	chmod 755 "$GATEWAY_LICENSE_DIR" || logErrorAndExit "could not chmod directory \"$GATEWAY_LICENSE_DIR\""
	echo "$SSG_LICENSE" | base64 -d -i | gunzip - > "$GATEWAY_LICENSE_FILE" || logErrorAndExit "could not decode license to disk. Make sure SSG_LICENSE is exported and the content is gzipped, then base64 encoded."
	chmod 644 "$GATEWAY_LICENSE_FILE" || logErrorAndExit "could not chmod file \"$GATEWAY_LICENSE_FILE\""
fi

# create the services files
if [ -d "$GATEWAY_SERVICES_DIR" ]; then
        logInfo "found gateway services dir at \"$GATEWAY_SERVICES_DIR\". Continuing."
else
        logInfo "no gateway services dir found at \"$GATEWAY_SERVICES_DIR\". Creating it."
        mkdir "$GATEWAY_SERVICES_DIR" || logErrorAndExit "could not create directory \"$GATEWAY_SERVICES_DIR\""
        chmod 755 "$GATEWAY_SERVICES_DIR" || logErrorAndExit "could not chmod directory \"$GATEWAY_SERVICES_DIR\""
fi
logInfo "DEBUG: SSG_INTERNAL_SERVICES=\"$SSG_INTERNAL_SERVICES\""
for SSG_INTERNAL_SERVICE in `echo "$SSG_INTERNAL_SERVICES"`; do
	touch "$GATEWAY_SERVICES_DIR/$SSG_INTERNAL_SERVICE" &> /dev/null
	if [ $? -ne 0 ]; then
		logErrorAndExit "could not create gateway internal service file at \"$GATEWAY_SERVICES_DIR/$SSG_INTERNAL_SERVICE\""
	fi
done

# start the process controller
logInfo "starting the process controller"
/etc/init.d/ssg start
doWaitForProcessControllerStartUp

# TODO: we probably need to replace this with a better way to wait for the MySQL instance to be ready for a connection
echo "WARNING! TEMP WORKAROUND: waiting for MySQL to become ready"
sleep 5

# run the gateway's headless autoconfiguration
# we don't need to start the gateway as the headless autoconfig does this for us
logInfo "running gateway's headless autoconfig"
# create-db
SSG_HEADLESS_AUTOCONFIG=$(
cat <<ENDOFFILE
### Node Configuration ###
## Node Enabled State
node.enable=true
## Configure the node.properties
configure.node=true

### Cluster Configuration ###
## Cluster Hostname
cluster.host=$SSG_CLUSTER_HOST
## Cluster Passphrase
cluster.pass=$SSG_CLUSTER_PASSWORD

### Database Connection ###
## The database type, either 'mysql' or 'embedded'
database.type=$SSG_DATABASE_TYPE
## Database Hostname
database.host=$SSG_DATABASE_HOST
## Database Port
database.port=$SSG_DATABASE_PORT
## Database Name
database.name=$SSG_DATABASE_NAME
## Database Username
database.user=$SSG_DATABASE_USER
## Database Password
database.pass=$SSG_DATABASE_PASSWORD
## Administrative Database Username
database.admin.user=$SSG_DATABASE_ADMIN_USER
## Administrative Database Password
database.admin.pass=$SSG_DATABASE_ADMIN_PASS

### SSM Administrative Account ###
## SSM Username
admin.user=$SSG_ADMIN_USER
## SSM Password
admin.pass=$SSG_ADMIN_PASS
.
ENDOFFILE
)
echo "SSG_HEADLESS_AUTOCONFIG=$SSG_HEADLESS_AUTOCONFIG"

#echo "$SSG_HEADLESS_AUTOCONFIG" | sudo -u layer7 "$SSGCONFIG_LAUNCH_PATH" headless appliance-full
(echo "create-db"; echo "$SSG_HEADLESS_AUTOCONFIG") | sudo -u layer7 "$SSGCONFIG_LAUNCH_PATH" -headless create
if [ $? -ne 0 ]; then
	logInfo "gateway headless autoconfig failed. Perhaps the database already exists. Retrying without the database creation."
	echo "$SSG_HEADLESS_AUTOCONFIG" | sudo -u layer7 "$SSGCONFIG_LAUNCH_PATH" -headless create
	if [ $? -ne 0 ]; then
		logErrorAndExit "gateway headless autoconfig failed. Exiting."
	fi
fi

logInfo "gateway is now starting up"
doWaitForSSGStartUp

# wait for shutdown file
logInfo "waiting for the shutdown file at \"$USERDATA_SHUTDOWN_FILE\" to be created"
while ( /bin/true ); do
	if [ -f "$USERDATA_SHUTDOWN_FILE" ]; then
		logInfo "found shutdown file"
		break;
	fi
	sleep 1
done

# stop the gateway, if needed
if ( ps -ef | grep java | grep Controller.jar ); then
	logInfo "stopping the gateway"
	/etc/init.d/ssg stop
	logInfo "waiting for the gateway to stop"
	while (/bin/true); do
		if ( ps -ef | grep java | grep Controller.jar ); then
			sleep 1
		else
			logInfo "gateway has stopped"
			break
		fi
	done
else
	logInfo "did not find a gateway to stop"
fi
