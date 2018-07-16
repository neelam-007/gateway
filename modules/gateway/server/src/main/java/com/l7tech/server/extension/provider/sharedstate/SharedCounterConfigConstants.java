package com.l7tech.server.extension.provider.sharedstate;

/**
 * Configuration parameters common to all shared counter implementations.
 */
public final class SharedCounterConfigConstants {

    private SharedCounterConfigConstants() {}

    /**
     * Properties that apply to all counters across all nodes in a cluster.
     */
    public static class ClusterProperty {

        private ClusterProperty() {}

        /**
         * Whether to persist the counters or not.
         */
        public static final String PERSIST_COUNTERS_DESCRIPTION = "Enable persistence for shared counters";
        public static final String PERSIST_COUNTERS_PROPNAME = "clusterPersistCountersEnabled";
        public static final String PERSIST_COUNTERS_CLUSTER_PROPNAME = "cluster.persistCounters.enabled";
        public static final boolean PERSIST_COUNTERS_DEFAULT = true;
        public static final String PERSIST_COUNTERS_VALIDATION_TYPE = "boolean";

    }

    /**
     * Properties that apply to some counter operations.
     */
    public static class CounterOperations {

        private CounterOperations() {}

        /**
         * Set value to
         * - false to use an async mode where write consistency is not guaranteed
         * - true if the counter should be updated in synchronous mode, where consistency is guaranteed
         */
        public static final String KEY_WRITE_SYNC = "counter.writeSync";

        /**
         * Set value to
         * - false to use asynchronous mode where read consistency is not guaranteed.
         * - true if the counter should be read in synchronous mode, where consistency is guaranteed.
         */
        public static final String KEY_READ_SYNC = "counter.readSync";

    }

}
