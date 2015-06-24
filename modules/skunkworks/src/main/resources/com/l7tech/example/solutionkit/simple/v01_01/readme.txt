How to build sample solution kit skar file (e.g. SimpleSolutionKit-1.0.skar).

Depends on Customization.jar.  See class Javadoc on how to build, com.l7tech.example.solutionkit.simple.vnn_nn.SimpleSolutionKitManagerCallback.

prompt> cd <l7_workspace>
prompt> cp -a modules/skunkworks/src/main/resources/com/l7tech/example/solutionkit/simple/v01_01/. modules/skunkworks/build/example/solutionkit/simple/v01_01/
prompt> cd modules/skunkworks/build/example/solutionkit/simple/v01_01
prompt> zip -X SimpleSolutionKit-1.1.skar Customization.jar -j ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_01/SolutionKit.xml
         ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_01/InstallBundle.xml ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_01/UpgradeBundle.xml
         ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_01/DeleteBundle.xml

Possible improvement: look into how to add this as a build target in the main build.xml.