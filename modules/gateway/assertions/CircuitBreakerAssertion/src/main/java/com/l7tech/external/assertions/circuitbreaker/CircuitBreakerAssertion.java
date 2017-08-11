package com.l7tech.external.assertions.circuitbreaker;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * Applies the Circuit Breaker pattern to a block of policy.
 */
public class CircuitBreakerAssertion extends CompositeAssertion {

    //Failure threshold hard coded values
    //10 seconds recovery period
    public static final long POLICY_FAILURE_RECOVERY_PERIOD = 10 * 1000;
    public static final long POLICY_FAILURE_MAX_COUNT = 5;
    //5 seconds sampling window
    public static final long POLICY_FAILURE_SAMPLING_WINDOW = 5 * 1000;

    //10 seconds recovery period
    public static final long LATENCY_FAILURE_RECOVERY_PERIOD = 10 * 1000;
    public static final long LATENCY_FAILURE_MAX_COUNT = 5;
    //5 seconds Sampling window
    public static final long LATENCY_FAILURE_SAMPLING_WINDOW = 5 * 1000;
    //500 ms latency failure limit
    public static final long LATENCY_FAILURE_LIMIT = 500;

    private String policyFailureTrackerId;
    private long policyFailureRecoveryPeriod = POLICY_FAILURE_RECOVERY_PERIOD;
    private long policyFailureMaxCount = POLICY_FAILURE_MAX_COUNT;
    private long policyFailureSamplingWindow = POLICY_FAILURE_SAMPLING_WINDOW;

    private String latencyFailureTrackerId;
    private long latencyRecoveryPeriod = LATENCY_FAILURE_RECOVERY_PERIOD;
    private long latencyFailureMaxCount = LATENCY_FAILURE_MAX_COUNT;
    private long latencyFailureSamplingWindow = LATENCY_FAILURE_SAMPLING_WINDOW;
    private long latencyFailureLimit = LATENCY_FAILURE_LIMIT;

    private boolean policyFailureEnabled = true;
    private boolean latencyFailureEnabled = true;

    public CircuitBreakerAssertion() {
    }

    public CircuitBreakerAssertion(List<? extends Assertion> children ) {
        super(children);
    }

    public String getPolicyFailureTrackerId() {
        return this.policyFailureTrackerId;
    }

    public void setPolicyFailureTrackerId(String policyFailureTrackerId) {
        this.policyFailureTrackerId = policyFailureTrackerId;
    }

    public long getPolicyFailureRecoveryPeriod() {
        return this.policyFailureRecoveryPeriod;
    }

    public void setPolicyFailureRecoveryPeriod(long policyFailureRecoveryPeriod) {
        this.policyFailureRecoveryPeriod = policyFailureRecoveryPeriod;
    }

    public long getPolicyFailureMaxCount() {
        return this.policyFailureMaxCount;
    }

    public void setPolicyFailureMaxCount(long policyFailureMaxCount) {
        this.policyFailureMaxCount = policyFailureMaxCount;
    }

    public long getPolicyFailureSamplingWindow() {
        return this.policyFailureSamplingWindow;
    }

    public void setPolicyFailureSamplingWindow(long policyFailureSamplingWindow) {
        this.policyFailureSamplingWindow = policyFailureSamplingWindow;
    }

    public String getLatencyFailureTrackerId() {
        return this.latencyFailureTrackerId;
    }

    public void setLatencyFailureTrackerId(String latencyFailureTrackerId) {
        this.latencyFailureTrackerId = latencyFailureTrackerId;
    }

    public long getLatencyRecoveryPeriod() {
        return this.latencyRecoveryPeriod;
    }

    public void setLatencyRecoveryPeriod(long latencyRecoveryPeriod) {
        this.latencyRecoveryPeriod = latencyRecoveryPeriod;
    }

    public long getLatencyFailureMaxCount() {
        return this.latencyFailureMaxCount;
    }

    public void setLatencyFailureMaxCount(long latencyFailureMaxCount) {
        this.latencyFailureMaxCount = latencyFailureMaxCount;
    }

    public long getLatencyFailureSamplingWindow() {
        return this.latencyFailureSamplingWindow;
    }

    public void setLatencyFailureSamplingWindow(long latencyFailureSamplingWindow) {
        this.latencyFailureSamplingWindow = latencyFailureSamplingWindow;
    }

    public long getLatencyFailureLimit() {
        return this.latencyFailureLimit;
    }

    public void setLatencyFailureLimit(long latencyFailureLimit) {
        this.latencyFailureLimit = latencyFailureLimit;
    }

    public boolean isPolicyFailureEnabled() {
        return this.policyFailureEnabled;
    }

    public void setPolicyFailureEnabled(boolean policyFailureEnabled) {
        this.policyFailureEnabled = policyFailureEnabled;
    }

    public boolean isLatencyFailureEnabled() {
        return this.latencyFailureEnabled;
    }

    public void setLatencyFailureEnabled(boolean latencyFailureEnabled) {
        this.latencyFailureEnabled = latencyFailureEnabled;
    }

    @Override
    public boolean permitsEmpty() {
        return true;
    }

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
        Map<String, String[]> props = new HashMap<>();  // TODO: add cluster properties for defaults
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

        meta.put(POLICY_NODE_CLASSNAME, "com.l7tech.external.assertions.circuitbreaker.console.CircuitBreakerAssertionTreeNode");

        meta.put(WSP_TYPE_MAPPING_CLASSNAME, "com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertionTypeMapping");

        meta.put(MODULE_LOAD_LISTENER_CLASSNAME, "com.l7tech.external.assertions.circuitbreaker.server.CircuitBreakerModuleLoadListener");

        meta.put(META_INITIALIZED, Boolean.TRUE);

        return meta;
    }

}
