package com.l7tech.external.assertions.policybundleinstaller;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

import java.util.logging.Logger;

/**
 * 
 */
public class PolicyBundleInstallerAssertion extends Assertion implements UsesVariables {
    protected static final Logger logger = Logger.getLogger(PolicyBundleInstallerAssertion.class.getName());

    @Override
    public String[] getVariablesUsed() {
        return new String[0];
    }

    private static final String META_INITIALIZED = PolicyBundleInstallerAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // do not show in palette
        //meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstallerLifecycle");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
