#!/bin/bash
#

. etc/defs.sh

BASE_DIR=`pwd`

setEnvironmentVariables() {
  if [ -z "${PATH}" ] ; then
    PATH="$JAVA_HOME/bin:$ANT_HOME:$ANT_HOME/bin"
  else
    if ! echo $PATH | /bin/egrep -q "(^|:)$JAVA_HOME/bin($|:)" ; then
      PATH="$JAVA_HOME/bin:$PATH"
    fi

    if ! echo $PATH | /bin/egrep -q "(^|:)$ANT_HOME($|:)" ; then
      PATH="$ANT_HOME:$PATH"
    fi

    if ! echo $PATH | /bin/egrep -q "(^|:)$ANT_HOME/bin($|:)" ; then
      PATH="$ANT_HOME/bin:$PATH"
    fi
  fi
}

checkNewBuildDir() {
  # Check for the required builds
  local count
  count="`ls -1 $NEW_BUILD_DIR/Manager-*.tar.gz 2> /dev/null | wc -l`"
  if [ "$count" -lt "1" ]; then
    echo "Manager build was not found."
    exit 1
  elif [ "$count" -gt "1" ]; then
    echo "Multiple manager builds were found in the $NEW_BUILD_DIR directory."
    exit 1
  fi

  count="`ls -1 $NEW_BUILD_DIR/Client-*.tar.gz 2> /dev/null | wc -l`"
  if [ "$count" -lt "1" ]; then
    echo "Client build was not found."
    exit 1
  elif [ "$count" -gt "1" ]; then
    echo "Multiple client builds were found in the $NEW_BUILD_DIR directory."
    exit 1
  fi

  count="`ls -1 $NEW_BUILD_DIR/ssg-*.noarch.rpm 2> /dev/null | wc -l`"
  if [ "$count" -lt "1" ]; then
    echo "SSG RPM was not found."
    exit 1
  elif [ "$count" -gt "1" ]; then
    echo "Multiple SSG RPMs were found in the $NEW_BUILD_DIR directory."
    exit 1
  fi

  local arch
  arch=`uname -i`
  count="`ls -1 $NEW_BUILD_DIR/ssg-appliance-*.$arch.rpm 2> /dev/null | wc -l`"
  if [ "$count" -lt "1" ]; then
    echo "SSG Appliance RPM was not found."
    exit 1
  elif [ "$count" -gt "1" ]; then
    echo "Multiple SSG Appliance RPMs were found in the $NEW_BUILD_DIR directory."
    exit 1
  fi
}

checkSoftware() {
  # Check for the required software
  if [ ! -d "$AUTOTEST_DIR" ]; then
    echo "AutoTest directory ($AUTOTEST) wasn't found."
    exit 1
  fi

  if [ ! -e "$SUPPORT_FILES_DIR/rpms/$TARARI_RPM" ]; then
    echo "Tarari RPM $SUPPORT_FILES_DIR/rpms/$TARARI_RPM wasn't found."
    exit 1
  fi

  if [ ! -e "$SUPPORT_FILES_DIR/rpms/$SITEMINDER_RPM" ]; then
    echo "SiteMinder RPM $SUPPORT_FILES_DIR/rpms/$SITEMINDER_RPM wasn't found."
    exit 1
  fi

  if [ ! -e "$SUPPORT_FILES_DIR/rpms/$SYMANTEC_RPM" ]; then
    echo "Symantic RPM $SUPPORT_FILES_DIR/rpms/$SYMANTEC_RPM wasn't found."
    exit 1
  fi

  if [ ! -e "$SUPPORT_FILES_DIR/rpms/$JSAM_RPM" ]; then
    echo "JSAM RPM $SUPPORT_FILES_DIR/rpms/$JSAM_RPM wasn't found."
    exit 1
  fi
}

checkServer() {
  nc -z "$1" "$2"
  if [ $? -ne 0 ]; then
    echo "Required AutoTest server not available: $1:$2. Continuing with out server. Dependent tests will fail."
    #exit 1
  fi
}

