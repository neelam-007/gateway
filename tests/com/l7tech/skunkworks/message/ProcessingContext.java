/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message;

import com.l7tech.policy.assertion.credential.LoginCredentials;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds request and response messages during processing, and provides a place to keep state throughout
 * message processing for both policy enforcement (SSG) and policy application (SSB).
 */
public abstract class ProcessingContext {
    private static final Logger logger = Logger.getLogger(ProcessingContext.class.getName());

    private final Message request;
    private final Message response;

    private LoginCredentials credentials;

    public ProcessingContext(Message request, Message response) {
        if (request == null || response == null) throw new NullPointerException();
        this.request = request;
        this.response = response;
    }

    public final LoginCredentials getCredentials() {
        return credentials;
    }

    public final void setCredentials(LoginCredentials credentials) {
        this.credentials = credentials;
    }

    public final Message getRequest() {
        return request;
    }

    public final Message getResponse() {
        return response;
    }

    /**
     * Free any resources being used by this ProcessingContext.  After this call, the behaviour of other methods
     * called on this ProcessingContext, or its Request or Response, is undefined.
     * <p>
     * This method calls {@link Message#close()} on {@link #request} and {@link #response}.
     * Subclasses that override this method must either chain to <code>super.close()</code> or
     * close the request and response themselves.
     */
    public void close() {
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
