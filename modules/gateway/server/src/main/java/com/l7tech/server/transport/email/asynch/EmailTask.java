package com.l7tech.server.transport.email.asynch;

import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.server.transport.email.EmailListenerConfig;
import com.l7tech.server.transport.email.EmailHandler;
import com.l7tech.server.transport.email.EmailHandlerImpl;
import com.l7tech.server.transport.email.EmailListenerRuntimeException;

import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * An email task is a task to process a just received email message.
 */
public class EmailTask implements Runnable {
    private static final Logger _logger = Logger.getLogger(EmailTask.class.getName());

    /** The Jms endpoint properties */
    private final EmailListenerConfig emailListenerCfg;
    /** The Jms message to process */
    protected final MimeMessage message;
    /** The request handler that invokes the message processor */
    private EmailHandler handler;
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
    public EmailTask(final EmailListenerConfig emailListenerCfg, MimeMessage message) {
        this.emailListenerCfg = emailListenerCfg;
        this.message = message;

        // do we need to pool handlers?
        this.handler = new EmailHandlerImpl(emailListenerCfg.getApplicationContext());

        // intitialize error list
        this.errors = new ArrayList();

        this.parentListener = "tobepopulated";
    }

    /**
     * Task execution.  This method performs the call to the EmailHandler and handles any errors
     * accordingly.
     */
    @Override
    public final void run() {
        // call the handler to process the message
        try {
            handleMessage();
        } catch (EmailListenerRuntimeException ex) {
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
     * @throws EmailListenerRuntimeException when the RequestHandler encounters errors while processing the message
     */
    protected void handleMessage() throws EmailListenerRuntimeException {
        // call the request handler to invoke the MessageProcessor
        HybridDiagnosticContext.put(
                GatewayDiagnosticContextKeys.EMAIL_LISTENER_ID,
                emailListenerCfg.getEmailListener().getGoid().toString() );
        try {
            handler.onMessage(emailListenerCfg, message);
        } finally {
            HybridDiagnosticContext.remove( GatewayDiagnosticContextKeys.EMAIL_LISTENER_ID );
        }

    }

    /**
     * Cleanup object references so resources can be GC'd.
     */
    private void cleanup() {
        this.handler = null;
    }
}
