package com.l7tech.external.assertions.apiportalintegration;

import com.l7tech.external.assertions.apiportalintegration.server.ModuleConstants;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * This is purely a configuration assertion used as the initialization point for the Api Portal integration
 * pieces.  The purpose of this assertion is to run define the ModuleLoadListener needed for SSG startup
 * and shutdown.
 * <p/>
 * Identifies a published service as a portal-managed API.
 *
 * @author vchan
 */
public class ApiPortalIntegrationAssertion extends Assertion implements SetsVariables {
    protected static final Logger logger = Logger.getLogger(ApiPortalIntegrationAssertion.class.getName());

    private String portalManagedApiFlag;

    /**
     * Name of the context variable prefix.
     */
    private String variablePrefix = "portal.managed.service";

    /**
     * A unique identifier for the API that should remain constant after any service migration process.
     * <p/>
     * Default is a generated uuid.
     */
    private String apiId = UUID.randomUUID().toString();

    /**
     * Name of the API group that the published service belongs to.
     */
    private String apiGroup;

    public String getPortalManagedApiFlag() {
        return portalManagedApiFlag;
    }

    public void setPortalManagedApiFlag(String portalManagedApiFlag) {
        this.portalManagedApiFlag = ModuleConstants.TEMP_PORTAL_MANAGED_SERVICE_INDICATOR; // hardcoded value that the portal will look for in the policy XML
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(final String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(final String apiId) {
        this.apiId = apiId;
    }

    public String getApiGroup() {
        return apiGroup;
    }

    public void setApiGroup(final String apiGroup) {
        this.apiGroup = apiGroup;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        VariableMetadata[] variableMetadata;
        if (variablePrefix == null) {
            variableMetadata = new VariableMetadata[0];
        } else {
            variableMetadata = new VariableMetadata[]{new VariableMetadata(variablePrefix + ".apiId"), new VariableMetadata(variablePrefix + ".apiGroup")};
        }

        return variableMetadata;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = ApiPortalIntegrationAssertion.class.getName() + ".metadataInitialized";

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
        meta.put(AssertionMetadata.SHORT_NAME, "Set as Portal Managed Service");
        meta.put(AssertionMetadata.LONG_NAME, "Flags the service as a \"Portal Managed\" service (For use by the Layer 7 API portal)");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"internalAssertions"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

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
