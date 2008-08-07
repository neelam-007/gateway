/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.server.management.config.host.HostConfig;
import com.l7tech.server.management.config.node.PCNodeConfig;
import com.l7tech.util.ResourceUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @author alex */
public class ProcessController {
    private static final Logger logger = Logger.getLogger(ProcessController.class.getName());

    @Resource
    private ConfigService configService;
    private ProcessBuilder processBuilder;

    @PostConstruct
    public void start() {
        logger.info("Starting");

        final HostConfig.OSType osType = configService.getGateway().getOsType();
        final String[] cmds;
        switch(osType) {
            case RHEL:
                cmds = new String[] { "/ssg/jdk/bin/java", "-jar", "/ssg/Gateway.jar" };
//                cmds = new String[] { "/ssg/bin/partitionControl.sh", "run", node.getName() };
//                env = null;
                break;
            default:
                throw new UnsupportedOperationException();
        }

        processBuilder = new ProcessBuilder(cmds);
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().clear();
        processBuilder.environment().put("SSG_HOME", "/ssg");
    }

    public void startNode(final PCNodeConfig node) {
        Thread t = new Thread("Service Node Babysitter-" + node.getName()) {
            @Override
            public void run() {
                final Process proc;
                try {
                    proc = processBuilder.start();

                    proc.getOutputStream().close();
                    
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e); // Can't happen
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e); // Can't happen
                }

                int port = node.getProcessControllerApiPort();
                

                final BufferPoolByteArrayOutputStream stdoutCollector = new BufferPoolByteArrayOutputStream(8192);
                InputStream stdout = null;
                final BufferPoolByteArrayOutputStream stderrCollector = new BufferPoolByteArrayOutputStream(8192);
                InputStream stderr = null;
                final AtomicBoolean quitSignal = new AtomicBoolean(false);
                try {
                    stdout = proc.getInputStream();
                    startOutputCollector(node.getName(), stdout, "stdout", stdoutCollector, quitSignal);
                    stderr = proc.getErrorStream();
                    startOutputCollector(node.getName(), stderr, "stderr", stderrCollector, quitSignal);

                    try {
                        // TODO loop on attempting to connect to the PC
                        sleep(10000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e); // Can't happen
                    }

                    quitSignal.set(true);

                    byte[] outBytes = stdoutCollector.toByteArray();
                    logger.info("Got " + outBytes.length + " stdout bytes");
                    if (outBytes.length > 0) {
                        try {
                            logger.info(new String(outBytes, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e); // Can't happen
                        }
                    }
                    byte[] errBytes = stdoutCollector.toByteArray();
                    logger.info("Got " + errBytes.length + " stderr bytes");
                    if (errBytes.length > 0) {
                        try {
                            logger.info(new String(errBytes, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e); // Can't happen
                        }
                    }
                } finally {
                    ResourceUtils.closeQuietly(stdout);
                    ResourceUtils.closeQuietly(stderr);
                }
            }
        };

        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e); // Can't happen
        }
    }

    private void startOutputCollector(final String node, final InputStream is, final String what, final OutputStream os, final AtomicBoolean quitter) {
        new Thread("Service Node " + what + " Reader-" + node) {
            { setDaemon(false); }
            
            @Override
            public void run() {
                byte[] buf = new byte[4096];
                int len;
                try {
                    logger.info("Reading " + node + "'s " + what + "...");
                    while (true) {
                        if (is.available() == 0) {
                            try {
                                logger.fine("Waiting for " + node + "'s " + what);
                                sleep(250);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e); // Can't happen
                            }
                        } else {
                            len = is.read(buf);
                            if (len > 0) {
                                os.write(buf, 0, len);
                                logger.info("Got " + len + " bytes from " + node + "'s " + what);
                            } else if (len == -1) {
                                logger.info(node + "'s " + what + " at EOF");
                            }
                        }

                        if (quitter.get()) {
                            logger.info("Received done signal");
                            break;
                        }
                    }
                } catch (IOException e) {
                    logger.log(Level.INFO, "Couldn't read child " + what, e);
                }
            }
        }.start();
    }

    public void doStuff() {
        System.out.println("Gateway: " + configService.getGateway());
    }

    public ConfigService getConfigService() {
        return configService;
    }

    /**
     * Note that this method actually does get invoked, but for some reason the log message doesn't get flushed in time
     * to survive shutdown.
     */
    @PreDestroy
    public void stop() {
        logger.info("Stopping");
    }
}
