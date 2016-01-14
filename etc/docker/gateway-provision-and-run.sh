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

function logWarning() {
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
        SERVICE_CHECK="$2"
        COUNTER=0
        END=120

        logInfo "waiting for the $SERVICE_NAME to start up"

        ns=`eval $SERVICE_CHECK`
        while [ ! -n "$ns" -a $COUNTER -le $END ]; do
                logInfo "$SERVICE_NAME is not running yet."
                sleep 5
                ns=`eval $SERVICE_CHECK`
                COUNTER=$(($COUNTER+1))
                if [ $COUNTER -gt $END ]; then
                        logErrorAndExit "giving up waiting for the $SERVICE_NAME to start up."
                fi
        done
}

function doWaitForSSGStartUp() {
        doWaitForServiceStartUp "gateway" 'find /opt/SecureSpan/Gateway/node/default/var -name started -print'
}

function doWaitForProcessControllerStartUp() {
	doWaitForServiceStartUp "process controller" 'netstat -tnap | grep 8765 | grep LISTEN'
}

# set environment variables using Consul or etcd if configured to do so
function collectConfig() {
	if [ "$SKIP_CONFIG_SERVER_CHECK" == "true" ]; then
		logInfo "skipping check for Consul or etcd server"
		return
	fi
	
	logInfo "checking if a Consul server has been set in the SSG_CONSUL_IP environment variable"
	if [ "$SSG_CONSUL_IP" == "" ]; then
		logInfo "no Consul server set via the SSG_CONSUL_IP environment variable"
	else
		logInfo "Consul server is \"$SSG_CONSUL_IP\". Running envconsul"
		# we do a recursive call of this script using the exported environment from Consul
		# to prevent infinite recursion, we test if we've checked for Consul before
		export SKIP_CONFIG_SERVER_CHECK="true"
		# note that we assume that the Consul API is available on the standard port (8500)
		envconsul -consul "$SSG_CONSUL_IP:8500" -prefix com/ca/apim -sanitize -upcase -log-level info -once "$0"
		if [ $? -ne 0 ]; then
			logErrorAndExit "failed to retrieve config from Consul or a different error has occurred"
		else
			logInfo "Gateway has shutdown"
			exit 0
		fi
	fi
	
	logInfo "checking for an etcd server"
	# if etcd is available, use it to set environment variables
	# for more details, see the Endpoint and DNS Discovery sections of https://github.com/coreos/etcd/tree/release-2.2/etcdctl
	if [ "$ETCDCTL_ENDPOINT" == "" ] && [ "$ETCDCTL_DISCOVERY_SRV" == "" ]; then
		logInfo "no etcd server available (neither ETCDCTL_ENDPOINT nor ETCDCTL_DISCOVERY_SRV were set in the environment"
	else
		logInfo "trying to use etcd server (ETCDCTL_ENDPOINT=\"$ETCDCTL_ENDPOINT\" and ETCDCTL_DISCOVERY_SRV=\"$ETCDCTL_DISCOVERY_SRV\")"
		ETCD_KEYS=`etcdctl ls /com/ca/apim/` || logErrorAndExit "failed to retrieve keys from etcd server"
		for ETCD_KEY in `echo "$ETCD_KEYS"`; do
			ETCD_VALUE=`etcdctl get "$ETCD_KEY"` || logErrorAndExit "failed to retrieve value for key \"$ETCD_KEY\" from etcd server"
			NEW_VAR_NAME=`basename "$ETCD_KEY" | tr '[:lower:]' '[:upper:]'`
			declare "$NEW_VAR_NAME=$ETCD_VALUE"
			export "$NEW_VAR_NAME"
		done

		# recusively call this script so that the settings from etcd are available in the environment
		export SKIP_CONFIG_SERVER_CHECK="true"
		"$0"
	fi
}

function createBootstrapDir() {
	# create a bootstap directory if we need to
	if [ -d "$GATEWAY_BOOTSTRAP_DIR" ]; then
		logInfo "found gateway bootstrap dir at \"$GATEWAY_BOOTSTRAP_DIR\". Continuing."
	else
		logInfo "no gateway bootstrap dir found at \"$GATEWAY_BOOTSTRAP_DIR\". Creating it."
		mkdir "$GATEWAY_BOOTSTRAP_DIR" || logErrorAndExit "could not create directory \"$GATEWAY_BOOTSTRAP_DIR\""
		chmod 755 "$GATEWAY_BOOTSTRAP_DIR" || logErrorAndExit "could not chmod directory \"$GATEWAY_BOOTSTRAP_DIR\""
	fi
}

