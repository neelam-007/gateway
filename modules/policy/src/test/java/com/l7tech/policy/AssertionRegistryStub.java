package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;

/**
 * Test AssertionRegistry impl
 */
public class AssertionRegistryStub extends AssertionRegistry {

    public AssertionRegistryStub() {
        super();

        // Pre-populate with hardcoded assertions
        for (Assertion assertion : AllAssertions.SERIALIZABLE_EVERYTHING)
            registerAssertion(assertion.getClass());
    }

}
