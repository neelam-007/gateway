package com.l7tech.server.transport.jms2.asynch;

import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.transport.jms.JmsUtil;
import com.l7tech.server.transport.jms2.AbstractJmsEndpointListener;
import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.JmsMessages;
import com.l7tech.util.ExceptionUtils;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.NamingException;
import java.beans.PropertyChangeEvent;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author: vchan
 */
public class PooledJmsEndpointListenerImpl extends AbstractJmsEndpointListener {

    private static final Logger _logger = Logger.getLogger(PooledJmsEndpointListenerImpl.class.getName());

    /** JmsBag that holds A Connection for the jms endpoint */
    private JmsBag _connBag;
    /** Inbound queue receiver */
    private QueueReceiver _consumer;
    /** Inbound jms queue */
    private Queue _queue;
    /** The failure queue */
    private Queue _failureQueue;
    /** Mutex object */
    private final Object sync = new Object();

    /**
     * Constructor.
     *
     * @param endpointConfig attributes for the Jms endpoint to listen to
     */
    public PooledJmsEndpointListenerImpl(final JmsEndpointConfig endpointConfig) {
        super(endpointConfig);
    }

    /**
     * @see com.l7tech.server.transport.jms2.AbstractJmsEndpointListener#getJmsBag()
     */
    protected JmsBag getJmsBag() throws JMSException, NamingException, JmsConfigException {

        synchronized(sync) {
            if (_connBag == null) {
                _logger.finest( "Getting new JmsBag" );
                _connBag = JmsUtil.connect(_endpointCfg, _endpointCfg.isTransactional(), Session.CLIENT_ACKNOWLEDGE);
            }
        }
        return _connBag;
    }

    /**
     *
     * @param bag
     * @return
     * @throws JMSException
     * @throws NamingException
     * @throws JmsConfigException
     */
    protected JmsTaskBag handOffJmsBag(JmsBag bag)  throws JMSException, NamingException, JmsConfigException {

        synchronized (sync) {
            JmsTaskBag handOff = new JmsTaskBag(bag.getJndiContext(), bag.getConnectionFactory(), bag.getConnection(), bag.getSession());

            // replace the jms connection bag with a new session
            _connBag = null;
            _connBag = JmsUtil.connect(handOff.getJndiContext(),
                    handOff.getConnection(), handOff.getConnectionFactory(),
                    _endpointCfg.isTransactional(), Session.CLIENT_ACKNOWLEDGE);

            return handOff;
        }
    }

    /**
     * @see com.l7tech.server.transport.jms2.AbstractJmsEndpointListener#handleMessage(javax.jms.Message)
     */
    @SuppressWarnings({"unchecked"})
    protected final void handleMessage(Message jmsMessage) throws JmsRuntimeException {

        // move message Ack to the JmsTask

        // create the JmsTask
        JmsTask task = newJmsTask(jmsMessage);

        try {
            // fire-and-forget
            JmsThreadPool.getInstance().newTask(task);

        } catch (RejectedExecutionException reject) {
            _logger.log(Level.WARNING, JmsMessages.WARN_THREADPOOL_LIMIT_REACHED, new String[] {ExceptionUtils.getMessage(reject)});
            task.cleanup();
            throw new JmsRuntimeException(reject);
        }
    }


    /**
     * Creates a Jms task for a request message.  To be sent to the JmsThreadPool for processing.
     *
     * @param jmsMessage the message for the task to run
     * @return new JmsTask instance for the given request
     * @throws JmsRuntimeException
     */
    protected JmsTask newJmsTask(Message jmsMessage) throws JmsRuntimeException {

        boolean ok = true;
        JmsTaskBag taskBag = null;
        try {

            // create the work task
            taskBag = handOffJmsBag(getJmsBag());
            JmsTask task = new JmsTask(getEndpointConfig(), taskBag, jmsMessage, getFailureQueue(), _consumer);
            return task;

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
                // sinse the consumer (QueueReceiver) is associated with the previous session,
                // remove it so that the next read will create a new one
                _consumer = null;
            } else if (taskBag != null) {
                taskBag.close();
            }
        }
    }

    /**
     * @see com.l7tech.server.transport.jms2.AbstractJmsEndpointListener#getConsumer()
     */
    protected QueueReceiver getConsumer() throws JMSException, NamingException, JmsConfigException {
        synchronized(sync) {
            if ( _consumer == null ) {
                _logger.finest( "Getting new MessageConsumer" );
                boolean ok = false;
                String message = null;
                try {
                    JmsBag bag = getJmsBag();
                    Session s = bag.getSession();
                    if ( !(s instanceof QueueSession) ) {
                        message = "Only QueueSessions are supported";
                        throw new JmsConfigException(message);
                    }
                    QueueSession qs = (QueueSession)s;
                    Queue q = getQueue();
                    _consumer = qs.createReceiver( q );
                    ok = true;
                } catch (JMSException e) {
                    message = ExceptionUtils.getMessage(e);
                    throw e;
                } catch (NamingException e) {
                    message = ExceptionUtils.getMessage(e);
                    throw e;
                } catch (JmsConfigException e) {
                    message = ExceptionUtils.getMessage(e);
                    throw e;
                } catch (RuntimeException e) {
                    message = ExceptionUtils.getMessage(e);
                    throw e;
                } finally {
                    if (!ok) {
                        fireConnectError(message);
                    }
                }
            }
            return _consumer;
        }
    }

    protected Queue getQueue() throws NamingException, JmsConfigException, JMSException {
        synchronized(sync) {
            if ( _queue == null ) {
                _logger.finest( "Getting new Queue" );
                JmsBag bag = getJmsBag();
                Context context = bag.getJndiContext();
                String qname = _endpointCfg.getEndpoint().getDestinationName();
                _queue = (Queue)context.lookup( qname );
            }
            return _queue;
        }
    }

    protected Queue getFailureQueue() throws NamingException, JmsConfigException, JMSException {
        synchronized(sync) {
            if ( _failureQueue == null &&
                    _endpointCfg.isTransactional() &&
                    _endpointCfg.getEndpoint().getFailureDestinationName() != null)
            {
                _logger.finest( "Getting new FailureQueue" );
                JmsBag bag = getJmsBag();
                Context context = bag.getJndiContext();
                String qname = _endpointCfg.getEndpoint().getFailureDestinationName();
                _failureQueue = (Queue)context.lookup( qname );
            }
            return _failureQueue;
        }
    }

    /**
     * @see com.l7tech.server.transport.jms2.AbstractJmsEndpointListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent evt) {

        super.propertyChange(evt);

        // also check if the JmsThreadPool properties were changed
        JmsThreadPool.getInstance().propertyChange(evt);
    }

    /**
     * @see com.l7tech.server.transport.jms2.AbstractJmsEndpointListener#cleanup()
     */
    protected void cleanup() {
        
        // close the consumer
        if ( _consumer != null ) {
            try {
                _consumer.close();
            } catch ( JMSException e ) {
                _logger.log( Level.INFO, "Caught JMSException during cleanup", e );
            }
            _consumer = null;
        }

        _queue = null;
        _failureQueue = null;

        super.cleanup();

        if ( _connBag != null ) {
            // this will close the session and cause rollback if transacted
            _connBag.close();
            _connBag = null;
        }
    }

}