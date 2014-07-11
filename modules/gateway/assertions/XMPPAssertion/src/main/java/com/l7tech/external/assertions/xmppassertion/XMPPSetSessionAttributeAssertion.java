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
 * Time: 1:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class XMPPSetSessionAttributeAssertion extends Assertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(XMPPSetSessionAttributeAssertion.class.getName());

    private boolean inbound = true;
    private String sessionId;
    private String attributeName;
    private String value;

    @Override
    public String[] getVariablesUsed() {
        String[] sessionIdVars = Syntax.getReferencedNames(sessionId);
        String[] attributeNameVars = Syntax.getReferencedNames(attributeName);
        String[] valueVars = Syntax.getReferencedNames(value);

        String[] retVal = new String[sessionIdVars.length + attributeNameVars.length + valueVars.length];
        System.arraycopy(sessionIdVars, 0, retVal, 0, sessionIdVars.length);
        System.arraycopy(attributeNameVars, 0, retVal, sessionIdVars.length, attributeNameVars.length);
        System.arraycopy(valueVars, 0, retVal, sessionIdVars.length + attributeNameVars.length, valueVars.length);
        return retVal;
    }

    public boolean isInbound() {
        return inbound;
    }

    public void setInbound(boolean inbound) {
        this.inbound = inbound;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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
        meta.put(AssertionMetadata.SHORT_NAME, "XMPP Set Session Attribute");
        meta.put(AssertionMetadata.LONG_NAME, "XMPP Set Session Attribute Assertion");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "routing" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmppassertion.console.XMPPSetSessionAttributeAssertionPropertiesDialog");

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