checkServers() {
  checkServer qatibcomq 7222 # JMS tests
  checkServer qatibcomqssl 7222
  checkServer qatibcomqssl 7243
  checkServer hugh 80 # Various servers
  #checkServer redroom.l7tech.com 443 # TFIM tests
  checkServer 192.168.1.140 80 # soran
  checkServer 192.168.1.49 8080 # BRA SSG VMWare 
  checkServer 192.168.1.49 8443 # BRA SSG VMWare 
  checkServer qavsftp 21 # FTP routing tests
  checkServer qaopenjms 21 # FTP routing
  checkServer 192.168.1.89 44442 # SiteMinder 6
  checkServer qawebspheremq 7777 # Symantec AV
}

stopSSG() {
  # Stop the currently running SSG if there is one running
  if [ -e "/ssg/etc/conf/partitions/default_/ssg.pid" ]; then
    if [ -d "/proc/`cat /ssg/etc/conf/partitions/default_/ssg.pid`" ]; then
      kill -9 `cat /ssg/etc/conf/partitions/default_/ssg.pid`
    else
      rm "/ssg/etc/conf/partitions/default_/ssg.pid"
    fi
  fi
}

uninstallSoftware() {
  # Uninstall the current build
  local result
  result=`rpm -q ssg-jsam`
  if [ "$result" != "package ssg-jsam is not installed" ]; then
    rpm -e ssg-jsam
  fi
  result=`rpm -q ssg-symantec`
  if [ "$result" != "package ssg-symantec is not installed" ]; then
    rpm -e ssg-symantec
  fi
  result=`rpm -q ssg-sm6`
  if [ "$result" != "package ssg-sm6 is not installed" ]; then
    rpm -e ssg-sm6
  fi
  result=`rpm -q ssg-tarari`
  if [ "$result" != "package ssg-tarari is not installed" ]; then
    rpm -e ssg-tarari
  fi
  result=`rpm -q ssg-appliance`
  if [ "$result" != "package ssg-appliance is not installed" ]; then
    rpm -e ssg ssg-appliance
  fi
  result=`rpm -q ssg`
  if [ "$result" != "package ssg is not installed" ]; then
    rpm -e ssg
  fi
  
  if [ "1" -le "`ls -l Manager-* 2> /dev/null | wc -l`" ]; then
    rm -rf Manager-*
  fi
  if [ "1" -le "`ls -l Client-* 2> /dev/null | wc -l`" ]; then
    rm -rf Client-*
  fi

  rm -f /ssg/configwizard/*.zip
  rm -f /ssg/etc/conf/*.rpmsave
}

installNewBuild() {
  # Install the new build
  local arch
  arch=`uname -i`
  rpm -ivh --nodeps $NEW_BUILD_DIR/ssg-*.noarch.rpm
  rpm -ivh --nodeps $NEW_BUILD_DIR/ssg-appliance-*.$arch.rpm
  rpm -ivh --nodeps "$SUPPORT_FILES_DIR/rpms/$TARARI_RPM"
  rpm -ivh --nodeps "$SUPPORT_FILES_DIR/rpms/$SITEMINDER_RPM"
  rpm -ivh --nodeps "$SUPPORT_FILES_DIR/rpms/$SYMANTEC_RPM"
  rpm -ivh --nodeps "$SUPPORT_FILES_DIR/rpms/$JSAM_RPM"
  if [ -d jars ]; then
    cp jars/*.jar /ssg/lib/ext
  fi
  cp AutoTest/ssglib/TestCustomAssertion.jar /ssg/modules/lib

  # Configure SiteMinder
  sed -i -e 's/^policy\.server\.address\s*=.*/policy.server.address = 192.168.1.89/' /ssg/etc/conf/sm_agent.properties
  sed -i -e 's/^agent\.name\s*=.*/agent.name     = layer7-agent/' /ssg/etc/conf/sm_agent.properties
  sed -i -e 's/^agent\.secret\s*=.*/agent.secret   = 7layer/' /ssg/etc/conf/sm_agent.properties

  # Configure Symantec
  sed -i -e 's/^savse\.scanner\.hostname=.*/savse.scanner.hostname=qawebspheremq/' /ssg/etc/conf/symantec_scanengine_client.properties

  # Configure JSAM
  sed -i -e 's/^com\.iplanet\.services\.debug\.level=.*/com.iplanet.services.debug.level=off/' /ssg/etc/conf/sun-jsam-client.properties
  sed -i -e 's/^com\.iplanet\.services\.debug\.directory=.*/com.iplanet.services.debug.directory=\/ssg\/logs\/sun_jsam_client/' /ssg/etc/conf/sun-jsam-client.properties
  sed -i -e 's/^com\.iplanet\.am\.naming\.url=.*/com.iplanet.am.naming.url=http:\/\/jsam71-release.l7tech.com:8080\/amserver\/namingservice/' /ssg/etc/conf/sun-jsam-client.properties
  sed -i -e 's/^com\.iplanet\.am\.notification\.url=.*/com.iplanet.am.notification.url=http:\/\/jsam71-release.l7tech.com:8080\/amserver\/notificationservice/' /ssg/etc/conf/sun-jsam-client.properties
  sed -i -e 's/^com\.sun\.identity\.agents\.app\.username=.*/com.sun.identity.agents.app.username=agent007/' /ssg/etc/conf/sun-jsam-client.properties
  sed -i -e 's/^com\.iplanet\.am\.service\.password=.*/com.iplanet.am.service.password=password/' /ssg/etc/conf/sun-jsam-client.properties
  sed -i -e 's/^com\.iplanet\.am\.server\.protocol=.*/com.iplanet.am.server.protocol=http/' /ssg/etc/conf/sun-jsam-client.properties
  sed -i -e 's/^com\.iplanet\.am\.server\.host=.*/com.iplanet.am.server.host=jsam71-release.l7tech.com/' /ssg/etc/conf/sun-jsam-client.properties
  sed -i -e 's/^com\.iplanet\.am\.server\.port=.*/com.iplanet.am.server.port=8080/' /ssg/etc/conf/sun-jsam-client.properties

  tar -xzf $NEW_BUILD_DIR/Manager-*.tar.gz -C .
  MANAGER_DIR="$PWD/"`ls -1d Manager-* | tail -1`
  tar -xzf $NEW_BUILD_DIR/Client-*.tar.gz -C .
  CLIENT_DIR="$PWD/"`ls -1d Client-* | tail -1`
}

