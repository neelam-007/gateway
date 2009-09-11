/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.validator.ValidatorFlag;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.util.Functions;

import java.util.Set;
import java.util.EnumSet;

/**
 * @author mike
 */
@RequiresSOAP(wss=true)
public class WssReplayProtection extends MessageTargetableAssertion implements IdentityTargetable, SecurityHeaderAddressable, UsesEntities {

    //- PUBLIC

    public WssReplayProtection() {
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

    final static String baseName = "Protect Against WS-Security Replay";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<WssReplayProtection>(){
        @Override
        public String getAssertionName( final WssReplayProtection assertion, final boolean decorate) {
            if(!decorate) return baseName;
            return AssertionUtils.decorateName(assertion, baseName);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, "Protect Against WS-Security Replay");
        meta.put(AssertionMetadata.DESCRIPTION, "The message must contain a WSS signed timestamp; if a signed wsa:MessageID is also present the MessageID value will be asserted unique; otherwise the timestamp's Created date is used.");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.putNull(AssertionMetadata.PROPERTIES_EDITOR_FACTORY);
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "threatProtection", "xmlSecurity" });
        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);
        meta.put(AssertionMetadata.CLIENT_ASSERTION_CLASSNAME, "com.l7tech.proxy.policy.assertion.xmlsec.ClientRequestWssReplayProtection");
        meta.put(AssertionMetadata.CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/proxy/resources/tree/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_VALIDATOR_FLAGS_FACTORY, new Functions.Unary<Set<ValidatorFlag>, WssReplayProtection>(){
            @Override
            public Set<ValidatorFlag> call(WssReplayProtection assertion) {
                return EnumSet.of(ValidatorFlag.PERFORMS_VALIDATION, ValidatorFlag.REQUIRE_SIGNATURE);
            }
        });

        return meta;
    }

    //- PRIVATE

    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private IdentityTarget identityTarget;
}
