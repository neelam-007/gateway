package com.l7tech.policy.assertion;

/**
 * Assertion that can validate the syntax of a target message's outer content type.
 */
public class ValidateContentTypeAssertion extends MessageTargetableAssertion {
    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.DESCRIPTION, "Checks that the target message has a syntactically-valid content-type.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "threatProtection" });
        return meta;
    }
}