configurePartition() {
  ant runConfigWizardAutomator
  if [ "$?" != "0" ]; then
    return 1
  fi
  cp -f etc/keys/* /ssg/etc/conf/partitions/default_/keys/
  chown gateway:gateway /ssg/etc/conf/partitions/default_/keys/*
  return 0
}

installDB() {

  mysqladmin -u $MYSQL_ROOT_USER --password=$MYSQL_ROOT_PASSWORD -f drop $DB_NAME
  mysqladmin -u $MYSQL_ROOT_USER --password=$MYSQL_ROOT_PASSWORD create $DB_NAME
  echo 'GRANT ALL ON '$DB_NAME'.* TO gateway@localhost' | mysql -u $MYSQL_ROOT_USER --password=$MYSQL_ROOT_PASSWORD
  echo "GRANT ALL ON $DB_NAME.* TO gateway@'%'" | mysql -u $MYSQL_ROOT_USER --password=$MYSQL_ROOT_PASSWORD

  #Need a ssg.sql here to create the database
  echo 'Creating empty SSG database using /ssg/etc/sql/ssg.sql'
  mysql -u $DB_USER --password=$DB_PASS $DB_NAME < "/ssg/etc/sql/ssg.sql"
  return 0
}

updateDB() {
  # Update the database
  mysql -f -u $DB_USER --password=$DB_PASS $DB_NAME < cat `ls -1 /ssg/etc/sql/upgrade_* | tail -1`
}

cleanDB() {
  # Remove old data
  mysql -f -u $DB_USER --password=$DB_PASS $DB_NAME -e "delete from counters;"
  mysql -f -u $DB_USER --password=$DB_PASS $DB_NAME -e "delete from audit_detail_params;"
  mysql -f -u $DB_USER --password=$DB_PASS $DB_NAME -e "delete from audit_detail;"
  mysql -f -u $DB_USER --password=$DB_PASS $DB_NAME -e "delete from audit_system;"
  mysql -f -u $DB_USER --password=$DB_PASS $DB_NAME -e "delete from audit_message;"
  mysql -f -u $DB_USER --password=$DB_PASS $DB_NAME -e "delete from audit_admin;"
  mysql -f -u $DB_USER --password=$DB_PASS $DB_NAME -e "delete from audit_main;"
  mysql -f -u $DB_USER --password=$DB_PASS $DB_NAME -e "delete from counters;"
  mysql -f -u $DB_USER --password=$DB_PASS $DB_NAME -e "delete from cluster_info;"
  mysql -f -u $DB_USER --password=$DB_PASS $DB_NAME -e "delete from keystore_file;"
}

startSSG() {
  echo "Starting the gateway."
  
  # Remove the log files
  rm -f /ssg/logs/*
  
  # Start the SSG
  service ssg start default_ > /dev/null 2>&1 &
  local initScriptPID
  initScriptPID=$!
  if [ -z "$SKIP_INIT_SCRIPT_TIMEOUT" ]; then
    sleep $INIT_SCRIPT_TIMEOUT
    if [ -d "/proc/$initScriptPID" ]; then
      kill -9 $initScriptPID
      echo "Init script didn't exit after $INIT_SCRIPT_TIMEOUT seconds, aborting test."
      stopSSG
      return 1
    fi
  fi

  local found
  local loopCount
  found="0"
  loopCount=0
  while [ $found = "0" -o $loopCount -gt "300" ]; do
    sleep 1

    nc -z localhost 8080 &>/dev/null
    if [ $? -eq 0 ] ; then
        found="1"
    fi

    loopCount=$(($loopCount+1))
  done

  if [ $found = "0" ]; then
    echo "The SSG failed to startup"
    return 1
  fi

  if [ ! -z "$SKIP_INIT_SCRIPT_TIMEOUT" -a -d "/proc/$initScriptPID" ]; then
    kill -9 $initScriptPID
    return 1
  else
    return 0
  fi
}

addTrustedCertificates() {
  ant -Dcert.file=etc/bra-test_keys/ssl.cer runAddTrustedCertificate
  ant runSetupPrivateKeys
}

startSnmptrapd() {
  cd $SNMPTRAPD_DIR
  ./start_trapd.sh
  cd $BASE_DIR
}

stopSnmptrapd() {
  if [ -e "$SNMPTRAPD_PID" -a -d "/proc/`cat $SNMPTRAPD_PID`" ]; then
    kill -9 `cat $SNMPTRAPD_PID`
    rm $SNMPTRAPD_PID
  fi
}

revokeCerts() {
  # Revoke certificates and reset passwords
  ant runAccountUpdater
}

configureAutoTest() {
  local hostname
  hostname=$(hostname -f)
  cd $AUTOTEST_DIR/etc
  sed -i -e 's/^host=.*/host='"$hostname"'/' junit.properties
  sed -i -e 's/^host_callback=.*/host_callback='"$hostname"'/' junit.properties  
  sed -i -e 's/^host_db=.*/host_db=localhost/' junit.properties
  sed -i -e 's/^db=.*/db='"$DB_NAME"'/' junit.properties
  sed -i -e 's/^db_user=.*/db_user='"$DB_USER"'/' junit.properties
  sed -i -e 's/^db_password=.*/db_password='"$DB_PASS"'/' junit.properties
  sed -i -e 's/^ssbPropertiesDir=.*/ssbPropertiesDir=\/root\/.l7tech\//' junit.properties
  sed -i -e 's/^ssblogfile=.*/ssblogfile=\/root\/.l7tech\/ssa0.log/' junit.properties

  sed -i -e 's/^ssg\.host=.*/ssg.host=devssg1.l7tech.com/' manager_automator.properties

  cd $BASE_DIR
  
  local escaped_value
  escaped_value=$(echo $MANAGER_DIR | sed -e 's/\//\\\//g')
  sed -i -e "s/<property name=\"uneasyrooster.ssmBuild.dir\" value=\".*\"\\/>/<property name=\"uneasyrooster.ssmBuild.dir\" value=\"$escaped_value\"\\/>/" build.xml
  escaped_value=$(echo "$JAVA_HOME/jre" | sed -e 's/\//\\\//g')
  sed -i -e "s/<property name=\"ssm.jre.dir\" value=\".*\"\\/>/<property name=\"ssm.jre.dir\" value=\"$escaped_value\"\\/>/" build.xml

  sed -i -e 's/^ssg\.host=.*/ssg.host=devssg1.l7tech.com/' src/manager_automator.properties

  cp "$SUPPORT_FILES_DIR/data/eicar.com" AutoTest/src/com/l7tech/test/threats/SentVirusDir
}

