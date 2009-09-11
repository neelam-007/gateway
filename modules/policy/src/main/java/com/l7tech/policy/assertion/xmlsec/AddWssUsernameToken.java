package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;
import com.l7tech.objectmodel.EntityHeader;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Add a WS-Security UsernameToken decoration.
 */
@RequiresSOAP(wss = true)
public class AddWssUsernameToken extends MessageTargetableAssertion implements SecurityHeaderAddressable, RequestIdentityTargetable, UsesVariables, UsesEntities {

    //- PUBLIC

    public AddWssUsernameToken() {
        super( TargetMessageType.RESPONSE );
    }

    public boolean isIncludePassword() {
        return includePassword;
    }

    public void setIncludePassword( boolean includePassword ) {
        this.includePassword = includePassword;
    }

    public boolean isIncludeNonce() {
        return includeNonce;
    }

    public void setIncludeNonce( boolean includeNonce ) {
        this.includeNonce = includeNonce;
    }

    public boolean isIncludeCreated() {
        return includeCreated;
    }

    public void setIncludeCreated( boolean includeCreated ) {
        this.includeCreated = includeCreated;
    }

    public boolean isDigest() {
        return digest;
    }

    public void setDigest( boolean digest ) {
        this.digest = digest;
    }

    public boolean isEncrypt() {
        return encrypt;
    }

    public void setEncrypt( boolean encrypt ) {
        this.encrypt = encrypt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername( String username ) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword( String password ) {
        this.password = password;
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
        if ( username != null ) allRefs.append( username );
        if ( password != null ) allRefs.append( password );
        String[] referencedVariables = Syntax.getReferencedNames( allRefs.toString() );
        vars.addAll( Arrays.asList(referencedVariables));

        return vars.toArray(new String[vars.size()]);
    }

    final static String baseName = "Add WS-Security UsernameToken";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<AddWssUsernameToken>(){
        @Override
        public String getAssertionName( final AddWssUsernameToken assertion, final boolean decorate) {
            StringBuilder nameBuilder = new StringBuilder();
            nameBuilder.append("Add");
            if (assertion.isEncrypt()) {
                nameBuilder.append(" encrypted");
            }
            nameBuilder.append(" WS-Security UsernameToken");
            if (assertion.isDigest()) {
                nameBuilder.append(" digest");
            }
            return (decorate) ? AssertionUtils.decorateName(assertion, nameBuilder) : baseName;
        }
    };
    
    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Add a WS-Security UsernameToken to the message.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.AddWssUsernameTokenPropertiesDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "WS-Security UsernameToken Properties");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.AddWssUsernameTokenValidator");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        return meta;
    }

    //- PRIVATE

    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private IdentityTarget identityTarget;
    private boolean includePassword = true;
    private boolean includeNonce = true;
    private boolean includeCreated = true;
    private boolean digest = false;
    private boolean encrypt = false;
    private String username;
    private String password;
}
