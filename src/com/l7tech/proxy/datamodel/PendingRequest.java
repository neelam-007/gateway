/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.proxy.NullRequestInterceptor;
import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.ClientProxy;
import com.l7tech.xmlenc.Session;
import org.w3c.dom.Document;

import java.net.URL;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;

/**
 * Holds request state while the client proxy is processing it.
 * User: mike
 * Date: Jun 16, 2003
 * Time: 12:04:09 PM
 */
public class PendingRequest {
    private ClientProxy clientProxy;
    private Document soapEnvelope;
    private Document initialEnvelope;
    private Ssg ssg;
    private RequestInterceptor requestInterceptor = NullRequestInterceptor.INSTANCE;
    private String soapAction = "";
    private String uri = "";
    private SsgResponse lastErrorResponse = null; // Last response received from SSG in the case of 401 or 500 status
    private boolean isPolicyUpdated = false;
    private URL originalUrl = null;
    private Long nonce = null; // nonce.  set on-demand, and only set once

    // Policy settings, filled in by traversing policy tree
    private static class PolicySettings {
        private boolean isSslRequired = false;
        private boolean isClientCertRequired = false;
        private boolean isBasicAuthRequired = false;
        private boolean isDigestAuthRequired = false;
        private boolean isNonceRequired = false;
        private Session session = null; // session for the response to this request.  can't be changed once sent
    }
    private PolicySettings policySettings = new PolicySettings();

    /** Construct a PendingRequest around the given SOAPEnvelope going to the given SSG. */
    public PendingRequest(ClientProxy clientProxy, Document soapEnvelope, Ssg ssg, RequestInterceptor requestInterceptor) {
        this.clientProxy = clientProxy;
        this.soapEnvelope = soapEnvelope;
        this.initialEnvelope = soapEnvelope;
        this.ssg = ssg;
        setRequestInterceptor(requestInterceptor);
    }

    public PendingRequest(ClientProxy clientProxy, Document soapEnvelope, Ssg ssg, RequestInterceptor ri, URL origUrl) {
        this(clientProxy, soapEnvelope, ssg, ri);
        this.originalUrl = origUrl;
    }

    /**
     * Reset all policy settings in preperation for starting processing over again with a different policy.
     */
    public void reset() {
        policySettings = new PolicySettings();
        soapEnvelope = initialEnvelope;
    }

    public long getNonce() {
        if (nonce == null)
            try {
                nonce = new Long(SecureRandom.getInstance("SHA1PRNG").nextLong());
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("misconfigured VM", e);  // can't happen
            }
        return nonce.longValue();
   }

    // Getters and setters

    public ClientProxy getClientProxy() {
        return clientProxy;
    }

    /**
     * Get (a copy of) the Document representing the request.  This returns a copy that can be
     * modified freely.  If you will not be modifying the returned Document in any way, you can
     * get additional performance by using getSoapEnvelopeDirect() instead.
     *
     * If you want your changes to stick, you'll need to save them back by calling setSoapEnvelope().
     *
     * @return A copy of the SOAP envelope Document, which may be freely modified.
     */
    public Document getSoapEnvelope() {
        return (Document) soapEnvelope.cloneNode(true);
    }

    /**
     * Get the actual Document representing the request, which should not be modified.  Any change
     * to this Document will prevent the reset() method from returning this PendingRequest to
     * its original state.  If you need to change the Document, use getSoapEnvelope() instead.
     *
     * @return A reference to the SOAP envelope Document, which must not be modified in any way.
     */
    public Document getSoapEnvelopeDirectly() {
        return soapEnvelope;
    }

    /**
     * Replace the active Soap Envelope with a new one.  The original envelope is in any case kept
     * squirreled away so that reset() will work.
     * @param newEnvelope
     */
    public void setSoapEnvelope(Document newEnvelope) {
        soapEnvelope = newEnvelope;
    }

    public Ssg getSsg() {
        return ssg;
    }

    public String getSoapAction() {
        return soapAction;
    }

    public void setSoapAction(String soapAction) {
        this.soapAction = soapAction;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public RequestInterceptor getRequestInterceptor() {
        return requestInterceptor;
    }

    public void setRequestInterceptor(RequestInterceptor requestInterceptor) {
        if (requestInterceptor == null)
            throw new IllegalArgumentException("requestInterceptor mustn't be null; use NullRequestInterceptor");
        this.requestInterceptor = requestInterceptor;
    }

    public boolean isSslRequired() {
        return policySettings.isSslRequired;
    }

    public void setSslRequired(boolean sslRequired) {
        policySettings.isSslRequired = sslRequired;
    }

    public boolean isClientCertRequired() {
        return policySettings.isClientCertRequired;
    }

    public void setClientCertRequired(boolean clientCertRequired) {
        policySettings.isClientCertRequired = clientCertRequired;
    }

    public boolean isNonceRequired() {
        return policySettings.isNonceRequired;
    }

    public void setNonceRequired(boolean isNonceRequired) {
        policySettings.isNonceRequired = isNonceRequired;
    }

    public boolean isBasicAuthRequired() {
        return policySettings.isBasicAuthRequired;
    }

    public void setBasicAuthRequired(boolean basicAuthRequired) {
        policySettings.isBasicAuthRequired = basicAuthRequired;
    }

    public boolean isDigestAuthRequired() {
        return policySettings.isDigestAuthRequired;
    }

    public void setDigestAuthRequired(boolean digestAuthRequired) {
        policySettings.isDigestAuthRequired = digestAuthRequired;
    }

    public boolean isPolicyUpdated() {
        return isPolicyUpdated;
    }

    public void setPolicyUpdated(boolean policyUpdated) {
        isPolicyUpdated = policyUpdated;
    }

    /**
     * Get the sssion that was passed with the initial request.
     *
     * @return
     */
    public Session getSession() {
        return policySettings.session;
    }

    /**
     * Set the session that will be referred to in the request, and used to decrypt/etc the reply.
     * A reference to it is kept here so that even if the real session is invalidated on the Ssg
     * by another thread after the request is sent, we'll still be able to work with the outstanding
     * reply for this request.  (We can't just reissue the request at that point, since it will already
     * have been successfully passed through the ssg to the target server.)
     *
     * @param session
     */
    public void setSession(Session session) {
        policySettings.session = session;
    }

    public SsgResponse getLastErrorResponse() {
        return lastErrorResponse;
    }

    public void setLastErrorResponse(SsgResponse lastErrorResponse) {
        this.lastErrorResponse = lastErrorResponse;
    }

    public URL getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(URL originalUrl) {
        this.originalUrl = originalUrl;
    }
}