runAutoTest() {
  # Run the AutoTest tests
  if [ -e ~/.l7tech ]; then
    mv ~/.l7tech ~/.l7tech.old
  fi

  mkdir ~/.l7tech

  cd $AUTOTEST_DIR
  local escaped_value
  escaped_value=$(echo $MANAGER_DIR | sed -e 's/\//\\\//g')
  sed -i -e "s/<property name=\"uneasyrooster.ssmBuild.dir\" value=\".*\"\/>/<property name=\"uneasyrooster.ssmBuild.dir\" value=\"$escaped_value\"\/>/" build.xml
  escaped_value=$(echo $CLIENT_DIR | sed -e 's/\//\\\//g')
  sed -i -e "s/<property name=\"uneasyrooster.ssbBuild.dir\" value=\".*\"\/>/<property name=\"uneasyrooster.ssbBuild.dir\" value=\"$escaped_value\"\/>/" build.xml
  escaped_value=$(echo "$JAVA_HOME/jre" | sed -e 's/\//\\\//g')
  sed -i -e "s/<property name=\"ssb.jre.dir\" value=\".*\"\/>/<property name=\"ssb.jre.dir\" value=\"$escaped_value\"\/>/" build.xml
  
  local status
  ant "$1"
  status=$?
  cd $BASE_DIR
  
  if [ -e ~/.l7tech ]; then
    rm -rf ~/.l7tech
  fi
  if [ -e ~/.l7tech.old ]; then
    mv ~/.l7tech.old ~/.l7tech
  fi

  return $status
}

