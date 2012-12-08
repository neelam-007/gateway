package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.variable.VariableMetadata;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * This assertion verifies that the soap message contained
 * an xml digital signature but does not care about which
 * elements were signed. The cert used for the signature is
 * remembered to identify the user. This cert can later
 * be used for comparison in an identity assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 14, 2004<br/>
 */
@RequiresSOAP(wss=true)
public class RequireWssX509Cert extends SecurityHeaderAddressableSupport implements MessageTargetable, UsesVariables {

    //- PUBLIC

    /**
     * The WSS X509 security token is credential source.
     *
     * @return always true
     */
    @Override
    public boolean isCredentialSource() {
        return true;
    }

    final static String baseName = "Require WS-Security Signature Credentials";
    
    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<RequireWssX509Cert>(){
        @Override
        public String getAssertionName( final RequireWssX509Cert assertion, final boolean decorate) {
            if(!decorate) return baseName;
            return AssertionUtils.decorateName(assertion, baseName);
        }
    };
    
    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "The SOAP message must contain a WS-Security signature with an X.509 security token.");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "WS-Security Signature Properties");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.WssX509CertPropertiesDialog");
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.WssX509CertValidator");
        meta.put(AssertionMetadata.CLIENT_ASSERTION_CLASSNAME, "com.l7tech.proxy.policy.assertion.xmlsec.ClientRequestWssX509Cert");
        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);
        meta.put(AssertionMetadata.CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/proxy/resources/tree/xmlencryption.gif");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        return meta;
    }

    public boolean isAllowMultipleSignatures() {
        return allowMultipleSignatures;
    }

    public void setAllowMultipleSignatures( final boolean allowMultipleSignatures ) {
        this.allowMultipleSignatures = allowMultipleSignatures;
    }

    public String getSignatureElementVariable() {
        return signatureElementVariable;
    }

    public void setSignatureElementVariable( final String signatureElementVariable ) {
        this.signatureElementVariable = signatureElementVariable;
    }

    public String getSignatureReferenceElementVariable() {
        return signatureReferenceElementVariable;
    }

    public void setSignatureReferenceElementVariable( final String signatureReferenceElementVariable ) {
        this.signatureReferenceElementVariable = signatureReferenceElementVariable;
    }

    @Override
    public TargetMessageType getTarget() {
        return messageTargetableSupport.getTarget();
    }

    @Override
    public void setTarget(TargetMessageType target) {
        messageTargetableSupport.setTarget(target);
    }

    @Override
    public String getOtherTargetMessageVariable() {
        return messageTargetableSupport.getOtherTargetMessageVariable();
    }

    @Override
    public void setOtherTargetMessageVariable(String otherTargetMessageVariable) {
        messageTargetableSupport.setOtherTargetMessageVariable(otherTargetMessageVariable);
    }

    @Override
    public String getTargetName() {
        return messageTargetableSupport.getTargetName();
    }

    @Override
    public boolean isTargetModifiedByGateway() {
        return messageTargetableSupport.isTargetModifiedByGateway();
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return messageTargetableSupport.getMessageTargetVariablesUsed().withVariables(
                signatureElementVariable,
                signatureReferenceElementVariable
        ).asArray();
    }


    @Override
    public VariableMetadata[] getVariablesSet() {
        return messageTargetableSupport.getMessageTargetVariablesSet().asArray();
    }

    @Override
    public RequireWssX509Cert clone() {
        RequireWssX509Cert requireWssX509Cert = (RequireWssX509Cert) super.clone();
        requireWssX509Cert.messageTargetableSupport = new MessageTargetableSupport( messageTargetableSupport );
        return requireWssX509Cert;
    }

    //- PRIVATE

    private boolean allowMultipleSignatures = false;
    private String signatureElementVariable;
    private String signatureReferenceElementVariable;
    private MessageTargetableSupport messageTargetableSupport
            = new MessageTargetableSupport(TargetMessageType.REQUEST, false);
}
