To run the fully automated AutoTest test suite:
1. Checkout the IntegrationTest project from Subversion.
2. Update the settings in IntegrationTest/ManagerAutomator/manager_automator.properties file.
3. Update the uneasyrooster.ssmBuild.dir and ssm.jre.dir properties in the IntegrationTest/ManagerAutomator/build.xml file.
4. Checkout the AutoTest project from Subversion into the IntegrationTest directory with the name "AutoTest".
5. Update the settings in the IntegrationTest/AutoTest/etc/junit.properties file.
6. Update the uneasyrooster.ssbBuild.dir and ssb.jre.dir properties in the IntegrationTest/AutoTest/build.xml file.
7. Install ant somewhere and set the ANT_HOME environment variable.
8. Checkout the UneasyRooster project from Subversion into the IntegrationTest directory with the name "UneasyRooster".

Directory Structure:
IntegrationTest
+--ant
+--AutoTest
+--bin
+--builds
+--etc
+--ManagerAutomator
+--new_build
+--UneasyRooster
+--snmptrapd
+--AutoTestLogParser

