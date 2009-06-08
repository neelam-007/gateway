-------------------------------------------------------------------------------
SecureSpan Manager Java API version 4.3
Layer 7 Technologies Inc.
February 2008
-------------------------------------------------------------------------------

The Manager Java API allows programmatical integration with the SecureSpan
Gateway (SSG). See the included java samples for more information on how to use this
api.

When running the java samples, all jars contained in the ./lib directory need
to be in the classpath. These samples require the availability of a
SecureSpan Gateway version 5.0 deployed and having a valid license.

eg. On Linux compiling and running the samples would look something like this:
-cd $MANAGER_API_HOME/samples/src/com/l7tech/example/manager/apidemo
-$JAVA_HOME/bin/javac -classpath .:<$MANAGER_API_HOME>/lib/* *.java
-cd $MANAGER_API_HOME/samples/src
-$JAVA_HOME/bin/javac -classpath .:<$MANAGER_API_HOME>/lib/* com.l7tech.example.manager.apidemo.Main

To get started writing your own code, look at Main.java located in
./sample/src/com/l7tech/example/manager/apidemo

Note that to run the samples (or any other application using the Manager API) trust must
already have been established with the SSG the Manager API will be connecting to.
The easiest way to do this is to connect the SecureSpan Manager to the gateway once.
This will add the SSG's SSL certificate to the trust store (located at ~/.l7tech/trustStore).
