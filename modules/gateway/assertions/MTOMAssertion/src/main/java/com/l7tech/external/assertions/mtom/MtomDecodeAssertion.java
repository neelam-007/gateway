package com.l7tech.external.assertions.mtom;

import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.AssertionMetadata;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.annotation.ProcessesMultipart;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.DataType;

/**
 *
 */
@ProcessesMultipart
public class MtomDecodeAssertion extends MessageTargetableAssertion implements UsesVariables, SetsVariables {

    //- PUBLIC

    public boolean isRequireEncoded() {
        return requireEncoded;
    }

    public void setRequireEncoded( boolean requireEncoded ) {
        this.requireEncoded = requireEncoded;
    }

    public boolean isRemovePackaging() {
        return removePackaging;
    }

    public void setRemovePackaging( final boolean removePackaging ) {
        this.removePackaging = removePackaging;
    }

    public boolean isProcessSecuredOnly() {
        return processSecuredOnly;
    }

    public void setProcessSecuredOnly( final boolean processSecuredOnly ) {
        this.processSecuredOnly = processSecuredOnly;
    }

    public MessageTargetableSupport getOutputTarget() {
        return outputTarget;
    }

    public void setOutputTarget( final MessageTargetableSupport outputTarget ) {
        this.outputTarget = outputTarget;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        VariableMetadata[] variables;

        if ( outputTarget!=null && outputTarget.getTarget()==TargetMessageType.OTHER ) {
            String name = outputTarget.getOtherTargetMessageVariable();
            variables = new VariableMetadata[]{ new VariableMetadata(name, false, false, name, true, DataType.MESSAGE) };
        } else {
            variables = new VariableMetadata[0];
        }

        return variables;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(SHORT_NAME, "MTOM Decode");
        meta.put(DESCRIPTION, "Process a SOAP message in Optimized MIME Multipart/Related Serialization (MTOM) format and optionally covert to a plain SOAP message.");
        meta.put(WSP_EXTERNAL_NAME, "MTOMDecodeAssertion");
        meta.put(PALETTE_FOLDERS, new String[] { "xml" });
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlelement.gif");
        meta.put(POLICY_NODE_ICON, "com/l7tech/console/resources/xmlelement.gif");
        meta.put(FEATURE_SET_NAME, "(fromClass)");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.mtom.console.MtomDecodeAssertionPropertiesDialog");
        
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    //- PRIVATE

    private static final String META_INITIALIZED = MtomDecodeAssertion.class.getName() + ".metadataInitialized";

    private MessageTargetableSupport outputTarget;
    private boolean requireEncoded;
    private boolean removePackaging = true;
    private boolean processSecuredOnly;
}
