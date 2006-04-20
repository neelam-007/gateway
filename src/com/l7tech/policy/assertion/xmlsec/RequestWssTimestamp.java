/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.common.util.TimeUnit;

/**
 * This assertion verifies that the soap request contains a wsu:Timestamp element in a SOAP header.
 *
 * Set {@link #setSignatureRequired} to require that the timestamp be signed (if set, this assertion
 * must follow one of {@link RequestWssX509Cert}, {@link SecureConversation} or {@link RequestWssSaml}).
 */
public class RequestWssTimestamp extends Assertion implements SecurityHeaderAddressable {
    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    public void setRecipientContext(XmlSecurityRecipientContext recipientContext) {
        this.recipientContext = recipientContext;
    }

    public boolean isSignatureRequired() {
        return signatureRequired;
    }

    public void setSignatureRequired(boolean signatureRequired) {
        this.signatureRequired = signatureRequired;
    }

    public int getMaxExpiryMilliseconds() {
        return maxExpiryMilliseconds;
    }

    public void setMaxExpiryMilliseconds(int maxExpiryMilliseconds) {
        this.maxExpiryMilliseconds = maxExpiryMilliseconds;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    private TimeUnit timeUnit = TimeUnit.SECONDS;
    private int maxExpiryMilliseconds = 60 * 60 * 1000; // One hour
    private boolean signatureRequired = true;
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
}
