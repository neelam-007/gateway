package com.l7tech.server.transport.jms2.asynch;

import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.transport.jms2.AbstractJmsEndpointListener;
import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.JmsMessages;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ThreadPool;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.naming.NamingException;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author vchan
 */
class PooledJmsEndpointListenerImpl extends AbstractJmsEndpointListener {

    private static final Logger _logger = Logger.getLogger(PooledJmsEndpointListenerImpl.class.getName());
    private static final long CLOSE_CONNECTION_WAIT = 3000L; //3 secs

    private final ThreadPoolBean threadPoolBean;

    /**
     * Constructor.
     *
     * @param endpointConfig attributes for the Jms endpoint to listen to
     * @param threadPoolBean Thread pool bean which JmsTasks can be submitted to.
     */
    PooledJmsEndpointListenerImpl(final JmsEndpointConfig endpointConfig, final ThreadPoolBean threadPoolBean) {
        super(endpointConfig, _logger);
        this.threadPoolBean = threadPoolBean;
    }

    /**
     *
     */
    JmsTaskBag handOffJmsBag(JmsBag bag) throws JMSException, JmsRuntimeException {

        synchronized (sync) {
            JmsTaskBag handOff = new JmsTaskBag(bag);

            // replace the jms connection bag with a new session
            _jmsBag = null;
            _jmsBag = resourceManager.borrowJmsBag(_endpointCfg);

            return handOff;
        }
    }

    @Override
    protected void beforeCleanup() {
        if (threadPoolBean.getActiveCount() != 0) {
            try {
                //give a change for running task to complete
                Thread.sleep(CLOSE_CONNECTION_WAIT);
            } catch (InterruptedException e) {
                _logger.info("Cleanup wait interrupted.");
            }
        }
    }

    private static boolean isConnectionPoolingEnabled() {
        return ConfigFactory.getCachedConfig().getIntProperty( "ioJmsConnectionCacheSize", 1 ) != 0;
    }

    /**
     * @see com.l7tech.server.transport.jms2.AbstractJmsEndpointListener#handleMessage(javax.jms.Message)
     */
    @Override
    @SuppressWarnings({"unchecked"})
    protected final void handleMessage(Message jmsMessage) throws JmsRuntimeException {

        // move message Ack to the JmsTask

        // create the JmsTask
        JmsTask task = newJmsTask(jmsMessage);

        if (!isConnectionPoolingEnabled()) {
            // Pooling is disabled for WebLogic compatibility. When enabled we fail to
            // reconnect to WebLogic when the WebLogic server is restarted.
            task.run();
            return;
        }

        try {
            // fire-and-forget
            threadPoolBean.submitTask(task);

        } catch (RejectedExecutionException reject) {
            _logger.log(Level.WARNING, JmsMessages.WARN_THREADPOOL_LIMIT_REACHED, new String[] {ExceptionUtils.getMessage(reject)});
            task.cleanup();
            throw new JmsRuntimeException(reject);
        } catch (ThreadPool.ThreadPoolShutDownException e) {
            _logger.log(Level.WARNING, "Cannot submit JmsTask to queue as it has been shutdown", ExceptionUtils.getDebugException(e));
            task.cleanup();
            throw new JmsRuntimeException(e);
        }
    }


    /**
     * Creates a Jms task for a request message.  To be sent to the threadPoolBean for processing.
     *
     * @param jmsMessage the message for the task to run
     * @return new JmsTask instance for the given request
     * @throws JmsRuntimeException
     */
    JmsTask newJmsTask(Message jmsMessage) throws JmsRuntimeException {

        boolean ok = true;
        JmsTaskBag taskBag = null;
        try {

            // create the work task
            taskBag = handOffJmsBag(getJmsBag());
            MessageConsumer consumer;
            synchronized(sync) {
                consumer = _consumer;
            }
            return new JmsTask(getEndpointConfig(), taskBag, jmsMessage, getFailureQueue(), consumer, resourceManager);

        } catch (JMSException jex) {
            ok = false;
            throw new JmsRuntimeException("From handleMessage", jex);

        } catch (JmsConfigException cex) {
            ok = false;
            throw new JmsRuntimeException("From handleMessage", cex);

        } catch (NamingException nex) {
            ok = false;
            throw new JmsRuntimeException("From handleMessage", nex);

        } finally {
            if (ok) {
                // since the consumer is associated with the previous session,
                // remove it so that the next read will create a new one
                synchronized(sync) {
                    _consumer = null;
                }
            } else if (taskBag != null) {
                taskBag.close();
            }
        }
    }
}
