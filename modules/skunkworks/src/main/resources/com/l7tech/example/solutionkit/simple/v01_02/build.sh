#!/bin/bash

# Build sample solution kit skar file (e.g. SimpleSolutionKit-1.2.skar).
# Possible improvement: look into how to add this as a build target in the main build.xml.

# Depends on Customization.jar.
#   Which in turn depends on <l7_workspace>/modules/gateway/api/build/layer7-api.jar (e.g. ./build.sh moduled -Dmodule=layer7-api)
#         and <l7_workspace>/modules/policy/build/layer7-policy.jar (e.g. ./build.sh moduled -Dmodule=layer7-policy)
#         and <l7_workspace>/modules/utility/build/layer7-utility.jar (e.g. ./build.sh moduled -Dmodule=layer7-utility)

# javac defaults to unix
JAVAC_CLASSPATH_SEPARATOR=":"
case "$(uname)" in
  CYGWIN*) JAVAC_CLASSPATH_SEPARATOR=";" ;;
esac

# cd <l7_workspace>
cd ../../../../../../../../../../..

# Make sure modules/skunkworks/build/example/solutionkit/simple/v01_02 directory exists
BUILD_DIR="modules/skunkworks/build/example/solutionkit/simple/v01_02"
mkdir -p $BUILD_DIR

cd modules/skunkworks
javac -sourcepath src/main/java/ -classpath "../gateway/api/build/layer7-api.jar${JAVAC_CLASSPATH_SEPARATOR}../policy/build/layer7-policy.jar${JAVAC_CLASSPATH_SEPARATOR}../utility/build/layer7-utility.jar${JAVAC_CLASSPATH_SEPARATOR}../common/build/layer7-common.jar${JAVAC_CLASSPATH_SEPARATOR}../../lib/repository/commons-lang/commons-lang-2.5.jar" src/main/java/com/l7tech/example/solutionkit/simple/v01_02/SimpleSolutionKitManagerCallback.java src/main/java/com/l7tech/example/solutionkit/simple/v01_02/console/SimpleSolutionKitManagerUi.java -d build/example/solutionkit/simple/v01_02
cd build/example/solutionkit/simple/v01_02
jar cvf Customization.jar com/l7tech/example/solutionkit/simple/v01_02/SimpleSolutionKitManagerCallback.class com/l7tech/example/solutionkit/simple/v01_02/console/SimpleSolutionKitManagerUi.class com/l7tech/example/solutionkit/simple/v01_02/console/SimpleSolutionKitManagerUi\$1.class

# cd <l7_workspace>
cd ../../../../../../..

cp -f build/installer/SkarSigner-HEAD-9.1.00.zip modules/skunkworks/src/main/resources/com/l7tech/example/solutionkit/simple/v01_02/*.* ${BUILD_DIR}
cd $BUILD_DIR

# build child skar with only Server Module File
zip -X SimpleServerModuleFile-1.1.skar --junk-paths ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_02/smf_only/SolutionKit.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_02/smf_only/InstallBundle.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_02/smf_only/UpgradeBundle.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_02/smf_only/DeleteBundle.xml

# build child skar for service
zip -X SimpleService-1.2.skar --junk-paths ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_02/service/SolutionKit.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_02/service/InstallBundle.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_02/service/DeleteBundle.xml

# build child skar for all other entities
zip -X SimpleOthers-1.2.skar Customization.jar --junk-paths ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_02/other/SolutionKit.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_02/other/InstallBundle.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_02/other/UpgradeBundle.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_02/other/DeleteBundle.xml

# build parent skar (container for child skars)
zip -X SimpleSolutionKit-1.2.skar --junk-paths ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_02/SolutionKit.xml SimpleServerModuleFile-1.1.skar SimpleService-1.2.skar SimpleOthers-1.2.skar

unzip -u SkarSigner-HEAD-9.1.00.zip

# 6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA generated from SkarSigner-HEAD/skar_signer.sh encodePassword -password 7layer
SkarSigner-HEAD/skar_signer.sh  sign --storeFile "../../../../../../../etc/signer/gatewayKeyStore.p12" --storePass "6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA" --keyPass "6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA" --fileToSign "SimpleSolutionKit-1.2.skar"

pwd
ls