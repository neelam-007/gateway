/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.message;

import java.io.Closeable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds request and response messages during processing, and provides a place to keep state throughout
 * message processing for both policy enforcement (SSG) and policy application (SSB).
 */
public abstract class ProcessingContext<CT extends CredentialContext> implements Closeable {
    private static final Logger logger = Logger.getLogger(ProcessingContext.class.getName());

    private final Message request;
    private final Message response;
    private final CT credentialContext = buildContext();
    private final Map<Message,CT> authenticationContexts = new HashMap<Message,CT>();

    private final List<Runnable> runOnClose = new ArrayList<Runnable>();

    /**
     * Create a processing context holding the specified request and response.
     *
     * @param request   request message holder, or null to create a new empty one.
     * @param response  response message holder, or null to create a new empty one.
     */
    public ProcessingContext(Message request, Message response) {
        if (request == null)
            request = new Message();
        if (response == null)
            response = new Message();
        this.request = request;
        this.response = response;
        request.notifyMessage(response, MessageRole.RESPONSE);
        response.notifyMessage(request, MessageRole.REQUEST);
    }

    public final Message getRequest() {
        return request;
    }

    public final Message getResponse() {
        return response;
    }

    public synchronized void runOnClose( Runnable runMe ) {
        runOnClose.add( runMe );
    }

    public CT getDefaultAuthenticationContext() {
        return credentialContext;
    }

    public CT getAuthenticationContext( final Message message ) {
        CT context;

        if ( message == getRequest() ) {
            context = credentialContext;
        } else {
            context = authenticationContexts.get(message);
            if ( context == null ) {
                context = buildContext();
                authenticationContexts.put(message, context);
            }
        }

        return context;
    }



    /**
     * Free any resources being used by this ProcessingContext, and run all {@link #runOnClose} {@link Runnable}s.
     * After this call, the behaviour of other methods
     * called on this ProcessingContext, or its Request or Response, is undefined.
     * <p>
     * This method calls {@link Message#close()} on {@link #request} and {@link #response}.
     * Subclasses that override this method must either chain to <code>super.close()</code> or
     * close the request and response themselves.
     */
    @Override
    public void close() {
        Runnable runMe;
        Iterator i = runOnClose.iterator();
        try {
            while ( i.hasNext() ) {
                runMe = (Runnable)i.next();
                try {
                    runMe.run();
                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Cleanup runnable threw exception", t);
                }
                i.remove();
            }
        } finally {
            try {
                response.close();
            } catch (Exception e) {
                logger.log(Level.INFO, "Caught exception closing response", e);
            } finally {
                try {
                    request.close();
                } catch (Exception e) {
                    logger.log(Level.INFO, "Caught exception closing request", e);
                }
            }
        }
    }

    protected abstract CT buildContext();
}
