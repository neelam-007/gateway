package com.l7tech.external.assertions.mysqlcounter;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

import java.util.HashMap;
import java.util.Map;

public class MysqlCounterAssertion extends Assertion {
    private static final String META_INITIALIZED = MysqlCounterAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<>();
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.mysqlcounter.server.MysqlCounterLoader");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}