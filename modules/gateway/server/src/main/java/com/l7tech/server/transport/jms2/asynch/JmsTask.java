package com.l7tech.server.transport.jms2.asynch;

import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.transport.jms.JmsUtil;
import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.JmsRequestHandlerImpl;
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
public class JmsTask implements MessageListener {

    private static final Logger _logger = Logger.getLogger(JmsTask.class.getName());

    /** The Jms endpoint properties */
    private final JmsEndpointConfig endpointCfg;
    /** Holds the Jms connection/session for this task */
    private JmsBag jmsBag;
    /** The MessageProducer for the failure queue */
    private MessageProducer failureProducer;
    /** The request handler that invokes the message processor */
    private JmsRequestHandlerImpl handler;

    /**
     * Constructor.
     */
    public JmsTask( final JmsEndpointConfig endpointCfg,
             final JmsBag jmsBag)
    {
        this.endpointCfg = endpointCfg;
        this.jmsBag = jmsBag;
        this.handler = new JmsRequestHandlerImpl(endpointCfg.getApplicationContext());
    }

    /**
     * Task execution.  This method performs the call to the JmsRequestHandler and handling any errors
     * accordingly.
     */
    @Override
    public void onMessage(Message message) {

        // call the handler to process the message
        try {

            // if not transactional, then ACK the message to remove from queue
            if ( !endpointCfg.isTransactional() )
                message.acknowledge();

            handleMessage(message);
        } catch (JMSException e) {
            _logger.log( Level.WARNING, "Error handling message: " + JmsUtil.getJMSErrorMessage(e), ExceptionUtils.getDebugException( e ) );
        } catch (JmsRuntimeException e) {
            final JMSException jmsException = e.getCause() instanceof JMSException ? (JMSException) e.getCause() : null;
            String detail = "";
            if ( jmsException != null ) {
                detail = ", due to " + JmsUtil.getJMSErrorMessage(jmsException);
            }
            _logger.log( Level.WARNING, "Error handling message: " + ExceptionUtils.getMessage( e ) + detail, ExceptionUtils.getDebugException( e ) );
        }
    }

    /**
     * Perform the actual work on the message by invoking the MessageProcessor (execute Policy, routing, etc).
     *
     * @throws JmsRuntimeException when the RequestHandler encounters errors while processing the message
     */
    void handleMessage(Message message) throws JmsRuntimeException {
        // call the request handler to invoke the MessageProcessor
        HybridDiagnosticContext.put(
                GatewayDiagnosticContextKeys.JMS_LISTENER_ID,
                endpointCfg.getEndpoint().getGoid().toString());
        try {
            handler.onMessage(endpointCfg, jmsBag, endpointCfg.isTransactional(), message);
        } finally {
            HybridDiagnosticContext.remove( GatewayDiagnosticContextKeys.JMS_LISTENER_ID );
        }
    }

}
