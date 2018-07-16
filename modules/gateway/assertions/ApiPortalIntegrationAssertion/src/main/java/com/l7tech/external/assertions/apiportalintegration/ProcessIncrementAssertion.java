package com.l7tech.external.assertions.apiportalintegration;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This assertion will read the json output from GetIncrementAssertion and update the SSG state.  This assertion will also
 * create a post-back message to the originator.
 *
 * @author chean22, 1/19/2016
 */
public class ProcessIncrementAssertion extends Assertion implements UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(ProcessIncrementAssertion.class.getName());

    public static final String SUFFIX_JSON = "json";
    public static final String SUFFIX_POSTBACK = "postback";
    public static final String SUFFIX_TYPE = "type";

    private String variablePrefix = "portal.sync.increment";

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[]{
                new VariableMetadata(variablePrefix + "." + SUFFIX_POSTBACK, false, false, null, true, DataType.STRING)
        };
    }

    @Override
    public String[] getVariablesUsed() {
        return VariableUseSupport.variables(
                variablePrefix + "." + SUFFIX_TYPE,
                variablePrefix + "." + SUFFIX_JSON).asArray();
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = ProcessIncrementAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Ensure inbound transport gets wired up
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.apiportalintegration.server.ModuleLoadListener");

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<>();
        //props.put(NAME, new String[] {
        //        DESCRIPTION,
        //        DEFAULT
        //});
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Portal Process Incremental Update");
        meta.put(AssertionMetadata.LONG_NAME, "Portal Process Incremental Update and Send Post-back Message");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"internalAssertions"});
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
