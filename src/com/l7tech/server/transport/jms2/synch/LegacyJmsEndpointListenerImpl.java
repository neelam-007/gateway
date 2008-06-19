package com.l7tech.server.transport.jms2.synch;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.transport.jms.JmsUtil;
import com.l7tech.server.transport.jms2.AbstractJmsEndpointListener;
import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.JmsRequestHandlerImpl;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 *
 * @author: vchan
 */
public class LegacyJmsEndpointListenerImpl extends AbstractJmsEndpointListener {

    private static final Logger _logger = Logger.getLogger(LegacyJmsEndpointListenerImpl.class.getName());

    final private JmsRequestHandlerImpl _handler;

    private JmsBag _jmsBag;
    private QueueReceiver _consumer;
    private Queue _queue;
    private QueueSender _failureSender;
    private Queue _failureQueue;

    private final Object sync = new Object();

    /**
     * @see com.l7tech.server.transport.jms2.AbstractJmsEndpointListener#AbstractJmsEndpointListener(com.l7tech.server.transport.jms2.JmsEndpointConfig)
     */
    public LegacyJmsEndpointListenerImpl(final JmsEndpointConfig endpointConfig) {
        super(endpointConfig);

        // changed back to constructor init
//        this._handler = (JmsRequestHandlerImpl) getEndpointConfig().getApplicationContext().getBean("jmsRequestHandler", JmsRequestHandlerImpl.class);
        this._handler = new JmsRequestHandlerImpl(endpointConfig.getApplicationContext());
    }

    /**
     * @see com.l7tech.server.transport.jms2.AbstractJmsEndpointListener#getJmsBag()
     */
    protected JmsBag getJmsBag() throws JMSException, NamingException, JmsConfigException {
        synchronized(sync) {
            if ( _jmsBag == null ) {
                _logger.finest( "Getting new JmsBag" );
                _jmsBag = JmsUtil.connect(_endpointCfg, _endpointCfg.isTransactional(), Session.CLIENT_ACKNOWLEDGE);
            }
            return _jmsBag;
        }
    }

    /**
     * @see com.l7tech.server.transport.jms2.AbstractJmsEndpointListener#handleMessage(javax.jms.Message)
     */
    protected void handleMessage(Message jmsMessage) throws JmsRuntimeException {

        try {
            if ( !_endpointCfg.isTransactional() ) {

                // if not transactional, then ACK the message to remove from queue
                jmsMessage.acknowledge();
            }

            _handler.onMessage(getEndpointConfig(), getJmsBag(), getEndpointConfig().isTransactional(), getFailureSender(), jmsMessage);

        } catch (JMSException ex) {
            throw new JmsRuntimeException(ex);

        } catch (JmsConfigException ex) {
            throw new JmsRuntimeException(ex);

        } catch (NamingException ex) {
            throw new JmsRuntimeException(ex);
        }
    }

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

    protected QueueSender getFailureSender() throws JMSException, NamingException, JmsConfigException {
        synchronized(sync) {
            if ( _failureSender == null &&
                    _endpointCfg.isTransactional() &&
                    _endpointCfg.getEndpoint().getFailureDestinationName()!=null ) {

                _logger.finest( "Getting new MessageSender" );
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
                    Queue q = getFailureQueue();
                    _failureSender = qs.createSender( q );
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
            return _failureSender;
        }
    }

    protected Queue getFailureQueue() throws NamingException, JmsConfigException, JMSException {
        synchronized(sync) {
            if ( _failureQueue == null ) {
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
     * @see com.l7tech.server.transport.jms2.AbstractJmsEndpointListener#cleanup()
     */
    protected void cleanup() {

        synchronized (sync) {

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

            // close the failureSender
            if ( _failureSender != null ) {
                try {
                    _failureSender.close();
                } catch ( JMSException e ) {
                    _logger.log( Level.INFO, "Caught JMSException during cleanup", e );
                }
                _failureSender = null;
            }

            _failureQueue = null;

            // call parent cleanup method
            super.cleanup();

            // close the Jms connection artifacts
            if ( _jmsBag != null ) {
                // this will close the session and cause rollback if transacted
                _jmsBag.close();
                _jmsBag = null;
            }
        }
    }
}
