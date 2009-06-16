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
import com.l7tech.policy.assertion.UsesEntities;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.util.Functions;
import com.l7tech.util.TimeUnit;
import com.l7tech.objectmodel.EntityHeader;

import java.text.MessageFormat;

/**
 * This assertion verifies that the soap message contains a wsu:Timestamp element in a SOAP header.
 *
 * Set {@link #setSignatureRequired} to require that the timestamp be signed (if set, this assertion
 * must follow one of {@link RequireWssX509Cert}, {@link SecureConversation} or {@link RequireWssSaml}).
 */
@RequiresSOAP(wss=true)
public class RequireWssTimestamp extends MessageTargetableAssertion implements IdentityTargetable, SecurityHeaderAddressable, UsesEntities {

    /**
     * The recommended max expiry time to use when creating request wss timestamps;
     */
    public static final int DEFAULT_MAX_EXPIRY_TIME = TimeUnit.HOURS.getMultiplier(); // One hour

    /**
     * Create a new RequestWssTimestamp with default properties.
     */
    public static RequireWssTimestamp newInstance() {
        RequireWssTimestamp timestamp = new RequireWssTimestamp();
        timestamp.setMaxExpiryMilliseconds(RequireWssTimestamp.DEFAULT_MAX_EXPIRY_TIME);
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

    public EntityHeader[] getEntitiesUsed() {
        return identityTarget != null ?
                identityTarget.getEntitiesUsed():
                new EntityHeader[0];
    }

    public void replaceEntity( final EntityHeader oldEntityHeader,
                               final EntityHeader newEntityHeader ) {
        if ( identityTarget != null ) {
            identityTarget.replaceEntity(oldEntityHeader, newEntityHeader);
        }
    }

    @Override
    public String toString() {
        return super.toString() + " signatureRequired=" + signatureRequired;
    }

    public void copyFrom(RequireWssTimestamp other) {
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
        meta.put(SHORT_NAME, "Require Timestamp");
        meta.put(DESCRIPTION, "The message must contain a Timestamp.");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(PALETTE_NODE_SORT_PRIORITY, 68000);
        meta.put(POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, RequireWssTimestamp>() {
            @Override
            public String call(RequireWssTimestamp assertion) {
                return AssertionUtils.decorateName(
                        assertion,
                        MessageFormat.format("Require {0}Timestamp",
                                assertion.isSignatureRequired() ? "signed " : ""));
            }
        });
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.RequireWssTimestampDialog");

        meta.put(ASSERTION_FACTORY, new Functions.Unary<RequireWssTimestamp, RequireWssTimestamp>(){
            @Override
            public RequireWssTimestamp call(final RequireWssTimestamp requestWssTimestamp) {
                return newInstance();
            }
        });
        meta.put(CLIENT_ASSERTION_CLASSNAME, "com.l7tech.proxy.policy.assertion.xmlsec.ClientRequestWssTimestamp");

        return meta;
    }

    private TimeUnit timeUnit = TimeUnit.MINUTES;
    private int maxExpiryMilliseconds;
    private boolean signatureRequired = true;
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private IdentityTarget identityTarget;
}
