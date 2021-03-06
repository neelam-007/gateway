package com.l7tech.policy.assertion;

/**
 * A very simple assertion to raise an error to stop branch/policy from execution.
 */
public class RaiseErrorAssertion extends Assertion {
    private static final String META_INITIALIZED = RaiseErrorAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"policyLogic"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/Stop16.gif");
        meta.put(AssertionMetadata.SHORT_NAME, "Raise Error");
        meta.put(AssertionMetadata.DESCRIPTION, "Raises an error to terminate the policy execution.");


        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }
}
