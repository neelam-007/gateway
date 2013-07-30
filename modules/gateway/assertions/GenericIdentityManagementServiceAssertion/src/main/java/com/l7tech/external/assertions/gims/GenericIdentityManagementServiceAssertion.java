package com.l7tech.external.assertions.gims;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.UsesVariables;

import java.util.logging.Logger;

/**
 * 
 */
public class GenericIdentityManagementServiceAssertion extends Assertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(GenericIdentityManagementServiceAssertion.class.getName());

    public String[] getVariablesUsed() {
        return new String[0];
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = GenericIdentityManagementServiceAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Load GIMS Service policy template
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.gims.GenericIdentityManagementServiceModuleLoadListener");
        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:GenericIdentityManagementService" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
