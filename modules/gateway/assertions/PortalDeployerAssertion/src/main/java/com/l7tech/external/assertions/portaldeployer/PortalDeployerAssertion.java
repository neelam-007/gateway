package com.l7tech.external.assertions.portaldeployer;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 
 */
public class PortalDeployerAssertion extends Assertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(PortalDeployerAssertion.class.getName());

    public String[] getVariablesUsed() {
        return new String[0]; //Syntax.getReferencedNames(...);
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = PortalDeployerAssertion.class.getName() + ".metadataInitialized";

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
        meta.put(AssertionMetadata.SHORT_NAME, "");
        meta.put(AssertionMetadata.LONG_NAME, "");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:PortalDeployer" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put( AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME,
                "com.l7tech.external.assertions.portaldeployer.server.PortalDeployerModuleLoadListener");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
