#!/bin/bash

# Build sample solution kit skar file (e.g. SimpleSolutionKit-1.3.skar).
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

# Make sure modules/skunkworks/build/example/solutionkit/simple/v01_03 directory exists
BUILD_DIR="modules/skunkworks/build/example/solutionkit/simple/v01_03"
mkdir -p $BUILD_DIR

cd modules/skunkworks
javac -sourcepath src/main/java/ -classpath "../gateway/api/build/layer7-api.jar${JAVAC_CLASSPATH_SEPARATOR}../policy/build/layer7-policy.jar${JAVAC_CLASSPATH_SEPARATOR}../utility/build/layer7-utility.jar${JAVAC_CLASSPATH_SEPARATOR}../common/build/layer7-common.jar${JAVAC_CLASSPATH_SEPARATOR}../../lib/repository/commons-lang/commons-lang-2.5.jar" src/main/java/com/l7tech/example/solutionkit/simple/v01_03/BaseSolutionKitManagerCallback.java src/main/java/com/l7tech/example/solutionkit/simple/v01_03/SimpleOtherSolutionKitManagerCallback.java src/main/java/com/l7tech/example/solutionkit/simple/v01_03/SimpleServiceSolutionKitManagerCallback.java src/main/java/com/l7tech/example/solutionkit/simple/v01_03/console/SimpleOtherSolutionKitManagerUi.java src/main/java/com/l7tech/example/solutionkit/simple/v01_03/console/SimpleServiceSolutionKitManagerUi.java -d build/example/solutionkit/simple/v01_03
cd build/example/solutionkit/simple/v01_03
jar cvf Customization.jar com/l7tech/example/solutionkit/simple/v01_03/BaseSolutionKitManagerCallback.class com/l7tech/example/solutionkit/simple/v01_03/SimpleOtherSolutionKitManagerCallback.class com/l7tech/example/solutionkit/simple/v01_03/SimpleServiceSolutionKitManagerCallback.class com/l7tech/example/solutionkit/simple/v01_03/console/SimpleOtherSolutionKitManagerUi.class com/l7tech/example/solutionkit/simple/v01_03/console/SimpleServiceSolutionKitManagerUi.class com/l7tech/example/solutionkit/simple/v01_03/console/SimpleOtherSolutionKitManagerUi\$1.class com/l7tech/example/solutionkit/simple/v01_03/console/SimpleServiceSolutionKitManagerUi\$1.class

# cd <l7_workspace>
cd ../../../../../../..

cp -f build/installer/SkarSigner-HEAD-9.2.00.zip modules/skunkworks/src/main/resources/com/l7tech/example/solutionkit/simple/v01_03/*.* ${BUILD_DIR}
cd $BUILD_DIR

# build child skar with only Server Module File
zip -X SimpleServerModuleFile-1.1.skar --junk-paths ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_03/smf_only/SolutionKit.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_03/smf_only/InstallBundle.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_03/smf_only/DeleteBundle.xml

# build child skar for service
zip -X SimpleService-1.3.skar Customization.jar --junk-paths ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_03/service/SolutionKit.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_03/service/InstallBundle.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_03/service/DeleteBundle.xml

# build child skar for all other entities
zip -X SimpleOthers-1.3.skar Customization.jar --junk-paths ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_03/other/SolutionKit.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_03/other/InstallBundle.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_03/other/DeleteBundle.xml

# build parent skar (container for child skars)
zip -X SimpleSolutionKit-1.3.skar --junk-paths ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_03/SolutionKit.xml SimpleServerModuleFile-1.1.skar SimpleService-1.3.skar SimpleOthers-1.3.skar

unzip -u SkarSigner-HEAD-9.2.00.zip

# 6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA generated from SkarSigner-HEAD/skar_signer.sh encodePassword -password 7layer
SkarSigner-HEAD/skar_signer.sh  sign --storeFile "../../../../../../../etc/signer/gatewayKeyStore.p12" --storePass "6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA" --keyPass "6fj1QDCbvjI.OBh1tzgR5MCLBQo72qH5gA" --fileToSign "SimpleSolutionKit-1.3.skar"

pwd
ls
