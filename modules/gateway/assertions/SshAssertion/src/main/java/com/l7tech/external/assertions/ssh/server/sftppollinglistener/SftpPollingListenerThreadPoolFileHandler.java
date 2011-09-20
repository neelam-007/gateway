package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ThreadPool;

import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pooled thread implementation of the SFTP polling listener.
 */
public class SftpPollingListenerThreadPoolFileHandler extends SftpPollingListener {
    private static final Logger _logger = Logger.getLogger(SftpPollingListenerThreadPoolFileHandler.class.getName());

    private final ThreadPoolBean threadPoolBean;

    public SftpPollingListenerThreadPoolFileHandler(SftpPollingListenerConfig sftpPollingListenerCfg, ThreadPoolBean threadPoolBean) {
        super(sftpPollingListenerCfg);
        this.threadPoolBean = threadPoolBean;
    }

    @Override
    protected void handleFile(String filename) throws SftpPollingListenerRuntimeException {
        // This is where the magic needs to happen to push the info to message processor
        SftpPollingListenerFileHandlerTask task = createSftpPollingListenerTask(filename);
        try {
            threadPoolBean.submitTask(task);
        } catch (RejectedExecutionException reject) {
            _logger.log(Level.WARNING, SftpPollingListenerMessages.WARN_THREADPOOL_LIMIT_REACHED, new String[] {ExceptionUtils.getMessage(reject)});
            task.cleanup();
            throw new SftpPollingListenerRuntimeException(reject);
        } catch (ThreadPool.ThreadPoolShutDownException e) {
            _logger.log(Level.WARNING, "Cannot submit SFTP file handler task as the thread pool has been shutdown", ExceptionUtils.getDebugException(e));
            task.cleanup();
            throw new SftpPollingListenerRuntimeException(e);
        }
    }

    /**
     * Creates a work task for a request message.  To be sent to the threadPoolBean for processing.
     *
     * @param fileName the file for the file handler task to run
     * @return new file handler task
     * @throws SftpPollingListenerRuntimeException
     */
    protected SftpPollingListenerFileHandlerTask createSftpPollingListenerTask(String fileName)
    throws SftpPollingListenerRuntimeException
    {
        // create the file handler task
        try {
            return new SftpPollingListenerFileHandlerTask(getSftpPollingListenerConfig(), getSftpClient(), fileName);
        } catch (SftpPollingListenerConfigException splce) {
            throw new SftpPollingListenerRuntimeException("From handleFile(...): ", splce);
        } catch (IOException ioe) {
            throw new SftpPollingListenerRuntimeException("From handleFile(...): ", ioe);
        }
    }

    /**
     * @see SftpPollingListener
     */
    @Override
    protected void cleanup() {
        super.cleanup();
    }
}
