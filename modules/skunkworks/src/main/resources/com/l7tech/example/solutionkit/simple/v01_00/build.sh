#!/bin/bash

# Build sample solution kit skar file (e.g. SimpleSolutionKit-1.0.skar).
# Possible improvement: look into how to add this as a build target in the main build.xml.

# cd <l7_workspace>
cd ../../../../../../../../../../..

# Make sure modules/skunkworks/build/example/solutionkit/simple/v01_00 directory exists
BUILD_DIR="modules/skunkworks/build/example/solutionkit/simple/v01_00"
mkdir -p $BUILD_DIR

cp -a modules/skunkworks/src/main/resources/com/l7tech/example/solutionkit/simple/v01_00/. modules/skunkworks/build/example/solutionkit/simple/v01_00/
cd $BUILD_DIR
zip -X SimpleSolutionKit-1.0.skar -j ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_00/SolutionKit.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_00/InstallBundle.xml

pwd
ls