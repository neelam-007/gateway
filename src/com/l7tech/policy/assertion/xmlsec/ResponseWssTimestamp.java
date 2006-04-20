/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.common.security.xml.KeyReference;
import com.l7tech.common.util.TimeUnit;

/**
 * Creates a wsu:Timestamp element and adds it to the SOAP security header in the response.
 *
 * @author alex
 */
public class ResponseWssTimestamp extends Assertion implements ResponseWssConfig {
    /**
     * Expiry time of the timestamp, always in milliseconds, regardless of {@link #timeUnit}.
     */
    private int expiryMillis;

    /** TimeUnit remembered purely for GUI purposes (no impact on {@link #expiryMillis})*/
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    private String keyReference = KeyReference.BST.getName();
    
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();

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
