package com.l7tech.external.assertions.ldapupdate;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

/**
 * Performs LDAP Update operations
 *
 * @author rraquepo
 */
public class LDAPUpdateAssertion extends Assertion implements UsesVariables, SetsVariables {
    public static final String OPERATION = "ldapman.operation"; //manage, validateOperation, clearCache
    public static final String PROVIDER_NAME = "ldapman.providerName";
    public static final String RESOURCE = "ldapman.resource";
    public static final String INJECTION_PROTECTION = "ldapman.ldapInjectionProtection";//default to true, but for some cases, we may want to disable that protection, that's why we have this setting
    public static final String RESPONSE_RESOURCE = "ldapman.resp.resource";
    public static final String RESPONSE_STATUS = "ldapman.resp.status";
    public static final String RESPONSE_DETAIL = "ldapman.resp.detail";

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[]{new VariableMetadata(RESPONSE_STATUS, false, false, null, false, DataType.INTEGER),
                new VariableMetadata(RESPONSE_RESOURCE), new VariableMetadata(RESPONSE_DETAIL)};
    }

    @Override
    public String[] getVariablesUsed() {
        return new String[]{OPERATION, PROVIDER_NAME, RESOURCE, INJECTION_PROTECTION};
    }

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;
        meta.put(AssertionMetadata.SHORT_NAME, "Manage API Portal LDAP");
        meta.put(AssertionMetadata.DESCRIPTION, "Manages API Portal LDAP");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{""});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/server16.gif");
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");
        // no properties need to be edited for this assertion
        meta.putNull(AssertionMetadata.PROPERTIES_EDITOR_FACTORY);
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    private static final String META_INITIALIZED = LDAPUpdateAssertion.class.getName() + ".metadataInitialized";
}
