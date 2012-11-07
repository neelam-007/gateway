/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.server.management.NodeStateType;
import com.l7tech.server.management.api.node.NodeApi;
import com.l7tech.server.management.config.node.PCNodeConfig;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;

import javax.xml.ws.soap.SOAPFaultException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
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
    private Died died = null;

    private volatile ProcessController.HasApi api;

    StartingNodeState(ProcessController processController, PCNodeConfig node) throws IOException {
        super(node, NodeStateType.STARTING);
        this.processController = processController;

        logger.info(node.getName() + " starting");

        final Process proc = processController.spawnProcess(node);
        proc.getOutputStream().close(); // We don't need no steenkin' stdin
        this.outputDoneSignal = new AtomicBoolean(false);

        stdoutThread = new OutputCollectorThread(node.getName(), "stdout", proc.getInputStream(), outputDoneSignal);
        stderrThread = new OutputCollectorThread(node.getName(), "stderr", proc.getErrorStream(), outputDoneSignal);
        stdoutThread.start();
        stderrThread.start();
        process = proc;
    }

    /**
     * Get the start status of this starting node, assumed to be represented by our owned subprocess.
     * <p/>
     * If the process we created has exited, this returns StartStatus.Died with the exit value and output.
     * Otherwise, this method just calls {@link #getStartStatusUsingPing()}.
     *
     * @return the start status: STARTING, STARTED, or an instance of Died.
     */
    StartStatus getStartStatusOfSubprocess() {
        long now = System.currentTimeMillis();
        if (now - sinceWhen < processController.getNodeStartTimeMin()) {
            logger.fine(node.getName() + " hasn't had enough time to start yet; not going to bother checking on it");
            return STARTING;
        }

        if (died != null)
            return died;

        try {
            int errorlevel = process.exitValue();
            logger.warning(node.getName() + " exited with status " + errorlevel);
            outputDoneSignal.set(true);
            final Pair<byte[], byte[]> byteses = finishOutput();
            died = new Died(errorlevel, byteses.left, byteses.right);
            return died;
        } catch (IllegalThreadStateException e) {
            logger.fine(node.getName() + " isn't dead yet!");
        }

        return getStartStatusUsingPing();
    }

    /**
     * Get the start status of this node, which may or may not be our owned subprocess (eg, we may be taking control
     * of an uncontrolled Gateway started by someone else).
     * <p/>
     * This method attempts to ping the node via the node API and returns a status based on the result.
     *
     * @return STARTED if ping is successful (or would be except that node control is disabled); new Died if the node
     *         is running but declines to have the PC take control of it because it is not configured for use with the PC;
     *         or STARTING in all other cases (eg, ping fails for connection refused or any other reason not listed).
     */
    StartStatus getStartStatusUsingPing() {
        final ProcessController.HasApi api = processController.getNodeApi(node);
        try {
            NodeApi nodeApi = api.getApi(false);
            if ( nodeApi != null ) {
                nodeApi.ping();
                this.api = api;
                outputDoneSignal.set(true); // We're live
                logger.info(node.getName() + " started successfully");
                return STARTED;
            }
        } catch (Exception e) {
            if ( processController.isDisabledApiException(e) ) {
                logger.info(node.getName() + " started successfully, but node control is disabled.");
                this.api = api;
                outputDoneSignal.set(true); // We're live
                return STARTED;
            } else if (e instanceof SOAPFaultException) {
                SOAPFaultException sfe = (SOAPFaultException)e;
                if (NodeApi.NODE_NOT_CONFIGURED_FOR_PC.equals(sfe.getFault().getFaultString())) {
                    logger.warning(node.getName() + " is already running but has not been configured for use with the PC; will not continue pinging it.");

                    outputDoneSignal.set(true);
                    final Pair<byte[], byte[]> byteses = finishOutput();
                    died = new Died(-1, byteses.left, byteses.right);
                    return died;
                }
            }

            if (ExceptionUtils.causedBy(e, SocketException.class)) {
                logger.fine(node.getName() + " is still starting...");
            } else {
                logger.log(Level.WARNING, node.getName() + " may still be starting, but API is throwing unexpected exceptions", e);
            }
        }

        return STARTING;
    }

    @Override
    public NodeApi getApi( final boolean fast ) {
        return api==null ?  null : api.getApi( fast );
    }

    @Override
    public ProcessController.HasApi getApiHaver() {
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

    @Override
    public Process getProcess() {
        return process;
    }

    static class OutputCollectorThread extends Thread {
        private final String node;
        private final String what;
        private final InputStream is;
        private final PoolByteArrayOutputStream os;
        private final AtomicBoolean quitter;

        public OutputCollectorThread(String node, String what, InputStream is, AtomicBoolean quitter) {
            super(node + " " + what + " " + " Reader");
            this.node = node;
            this.what = what;
            this.is = is;
            this.os = new PoolByteArrayOutputStream(16384);
            this.quitter = quitter;
            setDaemon(true);
        }

        @Override
        public void run() {
            byte[] buf = new byte[4096];
            int len;
            try {
                logger.fine(node + " " + what + " reader starting");
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
                        logger.fine(node + " " + what + " reader done");
                        break;
                    }
                }
            } catch (IOException e) {
                logger.log(Level.INFO, node + " " + what + " reader couldn't read", e);
            }
        }
    }

}
