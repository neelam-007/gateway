/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import org.apache.axis.message.SOAPEnvelope;

import java.security.Principal;

/**
 * Holds request state while the client proxy is processing it.
 * User: mike
 * Date: Jun 16, 2003
 * Time: 12:04:09 PM
 */
public class PendingRequest {
    private SOAPEnvelope soapEnvelope;
    private Ssg ssg;
    private String soapAction = "";
    private String uri = "";
    private boolean isSslRequired = false;
    private boolean isBasicAuthRequired = false;
    private String httpBasicUsername = "";
    private String httpBasicPassword = "";
    private boolean isDigestAuthRequired = false;
    private String httpDigestUsername = "";
    private String httpDigestPassword = "";

    /** Construct a PendingRequest around the given SOAPEnvelope going to the given SSG. */
    public PendingRequest(SOAPEnvelope soapEnvelope, Ssg ssg) {
        this.soapEnvelope = soapEnvelope;
        this.ssg = ssg;
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

    public boolean isSslRequired() {
        return isSslRequired;
    }

    public void setSslRequired(boolean sslRequired) {
        isSslRequired = sslRequired;
    }

    public boolean isBasicAuthRequired() {
        return isBasicAuthRequired;
    }

    public void setBasicAuthRequired(boolean basicAuthRequired) {
        isBasicAuthRequired = basicAuthRequired;
    }

    public String getHttpBasicUsername() {
        return httpBasicUsername;
    }

    public void setHttpBasicUsername(String httpBasicUsername) {
        this.httpBasicUsername = httpBasicUsername;
    }

    public String getHttpBasicPassword() {
        return httpBasicPassword;
    }

    public void setHttpBasicPassword(String httpBasicPassword) {
        this.httpBasicPassword = httpBasicPassword;
    }

    public boolean isDigestAuthRequired() {
        return isDigestAuthRequired;
    }

    public void setDigestAuthRequired(boolean digestAuthRequired) {
        isDigestAuthRequired = digestAuthRequired;
    }

    public String getHttpDigestUsername() {
        return httpDigestUsername;
    }

    public void setHttpDigestUsername(String httpDigestUsername) {
        this.httpDigestUsername = httpDigestUsername;
    }

    public String getHttpDigestPassword() {
        return httpDigestPassword;
    }

    public void setHttpDigestPassword(String httpDigestPassword) {
        this.httpDigestPassword = httpDigestPassword;
    }
}
