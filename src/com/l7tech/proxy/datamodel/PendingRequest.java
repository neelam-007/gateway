/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import org.apache.axis.message.SOAPEnvelope;
import com.l7tech.proxy.RequestInterceptor;
import com.l7tech.proxy.NullRequestInterceptor;

/**
 * Holds request state while the client proxy is processing it.
 * User: mike
 * Date: Jun 16, 2003
 * Time: 12:04:09 PM
 */
public class PendingRequest {
    private SOAPEnvelope soapEnvelope;
    private Ssg ssg;
    private RequestInterceptor requestInterceptor = NullRequestInterceptor.INSTANCE;
    private String soapAction = "";
    private String uri = "";
    private boolean isPolicyUpdated = false;

    /** Number of times credentials have been updated while processing this request. */
    private int timesCredentialsUpdated = 0;

    /**
     * Number of attempts we have made to contact the SSG for this request.
     * Breaker to prevent runaway repetition/recursion.
     */
    private int timesAttempted = 0;

    // Policy settings, filled in by traversing poliy tree
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
    public PendingRequest(SOAPEnvelope soapEnvelope, Ssg ssg, RequestInterceptor requestInterceptor) {
        this.soapEnvelope = soapEnvelope;
        this.ssg = ssg;
        setRequestInterceptor(requestInterceptor);
    }

    /**
     * Reset all policy settings in preperation for starting processing over again with a different policy.
     * TODO: Policies currently don't modify the soapEnvelope; but when they do, this must restore that too
     */
    public void reset() {
        policySettings = new PolicySettings();
    }

    // Getters and setters

    public SOAPEnvelope getSoapEnvelope() {
        return soapEnvelope;
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

    public int getTimesCredentialsUpdated() {
        return timesCredentialsUpdated;
    }

    public void setTimesCredentialsUpdated(int timesCredentialsUpdated) {
        this.timesCredentialsUpdated = timesCredentialsUpdated;
    }

    /** Record that the credentials have been updated for this request.  Returns the new counter value. */
    public int incrementTimesCredentialsUpdated() {
        return ++this.timesCredentialsUpdated;
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

    public int getTimesAttempted() {
        return timesAttempted;
    }

    public void setTimesAttempted(int timesAttempted) {
        this.timesAttempted = timesAttempted;
    }

    /** Record that a new attempt to call the SSG on behalf of this request has begun.  Returns the new counter value. */
    public int incrementTimesAttempted() {
        return ++timesAttempted;
    }
}
