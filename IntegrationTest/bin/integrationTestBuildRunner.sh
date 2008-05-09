#!/bin/bash

RESULTS_CACHE_DIR=/home/teamcity/IntegrationTestResultsCache
pwd
ls
# Get the required build files
LAST_BUILD_DIR="IntegrationTest/dist"
if [ ! -e "$LAST_BUILD_DIR/"ssg-*.noarch.rpm ]; then
  echo "SSG RPM was not found."
  exit 1
fi

if [ ! -e "$LAST_BUILD_DIR/"ssg-appliance-*.i386.rpm ]; then
  echo "SSG appliance RPM was not found."
  exit 1
fi

if [ ! -e "$LAST_BUILD_DIR/"Manager-*.tar.gz ]; then
  echo "Manager tarball was not found."
  exit 1
fi

if [ ! -e "$LAST_BUILD_DIR/"Client-*.tar.gz ]; then
  echo "VPN Client tarball was not found."
  exit 1
fi

# Create the IntegrationTest directory on the server
ssh root@devssg1 "rm -rf /tmp/IntegrationTest"
ssh root@devssg1 "mkdir /tmp/IntegrationTest"

# 
# Copy the files over
scp DEVLICENSE.xml root@devssg1:/tmp/IntegrationTest/
cd IntegrationTest
scp -r bin root@devssg1:/tmp/IntegrationTest/
scp -r etc root@devssg1:/tmp/IntegrationTest/
scp -r src root@devssg1:/tmp/IntegrationTest/
scp build.xml root@devssg1:/tmp/IntegrationTest/
scp -r lib root@devssg1:/tmp/IntegrationTest/
scp -r tools root@devssg1:/tmp/IntegrationTest/
scp -r snmptrapd root@devssg1:/tmp/IntegrationTest/
mkdir AutoTest/UneasyRooster
cp UneasyRooster/ServerWsiBspAssertion.rules.properties UneasyRooster/ServerWsiSamlAssertion.rules.properties AutoTest/UneasyRooster
scp -r AutoTest root@devssg1:/tmp/IntegrationTest/
mkdir PreviousAutoTestResults
rm -f PreviousAutoTestResults/*

. etc/defs.sh

nameSuffix="_$SSG_VERSION"
if [ "$nameSuffix" = "_" ]; then
  nameSuffix=""
fi
if [ -e "$RESULTS_CACHE_DIR/PreviousAutoTestResults$nameSuffix.tar.gz" ]; then
  tar xzfC "$RESULTS_CACHE_DIR/PreviousAutoTestResults$nameSuffix.tar.gz" PreviousAutoTestResults
fi
scp -r PreviousAutoTestResults root@devssg1:/tmp/IntegrationTest/
scp MANIFEST.MF root@devssg1:/tmp/IntegrationTest/
scp cfg_data.xml root@devssg1:/tmp/IntegrationTest/

# Copy the Tibco JAR files over
ssh root@devssg1 "mkdir /tmp/IntegrationTest/jars"
scp jars/*.jar root@devssg1:/tmp/IntegrationTest/jars/

# Copy the build files to the new build directory on devssg1
ssh root@devssg1 "mkdir /tmp/IntegrationTest/new_build"

LAST_BUILD_DIR="dist"
scp "$LAST_BUILD_DIR/"ssg-*.noarch.rpm "$LAST_BUILD_DIR/"ssg-appliance-*.i386.rpm "$LAST_BUILD_DIR/"Manager-*.tar.gz "$LAST_BUILD_DIR/"Client-*.tar.gz root@devssg1:/tmp/IntegrationTest/new_build

# Run the integration test
ssh root@devssg1 "cd /tmp/IntegrationTest; ./bin/runAutoTest.sh"

returnCode=$?
if [ $returnCode = "0" ]; then
  # Copy the results over so that TeamCity can pick them up as build artifacts
  scp root@devssg1:/tmp/IntegrationTest/artifacts.tar.gz .
  tar xzf artifacts.tar.gz
  rm -f artifacts.tar.gz
  if [ -e "$RESULTS_CACHE_DIR/previous_resolution_times.txt" ]; then
    cp "$RESULTS_CACHE_DIR/previous_resolution_times.txt" results/previous_resolution_times.txt
  fi
  ./bin/checkForErrors.pl
  if [ -e previous_resolution_times.txt ]; then
    cp -f results/previous_resolution_times.txt "$RESULTS_CACHE_DIR/previous_resolution_times.txt"
    mv previous_resolution_times.txt results/resolution_times.txt
  fi

  if [ -e results/AutoTest.tar.gz ]; then
    cp -f results/AutoTest.tar.gz "$RESULTS_CACHE_DIR/PreviousAutoTestResults$nameSuffix.tar.gz"
  fi

  exit $returnCode
else
  exit $returnCode
fi

