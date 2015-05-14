How to build sample solution kit skar file (e.g. SimpleSolutionKit-1.0.skar).

prompt> cd <l7_workspace>/modules/skunkworks/build/example/solutionkit/simple/v01_00
prompt> zip -X SimpleSolutionKit-1.0.skar -j ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_00/SolutionKit.xml
         ../../../../../src/main/resources/com/l7tech/example/solutionkit/simple/v01_00/InstallBundle.xml

Possible improvement: look into how to add this as a build target in the main build.xml.