enableDisableAuditing() {
  local threshold
  if [ -z "$1" ]; then
    threshold="INFO"
  else
    threshold="SEVERE"
  fi

  local escaped_value
  escaped_value=$(echo $MANAGER_DIR | sed -e 's/\//\\\//g')
  sed -i -e "s/<property name=\"uneasyrooster.ssmBuild.dir\" value=\".*\"\\/>/<property name=\"uneasyrooster.ssmBuild.dir\" value=\"$escaped_value\"\\/>/" build.xml
  escaped_value=$(echo "$JAVA_HOME/jre" | sed -e 's/\//\\\//g')
  sed -i -e "s/<property name=\"ssm.jre.dir\" value=\".*\"\\/>/<property name=\"ssm.jre.dir\" value=\"$escaped_value\"\\/>/" build.xml
  ant -DauditThreshold.value=$threshold runSetAuditThreshold
}

measureResolutionPerformance() {
  ant runCreateServices
  enableDisableAuditing 1

  ab -n 300000 -k -c 8 -p etc/ServiceResolution_PlaceOrder_REQUEST.xml -H "SoapAction: http://warehouse.acme.com/serviceresolution/placeOrder" -T "text/xml" http://devssg1.l7tech.com:8080/serviceresolution > /tmp/uri_resolution.txt
  ab -n 300000 -k -c 8 -p etc/ServiceResolution_PlaceOrder_REQUEST.xml -H "SoapAction: http://warehouse.acme.com/serviceresolution/placeOrder" -T "text/xml" http://devssg1.l7tech.com:8080/ssg/soap > /tmp/soap_action_resolution.txt
  ab -n 300000 -k -c 8 -p etc/ServiceResolution2_PlaceOrder_REQUEST.xml -T "text/xml" http://devssg1.l7tech.com:8080/ssg/soap > /tmp/namespace_resolution.txt

  enableDisableAuditing
}

