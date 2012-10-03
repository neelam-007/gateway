package com.l7tech.external.assertions.apiportalintegration;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.l7tech.policy.assertion.AssertionMetadata.PROPERTIES_ACTION_NAME;

/**
 * This assertion provides concurrent CRUD operations for Portal API keys against an abstracted backend data store.
 * The initial implementation for pilot will use a cluster-wide property to persist an XML document containing 
 *
 * @author vchan
 */
public class ManageApiKeyAssertion extends Assertion implements UsesVariables, SetsVariables {
    protected static final Logger logger = Logger.getLogger(ManageApiKeyAssertion.class.getName());

    public static final String ACTION_ADD = "Add";
    public static final String ACTION_UPDATE = "Update";
    public static final String ACTION_REMOVE = "Remove";

    public static final String SUFFIX_XML = "xml";
    public static final String SUFFIX_KEY = "key";
    public static final String SUFFIX_SERVICE = "service";
    public static final String SUFFIX_SECRET = "secret";
    public static final String SUFFIX_PLAN = "plan";
    
    private String variablePrefix = "apikey";
    private String action; // possible values, Add|Update|Remove
    private String apiKey; // ctx variable containing the api key value
    private String apiKeyElement; // message ctx variable for the entire XML element

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getApiKey() {
        return (apiKey == null ? "" : apiKey);
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKeyElement() {
        return (apiKeyElement == null ? "" : apiKeyElement);
    }

    public void setApiKeyElement(String apiKeyElement) {
        this.apiKeyElement = apiKeyElement;
    }

    public String getVariablePrefix() {
        return (variablePrefix == null ? "" : variablePrefix);
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        if (ACTION_ADD.equals(action))
            return new VariableMetadata[] {
                    new VariableMetadata(variablePrefix+"."+SUFFIX_KEY, false, false, null, true, DataType.STRING)
            };
        else
            return new VariableMetadata[0];
    }

    @Override
    public String[] getVariablesUsed() {
        StringBuffer sb = new StringBuffer();
        sb.append(getApiKey());
        sb.append(" ").append(getApiKeyElement());
        return Syntax.getReferencedNames(sb.toString());
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = ManageApiKeyAssertion.class.getName() + ".metadataInitialized";

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
        meta.put(AssertionMetadata.SHORT_NAME, "Manage API Keys");
        meta.put(AssertionMetadata.LONG_NAME, "Manage Portal API keys (Add | Update | Remove)");
        meta.put(PROPERTIES_ACTION_NAME, "Manage API Keys Properties");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "internalAssertions" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // Set up smart Getter for nice, informative policy node name, for GUI
//        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/.gif");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:ApiPortalIntegration" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
