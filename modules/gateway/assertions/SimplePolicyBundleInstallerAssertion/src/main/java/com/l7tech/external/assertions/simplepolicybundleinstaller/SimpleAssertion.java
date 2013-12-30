package com.l7tech.external.assertions.simplepolicybundleinstaller;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

/**
 * Rename SimpleAssertion and ServerSimpleAssertion to test the logic that checks if an Assertion (specified in Assertion.xml) exists on Gateway.
 * For example rename to SimpleAssertionBackup and ServerSimpleAssertionBackup.
 */
public class SimpleAssertion extends Assertion {
    private static final String META_INITIALIZED = SimpleAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "misc" });
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/trust.png");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/trust.png");

        // this assertion is for a development only, leave as "set:modularAssertion", no need to change to "(fromClass)"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