parseResults() {
  # Keep a copy of gateway's mailbox
  cp -f /var/spool/mail/gateway /tmp/gateway.mbox
  ant runAutoTestLogParser
}

prepareArtifacts() {
  mkdir results
  mkdir results/AutoTest
  mv $AUTOTEST_DIR/TEST-* results/AutoTest
  cd results/AutoTest
  tar czf ../AutoTest.tar.gz *
  cd ../..
  rm -rf results/AutoTest
  mv /tmp/uri_resolution.txt results
  mv /tmp/soap_action_resolution.txt results
  mv /tmp/namespace_resolution.txt results
  mv new_failures.txt results
  mv resolved_failures.txt results
  tar czf artifacts.tar.gz results
  rm -rf results
}

runIntegrationTest() {
  setEnvironmentVariables
  checkNewBuildDir
  checkSoftware
  checkServers
  stopSSG
  uninstallSoftware
  local status
  installNewBuild
  status=$?
  if [ "$?" = "0" ]; then
    installDB
    status=$?
    if [ "$status" = "0" ]; then
      configurePartition
      status=$?
    fi
  fi
  if [ "$status" = "0" ]; then

    echo 'Updating connector.....'
    mysql -f -u $DB_USER --password=$DB_PASS $DB_NAME -e "update connector set enabled = 1;"

    local retVal
    retVal=0
    startSSG
    if [ "$?" = "0" ]; then
      startSnmptrapd
      configureAutoTest

      echo 'Uploading all entities'
      ant runEntityManager -DentityManagerAction=upload
      ant runSetupPrivateKeys

      # Restart the SSG so that policies using the new certs are reloaded
      stopSSG
      startSSG

      local autoTestStatus
      runAutoTest testAll
      autoTestStatus=$?
      # Measure the performance of URI, Soap Action and namespace service resolution
      if [ "$autoTestStatus" = "0" ]; then
        measureResolutionPerformance
      fi
      stopSSG
      stopSnmptrapd
      if [ "$autoTestStatus" = "0" ]; then
        parseResults
        prepareArtifacts
      else
        retVal=1
      fi
    else
      retVal=1
    fi
  else
    retVal=1
  fi
  uninstallSoftware

  exit $retVal
}

runSingleTest() {
  setEnvironmentVariables
  MANAGER_DIR="$PWD/"`ls -1d Manager-* | tail -1`
  CLIENT_DIR="$PWD/"`ls -1d Client-* | tail -1`
  stopSSG
  startSSG
  startSnmptrapd
  revokeCerts
  runAutoTest "$1"
  stopSSG
  stopSnmptrapd
}

usage() {
cat << EOF
$0 [-a|-s AUTO_TEST_TARGET]
EOF
}

runAll="1"
autoTestTarget="testAll"

while getopts "as:ht" OPTION; do
  case $OPTION in
    a)
      runAll="1"
      autoTestTarget="testAll"
      break
      ;;
    s)
      runAll="0"
      autoTestTarget="$OPTARG"
      break
      ;;
    t)
      checkServers
      exit $?
      ;;
    h)
      usage
      exit 1
      ;;
  esac
done

if [ "$runAll" = "1" ]; then
  runIntegrationTest
else
  runSingleTest "$autoTestTarget"
fi

