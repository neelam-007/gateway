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
    private boolean isSslRequired = false;
    private boolean isBasicAuthRequired = false;
    private String httpBasicUsername = "";
    private String httpBasicPassword = "";


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
}
