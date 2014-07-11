package com.l7tech.external.assertions.xmppassertion;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.variable.Syntax;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 03/04/12
 * Time: 9:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class XMPPAssociateSessionsAssertion extends Assertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(XMPPAssociateSessionsAssertion.class.getName());

    private String inboundSessionId;
    private String outboundSessionId;

    @Override
    public String[] getVariablesUsed() {
        String[] inboundSessionIdVars = Syntax.getReferencedNames(inboundSessionId);
        String[] outboundSessionIdVars = Syntax.getReferencedNames(outboundSessionId);

        String[] retVal = new String[inboundSessionIdVars.length + outboundSessionIdVars.length];
        System.arraycopy(inboundSessionIdVars, 0, retVal, 0, inboundSessionIdVars.length);
        System.arraycopy(outboundSessionIdVars, 0, retVal, inboundSessionIdVars.length, outboundSessionIdVars.length);

        return retVal;
    }

    public String getInboundSessionId() {
        return inboundSessionId;
    }

    public void setInboundSessionId(String inboundSessionId) {
        this.inboundSessionId = inboundSessionId;
    }

    public String getOutboundSessionId() {
        return outboundSessionId;
    }

    public void setOutboundSessionId(String outboundSessionId) {
        this.outboundSessionId = outboundSessionId;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = XMPPOpenServerSessionAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        //props.put(NAME, new String[] {
        //        DESCRIPTION,
        //        DEFAULT
        //});
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "XMPP Associate Sessions");
        meta.put(AssertionMetadata.LONG_NAME, "XMPP Associate Sessions Assertion");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "routing" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmppassertion.console.XMPPAssociateSessionsAssertionPropertiesDialog");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:Mllp" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
