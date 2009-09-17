/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.xmlsec;

import static com.l7tech.policy.assertion.AssertionMetadata.*;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_VALIDATOR_FLAGS_FACTORY;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.validator.ValidatorFlag;
import com.l7tech.util.Functions;
import com.l7tech.util.TimeUnit;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;

import java.util.Set;
import java.util.EnumSet;

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

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.USERGROUP)
    public EntityHeader[] getEntitiesUsed() {
        return identityTarget != null ?
                identityTarget.getEntitiesUsed():
                new EntityHeader[0];
    }

    @Override
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

    final static String baseName = "Require Timestamp";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<RequireWssTimestamp>(){
        @Override
        public String getAssertionName( final RequireWssTimestamp assertion, final boolean decorate) {
            final String decoratedName = (assertion.isSignatureRequired()) ? "Require Signed Timestamp" : baseName;
            return (decorate)? AssertionUtils.decorateName(assertion, decoratedName): baseName;
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "The message must contain a Timestamp.");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(PALETTE_NODE_SORT_PRIORITY, 68000);
        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(PROPERTIES_ACTION_NAME, "Timestamp Properties");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.RequireWssTimestampDialog");
        meta.put(POLICY_VALIDATOR_FLAGS_FACTORY, new Functions.Unary<Set<ValidatorFlag>, RequireWssTimestamp>(){
            @Override
            public Set<ValidatorFlag> call(RequireWssTimestamp assertion) {
                Set<ValidatorFlag> flags = EnumSet.of(ValidatorFlag.PERFORMS_VALIDATION);
                if ( assertion.isSignatureRequired() ) {
                    flags.add(ValidatorFlag.REQUIRE_SIGNATURE);
                }
                return flags;
            }
        });
        meta.put(ASSERTION_FACTORY, new Functions.Unary<RequireWssTimestamp, RequireWssTimestamp>(){
            @Override
            public RequireWssTimestamp call(final RequireWssTimestamp requestWssTimestamp) {
                return newInstance();
            }
        });
        meta.put(CLIENT_ASSERTION_CLASSNAME, "com.l7tech.proxy.policy.assertion.xmlsec.ClientRequestWssTimestamp");
        meta.put(CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/proxy/resources/tree/xmlencryption.gif");
        meta.put(USED_BY_CLIENT, Boolean.TRUE);

        return meta;
    }

    private TimeUnit timeUnit = TimeUnit.MINUTES;
    private int maxExpiryMilliseconds;
    private boolean signatureRequired = true;
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private IdentityTarget identityTarget;
}
