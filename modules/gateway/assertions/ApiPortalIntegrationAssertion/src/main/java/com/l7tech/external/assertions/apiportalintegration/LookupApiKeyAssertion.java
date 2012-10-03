package com.l7tech.external.assertions.apiportalintegration;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.HashMap;
import java.util.Map;

/**
 * Assertion which looks up an api key and sets the look up results into context variables.
 */
public class LookupApiKeyAssertion extends Assertion implements UsesVariables, SetsVariables {
    public static final String DEFAULT_PREFIX = "apiKeyRecord";
    public static final String KEY_SUFFIX = "key";
    public static final String SERVICE_SUFFIX = "service";
    public static final String STATUS_SUFFIX = "status";
    public static final String PLAN_SUFFIX = "plan";
    public static final String SECRET_SUFFIX = "secret";
    public static final String FOUND_SUFFIX = "found";
    public static final String XML_SUFFIX = "xml";
    public static final String DEFAULT_SERVICE_ID = "${service.oid}";
    public static final String LABEL_SUFFIX = "label";
    public static final String PLATFORM_SUFFIX = "platform";
    public static final String OAUTH_CALLBACK_SUFFIX = "oauthCallbackUrl";
    public static final String OAUTH_SCOPE_SUFFIX = "oauthScope";
    public static final String OAUTH_TYPE_SUFFIX = "oauthType";
    public static final String VERSION_SUFFIX = "version";

    /**
     * The API key to look up.
     */
    private String apiKey;

    /**
     * The prefix to use when storing the look up results.
     */
    private String variablePrefix = DEFAULT_PREFIX;

    /**
     * Published service id which corresponds to the API key. Can be null if the key is not required to correspond to a service id.
     */
    private String serviceId = DEFAULT_SERVICE_ID;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(final String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(final String serviceId) {
        this.serviceId = serviceId;
    }

    public String[] getVariablesUsed() {
        return Syntax.getReferencedNames(apiKey, serviceId);
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        final VariableMetadata key = new VariableMetadata(getVariableName(variablePrefix, KEY_SUFFIX), true, false, getVariableName(variablePrefix, KEY_SUFFIX), false, DataType.STRING);
        final VariableMetadata status = new VariableMetadata(getVariableName(variablePrefix, STATUS_SUFFIX), true, false, getVariableName(variablePrefix, STATUS_SUFFIX), false, DataType.STRING);
        final VariableMetadata secret = new VariableMetadata(getVariableName(variablePrefix, SECRET_SUFFIX), true, false, getVariableName(variablePrefix, SECRET_SUFFIX), false, DataType.STRING);
        final VariableMetadata found = new VariableMetadata(getVariableName(variablePrefix, FOUND_SUFFIX), true, false, getVariableName(variablePrefix, FOUND_SUFFIX), false, DataType.BOOLEAN);
        final VariableMetadata xml = new VariableMetadata(getVariableName(variablePrefix, XML_SUFFIX), true, false, getVariableName(variablePrefix, XML_SUFFIX), false, DataType.STRING);
        final VariableMetadata service = new VariableMetadata(getVariableName(variablePrefix, SERVICE_SUFFIX), true, false, getVariableName(variablePrefix, SERVICE_SUFFIX), false, DataType.STRING);
        final VariableMetadata plan = new VariableMetadata(getVariableName(variablePrefix, PLAN_SUFFIX), true, false, getVariableName(variablePrefix, PLAN_SUFFIX), false, DataType.STRING);
        final VariableMetadata label = new VariableMetadata(getVariableName(variablePrefix, LABEL_SUFFIX), true, false, getVariableName(variablePrefix, LABEL_SUFFIX), false, DataType.STRING);
        final VariableMetadata platform = new VariableMetadata(getVariableName(variablePrefix, PLATFORM_SUFFIX), true, false, getVariableName(variablePrefix, PLATFORM_SUFFIX), false, DataType.STRING);
        final VariableMetadata callbackUrl = new VariableMetadata(getVariableName(variablePrefix, OAUTH_CALLBACK_SUFFIX), true, false, getVariableName(variablePrefix, OAUTH_CALLBACK_SUFFIX), false, DataType.STRING);
        final VariableMetadata scope = new VariableMetadata(getVariableName(variablePrefix, OAUTH_SCOPE_SUFFIX), true, false, getVariableName(variablePrefix, OAUTH_SCOPE_SUFFIX), false, DataType.STRING);
        final VariableMetadata type = new VariableMetadata(getVariableName(variablePrefix, OAUTH_TYPE_SUFFIX), true, false, getVariableName(variablePrefix, OAUTH_TYPE_SUFFIX), false, DataType.STRING);
        final VariableMetadata version = new VariableMetadata(getVariableName(variablePrefix, VERSION_SUFFIX), true, false, getVariableName(variablePrefix, VERSION_SUFFIX), false, DataType.STRING);
        return new VariableMetadata[]{key, found, status, secret, service, plan, xml, label, platform, callbackUrl, scope, type, version};
    }

    private String getVariableName(final String prefix, final String suffix) {
        return prefix + "." + suffix;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = LookupApiKeyAssertion.class.getName() + ".metadataInitialized";

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
        meta.put(AssertionMetadata.SHORT_NAME, "Look Up API Key");
        meta.put(AssertionMetadata.LONG_NAME, "Look up an API key and set the result on prefixed single-valued context variables.");

        // Add to palette folder(s) 
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing, 
        //   misc, audit, policyLogic, threatProtection 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"internalAssertions"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");

        // Enable automatic policy advice (default is no advice unless a matching Advice subclass exists)
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");

        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:LookupApiKey" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