function putLicenseOnDisk() {
	# copy in the license file from the environment
	mkdir "$GATEWAY_LICENSE_DIR" || logErrorAndExit "could not create directory \"$GATEWAY_LICENSE_DIR\""
	chmod 755 "$GATEWAY_LICENSE_DIR" || logErrorAndExit "could not chmod directory \"$GATEWAY_LICENSE_DIR\""
	if [ "$SSG_LICENSE" != "" ]; then
		logInfo "license found in SSG_LICENSE environment variable. Writing to disk."
		echo "$SSG_LICENSE" | base64 -d -i | gunzip - > "$GATEWAY_LICENSE_FILE" || logErrorAndExit "could not decode license to disk. Make sure SSG_LICENSE is exported and the content is gzipped, then base64 encoded."
	elif [ -r "/mnt/ssgconfig/license.xml" ]; then
		logInfo "license found in /mnt/ssgconfig/license.xml. Copying."
		cp /mnt/ssgconfig/license.xml "$GATEWAY_LICENSE_FILE" || logErrorAndExit "could not copy license file from /mnt/ssgconfig/license.xml to \"$GATEWAY_LICENSE_FILE\""
	else
		logErrorAndExit "no license found. Exiting."
	fi
	chmod 644 "$GATEWAY_LICENSE_FILE" || logErrorAndExit "could not chmod file \"$GATEWAY_LICENSE_FILE\""
}

function createServicesFiles() {
	# create the services files
	if [ -d "$GATEWAY_SERVICES_DIR" ]; then
	        logInfo "found gateway services dir at \"$GATEWAY_SERVICES_DIR\". Continuing."
	else
	        logInfo "no gateway services dir found at \"$GATEWAY_SERVICES_DIR\". Creating it."
	        mkdir "$GATEWAY_SERVICES_DIR" || logErrorAndExit "could not create directory \"$GATEWAY_SERVICES_DIR\""
	        chmod 755 "$GATEWAY_SERVICES_DIR" || logErrorAndExit "could not chmod directory \"$GATEWAY_SERVICES_DIR\""
	fi
	for SSG_INTERNAL_SERVICE in `echo "$SSG_INTERNAL_SERVICES"`; do
		touch "$GATEWAY_SERVICES_DIR/$SSG_INTERNAL_SERVICE" &> /dev/null
		if [ $? -ne 0 ]; then
			logErrorAndExit "could not create gateway internal service file at \"$GATEWAY_SERVICES_DIR/$SSG_INTERNAL_SERVICE\""
		fi
	done
}

function startTheProcessController() {
	# start the process controller
	logInfo "starting the process controller"
	/etc/init.d/ssg start
	doWaitForProcessControllerStartUp
}

function waitForMySQLToBeReady() {
	# TODO: we probably need to replace this with a better way to wait for the MySQL instance to be ready for a connection
	logInfo "waiting for MySQL to become ready"
	sleep 5
}

function setCredsForMySQLClient() {
	# preserve the Admin DB creds in case we need them
	echo "[client]" > /root/.my.cnf
	echo "user=$SSG_DATABASE_ADMIN_USER" >> /root/.my.cnf
	echo "password=$SSG_DATABASE_ADMIN_PASS" >> /root/.my.cnf
	chmod 600 /root/.my.cnf
}

function generateGatewayConfig() {
CONFIGURE_DATABASE="$1"

SSG_HEADLESS_AUTOCONFIG=$(
cat <<ENDOFFILE
### Node Configuration ###
## Node Enabled State
node.enable=true
## Configure the node.properties
configure.node=true

## Should a database be created
configure.db=$CONFIGURE_DATABASE

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
# echo "DEBUG: SSG_HEADLESS_AUTOCONFIG=$SSG_HEADLESS_AUTOCONFIG"
}

function provisionAndStartGateway() {
	# run the gateway's headless autoconfiguration
	# we don't need to start the gateway as the headless autoconfig does this for us
	logInfo "running gateway's headless autoconfig"
	generateGatewayConfig "true"
	echo "$SSG_HEADLESS_AUTOCONFIG" | sudo -u layer7 "$SSGCONFIG_LAUNCH_PATH" -headless create
	if [ $? -ne 0 ]; then
		logErrorAndExit "gateway headless autoconfig failed"
	fi
	
	logInfo "gateway is now starting up"
	doWaitForSSGStartUp
}

function waitForGatewayShutdownFile() {
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
}

#### MAIN ####

collectConfig
createBootstrapDir
putLicenseOnDisk
createServicesFiles
startTheProcessController
waitForMySQLToBeReady
setCredsForMySQLClient
provisionAndStartGateway
waitForGatewayShutdownFile

