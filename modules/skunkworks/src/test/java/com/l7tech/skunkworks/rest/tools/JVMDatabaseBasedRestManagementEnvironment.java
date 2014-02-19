package com.l7tech.skunkworks.rest.tools;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.SerializationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This starts an instance of the DatabaseBasedRestManagementEnvironment in a separate jvm.
 */
public class JVMDatabaseBasedRestManagementEnvironment {
    private static final Logger logger = Logger.getLogger(JVMDatabaseBasedRestManagementEnvironment.class.getName());

    private Process process;
    private Scanner scanner;
    private PrintWriter printWriter;

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());

    public JVMDatabaseBasedRestManagementEnvironment(String rdAddress) throws IOException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome +
                File.separator + "bin" +
                File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String className = DatabaseBasedRestManagementEnvironment.class.getCanonicalName();
        List<String> command = new ArrayList<>();
        command.add(javaBin);
        if(rdAddress != null && runFromIdea()){
            command.add("-agentlib:jdwp=transport=dt_shmem,server=y,suspend=n,address=" + rdAddress);
        }
        command.add(className);
        ProcessBuilder builder = new ProcessBuilder(command);
        logger.log(Level.INFO, builder.command().toString());
        builder.environment().put("CLASSPATH", classpath);
        process = builder.start();
        executor.execute(new Runnable() {
            public void run() {
                try {
                    //copy the error streams so that we can see the logging.
                    IOUtils.copyStream(process.getErrorStream(), System.err);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Error copying error messages: " + e.getMessage(), e);
                }
            }
        });

        printWriter = new PrintWriter(process.getOutputStream(), true);

        scanner = new Scanner(process.getInputStream());
        //wait till the DatabaseBasedRestManagementEnvironment has started. It will print 'ServerStarted' on its output.
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.startsWith(DatabaseBasedRestManagementEnvironment.FATAL_EXCEPTION)) {
                logger.log(Level.WARNING, "Environment could not be properly started: " + line);
                throw new RuntimeException("Environment could not be properly started: " + line);
            } else if (line.startsWith(DatabaseBasedRestManagementEnvironment.SERVER_STARTED)) {
                break;
            } else if (line.startsWith("Listening for transport")) {
                logger.log(Level.INFO, line);
            } else {
                logger.log(Level.WARNING, "Unknown message received: " + line);
                IOUtils.copyStream(process.getErrorStream(), System.err);
                executor.execute(new Runnable() {
                    public void run() {
                        try {
                            //copy the error streams so that we can see the logging.
                            IOUtils.copyStream(process.getInputStream(), System.out);
                        } catch (IOException e) {
                            logger.log(Level.WARNING, "Error copying error messages: " + e.getMessage(), e);
                        }
                    }
                });
                throw new RuntimeException("Unknown message received: " + line);
            }
        }
    }

    private boolean runFromIdea() {
        return (System.getProperty("sun.java.command") != null && System.getProperty("sun.java.command").contains("com.intellij.rt.execution.junit.JUnitStarter"));
    }

    public RestResponse processRequest(String uri, HttpMethod method, @Nullable String contentType, @NotNull String body) throws Exception {
        return processRequest(uri, null, method, contentType, body);
    }

    public RestResponse processRequest(String uri, String queryString, HttpMethod method, @Nullable String contentType, String body) throws Exception {
        // send the request to the other jvm process
        printWriter.println(DatabaseBasedRestManagementEnvironment.PROCESS);

        byte[] requestBytes = SerializationUtils.serialize(new RestRequest(uri, queryString, method, contentType, body));
        printWriter.println(Arrays.toString(requestBytes));

        //unmarshall and return the RestResponse
        String restResponseSerialized = scanner.nextLine();
        if (restResponseSerialized.startsWith(DatabaseBasedRestManagementEnvironment.FATAL_EXCEPTION)) {
            logger.log(Level.WARNING, "Exception processing a message: " + restResponseSerialized);
            throw new RuntimeException("Exception processing a message: " + restResponseSerialized);
        }
        List<Byte> bytes = Functions.map(Arrays.asList(restResponseSerialized.substring(1, restResponseSerialized.length() - 1).split(",")), new Functions.Unary<Byte, String>() {
            @Override
            public Byte call(String s) {
                return Byte.parseByte(s.trim());
            }
        });
        RestResponse response = (RestResponse) SerializationUtils.deserialize(ArrayUtils.toPrimitive(bytes.toArray(new Byte[bytes.size()])));
        logger.log(Level.INFO, response.toString());
        return response;
    }

    /**
     * Close and destroy the other test process.
     */
    public void close() {
        final CountDownLatch closeLatch = new CountDownLatch(1);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    logger.log(Level.INFO, "Shutting Down.");
                    printWriter.println(DatabaseBasedRestManagementEnvironment.EXIT);
                    process.waitFor();
                    int exitValue = process.exitValue();
                    if (exitValue != 0) {
                        logger.log(Level.WARNING, "Bad Exit Value: " + exitValue);
                    }
                } catch (InterruptedException e) {
                    process.destroy();
                    logger.log(Level.WARNING, "Failed to properly close environment: " + e.getMessage());
                } finally {
                    closeLatch.countDown();
                }
            }
        });
        boolean completedSuccessfully = false;
        try {
            completedSuccessfully = closeLatch.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            process.destroy();
            logger.log(Level.WARNING, "Trouble shutting down JVM", e);
        }
        if (!completedSuccessfully) {
            process.destroy();
            logger.log(Level.WARNING, "Trouble shutting down JVM");
        }
        executor.shutdown();
    }
}
