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

    // saml (model as a different bean when serializer supports it)
    private boolean attachSamlSenderVouches;
    private boolean useThumbprintInSamlSignature;
    private boolean useThumbprintInSamlSubject;
    private int samlAssertionVersion = 1; // backwards compatible
    private int samlAssertionExpiry = 5;
    private boolean groupMembershipStatement;
    private String xmlSecurityActorToPromote;
    private int currentSecurityHeaderHandling = REMOVE_CURRENT_SECURITY_HEADER;
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();

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

    /**
     * @return true if the signature of any attached Sender-Vouches SAML assertion should contain a thumbprint instead of the whole signing certificate
     */
    public boolean isUseThumbprintInSamlSignature() {
        return useThumbprintInSamlSignature;
    }

    /**
     * @param useThumbprintInSamlSignature true if the signature of any attached Sender-Vouches SAML assertion should contain a thumbprint instead of the whole signing certificate
     */
    public void setUseThumbprintInSamlSignature(boolean useThumbprintInSamlSignature) {
        this.useThumbprintInSamlSignature = useThumbprintInSamlSignature;
    }

    /**
     * @return true if the subject cert in any attached Sender-Vouches SAML assertion should be replaced with a thumbprint
     */
    public boolean isUseThumbprintInSamlSubject() {
        return useThumbprintInSamlSubject;
    }

    /**
     * @param useThumbprintInSamlSubject true if the subject cert in any attached Sender-Vouches SAML assertion should be replaced with a thumbprint
     */
    public void setUseThumbprintInSamlSubject(boolean useThumbprintInSamlSubject) {
        this.useThumbprintInSamlSubject = useThumbprintInSamlSubject;
    }

    public int getSamlAssertionVersion() {
        return samlAssertionVersion;
    }

    public void setSamlAssertionVersion(int samlAssertionVersion) {
        this.samlAssertionVersion = samlAssertionVersion;
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
        this.setUseThumbprintInSamlSignature(source.isUseThumbprintInSamlSignature());
        this.setUseThumbprintInSamlSubject(source.isUseThumbprintInSamlSubject());
        this.setAttachSamlSenderVouches(source.isAttachSamlSenderVouches());
        this.setCurrentSecurityHeaderHandling(source.getCurrentSecurityHeaderHandling());
        this.setGroupMembershipStatement(source.isGroupMembershipStatement());
        this.setSamlAssertionExpiry(source.getSamlAssertionExpiry());
        this.setSamlAssertionVersion(source.getSamlAssertionVersion());
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

