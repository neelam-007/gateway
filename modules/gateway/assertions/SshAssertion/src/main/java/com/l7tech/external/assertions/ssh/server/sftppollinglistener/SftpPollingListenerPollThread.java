package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

import com.l7tech.util.ExceptionUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/*
 * Polling thread logic.
 */
public class SftpPollingListenerPollThread extends Thread {
    /** The amount of time the thread sleeps when the MAXIMUM_OOPSES limit is reached */
    protected final AtomicInteger oopsSleep = new AtomicInteger(SftpPollingListener.DEFAULT_OOPS_SLEEP);

    private SftpPollingListener sftpPollingListener;

    public SftpPollingListenerPollThread(SftpPollingListener sftpPollingListener, String threadName) {
        super(threadName);
        this.sftpPollingListener = sftpPollingListener;
    }

    /**
     * poll listener thread logic
     */
    @Override
    public final void run() {
        sftpPollingListener.log(Level.INFO, SftpPollingListenerMessages.INFO_LISTENER_POLLING_START, sftpPollingListener.getResourceConfig().getName());

        int oopses = 0;
        String messageFilename;
        String lastMessageFilename = null;
        boolean retryLastMsg = false;
        SftpPollingListenerResource resourceConfig = sftpPollingListener.getResourceConfig();
        try {
            List<String> fileNames = new LinkedList<String>();
            while(!sftpPollingListener.isStop()) {
                try {
                    if(!retryLastMsg || lastMessageFilename == null) {
                        // look for files to process
                        if(fileNames.isEmpty()) {
                            sftpPollingListener.scanDirectoryForFiles(fileNames);
                        }

                        // if still empty, sleep then check again for files
                        if(fileNames.isEmpty()) {
                            try {
                                Thread.sleep(resourceConfig.getPollingInterval() * 1000);
                            } catch(InterruptedException ie) {
                                // ignore
                            }
                            continue;
                        }

                        // work with the first file from list
                        messageFilename = fileNames.remove(0);

                        sftpPollingListener.log(Level.FINE, SftpPollingListenerMessages.INFO_LISTENER_RECEIVE_MSG,
                                new Object[]{resourceConfig.getName(), messageFilename});

                        retryLastMsg = false;
                        lastMessageFilename = null;
                    } else {
                        retryLastMsg = false;
                        messageFilename = lastMessageFilename;
                    }

                    if ( messageFilename != null && !sftpPollingListener.isStop() ) {
                        oopses = 0;

                        // process the message
                        lastMessageFilename = messageFilename;
                        sftpPollingListener.handleFile(messageFilename);
                        messageFilename = null;
                    }
                } catch ( Throwable e ) {
                    if (ExceptionUtils.causedBy(e, InterruptedException.class)) {
                        sftpPollingListener.log(Level.FINE, "SFTP polling listener on {0} caught throwable: " + ExceptionUtils.getMessage(e),
                                ExceptionUtils.getDebugException(e));
                        messageFilename = null;
                        continue;
                    }

                    if (!ExceptionUtils.causedBy(e, RejectedExecutionException.class)) {
                        sftpPollingListener.log(Level.WARNING, sftpPollingListener.formatMessage(
                                SftpPollingListenerMessages.WARN_LISTENER_RECEIVE_ERROR,
                                resourceConfig.getName()),
                                ExceptionUtils.getDebugException(e));
                        sftpPollingListener.cleanup();
                    } else {
                        sftpPollingListener.log(Level.WARNING, "Running out of threads in the SFTP polling listener ThreadPool, " +
                                "consider increasing the sftpPolling.listenerThreadLimit : {0}", e.getMessage());
                        retryLastMsg = true;
                    }

                    if ( ++oopses < SftpPollingListener.MAXIMUM_OOPSES ) {
                        // sleep for a short period of time before retrying
                        sftpPollingListener.log(Level.FINE, "SFTP polling listener on {0} sleeping for {1} milliseconds.",
                                new Object[]{resourceConfig, sftpPollingListener.OOPS_RETRY});
                        try {
                            Thread.sleep(SftpPollingListener.OOPS_RETRY);
                        } catch ( InterruptedException e1 ) {
                            sftpPollingListener.log(Level.INFO, SftpPollingListenerMessages.INFO_LISTENER_POLLING_INTERRUPTED, new Object[]{"retry interval"});
                        }
                    } else {
                        // max oops reached .. sleep for a longer period of time before retrying
                        int sleepTime = oopsSleep.get();
                        sftpPollingListener.log(Level.WARNING, SftpPollingListenerMessages.WARN_LISTENER_MAX_OOPS_REACHED,
                                new Object[]{resourceConfig.getName(), SftpPollingListener.MAXIMUM_OOPSES, sleepTime});
                        try {
                            Thread.sleep(sleepTime);
                        } catch ( InterruptedException e1 ) {
                            sftpPollingListener.log(Level.INFO, SftpPollingListenerMessages.INFO_LISTENER_POLLING_INTERRUPTED, new Object[]{"sleep interval"});
                        }
                    }
                }
            }
        } finally {
            sftpPollingListener.log(Level.INFO, SftpPollingListenerMessages.INFO_LISTENER_POLLING_STOP, resourceConfig.getName());
            sftpPollingListener.cleanup();
        }
    }
}
