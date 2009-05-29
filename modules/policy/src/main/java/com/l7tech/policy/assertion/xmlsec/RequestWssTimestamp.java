/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.AssertionMetadata;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.IdentityTargetable;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.util.Functions;
import com.l7tech.util.TimeUnit;

import java.text.MessageFormat;

/**
 * This assertion verifies that the soap message contains a wsu:Timestamp element in a SOAP header.
 *
 * Set {@link #setSignatureRequired} to require that the timestamp be signed (if set, this assertion
 * must follow one of {@link RequestWssX509Cert}, {@link SecureConversation} or {@link RequestWssSaml}).
 */
@RequiresSOAP(wss=true)
public class RequestWssTimestamp extends MessageTargetableAssertion implements IdentityTargetable, SecurityHeaderAddressable {

    /**
     * The recommended max expiry time to use when creating request wss timestamps;
     */
    public static final int DEFAULT_MAX_EXPIRY_TIME = TimeUnit.HOURS.getMultiplier(); // One hour

    /**
     * Create a new RequestWssTimestamp with default properties.
     */
    public static RequestWssTimestamp newInstance() {
        RequestWssTimestamp timestamp = new RequestWssTimestamp();
        timestamp.setMaxExpiryMilliseconds(RequestWssTimestamp.DEFAULT_MAX_EXPIRY_TIME);
        return timestamp;
    }

    @Override
    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    @Override
    public void setRecipientContext(XmlSecurityRecipientContext recipientContext) {
        if (recipientContext == null) recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
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

    @Override
    public IdentityTarget getIdentityTarget() {
        return identityTarget;
    }

    @Override
    public void setIdentityTarget(IdentityTarget identityTarget) {
        this.identityTarget = identityTarget;
    }

    @Override
    public String toString() {
        return super.toString() + " signatureRequired=" + signatureRequired;
    }

    public void copyFrom(RequestWssTimestamp other) {
        super.copyFrom(other);
        this.timeUnit = other.timeUnit;
        this.maxExpiryMilliseconds = other.maxExpiryMilliseconds;
        this.signatureRequired = other.signatureRequired;
        this.recipientContext = other.recipientContext;
        this.setIdentityTarget(other.getIdentityTarget()==null ? null : new IdentityTarget(other.getIdentityTarget()));
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        meta.put(PALETTE_NODE_NAME, "Require Timestamp");
        meta.put(DESCRIPTION, "The message must contain a Timestamp.");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, RequestWssTimestamp>() {
            @Override
            public String call(RequestWssTimestamp assertion) {
                return AssertionUtils.decorateName(
                        assertion,
                        MessageFormat.format("Require {0}Timestamp",
                                assertion.isSignatureRequired() ? "signed " : ""));
            }
        });
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.RequestWssTimestampDialog");

        meta.put(ASSERTION_FACTORY, new Functions.Unary<RequestWssTimestamp,RequestWssTimestamp>(){
            @Override
            public RequestWssTimestamp call(final RequestWssTimestamp requestWssTimestamp) {
                return newInstance();
            }
        });

        return meta;
    }

    private TimeUnit timeUnit = TimeUnit.MINUTES;
    private int maxExpiryMilliseconds;
    private boolean signatureRequired = true;
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private IdentityTarget identityTarget;
}
