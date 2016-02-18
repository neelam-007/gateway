#!/bin/bash
# This script copies the jars in lib/respository into maven folder structure
# This is an alternative to ivy:publish (ant target is publish-third-party-all) if local import from the Artifactory server is desired

# Define source directory and the base directory of target
SOURCE=lib/repository
BASE=build/maven/repository

# Collect all the ivy files
XMLS=$SOURCE/*/*.xml

# For each ivy file, copy the defined jar and xml from source to target
for f in $XMLS
do
	# Parse multi lines
	ORGANISATION=`awk '{FS = "\""} $1 ~ /organisation=/ {print $2}' $f`
	MODULE=`awk '{FS = "\""} $1 ~ /module=/ {print $2}' $f`
	REVISION=`awk '{FS = "\""} $1 ~ /revision=/ {print $2}' $f`
	
	# Parse single line
	if [ "$MODULE" == "" ]
	then
		MODULE=`awk '{FS = "\""} $1 ~ /<info/ {print $4}' $f`
		REVISION=`awk '{FS = "\""} $1 ~ /<info/ {print $6}' $f`
	fi
	
	echo "$ORGANISATION		$MODULE		$REVISION"
	
	# Convert '.' to '/' to make maven directory structure
	ORGANISATION_MAVEN=${ORGANISATION//./\/}
	
	TARGET=$BASE/$ORGANISATION_MAVEN/$MODULE/$REVISION
	mkdir -p $TARGET
	
	# Copy the jar if exist
	JAR=$SOURCE/$ORGANISATION/$MODULE-$REVISION.jar
	if [ -f $JAR ]
	then
		cp $JAR $TARGET
		echo "$JAR copied"
	else
		echo "$JAR not found"
	fi
	
	# Ivy xml may be named in either format
	XML=$SOURCE/$ORGANISATION/$MODULE-ivy-$REVISION.xml
	if [ ! -f $XML ]
	then
		XML=$SOURCE/$ORGANISATION/$MODULE-$REVISION.xml
	fi
	
	# Copy the ivy xml if exist
	if [ -f $XML ]
	then
		cp $XML $TARGET
		echo "$XML copied"
	else
		echo "$XML not found"
	fi
	
	echo "********************************************************************************************"
done

echo "Total artifact in $SOURCE: "
find $SOURCE -type f | wc -l

echo "Total artifact copied to $BASE: "
find $BASE -type f | wc -l

exit
