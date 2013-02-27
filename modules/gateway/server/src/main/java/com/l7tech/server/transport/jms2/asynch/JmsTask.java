package com.l7tech.server.transport.jms2.asynch;

import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.transport.jms.JmsUtil;
import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.JmsRequestHandlerImpl;
import com.l7tech.server.transport.jms2.JmsResourceManager;
import com.l7tech.util.ExceptionUtils;

import javax.jms.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Worker task that handles inbound Jms requests.  This task is used in conjunction with the
 * PooledPollingEmailListenerImpl to handle asynchronous processing of Jms messages.
 *
 * @see com.l7tech.server.transport.email.asynch.PooledPollingEmailListenerImpl
 * @author: vchan
 */
class JmsTask implements Runnable {

    private static final Logger _logger = Logger.getLogger(JmsTask.class.getName());

    /** The Jms endpoint properties */
    private final JmsEndpointConfig endpointCfg;
    /** Holds the Jms connection/session for this task */
    private JmsTaskBag jmsBag;
    /** The message consumer (receiver) that was used to de-queue the message - used in the cleanup only */
    private MessageConsumer consumer;
    /** The Jms message to process */
    protected final Message jmsMessage;
    /** The failure queue */
    private final Queue failureQ;
    /** The MessageProducer for the failure queue */
    private MessageProducer failureProducer;
    /** The request handler that invokes the message processor */
    private JmsRequestHandlerImpl handler;


    private JmsResourceManager resourceManager;

    /**
     * Constructor.
     */
    JmsTask( final JmsEndpointConfig endpointCfg,
             final JmsTaskBag jmsBag,
             final Message jmsMessage,
             final Queue failureQ,
             final MessageConsumer consumer,
             final JmsResourceManager resourceManager)
    {
        this.endpointCfg = endpointCfg;
        this.consumer = consumer;
        this.jmsBag = jmsBag;
        this.jmsMessage = jmsMessage;
        this.failureQ = failureQ;
        this.handler = new JmsRequestHandlerImpl(endpointCfg.getApplicationContext());
        this.resourceManager = resourceManager;
    }

    /**
     * Task execution.  This method performs the call to the JmsRequestHandler and handling any errors
     * accordingly.
     */
    @Override
    public final void run() {

        // call the handler to process the message
        try {

            // if not transactional, then ACK the message to remove from queue
            if ( !endpointCfg.isTransactional() )
                jmsMessage.acknowledge();

            handleMessage();
        } catch (JMSException e) {
            _logger.log( Level.WARNING, "Error acknowledging message: " + JmsUtil.getJMSErrorMessage(e), ExceptionUtils.getDebugException( e ) );
        } catch (JmsRuntimeException e) {
            final JMSException jmsException = e.getCause() instanceof JMSException ? (JMSException) e.getCause() : null;
            String detail = "";
            if ( jmsException != null ) {
                detail = ", due to " + JmsUtil.getJMSErrorMessage(jmsException);
            }
            _logger.log( Level.WARNING, "Error handling message: " + ExceptionUtils.getMessage( e ) + detail, ExceptionUtils.getDebugException( e ) );
        } finally {
            cleanup();
        }
    }

    /**
     * Perform the actual work on the message by invoking the MessageProcessor (execute Policy, routing, etc).
     *
     * @throws JmsRuntimeException when the RequestHandler encounters errors while processing the message
     */
    void handleMessage() throws JmsRuntimeException {
        // call the request handler to invoke the MessageProcessor
        HybridDiagnosticContext.put(
                GatewayDiagnosticContextKeys.JMS_LISTENER_ID,
                Long.toString( endpointCfg.getEndpoint().getOid() ) );
        try {
            handler.onMessage(endpointCfg, jmsBag, endpointCfg.isTransactional(), createFailureProducer(), jmsMessage);
        } finally {
            HybridDiagnosticContext.remove( GatewayDiagnosticContextKeys.JMS_LISTENER_ID );
        }
    }

    /**
     * Creates the MessageProducer for the failure queue.
     */
    private MessageProducer createFailureProducer() throws JmsRuntimeException {

        final JmsBag bag = jmsBag;
        if (failureQ != null && bag != null) {
            _logger.finest( "Getting new failure MessageProducer" );
            try {
                failureProducer = JmsUtil.createMessageProducer( bag.getSession(), failureQ );
            } catch (JMSException jex) {
                throw new JmsRuntimeException(jex);
            }
        }
        return failureProducer;
    }


    /**
     * Cleanup object references so resources can be GC'd.
     */
    void cleanup() {

        this.handler = null;

        if ( failureProducer != null ) {
            try {
                failureProducer.close();
            } catch ( JMSException e ) {
                handleCleanupError( "failure producer", e );
            }
        }

        if ( consumer != null ) {
            try {
                consumer.close();
            } catch ( JMSException e ) {
                handleCleanupError( "consumer", e );
            }
        }

        try {
            // return the jms bag
            resourceManager.returnJmsBag(this.endpointCfg, this.jmsBag);
        } catch (JmsRuntimeException e) {
            handleCleanupError("Return Jms Session", e);
        }
        this.jmsBag = null;
    }

    private void handleCleanupError( final String detail, final Exception exception ) {
        _logger.log(
                Level.WARNING,
                "Error during JmsTask cleanup ("+detail+"): " + ExceptionUtils.getMessage( exception ),
                ExceptionUtils.getDebugException(exception) );
    }

}
