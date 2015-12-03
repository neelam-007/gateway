/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.ValidatorFlag;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;

import java.util.Set;
import java.util.EnumSet;

/**
 * @author mike
 */
public class WssReplayProtection extends MessageTargetableAssertion implements IdentityTargetable, SecurityHeaderAddressable, UsesEntities, UsesVariables, SetsVariables {

    //- PUBLIC

    public WssReplayProtection() {
        super(false);
    }

    public boolean isCustomProtection() {
        return customProtection;
    }

    public void setCustomProtection( final boolean customProtection ) {
        this.customProtection = customProtection;
    }

    public String getCustomScope() {
        return customScope;
    }

    public void setCustomScope( final String customScope ) {
        this.customScope = customScope;
    }

    public String getCustomIdentifierVariable() {
        return customIdentifierVariable;
    }

    public void setCustomIdentifierVariable( final String customIdentifierVariable ) {
        this.customIdentifierVariable = customIdentifierVariable;
    }

    public int getCustomExpiryTime() {
        return customExpiryTime;
    }

    public void setCustomExpiryTime( final int customExpiryTime ) {
        this.customExpiryTime = customExpiryTime;
    }

    @Override
    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    @Override
    public void setRecipientContext(final XmlSecurityRecipientContext recipientContext) {
        this.recipientContext = recipientContext == null ?
                XmlSecurityRecipientContext.getLocalRecipient() :
                recipientContext;
    }

    @Override
    public IdentityTarget getIdentityTarget() {
        return identityTarget;
    }

    @Override
    public void setIdentityTarget(final IdentityTarget identityTarget) {
        this.identityTarget = identityTarget;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.USERGROUP)
    public EntityHeader[] getEntitiesUsed() {
        return identityTarget != null ?
                identityTarget.getEntitiesUsed():
                new EntityHeader[0];
    }

    public boolean isBypassUniqueCheck() {
        return bypassUniqueCheck;
    }

    public void setBypassUniqueCheck(boolean bypassUniqueCheck) {
        this.bypassUniqueCheck = bypassUniqueCheck;
    }

    public boolean isSaveIdAndExpiry() {
        return saveIdAndExpiry;
    }

    public void setSaveIdAndExpiry(boolean saveIdAndExpiry) {
        this.saveIdAndExpiry = saveIdAndExpiry;
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    /**
     * @return an array of all the variable suffixes.
     */
    public static String[] getVariableSuffixes() {
        return new String[] {WssReplayProtection.ID_SUFFIX, WssReplayProtection.EXPIRY_SUFFIX};
    }

    @Override
    public void replaceEntity( final EntityHeader oldEntityHeader,
                               final EntityHeader newEntityHeader ) {
        if ( identityTarget != null ) {
            identityTarget.replaceEntity(oldEntityHeader, newEntityHeader);
        }
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Protect against replay of messages. By default the message must contain a WSS signed timestamp; if a signed wsa:MessageID is also present the MessageID value will be asserted unique; otherwise the timestamp's Created date is used. If custom protection is used then a context variable will be asserted unique within a specified scope.");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Message Replay Protection Properties");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.ReplayProtectionPropertiesDialog" );
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "threatProtection", "xmlSecurity" });
        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);
        meta.put(AssertionMetadata.CLIENT_ASSERTION_CLASSNAME, "com.l7tech.proxy.policy.assertion.xmlsec.ClientRequestWssReplayProtection");
        meta.put(AssertionMetadata.CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/proxy/resources/tree/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_VALIDATOR_FLAGS_FACTORY, new Functions.Unary<Set<ValidatorFlag>, WssReplayProtection>(){
            @Override
            public Set<ValidatorFlag> call(WssReplayProtection assertion) {
                if ( assertion.isCustomProtection() ) {
                    return EnumSet.of(ValidatorFlag.PERFORMS_VALIDATION);
                } else {
                    return EnumSet.of(ValidatorFlag.PERFORMS_VALIDATION, ValidatorFlag.REQUIRE_SIGNATURE);
                }
            }
        });
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.WssReplayProtectionValidator");

        return meta;
    }

    //- PROTECTED

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return isCustomProtection() ?
                super.doGetVariablesUsed().withExpressions(customScope).withVariables(customIdentifierVariable) :
                super.doGetVariablesUsed();
    }

    @Override
    protected VariablesSet doGetVariablesSet() {
        return saveIdAndExpiry
                ? super.doGetVariablesSet().withVariables(
                new VariableMetadata(variablePrefix + "." + ID_SUFFIX, true, false, null, true, DataType.STRING),
                new VariableMetadata(variablePrefix + "." + EXPIRY_SUFFIX, true, false, null, true, DataType.STRING)
        )
                : super.doGetVariablesSet();
    }

    //- PRIVATE

    public static final String VARIABLE_PREFIX = "messageIdAndExpiry";
    public static final String ID_SUFFIX = "id";
    public static final String EXPIRY_SUFFIX = "expiry";

    private static final String baseName = "Protect Against Message Replay";
    private static final AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<WssReplayProtection>(){
        @Override
        public String getAssertionName( final WssReplayProtection assertion, final boolean decorate ) {
            if(!decorate) return baseName;
            return AssertionUtils.decorateName(assertion, baseName);
        }
    };

    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private IdentityTarget identityTarget;
    private boolean customProtection;
    private String customScope;
    private String customIdentifierVariable;
    private int customExpiryTime;
    private boolean bypassUniqueCheck;
    private boolean saveIdAndExpiry;
    private String variablePrefix = VARIABLE_PREFIX;
}
