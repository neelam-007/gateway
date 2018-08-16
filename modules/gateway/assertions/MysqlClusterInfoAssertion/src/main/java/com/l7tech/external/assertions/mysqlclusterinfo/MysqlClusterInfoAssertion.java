package com.l7tech.external.assertions.mysqlclusterinfo;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.UsesVariables;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class MysqlClusterInfoAssertion extends Assertion implements UsesVariables {
    public String[] getVariablesUsed() {
        return new String[0];
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = MysqlClusterInfoAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<>();
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "set:modularAssertions");
        meta.put(AssertionMetadata.MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.mysqlclusterinfo.server.MysqlClusterInfoServiceLoader");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
