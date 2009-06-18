package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.Functions;
import com.l7tech.util.ArrayUtils;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;

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

    public String getRecipientTrustedCertificateName() {
        return recipientTrustedCertificateName;
    }

    public void setRecipientTrustedCertificateName(String recipientTrustedCertificateName) {
        this.recipientTrustedCertificateName = recipientTrustedCertificateName;
    }

    public long getRecipientTrustedCertificateOid() {
        return recipientTrustedCertificateOid;
    }

    public void setRecipientTrustedCertificateOid(long recipientTrustedCertificateOid) {
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

    public boolean isUseSecurityHeaderMustUnderstand() {
        return useSecurityHeaderMustUnderstand;
    }

    public void setUseSecurityHeaderMustUnderstand(boolean useSecurityHeaderMustUnderstand) {
        this.useSecurityHeaderMustUnderstand = useSecurityHeaderMustUnderstand;
    }

    public String getWsSecurityVersion() {
        return wsSecurityVersion;
    }

    public void setWsSecurityVersion(String wsSecurityVersion) {
        this.wsSecurityVersion = wsSecurityVersion;
    }

    @Override
    public String[] getVariablesUsed() {
        String[] usedVariables = super.getVariablesUsed();

        if ( recipientTrustedCertificateName != null ) {
            String[] referenced = Syntax.getReferencedNames( recipientTrustedCertificateName );
            if ( referenced.length > 0 ) {
                usedVariables = ArrayUtils.concat( usedVariables, referenced );
            }
        }

        return usedVariables;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, resolver = PropertyResolver.Type.ASSERTION)
    public EntityHeader[] getEntitiesUsed() {
        EntityHeader[] headers = new EntityHeader[0];

        if ( recipientTrustedCertificateOid != 0 ) {
            headers = new EntityHeader[]{ new EntityHeader( recipientTrustedCertificateOid, EntityType.TRUSTED_CERT, null, null) };
        }

        return headers;
    }

    @Override
    public void replaceEntity( final EntityHeader oldEntityHeader, final EntityHeader newEntityHeader ) {
        if( oldEntityHeader.getType() == EntityType.TRUSTED_CERT &&
            newEntityHeader.getType() == EntityType.TRUSTED_CERT &&
            recipientTrustedCertificateOid == oldEntityHeader.getOid())
        {
            recipientTrustedCertificateOid = newEntityHeader.getOid();
        }
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.SHORT_NAME, "Add or remove WS-Security");
        meta.put(AssertionMetadata.DESCRIPTION, "Add, remove or modify the WS-Security related contents of a message.");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.WsSecurityPropertiesDialog");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String,WsSecurity>(){
            @Override
            public String call( final WsSecurity wsSecurity ) {
                StringBuilder nameBuilder = new StringBuilder();

                if ( wsSecurity.isApplyWsSecurity() ) {
                    nameBuilder.append("Apply ");
                }

                nameBuilder.append("WS-Security");
                if ( wsSecurity.isApplyWsSecurity() ) {
                    nameBuilder.append(" ");
                    nameBuilder.append(wsSecurity.getWsSecurityVersion());
                }

                return AssertionUtils.decorateName( wsSecurity, nameBuilder );
            }
        });

        return meta;
    }

    //- PRIVATE

    private boolean replaceSecurityHeader = true;
    private boolean removeUnmatchedSecurityHeaders = false;
    private boolean useSecurityHeaderMustUnderstand = true;
    private boolean useSecureSpanActor = false;
    private boolean applyWsSecurity = true;
    private String wsSecurityVersion = "1.0";
    private long recipientTrustedCertificateOid;
    private String recipientTrustedCertificateName;

}
