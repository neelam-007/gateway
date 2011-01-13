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
 *
 */
public class AddWssSecurityContextToken extends MessageTargetableAssertion implements SecurityHeaderAddressable, RequestIdentityTargetable, UsesVariables, UsesEntities {
    public AddWssSecurityContextToken() {
        super( TargetMessageType.RESPONSE, true );
    }

    /**
     * @return an expression that, when expanded at runtime, will produce the security context identifier string to use; or null if not yet set.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier an expression that, when expanded at runtime, will produce the security context identifier string to use; or null to clear it.
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
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
        if ( identifier != null ) allRefs.append( identifier );
        String[] referencedVariables = Syntax.getReferencedNames( allRefs.toString() );
        vars.addAll( Arrays.asList(referencedVariables));

        return vars.toArray(new String[vars.size()]);
    }

    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private IdentityTarget identityTarget;
    private String identifier;
}

