/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.server.management.NodeStateType;
import com.l7tech.server.management.api.node.NodeApi;
import com.l7tech.server.management.config.node.PCNodeConfig;
import com.l7tech.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @author alex */
class StartingNodeState extends ProcessController.SimpleNodeState implements ProcessController.HasProcess, ProcessController.HasApi {
    private static final Logger logger = Logger.getLogger(StartingNodeState.class.getName());

    private final AtomicBoolean outputDoneSignal;
    private final OutputCollectorThread stdoutThread, stderrThread;
    private final Process process;
    private final ProcessController processController;

    private volatile NodeApi api;

    StartingNodeState(ProcessController processController, PCNodeConfig node) throws IOException {
        super(node, NodeStateType.STARTING);
        this.processController = processController;

        final Process proc = processController.spawnProcess(node);
        proc.getOutputStream().close(); // We don't need no steenkin' stdin
        this.outputDoneSignal = new AtomicBoolean(false);

        stdoutThread = new OutputCollectorThread(node.getName(), "stdout", proc.getInputStream(), outputDoneSignal);
        stderrThread = new OutputCollectorThread(node.getName(), "stderr", proc.getErrorStream(), outputDoneSignal);
        stdoutThread.start();
        stderrThread.start();
        process = proc;
    }

    StartStatus getStatus() {
        long now = System.currentTimeMillis();
        if (now - sinceWhen < ProcessController.NODE_START_TIME_MIN) {
            logger.fine(node.getName() + " hasn't had enough time to start yet; not going to bother checking on it");
            return STARTING;
        }

        try {
            int errorlevel = process.exitValue();
            logger.warning(node.getName() + " exited with status " + errorlevel);
            outputDoneSignal.set(true);
            final Pair<byte[], byte[]> byteses = finishOutput();

            return new Died(errorlevel, byteses.left, byteses.right);
        } catch (IllegalThreadStateException e) {
            logger.fine(node.getName() + " isn't dead yet!");
        }

        final NodeApi api = processController.getNodeApi(node);
        try {
            api.ping();
            this.api = api;
            outputDoneSignal.set(true); // We're live
            return STARTED;
        } catch (Exception e) {
            logger.fine(node.getName() + " is still starting...");
            return STARTING;
        }
    }

    public NodeApi getApi() {
        return api;
    }

    static abstract class StartStatus { }
    static final StartStatus STARTING = new StartStatus() {}; // TODO needs more Scala "case object"
    static final StartStatus STARTED = new StartStatus() {};
    static final class Died extends StartStatus {
        final int exitValue;
        final byte[] stdout;
        final byte[] stderr;

        private Died(int exitValue, byte[] stdout, byte[] stderr) {
            this.exitValue = exitValue;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }

    Pair<byte[], byte[]> finishOutput() {
        outputDoneSignal.set(true);
        byte[] outbytes = finishOutput("stdout", stdoutThread);
        byte[] errbytes = finishOutput("stderr", stderrThread);
        return new Pair<byte[],byte[]>(outbytes, errbytes);
    }

    private byte[] finishOutput(final String what,
                                final OutputCollectorThread collectorThread)
    {
        byte[] bytes = null;
        try {
            bytes = collectorThread.os.toByteArray();
            collectorThread.join(1000);
            return bytes;
        } catch (InterruptedException e) {
            logger.warning("Interrupted waiting for " + what + " collector thread to terminate");
            return bytes; // Will never actually be null
        } finally {
            collectorThread.os.close();
        }
    }

    public Process getProcess() {
        return process;
    }

    static class OutputCollectorThread extends Thread {
        private final String node;
        private final String what;
        private final InputStream is;
        private final BufferPoolByteArrayOutputStream os;
        private final AtomicBoolean quitter;

        public OutputCollectorThread(String node, String what, InputStream is, AtomicBoolean quitter) {
            super(node + " " + what + " " + " Reader");
            this.node = node;
            this.what = what;
            this.is = is;
            this.os = new BufferPoolByteArrayOutputStream(16384);
            this.quitter = quitter;
            setDaemon(false);
        }

        @Override
        public void run() {
            byte[] buf = new byte[4096];
            int len;
            try {
                logger.info(node + " " + what + " reader starting");
                while (true) {
                    if (is.available() == 0) {
                        try {
                            logger.fine(node + " " + what + " reader waiting for data");
                            sleep(250);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e); // Can't happen
                        }
                    } else {
                        len = is.read(buf);
                        if (len > 0) {
                            os.write(buf, 0, len);
                            logger.fine(node + " " + what + " reader got " + len + " bytes");
                        } else if (len == -1) {
                            logger.info(node + " " + what + " at EOF");
                        }
                    }

                    if (quitter.get()) {
                        logger.info(node + " " + what + " reader done");
                        break;
                    }
                }
            } catch (IOException e) {
                logger.log(Level.INFO, node + " " + what + " reader couldn't read", e);
            }
        }
    }

}
