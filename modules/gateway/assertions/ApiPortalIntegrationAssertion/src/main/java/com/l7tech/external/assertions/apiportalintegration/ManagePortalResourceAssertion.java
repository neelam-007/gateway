package com.l7tech.external.assertions.apiportalintegration;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

/**
 * Assertion for performing CRUD operations on API Portal resources such as APIs, API Keys, and API Plans.
 */
public class ManagePortalResourceAssertion extends Assertion implements UsesVariables, SetsVariables {
    public static final String OPERATION = "pman.operation";
    public static final String RESOURCE_URI = "pman.resUri";
    public static final String OPTION_API_GROUP = "pman.options.apiGroup";
    public static final String OPTION_API_KEY_STATUS = "pman.options.status";
    public static final String OPTION_REMOVE_OMITTED = "pman.options.removeOmitted";
    public static final String RESOURCE = "pman.resource";
    public static final String RESPONSE_RESOURCE = "pman.resp.resource";
    public static final String RESPONSE_STATUS = "pman.resp.status";
    public static final String RESPONSE_DETAIL = "pman.resp.detail";
    public static final String OPTION_POLICY_GUID = "pman.options.policyGUID";

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[]{new VariableMetadata(RESPONSE_STATUS, false, false, null, false, DataType.INTEGER),
                new VariableMetadata(RESPONSE_RESOURCE), new VariableMetadata(RESPONSE_DETAIL)};
    }

    @Override
    public String[] getVariablesUsed() {
        return new String[]{OPERATION, RESOURCE_URI, RESOURCE, OPTION_API_GROUP, OPTION_REMOVE_OMITTED, OPTION_API_KEY_STATUS, OPTION_POLICY_GUID};
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;
        meta.put(AssertionMetadata.SHORT_NAME, "Manage API Portal Resources");
        meta.put(AssertionMetadata.DESCRIPTION, "Manages API Portal Resources");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"internalAssertions"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");
        // no properties need to be edited for this assertion
        meta.putNull(AssertionMetadata.PROPERTIES_EDITOR_FACTORY);
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    private static final String META_INITIALIZED = ManagePortalResourceAssertion.class.getName() + ".metadataInitialized";
}
