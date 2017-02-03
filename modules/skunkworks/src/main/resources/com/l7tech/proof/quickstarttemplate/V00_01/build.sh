#!/bin/bash

# Build quick start template proof-of-concept skar file (e.g. quickstarttemplate-proof-0.01.skar).

# cd <l7_workspace>
cd ../../../../../../../../../..

# Make sure modules/skunkworks/src/main/resources/com/l7tech/proof/quickstarttemplate/V00_01 directory exists
BUILD_DIR="modules/skunkworks/src/main/resources/com/l7tech/proof/quickstarttemplate/V00_01"
mkdir -p $BUILD_DIR

cp -f build/installer/SkarSigner-HEAD-9.2.00.zip modules/skunkworks/src/main/resources/com/l7tech/proof/quickstarttemplate/V00_01/*.* ${BUILD_DIR}
cd $BUILD_DIR
zip -X quickstarttemplate-proof-0.01.skar -j ../../../../../../../../src/main/resources/com/l7tech/proof/quickstarttemplate/V00_01/SolutionKit.xml ../../../../../../../../src/main/resources/com/l7tech/proof/quickstarttemplate/V00_01/InstallBundle.xml ../../../../../../../../src/main/resources/com/l7tech/proof/quickstarttemplate/V00_01/DeleteBundle.xml

unzip -u SkarSigner-HEAD-9.2.00.zip

# 6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA generated from SkarSigner-HEAD/skar_signer.sh encodePassword -password 7layer
SkarSigner-HEAD/skar_signer.sh  sign --storeFile "../../../../../../../../../../etc/signer/gatewayKeyStore.p12" --storePass "6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA" --keyPass "6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA" --fileToSign "quickstarttemplate-proof-0.01.skar"

pwd
ls