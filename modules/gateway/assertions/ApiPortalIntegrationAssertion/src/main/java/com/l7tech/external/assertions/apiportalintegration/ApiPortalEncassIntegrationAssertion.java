package com.l7tech.external.assertions.apiportalintegration;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This is purely a configuration assertion used as the initialization point for the Api Portal integration pieces.  The
 * purpose of this assertion is to run define the ModuleLoadListener needed for SSG startup and shutdown.
 * <p/>
 * Identifies an encass used policy as a portal-managed encass.
 *
 * @author Victor Kazakov
 */
public class ApiPortalEncassIntegrationAssertion extends Assertion implements SetsVariables {
    protected static final Logger logger = Logger.getLogger(ApiPortalEncassIntegrationAssertion.class.getName());

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[0];
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = ApiPortalEncassIntegrationAssertion.class.getName() + ".metadataInitialized";

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
        meta.put(AssertionMetadata.SHORT_NAME, "Set as Portal Publishable Fragment");
        meta.put(AssertionMetadata.LONG_NAME, "Flags the Encapsulated Assertions using the policy as a \"Portal Managed\" encapsulated assertion (For use by the Layer 7 API portal)");

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"internalAssertions"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        // Removing the policy advice makes it so that the properties dialog does not show up when the assertion is added to policy
        // meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:ApiPortalIntegration" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        // no properties need to be edited for this assertion
        // meta.putNull(AssertionMetadata.PROPERTIES_EDITOR_FACTORY);

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
