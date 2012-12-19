package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

import com.jscape.inet.sftp.Sftp;
import com.jscape.inet.sftp.SftpFile;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.UnaryThrows;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static com.l7tech.gateway.common.transport.SsgActiveConnector.PROPERTIES_KEY_POLLING_INTERVAL;
import static java.text.MessageFormat.format;

/*
 * Polling thread logic.
 */
class SftpPollingListenerPollThread extends Thread {
    /** The amount of time the thread sleeps when the MAXIMUM_OOPSES limit is reached */
    protected final AtomicInteger oopsSleep = new AtomicInteger(SftpPollingListener.DEFAULT_OOPS_SLEEP);

    private final SftpPollingListener sftpPollingListener;
    private final String connectorInfo;

    SftpPollingListenerPollThread(SftpPollingListener sftpPollingListener, String threadName) {
        super(threadName);
        setDaemon( true );
        this.sftpPollingListener = sftpPollingListener;
        this.connectorInfo = sftpPollingListener.getDisplayName();
    }

    public void setOopsSleep(int oopsSleepInt) {
        this.oopsSleep.set(oopsSleepInt);
    }

    /**
     * poll listener thread logic
     */
    @Override
    public final void run() {
        sftpPollingListener.log(Level.INFO, SftpPollingListenerMessages.INFO_LISTENER_POLLING_START, connectorInfo);

        final SsgActiveConnector ssgActiveConnector = sftpPollingListener.getSsgActiveConnector();
        final long pollInterval = ssgActiveConnector.getLongProperty( PROPERTIES_KEY_POLLING_INTERVAL, 60L ) * 1000L;

        int oopses = 0;
        SftpFile currentFile;
        SftpFile previousFile = null;
        boolean retryLastMsg = false;
        try {
            final List<SftpFile> fileNames = new LinkedList<SftpFile>();
            while(!sftpPollingListener.isStop()) {
                try {
                    if(!retryLastMsg || previousFile == null) {
                        // look for files to process
                        if( fileNames.isEmpty() ) {
                            sftpPollingListener.doWithSftpClient( new UnaryThrows<Void, Sftp, IOException>() {
                                @Override
                                public Void call( final Sftp sftp ) throws IOException {
                                    fileNames.addAll( sftpPollingListener.scanDirectoryForFilesSetProcessing(sftp) );
                                    return null;
                                }
                            } );
                        }

                        // if still empty, sleep then check again for files
                        if(fileNames.isEmpty()) {
                            try {
                                Thread.sleep( pollInterval );
                            } catch(InterruptedException ie) {
                                // ignore
                            }
                            continue;
                        }

                        // work with the first file from list
                        currentFile = fileNames.remove(0);

                        sftpPollingListener.log(Level.FINE, SftpPollingListenerMessages.INFO_LISTENER_RECEIVE_MSG,
                                connectorInfo, currentFile.getFilename() );

                        retryLastMsg = false;
                        previousFile = null;
                    } else {
                        retryLastMsg = false;
                        currentFile = previousFile;
                    }

                    if ( currentFile != null && !sftpPollingListener.isStop() ) {
                        oopses = 0;

                        // process the message
                        previousFile = currentFile;
                        sftpPollingListener.handleFile(currentFile);
                    }
                } catch ( Throwable e ) {
                    if (ExceptionUtils.causedBy(e, InterruptedException.class)) {
                        sftpPollingListener.log(Level.FINE, "SFTP polling listener on {0} caught throwable: " + ExceptionUtils.getMessage(e),
                                ExceptionUtils.getDebugException(e));
                        continue;
                    }

                    if (!ExceptionUtils.causedBy(e, RejectedExecutionException.class)) {
                        sftpPollingListener.log(Level.WARNING, format(
                                SftpPollingListenerMessages.WARN_LISTENER_RECEIVE_ERROR,
                                connectorInfo, ExceptionUtils.getMessage( e ) ),
                                ExceptionUtils.getDebugException(e));
                        sftpPollingListener.cleanup();
                    } else {
                        sftpPollingListener.log(Level.WARNING, "Running out of threads in the SFTP polling listener ThreadPool, " +
                                "consider increasing the sftpPolling.listenerThreadLimit : {0}", e.getMessage());
                        retryLastMsg = true;
                    }

                    if ( ++oopses < SftpPollingListener.MAXIMUM_OOPSES ) {
                        // sleep for a short period of time before retrying
                        sftpPollingListener.log(Level.FINE, "SFTP polling listener ''{0}'' sleeping for {1} milliseconds.",
                                connectorInfo, SftpPollingListener.OOPS_RETRY );
                        try {
                            Thread.sleep( (long) SftpPollingListener.OOPS_RETRY );
                        } catch ( InterruptedException e1 ) {
                            sftpPollingListener.log(Level.INFO, SftpPollingListenerMessages.INFO_LISTENER_POLLING_INTERRUPTED, "retry interval" );
                        }
                    } else {
                        // max oops reached .. sleep for a longer period of time before retrying
                        int sleepTime = oopsSleep.get();
                        sftpPollingListener.log(Level.WARNING, SftpPollingListenerMessages.WARN_LISTENER_MAX_OOPS_REACHED,
                                connectorInfo, SftpPollingListener.MAXIMUM_OOPSES, sleepTime );
                        try {
                            Thread.sleep( (long) sleepTime );
                        } catch ( InterruptedException e1 ) {
                            sftpPollingListener.log(Level.INFO, SftpPollingListenerMessages.INFO_LISTENER_POLLING_INTERRUPTED, "sleep interval" );
                        }
                    }
                }
            }
        } finally {
            sftpPollingListener.log( Level.INFO, SftpPollingListenerMessages.INFO_LISTENER_POLLING_STOP, connectorInfo );
            sftpPollingListener.cleanup();
        }
    }
}
