package com.l7tech.external.assertions.circuitbreaker;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

import java.util.HashMap;
import java.util.Map;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Applies the Circuit Breaker pattern to a block of policy.
 */
public class CircuitBreakerAssertion extends Assertion implements UsesVariables {

    public String[] getVariablesUsed() {
        return new String[0]; //Syntax.getReferencedNames(...);
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = CircuitBreakerAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();

        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();  // TODO: add cluster properties for defaults
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);

        // Set name and description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, "Circuit Breaker");
        meta.put(AssertionMetadata.LONG_NAME, "Circuit Breaker");
        meta.put(AssertionMetadata.DESCRIPTION, "Applies the Circuit Breaker pattern to a block of policy.");

        // Add to palette folder(s) 
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "policyLogic" });

        // Icons
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/folder.gif");
        meta.put(POLICY_NODE_ICON_OPEN, "com/l7tech/console/resources/folderOpen.gif");
        meta.put(CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/proxy/resources/tree/folder.gif");
        meta.put(CLIENT_ASSERTION_POLICY_ICON_OPEN, "com/l7tech/proxy/resources/tree/folderOpen.gif");

        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }

}
