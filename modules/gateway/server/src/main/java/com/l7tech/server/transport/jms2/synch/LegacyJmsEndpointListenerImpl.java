package com.l7tech.server.transport.jms2.synch;

import com.l7tech.server.transport.jms2.JmsConnectionMaxWaitException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.transport.jms.JmsUtil;
import com.l7tech.server.transport.jms2.AbstractJmsEndpointListener;
import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.JmsRequestHandlerImpl;

import javax.jms.*;
import javax.naming.NamingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 *
 * @author: vchan
 */
class LegacyJmsEndpointListenerImpl extends AbstractJmsEndpointListener {

    private static final Logger _logger = Logger.getLogger(LegacyJmsEndpointListenerImpl.class.getName());

    private final JmsRequestHandlerImpl _handler;

    private MessageProducer _failureProducer;

    /**
     * @see com.l7tech.server.transport.jms2.AbstractJmsEndpointListener#AbstractJmsEndpointListener(com.l7tech.server.transport.jms2.JmsEndpointConfig,java.util.logging.Logger)
     */
    LegacyJmsEndpointListenerImpl(final JmsEndpointConfig endpointConfig) {
        super(endpointConfig, _logger);

        // changed back to constructor init
        this._handler = new JmsRequestHandlerImpl(endpointConfig.getApplicationContext());
    }

    MessageProducer getFailureProducer() throws JMSException, NamingException, JmsConnectionMaxWaitException, JmsRuntimeException {
        synchronized(sync) {
            if ( _failureProducer == null &&
                 _endpointCfg.isTransactional() &&
                 _endpointCfg.getEndpoint().getFailureDestinationName() != null ) {

                _logger.finest( "Getting new MessageProducer" );
                boolean ok = false;
                String message = null;
                try {
                    final JmsBag bag = getJmsBag();
                    final Session session = bag.getSession();
                    final Queue failureQueue = getFailureQueue();
                    _failureProducer = JmsUtil.createMessageProducer( session, failureQueue );
                    ok = true;

                } catch (JMSException e) {
                    message = ExceptionUtils.getMessage(e);
                    throw e;
                } catch (NamingException e) {
                    message = ExceptionUtils.getMessage(e);
                    throw e;
                } catch (JmsConnectionMaxWaitException e) {
                    message = ExceptionUtils.getMessage(e);
                    throw e;
                } catch (RuntimeException e) {
                    message = ExceptionUtils.getMessage(e);
                    throw e;
                } catch (JmsRuntimeException e) {
                    message = ExceptionUtils.getMessage(e);
                    throw e;
                } finally {
                    if (!ok) {
                        fireConnectError(message);
                    }
                }
            }
            return _failureProducer;
        }
    }

    /**
     * @see com.l7tech.server.transport.jms2.AbstractJmsEndpointListener#cleanup()
     */
    @Override
    protected void cleanup() {
        synchronized (sync) {

            // close the failureSender
            if ( _failureProducer != null ) {
                try {
                    _failureProducer.close();
                } catch ( JMSException e ) {
                    _logger.log( Level.INFO, "Caught JMSException during cleanup", e );
                }
                _failureProducer = null;
            }

            // call parent cleanup method
            super.cleanup();
        }
    }
}
