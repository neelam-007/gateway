/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import java.io.Serializable;

/**
 * @author alex
 */
public abstract class RoutingAssertion extends Assertion implements Cloneable, Serializable {
    public static final int REMOVE_CURRENT_SECURITY_HEADER = 0;
    public static final int LEAVE_CURRENT_SECURITY_HEADER_AS_IS = 1;
    public static final int PROMOTE_OTHER_SECURITY_HEADER = 2;

    // saml (model as a different bean when serializer supports it)
    private boolean attachSamlSenderVouches;
    private int samlAssertionExpiry = 5;
    private boolean groupMembershipStatement;
    private boolean taiCredentialChaining = false;
    private String xmlSecurityActorToPromote;
    private int currentSecurityHeaderHandling = REMOVE_CURRENT_SECURITY_HEADER;
    private String[] customIpAddresses = null;

    /**
     * This is the value of the soap security header actor attribute that should be promoted to
     * the default value (no actor value). If there is a soap security header present that has
     * this actor attribute value, then that soap security header should be promoted to the
     * default security header before it is routed downstream.
     *
     * @return null means there is no actor to promote, otherwise the actor to promote
     */
    public String getXmlSecurityActorToPromote() {
        return xmlSecurityActorToPromote;
    }

    /**
     * This is the value of the soap security header actor attribute that should be promoted to
     * the default value (no actor value). If there is a soap security header present that has
     * this actor attribute value, then that soap security header should be promoted to the
     * default security header before it is routed downstream.
     *
     * @param xmlSecurityActorToPromote the actor to promote or null if no promotion is required
     */
    public void setXmlSecurityActorToPromote(String xmlSecurityActorToPromote) {
        this.xmlSecurityActorToPromote = xmlSecurityActorToPromote;
    }

    public boolean isAttachSamlSenderVouches() {
        return attachSamlSenderVouches;
    }

    public void setAttachSamlSenderVouches(boolean attachSamlSenderVouches) {
        this.attachSamlSenderVouches = attachSamlSenderVouches;
    }

    public int getSamlAssertionExpiry() {
        return samlAssertionExpiry;
    }

    public void setSamlAssertionExpiry(int samlAssertionExpiry) {
        if (samlAssertionExpiry <= 0) {
            throw new IllegalArgumentException();
        }
        this.samlAssertionExpiry = samlAssertionExpiry;
    }

    public boolean isGroupMembershipStatement() {
        return groupMembershipStatement;
    }

    public void setGroupMembershipStatement(boolean groupMembershipStatement) {
        this.groupMembershipStatement = groupMembershipStatement;
    }

    public boolean isTaiCredentialChaining() {
        return taiCredentialChaining;
    }

    public void setTaiCredentialChaining(boolean taiCredentialChaining) {
        this.taiCredentialChaining = taiCredentialChaining;
    }

    /**
     * This setting controls what this routing assertion should do with the current security header
     * before routing the request. Possible values are:
     * REMOVE_CURRENT_SECURITY_HEADER,
     * LEAVE_CURRENT_SECURITY_HEADER_AS_IS,
     * PROMOTE_OTHER_SECURITY_HEADER
     */ 
    public int getCurrentSecurityHeaderHandling() {
        return currentSecurityHeaderHandling;
    }

    /**
     * Set what this routing assertion should do with the current security header before routing the request. Possible
     * values are:
     * REMOVE_CURRENT_SECURITY_HEADER,
     * LEAVE_CURRENT_SECURITY_HEADER_AS_IS,
     * PROMOTE_OTHER_SECURITY_HEADER
     * @param currentSecurityHeaderHandling see description for possible values
     */
    public void setCurrentSecurityHeaderHandling(int currentSecurityHeaderHandling) {
        this.currentSecurityHeaderHandling = currentSecurityHeaderHandling;
    }

    /** @return the custom IP addresses to use as an array of String, or null if no custom IP address list is configured. */
    public String[] getCustomIpAddresses() {
        return customIpAddresses;
    }

    /** @param customIpAddresses custom addresses to use, or null if no custom addresses should be used. */
    public void setCustomIpAddresses(String[] customIpAddresses) {
        this.customIpAddresses = customIpAddresses;
    }
}

