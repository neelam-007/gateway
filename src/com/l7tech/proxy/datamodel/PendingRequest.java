/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.proxy.NullRequestInterceptor;
import com.l7tech.proxy.RequestInterceptor;
import org.w3c.dom.Document;

import java.net.URL;

/**
 * Holds request state while the client proxy is processing it.
 * User: mike
 * Date: Jun 16, 2003
 * Time: 12:04:09 PM
 */
public class PendingRequest {
    private Document soapEnvelope;
    private Document initialEnvelope;
    private Ssg ssg;
    private RequestInterceptor requestInterceptor = NullRequestInterceptor.INSTANCE;
    private String soapAction = "";
    private String uri = "";
    private SsgResponse lastErrorResponse = null; // Last response received from SSG in the case of 401 or 500 status
    private boolean isPolicyUpdated = false;
    private URL originalUrl = null;

    // Policy settings, filled in by traversing policy tree
    private static class PolicySettings {
        private boolean isSslRequired = false;
        private boolean isClientCertRequired = false;
        private boolean isBasicAuthRequired = false;
        private String httpBasicUsername = "";
        private char[] httpBasicPassword = "".toCharArray();
        private boolean isDigestAuthRequired = false;
        private String httpDigestUsername = "";
        private char[] httpDigestPassword = "".toCharArray();
        private boolean credentialsWouldHaveHelped = false;
        private boolean clientCertWouldHaveHelped = false;
    }
    private PolicySettings policySettings = new PolicySettings();

    /** Construct a PendingRequest around the given SOAPEnvelope going to the given SSG. */
    public PendingRequest(Document soapEnvelope, Ssg ssg, RequestInterceptor requestInterceptor) {
        this.soapEnvelope = soapEnvelope;
        this.initialEnvelope = soapEnvelope;
        this.ssg = ssg;
        setRequestInterceptor(requestInterceptor);
    }

    public PendingRequest(Document soapEnvelope, Ssg ssg, RequestInterceptor ri, URL origUrl) {
        this(soapEnvelope, ssg, ri);
        this.originalUrl = origUrl;
    }

    /**
     * Reset all policy settings in preperation for starting processing over again with a different policy.
     */
    public void reset() {
        policySettings = new PolicySettings();
        soapEnvelope = initialEnvelope;
    }

    // Getters and setters

    /**
     * Get (a copy of) the Document representing the request.  This returns a copy that can be
     * modified freely.  If you will not be modifying the returned Document in any way, you can
     * get additional performance by using getSoapEnvelopeDirect() instead.
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

    public boolean isBasicAuthRequired() {
        return policySettings.isBasicAuthRequired;
    }

    public void setBasicAuthRequired(boolean basicAuthRequired) {
        policySettings.isBasicAuthRequired = basicAuthRequired;
    }

    public String getHttpBasicUsername() {
        return policySettings.httpBasicUsername;
    }

    public void setHttpBasicUsername(String httpBasicUsername) {
        this.policySettings.httpBasicUsername = httpBasicUsername;
    }

    public char[] getHttpBasicPassword() {
        return policySettings.httpBasicPassword;
    }

    public void setHttpBasicPassword(char[] httpBasicPassword) {
        this.policySettings.httpBasicPassword = httpBasicPassword;
    }

    public boolean isDigestAuthRequired() {
        return policySettings.isDigestAuthRequired;
    }

    public void setDigestAuthRequired(boolean digestAuthRequired) {
        policySettings.isDigestAuthRequired = digestAuthRequired;
    }

    public String getHttpDigestUsername() {
        return policySettings.httpDigestUsername;
    }

    public void setHttpDigestUsername(String httpDigestUsername) {
        this.policySettings.httpDigestUsername = httpDigestUsername;
    }

    public char[] getHttpDigestPassword() {
        return policySettings.httpDigestPassword;
    }

    public void setHttpDigestPassword(char[] httpDigestPassword) {
        this.policySettings.httpDigestPassword = httpDigestPassword;
    }

    public boolean isPolicyUpdated() {
        return isPolicyUpdated;
    }

    public void setPolicyUpdated(boolean policyUpdated) {
        isPolicyUpdated = policyUpdated;
    }

    public boolean isCredentialsWouldHaveHelped() {
        return policySettings.credentialsWouldHaveHelped;
    }

    public void setCredentialsWouldHaveHelped(boolean credentialsWouldHaveHelped) {
        this.policySettings.credentialsWouldHaveHelped = credentialsWouldHaveHelped;
    }

    public boolean isClientCertWouldHaveHelped() {
        return policySettings.clientCertWouldHaveHelped;
    }

    public void setClientCertWouldHaveHelped(boolean clientCertWouldHaveHelped) {
        this.policySettings.clientCertWouldHaveHelped = clientCertWouldHaveHelped;
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
