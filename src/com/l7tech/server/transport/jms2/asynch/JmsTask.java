package com.l7tech.server.transport.jms2.asynch;

import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.JmsRequestHandlerImpl;

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
    /** The parent listener that this task was created by */
    private final String parentListener;

    /**
     * Constructor.
     */
    public JmsTask(final JmsEndpointConfig endpointCfg, JmsTaskBag jmsBag, Message jmsMessage, Queue failureQ) {
        this.endpointCfg = endpointCfg;
        this.jmsBag = jmsBag;
        this.jmsMessage = jmsMessage;
        this.failureQ = failureQ;

        // do we need to pool handlers?
//        this.handler = (JmsRequestHandlerImpl)endpointCfg.getApplicationContext().getBean("jmsRequestHandler", JmsRequestHandlerImpl.class);
        this.handler = new JmsRequestHandlerImpl(endpointCfg.getApplicationContext());

        // intitialize error list
        this.errors = new ArrayList();

        this.parentListener = "tobepopulated";
    }

    /**
     * Task execution.  This method performs the call to the JmsRequestHandler and handling any errors
     * accordingly.
     */
    public final void run() {

//        try {
//            _logger.info(Thread.currentThread().getName() + ": handling message=" + jmsMessage.getJMSMessageID());
//        } catch (JMSException ex) {} // ignore

        // call the handler to process the message
        try {
            handleMessage();

        } catch (JmsRuntimeException ex) {

            // possible actions
            // -- check response message sent?
            // -- let timeout expire?
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
     * @throws JMSException
     * @throws JmsConfigException
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
//                message = ExceptionUtils.getMessage(jex);
                throw new JmsRuntimeException(jex);

            } catch (JmsConfigException cex) {
//                message = ExceptionUtils.getMessage(cex);
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
    private void cleanup() {

        this.handler = null;

        // close the queueSender
        if (this.failureSender != null) {
            try {
                failureSender.close();
            } catch (JMSException jex) {
                // log and ignore
            }
        }

//        // close the jms session
        this.jmsBag.close();
        this.jmsBag = null;
    }
}
