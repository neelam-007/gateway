#!/bin/bash

# Build sample solution kit skar file (e.g. SolutionKitDemoInstanceModifiable.skar).
# Possible improvement: look into how to add this as a build target in the main build.xml.

# javac defaults to unix
JAVAC_CLASSPATH_SEPARATOR=":"
case "$(uname)" in
  CYGWIN*) JAVAC_CLASSPATH_SEPARATOR=";" ;;
esac

# cd <l7_workspace>
cd ../../../../../../../../../..

# Make sure modules/skunkworks/build/example/solutionkit/instance_modifiable directory exists
BUILD_DIR="modules/skunkworks/build/example/solutionkit/instance_modifiable"
mkdir -p $BUILD_DIR

cp -f build/installer/SkarSigner-HEAD-9.2.00.zip modules/skunkworks/src/main/resources/com/l7tech/example/solutionkit/instance_modifiable/*.* ${BUILD_DIR}
cd $BUILD_DIR

# build child skar with Policy-Backed Identity Provider
zip -X SolutionKitDemoPolicyBackedIdentityProvider.skar -j ../../../../src/main/resources/com/l7tech/example/solutionkit/instance_modifiable/pbip/SolutionKit.xml ../../../../src/main/resources/com/l7tech/example/solutionkit/instance_modifiable/pbip/InstallBundle.xml  ../../../../src/main/resources/com/l7tech/example/solutionkit/instance_modifiable/pbip/DeleteBundle.xml ../../../../src/main/resources/com/l7tech/example/solutionkit/instance_modifiable/pbip/UpgradeBundle.xml

# build parent skar (container for child skars)
zip -X SolutionKitDemoInstanceModifiable.skar -j ../../../../src/main/resources/com/l7tech/example/solutionkit/instance_modifiable/SolutionKit.xml SolutionKitDemoPolicyBackedIdentityProvider.skar

unzip -u SkarSigner-HEAD-9.2.00.zip

# 6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA generated from SkarSigner-HEAD/skar_signer.sh encodePassword -password 7layer
SkarSigner-HEAD/skar_signer.sh  sign --storeFile "../../../../../../etc/signer/gatewayKeyStore.p12" --storePass "6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA" --keyPass "6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA" --fileToSign "SolutionKitDemoInstanceModifiable.skar"

pwd
ls