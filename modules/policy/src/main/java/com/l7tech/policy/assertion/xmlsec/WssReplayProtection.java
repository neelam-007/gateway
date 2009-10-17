/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.ValidatorFlag;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.util.Functions;

import java.util.Set;
import java.util.EnumSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author mike
 */
public class WssReplayProtection extends MessageTargetableAssertion implements IdentityTargetable, SecurityHeaderAddressable, UsesEntities, UsesVariables {

    //- PUBLIC

    public WssReplayProtection() {
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

    @Override
    public void replaceEntity( final EntityHeader oldEntityHeader,
                               final EntityHeader newEntityHeader ) {
        if ( identityTarget != null ) {
            identityTarget.replaceEntity(oldEntityHeader, newEntityHeader);
        }
    }

    @Override
    public String[] getVariablesUsed() {
        if ( !isCustomProtection() ) {
            return super.getVariablesUsed();
        }

        List<String> variables = new ArrayList<String>( Arrays.asList(super.getVariablesUsed()) );
        variables.add( customIdentifierVariable );
        if ( customScope != null ) {
            variables.addAll( Arrays.asList(Syntax.getReferencedNames( customScope )) );           
        }
        return variables.toArray( new String[ variables.size() ] );
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Protect against replay of messages. By default the message must contain a WSS signed timestamp; if a signed wsa:MessageID is also present the MessageID value will be asserted unique; otherwise the timestamp's Created date is used. If custom protection is used then a context variable will be asserted unique within a specified scope.");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
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

    //- PRIVATE

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
}
