package com.l7tech.server.transport.jms2.asynch;

import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.JmsRequestHandlerImpl;
import com.l7tech.util.ExceptionUtils;

import javax.jms.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Worker task that handles inbound Jms requests.  This task is used in conjunction with the
 * JmsThreadPool to handle asynchronous processing of Jms messages.
 *
 * @see com.l7tech.server.transport.jms2.asynch.JmsThreadPool
 * @author: vchan
 */
public class JmsTask implements Runnable {

    private static final Logger _logger = Logger.getLogger(JmsTask.class.getName());

    /** The Jms endpoint properties */
    private final JmsEndpointConfig endpointCfg;
    /** Holds the Jms connection/session for this task */
    private JmsTaskBag jmsBag;
    /** The message consumer (receiver) that was used to de-queue the mssage - used in the cleanup only */
    private MessageConsumer consumer;
    /** The Jms message to process */
    protected final Message jmsMessage;
    /** The failure queue */
    private final Queue failureQ;
    /** The QueueSender for the failure queue */
    private QueueSender failureSender;
    /** The request handler that invokes the message processor */
    private JmsRequestHandlerImpl handler;
    /** Flag specifying whether the task is complete */
    private boolean complete;
    /** Flag specifying whether */
    private boolean success;
    /** List of errors */
    private List errors;

    /**
     * Constructor.
     */
    public JmsTask(final JmsEndpointConfig endpointCfg,
                   final JmsTaskBag jmsBag,
                   final Message jmsMessage,
                   final Queue failureQ,
                   final MessageConsumer consumer)
    {
        this.endpointCfg = endpointCfg;
        this.consumer = consumer;
        this.jmsBag = jmsBag;
        this.jmsMessage = jmsMessage;
        this.failureQ = failureQ;
        this.handler = new JmsRequestHandlerImpl(endpointCfg.getApplicationContext());

        // intitialize error list
        this.errors = new ArrayList();
    }

    /**
     * Task execution.  This method performs the call to the JmsRequestHandler and handling any errors
     * accordingly.
     */
    public final void run() {

        // call the handler to process the message
        try {

            // if not transactional, then ACK the message to remove from queue
            if ( !endpointCfg.isTransactional() )
                jmsMessage.acknowledge();

            handleMessage();

        } catch (JMSException jex) {
            _logger.warning("Error encountered while acknowledging message: " + ExceptionUtils.getMessage(jex));

        } catch (JmsRuntimeException ex) {

            _logger.info("Runtime exception encountered: " + ex);

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
     * @throws JmsRuntimeException when the RequestHandler encounters errors while processing the message
     */
    protected void handleMessage() throws JmsRuntimeException {

        // call the request handler to invoke the MessageProcessor
        handler.onMessage(endpointCfg, jmsBag, endpointCfg.isTransactional(), createFailureSender(), jmsMessage);

    }

    /**
     * Creates the QueueSender for the failure queue.  Needed by the RequestHandler.
     *
     * @return the failure QueueSender
     * @throws JmsRuntimeException
     */
    private QueueSender createFailureSender() throws JmsRuntimeException {

        if (failureQ != null) {
            _logger.finest( "Getting new MessageSender" );
            boolean ok = false;
            String message = null;
            try {
                JmsBag bag = jmsBag;
                Session s = bag.getSession();
                if ( !(s instanceof QueueSession) ) {
                    message = "Only QueueSessions are supported";
                    throw new JmsConfigException(message);
                }
                QueueSession qs = (QueueSession)s;
                failureSender = qs.createSender(failureQ);
                ok = true;

            } catch (JMSException jex) {
                throw new JmsRuntimeException(jex);

            } catch (JmsConfigException cex) {
                throw new JmsRuntimeException(cex);

            } finally {
                if (!ok) {
                    // TODO: do what now?
//                    fireConnectError(message);
                }
            }
        }
        return failureSender;
    }


    /**
     * Cleanup object references so resources can be GC'd.
     */
    protected void cleanup() {

        this.handler = null;

        // close the queueSender
        if (this.failureSender != null) {
            try {
                failureSender.close();
            } catch (JMSException jex) {
                // ignore at this point
            }
        }

        // close the message consumer
        try {
            if (consumer != null)
                consumer.close();
        } catch (JMSException jex) {
            // ignore at this point
        }

        // close the jms session
        this.jmsBag.close();
        this.jmsBag = null;
    }
}
