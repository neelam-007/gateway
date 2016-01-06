package com.l7tech.external.assertions.apiportalintegration;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.VariableUseSupport;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This assertion will query the portal database via the supplied jdbc and retrieve the increment.  The output of this
 * assertion will be the json payload with the increment.
 *
 * @author wlui
 */
public class GetIncrementAssertion extends Assertion implements UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(GetIncrementAssertion.class.getName());

    private String variablePrefix = "portal.sync.increment";


    public static final String SUFFIX_JDBC_CONNECTION = "jdbc";
    public static final String SUFFIX_SINCE = "since";
    public static final String SUFFIX_TYPE = "type";
    public static final String SUFFIX_JSON = "json";


    public String getVariablePrefix() {
    return variablePrefix;
  }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
                new VariableMetadata(variablePrefix+"."+SUFFIX_JSON, false, false, null, true, DataType.STRING)
        };
    }

    @Override
    public String[] getVariablesUsed() {
        return VariableUseSupport.variables(variablePrefix +"."+ SUFFIX_SINCE,
                                            variablePrefix +"."+ SUFFIX_JDBC_CONNECTION,
                                            variablePrefix +"."+ SUFFIX_TYPE).asArray();
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = GetIncrementAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Ensure inbound transport gets wired up
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.apiportalintegration.server.ModuleLoadListener");

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        //props.put(NAME, new String[] {
        //        DESCRIPTION,
        //        DEFAULT
        //});
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Portal Get Incremental Update");
        meta.put(AssertionMetadata.LONG_NAME, "Portal Get Incremental Update Json message");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "internalAssertions" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:ApiPortalIntegration" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
