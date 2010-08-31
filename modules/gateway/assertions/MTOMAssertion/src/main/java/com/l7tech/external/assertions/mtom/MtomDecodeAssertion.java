package com.l7tech.external.assertions.mtom;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.ProcessesMultipart;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.Collections;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 *
 */
@ProcessesMultipart
public class MtomDecodeAssertion extends MessageTargetableAssertion implements UsesVariables, SetsVariables {

    //- PUBLIC

    public static final String PROP_DECODE_SECURED = "mtom.decodeSecuredMessages";

    public MtomDecodeAssertion() {
        // Primary target is only read; outputTarget is modified
        super(false);
    }

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
        if (outputTarget != null)
            outputTarget.setTargetModifiedByGateway(true);
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return outputTarget == null ? new VariableMetadata[0] : outputTarget.getVariablesSet();
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(SHORT_NAME, "Decode MTOM Message");
        meta.put(PROPERTIES_ACTION_NAME, "MTOM Decode Properties");
        meta.put(DESCRIPTION, "Process a SOAP message in Optimized MIME Multipart/Related Serialization (MTOM) format and optionally covert to a plain SOAP message.");
        meta.put(WSP_EXTERNAL_NAME, "MTOMDecodeAssertion");
        meta.put(PALETTE_FOLDERS, new String[] { "xml" });
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlelement.gif");
        meta.put(POLICY_NODE_ICON, "com/l7tech/console/resources/xmlelement.gif");
        meta.put(FEATURE_SET_NAME, "(fromClass)");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.mtom.console.MtomDecodeAssertionPropertiesDialog");
        meta.put(CLUSTER_PROPERTIES, Collections.singletonMap( PROP_DECODE_SECURED, new String[]{
                "Should MTOM messages containing WS-Security for processing by this gateway be automatically decoded (true/false)",
                "true" } ));
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");
        
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
