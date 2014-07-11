package com.l7tech.external.assertions.xmppassertion;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 23/03/12
 * Time: 11:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class XMPPGetRemoteCertificateAssertion extends Assertion implements UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(XMPPOpenServerSessionAssertion.class.getName());

    private String sessionId;
    private boolean inbound;
    private String variableName;
    
    @Override
    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(sessionId);
    }
    
    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {new VariableMetadata(variableName, false, false, null, false, DataType.CERTIFICATE)};
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isInbound() {
        return inbound;
    }

    public void setInbound(boolean inbound) {
        this.inbound = inbound;
    }
    
    public String getVariableName() {
        return variableName;
    }
    
    public void setVariableName(String variableName) {
        this.variableName = variableName;
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
        meta.put(AssertionMetadata.SHORT_NAME, "XMPP Get Client Certificate");
        meta.put(AssertionMetadata.LONG_NAME, "XMPP Get Client Certificate Assertion");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "transportLayerSecurity" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmppassertion.console.XMPPGetRemoteCertificateAssertionPropertiesDialog");

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
