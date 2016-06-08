#!/bin/bash

# Build sample solution kit skar file (e.g. SolutionKitDemoUserConfigurable.skar).
# Possible improvement: look into how to add this as a build target in the main build.xml.

# javac defaults to unix
JAVAC_CLASSPATH_SEPARATOR=":"
case "$(uname)" in
  CYGWIN*) JAVAC_CLASSPATH_SEPARATOR=";" ;;
esac

# cd <l7_workspace>
cd ../../../../../../../../../..

# Make sure modules/skunkworks/build/example/solutionkit/user_configurable directory exists
BUILD_DIR="modules/skunkworks/build/example/solutionkit/user_configurable"
mkdir -p $BUILD_DIR

cp -f build/installer/SkarSigner-HEAD-9.2.00.zip modules/skunkworks/src/main/resources/com/l7tech/example/solutionkit/user_configurable/*.* ${BUILD_DIR}
cd $BUILD_DIR

# build child skar with Cassandra Connection
zip -X SolutionKitDemoCassandraConnection.skar -j ../../../../src/main/resources/com/l7tech/example/solutionkit/user_configurable/cassandra_connection/SolutionKit.xml ../../../../src/main/resources/com/l7tech/example/solutionkit/user_configurable/cassandra_connection/InstallBundle.xml

# build child skar with Encapsulated Assertion
zip -X SolutionKitDemoEncapsulatedAssertion.skar -j ../../../../src/main/resources/com/l7tech/example/solutionkit/user_configurable/encapsulated_assertion/SolutionKit.xml ../../../../src/main/resources/com/l7tech/example/solutionkit/user_configurable/encapsulated_assertion/InstallBundle.xml

# build parent skar (container for child skars)
zip -X SolutionKitDemoUserConfigurable.skar -j ../../../../src/main/resources/com/l7tech/example/solutionkit/user_configurable/SolutionKit.xml SolutionKitDemoCassandraConnection.skar SolutionKitDemoEncapsulatedAssertion.skar

unzip -u SkarSigner-HEAD-9.2.00.zip

# 6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA generated from SkarSigner-HEAD/skar_signer.sh encodePassword -password 7layer
SkarSigner-HEAD/skar_signer.sh  sign --storeFile "../../../../../../etc/signer/gatewayKeyStore.p12" --storePass "6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA" --keyPass "6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA" --fileToSign "SolutionKitDemoUserConfigurable.skar"

pwd
ls
