package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

import com.l7tech.util.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Worker task that handles file from the SFTP server.
 */
public class SftpPollingListenerFileHandlerTask implements Runnable {

    private static final Logger _logger = Logger.getLogger(SftpPollingListenerFileHandlerTask.class.getName());

    private SftpPollingListenerConfig listenerConfig;
    private ThreadSafeSftpClient sftpClient;
    private String fileName;  // file to process
    private SftpPollingListenerFileHandler fileHandler;   // fileHandler for this task
    private boolean complete;   // flag specifying whether the task is complete
    private boolean success;   // flag specifying whether the task is a success
    private List errors;

    public SftpPollingListenerFileHandlerTask(final SftpPollingListenerConfig listenerCfg,
                                              final ThreadSafeSftpClient sftpPollingListenerResBag,
                                              final String fileName) {
        this.listenerConfig = listenerCfg;
        this.sftpClient = sftpPollingListenerResBag;
        this.fileName = fileName;
        this.fileHandler = new SftpPollingListenerFileHandler(listenerCfg.getApplicationContext());

        // initialize error list
        this.errors = new ArrayList();
    }

    /**
     * Task execution.  This method performs the call to the file handler.
     */
    @Override
    public final void run() {
        // call the fileHandler to process the file
        try {
            handleFile();
        } catch (SftpPollingListenerRuntimeException ex) {
            _logger.log(Level.WARNING, "Runtime exception encountered: " + ExceptionUtils.getMessage(ex), ExceptionUtils.getDebugException(ex));
        } finally {
            if (errors.isEmpty())
                success = true;
            complete = true;
            cleanup();
        }
    }

    /**
     * Perform the actual work on the message by invoking the MessageProcessor (execute Policy, routing, etc).
     *
     * @throws SftpPollingListenerRuntimeException when the RequestHandler encounters errors while processing the message
     */
    protected void handleFile() throws SftpPollingListenerRuntimeException {
        // call the fileHandler to invoke the MessageProcessor
        fileHandler.onMessage(listenerConfig.getSftpPollingListenerResource(), sftpClient, fileName);
    }

    /**
     * Cleanup object references so resources can be GC'd.
     */
    protected void cleanup() {
        this.listenerConfig = null;
        this.sftpClient = null;
        this.fileName = null;
        this.fileHandler = null;
        this.errors = null;
    }
}