package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * Adds a SAML assertion to the WSS decoration requirements for a target message.
 * The SAML assertion must already exist and is provided to this assertion as a context variable.
 */
public class AddWssSamlToken extends MessageTargetableAssertion implements SecurityHeaderAddressable, RequestIdentityTargetable, UsesVariables, UsesEntities {
    public AddWssSamlToken() {
        super( TargetMessageType.RESPONSE, true );
    }

    /**
     * @return an expression that, when expanded at runtime, will produce the XML of the SAML assertion to add to the decoration requirements.
     */
    public String getSamlAssertionVariable() {
        return samlAssertionVariable;
    }

    /**
     * @param samlAssertionVariable an expression that, when expanded at runtime, will produce the XML of the SAML assertion to add to the decoration requirements.
     */
    public void setSamlAssertionVariable(String samlAssertionVariable) {
        this.samlAssertionVariable = samlAssertionVariable;
    }

    @Override
    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    @Override
    public void setRecipientContext( final XmlSecurityRecipientContext recipientContext ) {
        this.recipientContext = recipientContext == null ?
                XmlSecurityRecipientContext.getLocalRecipient() :
                recipientContext;
    }

    @Override
    public IdentityTarget getIdentityTarget() {
        return identityTarget;
    }

    @Override
    public void setIdentityTarget( final IdentityTarget identityTarget ) {
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
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        List<String> vars = new ArrayList<String>( Arrays.asList(super.getVariablesUsed()));

        StringBuilder allRefs = new StringBuilder();
        if ( samlAssertionVariable != null ) allRefs.append( samlAssertionVariable );
        String[] referencedVariables = Syntax.getReferencedNames( allRefs.toString() );
        vars.addAll( Arrays.asList(referencedVariables));

        return vars.toArray(new String[vars.size()]);
    }

    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private IdentityTarget identityTarget;
    private String samlAssertionVariable;
}
