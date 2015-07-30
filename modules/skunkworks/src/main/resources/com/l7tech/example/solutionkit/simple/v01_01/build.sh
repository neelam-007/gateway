#!/bin/bash

# Build sample solution kit skar file (e.g. SimpleSolutionKit-1.1.skar).
# Possible improvement: look into how to add this as a build target in the main build.xml.

# Depends on Customization.jar.
#   Which in turn depends on <l7_workspace>/modules/gateway/api/build/layer7-api.jar (e.g. ./build.sh moduled -Dmodule=layer7-api)
#         and <l7_workspace>/modules/policy/build/layer7-policy.jar (e.g. ./build.sh moduled -Dmodule=layer7-policy)
#         and <l7_workspace>/modules/utility/build/layer7-utility.jar (e.g. ./build.sh moduled -Dmodule=layer7-utility)

# cd <l7_workspace>
cd ../../../../../../../../../../..

# Make sure modules/skunkworks/build/example/solutionkit/simple/v01_01 directory exists
BUILD_DIR="modules/skunkworks/build/example/solutionkit/simple/v01_01"
mkdir -p $BUILD_DIR

cd modules/skunkworks
javac -sourcepath src/main/java/ -classpath ../gateway/api/build/layer7-api.jar:../policy/build/layer7-policy.jar:../utility/build/layer7-utility.jar src/main/java/com/l7tech/example/solutionkit/simple/v01_01/SimpleSolutionKitManagerCallback.java src/main/java/com/l7tech/example/solutionkit/simple/v01_01/console/SimpleSolutionKitManagerUi.java -d build/example/solutionkit/simple/v01_01
cd build/example/solutionkit/simple/v01_01
jar cvf Customization.jar com/l7tech/example/solutionkit/simple/v01_01/SimpleSolutionKitManagerCallback.class com/l7tech/example/solutionkit/simple/v01_01/console/SimpleSolutionKitManagerUi.class com/l7tech/example/solutionkit/simple/v01_01/console/SimpleSolutionKitManagerUi\$1.class

# cd <l7_workspace>
cd ../../../../../../..

cp -a modules/skunkworks/src/main/resources/com/l7tech/example/solutionkit/simple/v01_01/. modules/skunkworks/build/example/solutionkit/simple/v01_01/
cd $BUILD_DIR
zip -X SimpleSolutionKit-1.1.skar Customization.jar -j ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_01/SolutionKit.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_01/InstallBundle.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_01/UpgradeBundle.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_01/DeleteBundle.xml

pwd
ls