package com.l7tech.external.assertions.hazelcastembeddedprovider;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

import static com.l7tech.policy.assertion.AssertionMetadata.FEATURE_SET_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME;
import static java.lang.Boolean.TRUE;


/**
 * HazelcastEmbeddedProviderAssertion extends the Assertion framework to provide Hazelcast implementation of Gateway Extensions
 */
public class HazelcastEmbeddedProviderAssertion extends Assertion {

    private static final String META_INITIALIZED = HazelcastEmbeddedProviderAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (TRUE.equals(meta.get(META_INITIALIZED))) {
            return meta;
        }

        meta.put(FEATURE_SET_NAME, "set:modularAssertions");
        meta.put(MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.hazelcastembeddedprovider.server.HazelcastLoader");
        meta.put(META_INITIALIZED, TRUE);

        return meta;
    }
}
