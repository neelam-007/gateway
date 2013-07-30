package com.l7tech.external.assertions.concall;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * 
 */
public class ConcurrentAllAssertion extends CompositeAssertion {
    protected static final Logger logger = Logger.getLogger(ConcurrentAllAssertion.class.getName());

    // ServerConfig property names and cluster property names for our dynamically-registered cluster properties
    public static final String SC_MAX_CONC = "concallGlobalMaxConcurrency";
    private static final String CP_MAX_CONC = "concall.globalMaxConcurrency";

    public static final String SC_CORE_CONC = "concallGlobalCoreConcurrency";
    private static final String CP_CORE_CONC = "concall.globalCoreConcurrency";

    public static final String SC_MAX_QUEUE = "concallGlobalMaxWorkQueue";
    private static final String CP_MAX_QUEUE = "concall.globalMaxWorkQueue";

    public ConcurrentAllAssertion() {
    }

    public ConcurrentAllAssertion( List<? extends Assertion> children ) {
        super( children );
    }

    /**
     * Check if the assertion is the root assertion.
     * @return true if this AllAssertion has no parent.
     */
    public boolean isRoot() {
        return getParent() == null;
    }

    @Override
    public boolean permitsEmpty() {
        return true;
    }

    //
    // Metadata
    //
    private static final String META_INITIALIZED = ConcurrentAllAssertion.class.getName() + ".metadataInitialized";

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(SHORT_NAME, "Run All Assertions Concurrently");
        meta.put(DESCRIPTION, "All child assertions are " +
                "run concurrently and must evaluate to true. Can help reduce overall latency but " +
                "use with caution.");

        meta.put(PROPERTIES_ACTION_NAME, "Add 'All...' Folder");
        
        // Cluster properties used by this assertion
        Map<String, String[]> props = new HashMap<String, String[]>();
        props.put(CP_MAX_CONC, new String[] {
                "Maximum number of assertions that may execute concurrently by Concurrent All assertions.  " +
                        "This is a global limit across all such assertions. (default=64)",
                "64"
        });
        props.put(CP_CORE_CONC, new String[] {
                "Core number of assertions that may execute concurrently by Concurrent All assertions.  " +
                        "This is a soft limit that may be temporarily exceeded if necessary. " +
                        "This is a global limit across all such assertions. (default=32)",
                "32"
        });
        props.put(CP_MAX_QUEUE, new String[] {
                "Maximum number of assertions that may be waiting to execute concurrently.  " +
                        "When this limit is reached assertions will be run serially until the system catches up. " +
                        "This is a global limit across all such assertions. (default=64)",
                "64"
        });
        meta.put(AssertionMetadata.CLUSTER_PROPERTIES, props);
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "policyLogic" });

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/folderConc.gif");
        meta.put(POLICY_NODE_ICON_OPEN, "com/l7tech/console/resources/folderOpenConc.gif");

        meta.put(POLICY_VALIDATOR_CLASSNAME, "com.l7tech.external.assertions.concall.ConcurrentAllAssertionValidator");
        
        meta.put(AssertionMetadata.POLICY_NODE_CLASSNAME, "com.l7tech.external.assertions.concall.console.ConcurrentAllAssertionPolicyNode");

        meta.put(AssertionMetadata.WSP_TYPE_MAPPING_CLASSNAME, "com.l7tech.external.assertions.concall.ConcurrentAllAssertionTypeMapping");

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
