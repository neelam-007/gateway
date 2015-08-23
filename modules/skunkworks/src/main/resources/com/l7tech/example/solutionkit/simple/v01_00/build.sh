#!/bin/bash

# Build sample solution kit skar file (e.g. SimpleSolutionKit-1.0.skar).
# Possible improvement: look into how to add this as a build target in the main build.xml.

# cd <l7_workspace>
cd ../../../../../../../../../../..

# Make sure modules/skunkworks/build/example/solutionkit/simple/v01_00 directory exists
BUILD_DIR="modules/skunkworks/build/example/solutionkit/simple/v01_00"
mkdir -p $BUILD_DIR

cp -a build/installer/SkarSigner-HEAD-9.0.00.zip modules/skunkworks/src/main/resources/com/l7tech/example/solutionkit/simple/v01_00/. modules/skunkworks/build/example/solutionkit/simple/v01_00/
cd $BUILD_DIR
zip -X SimpleSolutionKit-1.0.skar -j ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_00/SolutionKit.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_00/InstallBundle.xml

unzip -u SkarSigner-HEAD-9.0.00.zip

# 6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA generated from SkarSigner-HEAD/skar_signer.sh encodePassword -password 7layer
SkarSigner-HEAD/skar_signer.sh  sign --storeFile "../../../../../../../etc/signer/gatewayKeyStore.p12" --storePass "6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA" --keyPass "6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA" --fileToSign "SimpleSolutionKit-1.0.skar"

pwd
ls