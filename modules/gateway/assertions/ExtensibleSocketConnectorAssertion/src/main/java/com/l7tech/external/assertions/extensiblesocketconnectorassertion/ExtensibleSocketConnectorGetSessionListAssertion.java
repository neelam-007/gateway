package com.l7tech.external.assertions.extensiblesocketconnectorassertion;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.UsesVariables;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 24/01/14
 * Time: 2:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExtensibleSocketConnectorGetSessionListAssertion extends Assertion implements UsesVariables {

    private String targetVariable = "";
    private Goid socketConnectorGoid = Goid.DEFAULT_GOID;

    public Goid getSocketConnectorGoid() {
        return socketConnectorGoid;
    }

    public void setSocketConnectorGoid(Goid socketConnectorGoid) {
        this.socketConnectorGoid = socketConnectorGoid;
    }

    public String getTargetVariable() {
        return targetVariable;
    }

    public void setTargetVariable(String targetVariable) {
        this.targetVariable = targetVariable;
    }

    @Override
    public String[] getVariablesUsed() {

        if (targetVariable != null && !targetVariable.trim().isEmpty()) {
            return new String[]{targetVariable};
        } else {
            return new String[0];
        }
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = ExtensibleSocketConnectorGetSessionListAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Get Remote Socket Session List");
        meta.put(AssertionMetadata.LONG_NAME, "Get a list of active sessions for a remote socket configuration.");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"misc"});

        //setup icons
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/external/assertions/splitjoin/console/split16.gif");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/external/assertions/splitjoin/console/split16.gif");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.extensiblesocketconnectorassertion.console.ExtensibleSocketConnectorAssertionGetSessionListPropertiesDialog");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
