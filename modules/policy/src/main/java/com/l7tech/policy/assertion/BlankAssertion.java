package com.l7tech.policy.assertion;

/**
 * An assertion with blank information is inserted into a policy tree in the Policy Diff feature.
 */
public class BlankAssertion extends Assertion {
    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = new DefaultAssertionMetadata(this);
        meta.put(AssertionMetadata.SHORT_NAME, "");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/Transparent16x1.png");

        // BlankAssertion does not affect validation.
        // It's added for formatting display in features like the Policy Diff (which is read only and does not require policy validation).
        meta.put(AssertionMetadata.POLICY_VALIDATION_ADVICE_ENABLED, false);

        return meta;
    }

    @Override
    public int getOrdinal() {
        return -1;   // Any arbitrary value less than 1 (the ordinal of root AllAssertion)
    }
}