
Usage:  

 java -jar SoftwareDbExtractor.jar <extract|store> <configDir> <ksPath> <ksPassphrase>


Example - Extract Software DB to PKCS#12 file

 $JAVA_HOME/bin/java -jar SoftwareDbExtractor.jar extract /opt/SecureSpan/Gateway/node/default/etc/conf /tmp/test.p12 7layer


Example - Replace Software DB contents from PKCS#12 file

 $JAVA_HOME/bin/java -jar SoftwareDbExtractor.jar store /opt/SecureSpan/Gateway/node/default/etc/conf /tmp/test.p12 7layer
