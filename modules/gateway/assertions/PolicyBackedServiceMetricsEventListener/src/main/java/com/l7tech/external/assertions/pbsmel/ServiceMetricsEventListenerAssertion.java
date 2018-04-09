package com.l7tech.external.assertions.pbsmel;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

/**
 * Support assertion for policy-backed service for service metrics event.
 */
public class ServiceMetricsEventListenerAssertion extends Assertion {
    //
    // Metadata
    //
    private static final String META_INITIALIZED = ServiceMetricsEventListenerAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.pbsmel.server.ServiceMetricsEventListenerManager");
        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }
}
