package com.l7tech.external.assertions.circuitbreaker;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import java.util.List;

import static com.l7tech.external.assertions.circuitbreaker.CircuitBreakerConstants.*;
import static com.l7tech.policy.assertion.AssertionMetadata.*;


/**
 * Applies the Circuit Breaker pattern to a block of policy.
 */
public class CircuitBreakerAssertion extends CompositeAssertion {

    private static final String BASE_NAME = "Apply Circuit Breaker";
    private static final String CIRCUIT_BREAKER_PACKAGE_PREFIX = "com.l7tech.external.assertions.circuitbreaker.";

    private String policyFailureCircuitTrackerId;
    private int policyFailureCircuitRecoveryPeriod = CB_POLICY_FAILURE_CIRCUIT_RECOVERY_PERIOD_DEFAULT;
    private int policyFailureCircuitMaxFailures = CB_POLICY_FAILURE_CIRCUIT_MAX_FAILURES_DEFAULT;
    private int policyFailureCircuitSamplingWindow = CB_POLICY_FAILURE_CIRCUIT_SAMPLING_WINDOW_DEFAULT;

    private String latencyCircuitTrackerId;
    private int latencyCircuitRecoveryPeriod = CB_LATENCY_CIRCUIT_RECOVERY_PERIOD_DEFAULT;
    private int latencyCircuitMaxFailures = CB_LATENCY_CIRCUIT_MAX_FAILURES_DEFAULT;
    private int latencyCircuitSamplingWindow = CB_LATENCY_CIRCUIT_SAMPLING_WINDOW_DEFAULT;
    private int latencyCircuitMaxLatency = CB_LATENCY_CIRCUIT_MAX_LATENCY_DEFAULT;

    private boolean policyFailureCircuitEnabled = true;
    private boolean latencyCircuitEnabled = false;

    public CircuitBreakerAssertion() {
    }

    public CircuitBreakerAssertion(List<? extends Assertion> children ) {
        super(children);
    }

    public String getPolicyFailureCircuitTrackerId() {
        return this.policyFailureCircuitTrackerId;
    }

    public void setPolicyFailureCircuitTrackerId(String policyFailureCircuitTrackerId) {
        this.policyFailureCircuitTrackerId = policyFailureCircuitTrackerId;
    }

    public int getPolicyFailureCircuitRecoveryPeriod() {
        return this.policyFailureCircuitRecoveryPeriod;
    }

    public void setPolicyFailureCircuitRecoveryPeriod(int policyFailureCircuitRecoveryPeriod) {
        this.policyFailureCircuitRecoveryPeriod = policyFailureCircuitRecoveryPeriod;
    }

    public int getPolicyFailureCircuitMaxFailures() {
        return this.policyFailureCircuitMaxFailures;
    }

    public void setPolicyFailureCircuitMaxFailures(int policyFailureCircuitMaxFailures) {
        this.policyFailureCircuitMaxFailures = policyFailureCircuitMaxFailures;
    }

    public int getPolicyFailureCircuitSamplingWindow() {
        return this.policyFailureCircuitSamplingWindow;
    }

    public void setPolicyFailureCircuitSamplingWindow(int policyFailureCircuitSamplingWindow) {
        this.policyFailureCircuitSamplingWindow = policyFailureCircuitSamplingWindow;
    }

    public String getLatencyCircuitTrackerId() {
        return this.latencyCircuitTrackerId;
    }

    public void setLatencyCircuitTrackerId(String latencyCircuitTrackerId) {
        this.latencyCircuitTrackerId = latencyCircuitTrackerId;
    }

    public int getLatencyCircuitRecoveryPeriod() {
        return this.latencyCircuitRecoveryPeriod;
    }

    public void setLatencyCircuitRecoveryPeriod(int latencyCircuitRecoveryPeriod) {
        this.latencyCircuitRecoveryPeriod = latencyCircuitRecoveryPeriod;
    }

    public int getLatencyCircuitMaxFailures() {
        return this.latencyCircuitMaxFailures;
    }

    public void setLatencyCircuitMaxFailures(int latencyCircuitMaxFailures) {
        this.latencyCircuitMaxFailures = latencyCircuitMaxFailures;
    }

    public int getLatencyCircuitSamplingWindow() {
        return this.latencyCircuitSamplingWindow;
    }

    public void setLatencyCircuitSamplingWindow(int latencyCircuitSamplingWindow) {
        this.latencyCircuitSamplingWindow = latencyCircuitSamplingWindow;
    }

    public int getLatencyCircuitMaxLatency() {
        return this.latencyCircuitMaxLatency;
    }

    public void setLatencyCircuitMaxLatency(int latencyCircuitMaxLatency) {
        this.latencyCircuitMaxLatency = latencyCircuitMaxLatency;
    }

    public boolean isPolicyFailureCircuitEnabled() {
        return this.policyFailureCircuitEnabled;
    }

    public void setPolicyFailureCircuitEnabled(boolean policyFailureCircuitEnabled) {
        this.policyFailureCircuitEnabled = policyFailureCircuitEnabled;
    }

    public boolean isLatencyCircuitEnabled() {
        return this.latencyCircuitEnabled;
    }

    public void setLatencyCircuitEnabled(boolean latencyCircuitEnabled) {
        this.latencyCircuitEnabled = latencyCircuitEnabled;
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

        // Set name and description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, BASE_NAME);
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, BASE_NAME);
        meta.put(AssertionMetadata.DESCRIPTION, "Applies the Circuit Breaker pattern to a block of policy.");

        // Dialogs, icons, and other GUI code
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "policyLogic" });
        meta.put(PALETTE_NODE_ICON, "com/l7tech/external/assertions/circuitbreaker/console/circuit-breaker-16.png");
        meta.put(POLICY_NODE_ICON_OPEN, "com/l7tech/external/assertions/circuitbreaker/console/circuit-breaker-open-16.png");

        meta.put(POLICY_NODE_CLASSNAME, CIRCUIT_BREAKER_PACKAGE_PREFIX + "console.CircuitBreakerAssertionTreeNode");
        meta.put(POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(PROPERTIES_EDITOR_CLASSNAME, CIRCUIT_BREAKER_PACKAGE_PREFIX + "console.CircuitBreakerAssertionPropertiesDialog");
        meta.put(PROPERTIES_ACTION_CLASSNAME, CIRCUIT_BREAKER_PACKAGE_PREFIX + "console.CircuitBreakerAssertionPropertiesAction");

        meta.put(WSP_TYPE_MAPPING_CLASSNAME, CIRCUIT_BREAKER_PACKAGE_PREFIX + "CircuitBreakerAssertionTypeMapping");

        meta.put(MODULE_LOAD_LISTENER_CLASSNAME, CIRCUIT_BREAKER_PACKAGE_PREFIX + "server.CircuitBreakerModuleLoadListener");

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        meta.put(META_INITIALIZED, Boolean.TRUE);

        meta.put(PROPERTIES_ACTION_NAME, "Circuit Breaker Properties");

        return meta;
    }
}
