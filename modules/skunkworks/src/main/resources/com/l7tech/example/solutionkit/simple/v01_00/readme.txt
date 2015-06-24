How to build sample solution kit skar file (e.g. SimpleSolutionKit-1.0.skar).

prompt> cd <l7_workspace>
Make sure modules/skunkworks/build/example/solutionkit/simple/v01_01 directory exists (e.g. mkdir -p modules/skunkworks/build/example/solutionkit/simple/v01_00)

prompt> cp -a modules/skunkworks/src/main/resources/com/l7tech/example/solutionkit/simple/v01_00/. modules/skunkworks/build/example/solutionkit/simple/v01_00/
prompt> cd modules/skunkworks/build/example/solutionkit/simple/v01_00
prompt> zip -X SimpleSolutionKit-1.0.skar -j ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_00/SolutionKit.xml
         ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_00/InstallBundle.xml

Possible improvement: look into how to add this as a build target in the main build.xml.