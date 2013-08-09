package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.security.xml.WsSecurityVersion;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

/**
 * Assertion for applying WS-Security to a message or removing security headers.
 *
 * @author jbufu
 */
@RequiresSOAP(wss = true)
public class WsSecurity extends MessageTargetableAssertion implements UsesEntities {

    //- PUBLIC

    public WsSecurity() {
    }

    public boolean isApplyWsSecurity() {
        return applyWsSecurity;
    }

    public void setApplyWsSecurity(boolean applyWsSecurity) {
        this.applyWsSecurity = applyWsSecurity;
    }

    @Nullable
    public String getRecipientTrustedCertificateName() {
        return recipientTrustedCertificateName;
    }

    public void setRecipientTrustedCertificateName( @Nullable final String recipientTrustedCertificateName ) {
        this.recipientTrustedCertificateName = recipientTrustedCertificateName;
    }

    @Nullable
    public String getRecipientTrustedCertificateVariable() {
        return recipientTrustedCertificateVariable;
    }

    public void setRecipientTrustedCertificateVariable( @Nullable final String recipientTrustedCertificateVariable ) {
        this.recipientTrustedCertificateVariable = recipientTrustedCertificateVariable;
    }

    public Goid getRecipientTrustedCertificateOid() {
        return recipientTrustedCertificateOid;
    }

    public void setRecipientTrustedCertificateOid( @Nullable Goid recipientTrustedCertificateOid) {
        this.recipientTrustedCertificateOid = recipientTrustedCertificateOid;
    }

    public boolean isRemoveUnmatchedSecurityHeaders() {
        return removeUnmatchedSecurityHeaders;
    }

    public void setRemoveUnmatchedSecurityHeaders(boolean removeUnmatchedSecurityHeaders) {
        this.removeUnmatchedSecurityHeaders = removeUnmatchedSecurityHeaders;
    }

    public boolean isReplaceSecurityHeader() {
        return replaceSecurityHeader;
    }

    public void setReplaceSecurityHeader(boolean replaceSecurityHeader) {
        this.replaceSecurityHeader = replaceSecurityHeader;
    }

    public boolean isUseSecureSpanActor() {
        return useSecureSpanActor;
    }

    public void setUseSecureSpanActor(boolean useSecureSpanActor) {
        this.useSecureSpanActor = useSecureSpanActor;
    }

    public boolean isClearDecorationRequirements() {
        return clearDecorationRequirements;
    }

    public void setClearDecorationRequirements( final boolean clearDecorationRequirements ) {
        this.clearDecorationRequirements = clearDecorationRequirements;
    }

    public boolean isUseSecurityHeaderMustUnderstand() {
        return useSecurityHeaderMustUnderstand;
    }

    public void setUseSecurityHeaderMustUnderstand(boolean useSecurityHeaderMustUnderstand) {
        this.useSecurityHeaderMustUnderstand = useSecurityHeaderMustUnderstand;
    }

    @Nullable
    public WsSecurityVersion getWsSecurityVersion() {
        return wsSecurityVersion;
    }

    public void setWsSecurityVersion( @Nullable final WsSecurityVersion wsSecurityVersion ) {
        this.wsSecurityVersion = wsSecurityVersion;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, resolver = PropertyResolver.Type.ASSERTION)
    public EntityHeader[] getEntitiesUsed() {
        EntityHeader[] headers = new EntityHeader[0];

        if ( recipientTrustedCertificateOid != null ) {
            headers = new EntityHeader[]{ new EntityHeader( recipientTrustedCertificateOid, EntityType.TRUSTED_CERT, null, null) };
        }

        return headers;
    }

    @Override
    public void replaceEntity( final EntityHeader oldEntityHeader, final EntityHeader newEntityHeader ) {
        if( oldEntityHeader.getType() == EntityType.TRUSTED_CERT &&
            newEntityHeader.getType() == EntityType.TRUSTED_CERT &&
            recipientTrustedCertificateOid != null &&
            recipientTrustedCertificateOid.equals(oldEntityHeader.getGoid()))
        {
            recipientTrustedCertificateOid = newEntityHeader.getGoid();
        }
    }

    final static String baseName = "Add or Remove WS-Security";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<WsSecurity>(){
        @Override
        public String getAssertionName( final WsSecurity assertion, final boolean decorate) {
            StringBuilder nameBuilder = new StringBuilder();

            if (assertion.isApplyWsSecurity()) {
                nameBuilder.append("Apply ");
            } else if (assertion.isClearDecorationRequirements()) {
                nameBuilder.append("Clear ");
            }

            nameBuilder.append("WS-Security");
            if (assertion.isApplyWsSecurity() &&
                    !Assertion.isResponse(assertion) &&
                    assertion.getWsSecurityVersion() != null) {
                nameBuilder.append(" ");
                nameBuilder.append(assertion.getWsSecurityVersion());
            }

            return (decorate)? AssertionUtils.decorateName(assertion, nameBuilder): baseName;
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Add, remove or modify the WS-Security related contents of a message.");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.WsSecurityPropertiesDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "WS-Security Properties");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(
            Collections.<TypeMapping>singleton(new Java5EnumTypeMapping(WsSecurityVersion.class, "wsSecurityVersion"))));
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.WsSecurityValidator");        

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    //- PROTECTED

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withExpressions( recipientTrustedCertificateName ).withVariables( recipientTrustedCertificateVariable );
    }

    //- PRIVATE

    private static final String META_INITIALIZED = WsSecurity.class.getName() + ".metadataInitialized";

    private boolean replaceSecurityHeader = true;
    private boolean removeUnmatchedSecurityHeaders = false;
    private boolean useSecurityHeaderMustUnderstand = true;
    private boolean useSecureSpanActor = false;
    private boolean clearDecorationRequirements = false;
    private boolean applyWsSecurity = true;
    private WsSecurityVersion wsSecurityVersion;
    private Goid recipientTrustedCertificateOid;
    private String recipientTrustedCertificateName;
    private String recipientTrustedCertificateVariable;

}
