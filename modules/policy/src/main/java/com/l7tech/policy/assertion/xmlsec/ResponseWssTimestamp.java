/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.annotation.ProcessesResponse;
import com.l7tech.security.xml.KeyReference;
import com.l7tech.util.TimeUnit;

/**
 * Creates a wsu:Timestamp element and adds it to the SOAP security header in the response.
 *
 * @author alex
 */
@ProcessesResponse
@RequiresSOAP(wss=true)
public class ResponseWssTimestamp extends Assertion implements ResponseWssConfig {

    /**
     * The recommended expiry time to use when creating response wss timestamps;
     */
    public static final int DEFAULT_EXPIRY_TIME = 5 * TimeUnit.MINUTES.getMultiplier();

    /**
     * Create a new ResponseWssTimestamp with default properties.
     */
    public static ResponseWssTimestamp newInstance() {
        ResponseWssTimestamp timestamp = new ResponseWssTimestamp();
        timestamp.setExpiryMilliseconds(ResponseWssTimestamp.DEFAULT_EXPIRY_TIME);
        return timestamp;
    }

    /**
     * Expiry time of the timestamp, always in milliseconds, regardless of {@link #timeUnit}.
     */
    private int expiryMillis;

    /** TimeUnit remembered purely for GUI purposes (no impact on {@link #expiryMillis})*/
    private TimeUnit timeUnit = TimeUnit.MINUTES;

    private String keyReference = KeyReference.BST.getName();
    
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();

    private boolean signatureRequired = true;

    public void copyFrom(ResponseWssTimestamp other) {
        this.expiryMillis = other.expiryMillis;
        this.timeUnit = other.timeUnit;
        this.keyReference = other.keyReference;
        this.recipientContext = other.recipientContext;
        this.signatureRequired = other.signatureRequired;
    }

    /**
     * Should the response timestamp be signed (default true).
     *
     * @return true for signing
     */
    public boolean isSignatureRequired() {
        return signatureRequired;
    }

    /**
     * Set the timestamp signature flag (default true).
     *
     * @param signatureRequired True to sign the response signature.
     */
    public void setSignatureRequired(boolean signatureRequired) {
        this.signatureRequired = signatureRequired;
    }

    /**
     * Expiry time of the timestamp, always in milliseconds, regardless of {@link #getTimeUnit}.
     */
    public int getExpiryMilliseconds() {
        return expiryMillis;
    }

    /**
     * Expiry time of the timestamp, always in milliseconds, regardless of {@link #getTimeUnit}.
     */
    public void setExpiryMilliseconds(int expiryMillis) {
        this.expiryMillis = expiryMillis;
    }

    /** TimeUnit remembered purely for GUI purposes (no impact on {@link #expiryMillis})*/
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /** TimeUnit remembered purely for GUI purposes (no impact on {@link #expiryMillis})*/
    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    public void setRecipientContext(XmlSecurityRecipientContext recipientContext) {
        this.recipientContext = recipientContext;
    }

    public String getKeyReference() {
        return keyReference;
    }

    public void setKeyReference(String keyReference) {
        this.keyReference = keyReference;
    }
}
