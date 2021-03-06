/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;

import java.io.Serializable;

/**
 * @author alex
 */
public abstract class RoutingAssertion extends Assertion implements Cloneable, Serializable, SecurityHeaderAddressable {
    public static final int REMOVE_CURRENT_SECURITY_HEADER = 0;
    public static final int CLEANUP_CURRENT_SECURITY_HEADER = 1;
    public static final int PROMOTE_OTHER_SECURITY_HEADER = 2;
    public static final int IGNORE_SECURITY_HEADER = 3;

    private String xmlSecurityActorToPromote;
    private int currentSecurityHeaderHandling = REMOVE_CURRENT_SECURITY_HEADER;
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();

    /**
     * Indicates if the assertion initializes the Request message.
     *
     * @return true if the assertion always initializes the Request message, false otherwise.
     */
    public abstract boolean initializesRequest();

    /**
     * Indicates if the assertion needs the Request message to be initialized by a previous assertion.
     *
     * @return true if the assertion needs the Request message to be already initialized (and will very likely fail if it's not);
     *         false otherwise
     */
    public abstract boolean needsInitializedRequest();

    /**
     * Indicates if the assertion initializes the response message.
     *
     * @return true if the assertion always initializes the response message, false otherwise.
     */
    public abstract boolean initializesResponse();

    /**
     * Indicates if the assertion needs the response message to be initialized by a previous assertion.
     *
     * @return true if the assertion needs the response message to be already initialized (and will very likely fail if it's not);
     *         false otherwise
     */
    public abstract boolean needsInitializedResponse();

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

    /**
     * This setting controls what this routing assertion should do with the current security header
     * before routing the request. Possible values are:
     * {@link #REMOVE_CURRENT_SECURITY_HEADER},
     * {@link #CLEANUP_CURRENT_SECURITY_HEADER},
     * {@link #PROMOTE_OTHER_SECURITY_HEADER}
     * {@link #IGNORE_SECURITY_HEADER}
     */
    public int getCurrentSecurityHeaderHandling() {
        return currentSecurityHeaderHandling;
    }

    /**
     * Set what this routing assertion should do with the current security header
     * before routing the request. Possible values are:
     * {@link #REMOVE_CURRENT_SECURITY_HEADER},
     * {@link #CLEANUP_CURRENT_SECURITY_HEADER},
     * {@link #PROMOTE_OTHER_SECURITY_HEADER}
     * {@link #IGNORE_SECURITY_HEADER}
     * @param currentSecurityHeaderHandling see description for possible values
     */
    public void setCurrentSecurityHeaderHandling(int currentSecurityHeaderHandling) {
        this.currentSecurityHeaderHandling = currentSecurityHeaderHandling;
    }

    /** Subclasses can choose to offer this functionality by adding a public method that chains to this one. */
    protected void copyFrom(RoutingAssertion source) {
        this.setCurrentSecurityHeaderHandling(source.getCurrentSecurityHeaderHandling());
        this.setXmlSecurityActorToPromote(source.getXmlSecurityActorToPromote());
        this.setRecipientContext(source.getRecipientContext());
    }

    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    public void setRecipientContext(XmlSecurityRecipientContext recipientContext) {
        if (recipientContext == null) recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
        this.recipientContext = recipientContext;
    }
}

