package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * This assertion verifies that the soap request contained
 * an xml digital signature but does not care about which
 * elements were signed. The cert used for the signature is
 * remembered to identify the user. This cert can later
 * be used for comparaison in an identity assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 14, 2004<br/>
 * $Id$<br/>
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

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, "WSS Signature");
        meta.put(AssertionMetadata.DESCRIPTION, "The soap request must contain a WSS signature with an X509 SecurityToken");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME, "Require WSS Signature");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.WssX509CertPropertiesDialog");
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.WssX509CertValidator");
        meta.put(AssertionMetadata.CLIENT_ASSERTION_CLASSNAME, "com.l7tech.proxy.policy.assertion.xmlsec.ClientRequestWssX509Cert");

        return meta;
    }

    public boolean isAllowMultipleSignatures() {
        return allowMultipleSignatures;
    }

    public void setAllowMultipleSignatures( final boolean allowMultipleSignatures ) {
        this.allowMultipleSignatures = allowMultipleSignatures;
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
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        return messageTargetableSupport.getVariablesUsed();
    }

    //- PRIVATE

    private boolean allowMultipleSignatures = false;
    private final MessageTargetableSupport messageTargetableSupport
            = new MessageTargetableSupport(TargetMessageType.REQUEST);
}
