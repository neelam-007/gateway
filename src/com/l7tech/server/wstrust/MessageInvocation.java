package com.l7tech.server.wstrust;

import org.w3c.dom.Document;

import java.util.logging.Logger;

import com.l7tech.policy.assertion.credential.LoginCredentials;

/**
 * Message invocation. Hooks into message chain invocatoin via
 * <code>MessageInvocation.invokeNext()</code> method.
 *
 * @author emil
 * @version 12-Aug-2004
 */
public class MessageInvocation {
    private final Logger logger = Logger.getLogger(getClass().getName());

    private Document requestDocument;
    private Document responseDocument;
    private MessageContext messageContext;
    private Handler[] handlers;
    private LoginCredentials credentials;
    private int index = 0;

    public MessageInvocation(Document requestDocument, MessageContext messageContext, Handler[] handlers) {
        if (handlers == null) {
            throw new IllegalArgumentException();
        }
        if (requestDocument == null) {
            throw new IllegalArgumentException();
        }
        if (messageContext == null) {
            throw new IllegalArgumentException();
        }
        this.requestDocument = requestDocument;
        this.messageContext = messageContext;
        this.handlers = handlers;
    }

    public Document getRequestDocument() {
        return requestDocument;
    }

    public Document getResponseDocument() {
        return responseDocument;
    }

    public void setResponseDocument(Document responseDocument) {
        this.responseDocument = responseDocument;
    }

    public MessageContext getMessageContext() {
        return messageContext;
    }

    public LoginCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(LoginCredentials credentials) {
        this.credentials = credentials;
    }

    /**
     * Invokes next invocation in chain.
     */
    public void invokeNext() throws Throwable {
        if (handlers == null || this.index == handlers.length) {
        } else
            this.handlers[this.index++].invoke(this);
    }
}
