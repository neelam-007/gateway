/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.message;

import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.util.Closeable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds request and response messages during processing, and provides a place to keep state throughout
 * message processing for both policy enforcement (SSG) and policy application (SSB).
 */
public abstract class ProcessingContext implements Closeable {
    private static final Logger logger = Logger.getLogger(ProcessingContext.class.getName());

    private final Message request;
    private final Message response;

    private List<LoginCredentials> credentials = new ArrayList<LoginCredentials>();
    private LoginCredentials lastCredentials;
    private final List runOnClose = new ArrayList();
    private boolean isAuthenticationMissing = false;

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
    }

    /**
     * Returns only one set of credentials.
     *
     * @return null if there are no credentials present, a LoginCredentials if there is only one present
     */
    public LoginCredentials getLastCredentials() {
        return lastCredentials;
    }

    public List<LoginCredentials> getCredentials() {
        return credentials;
    }

    public void addCredentials(LoginCredentials credentials) {
        for (LoginCredentials l : this.credentials) {
            if (l.getCredentialSourceAssertion() == null && credentials.getCredentialSourceAssertion() == null) {
                logger.warning("A credential of type null was already added in this context");
                return;
            } else if (l.getCredentialSourceAssertion().equals(credentials.getCredentialSourceAssertion().getClass())) {
                logger.warning("A credential of type " + l.getCredentialSourceAssertion().getName() +
                               " was already added in this context");
                return;
            }
        }
        this.credentials.add(credentials);
        lastCredentials = credentials;
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

    /**
     * Free any resources being used by this ProcessingContext, and run all {@link #runOnClose} {@link Runnable}s.
     * After this call, the behaviour of other methods
     * called on this ProcessingContext, or its Request or Response, is undefined.
     * <p>
     * This method calls {@link Message#close()} on {@link #request} and {@link #response}.
     * Subclasses that override this method must either chain to <code>super.close()</code> or
     * close the request and response themselves.
     */
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

    /**
     * Check if some authentication credentials that were expected in the request were not found.
     * (For PolicyEnforcementContext, this implies PolicyViolated, as well.)
     *
     * @return true if the policy expected credentials, but they weren't present
     */
    public boolean isAuthenticationMissing() {
        return isAuthenticationMissing;
    }

    /**
     * Report that some authentication credentials that were expected in the request were not found.
     * This implies requestPolicyViolated, as well.
     */
    public void setAuthenticationMissing() {
        isAuthenticationMissing = true;
    }
}
