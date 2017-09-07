package com.l7tech.external.assertions.circuitbreaker;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.variable.Syntax;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.l7tech.external.assertions.circuitbreaker.CircuitBreakerConstants.*;
import static com.l7tech.policy.assertion.AssertionMetadata.*;


/**
 * Applies the Circuit Breaker pattern to a block of policy.
 */
public class CircuitBreakerAssertion extends CompositeAssertion implements UsesVariables {

    private static final String BASE_NAME = "Apply Circuit Breaker";
    private static final String CIRCUIT_BREAKER_PACKAGE_PREFIX = "com.l7tech.external.assertions.circuitbreaker.";

    private static final AssertionNodeNameFactory NODE_NAME_FACTORY =
            (AssertionNodeNameFactory<CircuitBreakerAssertion>) (assertion, decorate) -> {
        if (!decorate) return BASE_NAME;

        StringBuilder name = new StringBuilder(BASE_NAME);

        if (!assertion.isPolicyFailureCircuitEnabled() && !assertion.isLatencyCircuitEnabled()) {
            name.append(" (No Circuits enabled)");
        }

        return name.toString();
    };

    private boolean policyFailureCircuitCustomTrackerIdEnabled;
    private String policyFailureCircuitTrackerId;
    private String policyFailureCircuitRecoveryPeriod = String.valueOf(CB_POLICY_FAILURE_CIRCUIT_RECOVERY_PERIOD_DEFAULT);
    private String policyFailureCircuitMaxFailures = String.valueOf(CB_POLICY_FAILURE_CIRCUIT_MAX_FAILURES_DEFAULT);
    private String policyFailureCircuitSamplingWindow = String.valueOf(CB_POLICY_FAILURE_CIRCUIT_SAMPLING_WINDOW_DEFAULT);

    private boolean latencyCircuitCustomTrackerIdEnabled;
    private String latencyCircuitTrackerId;
    private String latencyCircuitRecoveryPeriod = String.valueOf(CB_LATENCY_CIRCUIT_RECOVERY_PERIOD_DEFAULT);
    private String latencyCircuitMaxFailures = String.valueOf(CB_LATENCY_CIRCUIT_MAX_FAILURES_DEFAULT);
    private String latencyCircuitSamplingWindow = String.valueOf(CB_LATENCY_CIRCUIT_SAMPLING_WINDOW_DEFAULT);
    private String latencyCircuitMaxLatency = String.valueOf(CB_LATENCY_CIRCUIT_MAX_LATENCY_DEFAULT);

    private boolean policyFailureCircuitEnabled = true;
    private boolean latencyCircuitEnabled = false;

    public CircuitBreakerAssertion() {}

    public CircuitBreakerAssertion(List<? extends Assertion> children ) {
        super(children);
    }

    public boolean isPolicyFailureCircuitEnabled() {
        return this.policyFailureCircuitEnabled;
    }

    public void setPolicyFailureCircuitEnabled(boolean policyFailureCircuitEnabled) {
        this.policyFailureCircuitEnabled = policyFailureCircuitEnabled;
    }

    public boolean isPolicyFailureCircuitCustomTrackerIdEnabled() {
        return policyFailureCircuitCustomTrackerIdEnabled;
    }

    public void setPolicyFailureCircuitCustomTrackerIdEnabled(boolean policyFailureCircuitCustomTrackerIdEnabled) {
        this.policyFailureCircuitCustomTrackerIdEnabled = policyFailureCircuitCustomTrackerIdEnabled;
    }
    public String getPolicyFailureCircuitTrackerId() {
        return this.policyFailureCircuitTrackerId;
    }

    public void setPolicyFailureCircuitTrackerId(String policyFailureCircuitTrackerId) {
        this.policyFailureCircuitTrackerId = policyFailureCircuitTrackerId;
    }

    public String getPolicyFailureCircuitRecoveryPeriod() {
        return this.policyFailureCircuitRecoveryPeriod;
    }

    public void setPolicyFailureCircuitRecoveryPeriod(String policyFailureCircuitRecoveryPeriod) {
        this.policyFailureCircuitRecoveryPeriod = policyFailureCircuitRecoveryPeriod;
    }

    public String getPolicyFailureCircuitMaxFailures() {
        return this.policyFailureCircuitMaxFailures;
    }

    public void setPolicyFailureCircuitMaxFailures(String policyFailureCircuitMaxFailures) {
        this.policyFailureCircuitMaxFailures = policyFailureCircuitMaxFailures;
    }

    public String getPolicyFailureCircuitSamplingWindow() {
        return this.policyFailureCircuitSamplingWindow;
    }

    public void setPolicyFailureCircuitSamplingWindow(String policyFailureCircuitSamplingWindow) {
        this.policyFailureCircuitSamplingWindow = policyFailureCircuitSamplingWindow;
    }

    public boolean isLatencyCircuitEnabled() {
        return this.latencyCircuitEnabled;
    }

    public void setLatencyCircuitEnabled(boolean latencyCircuitEnabled) {
        this.latencyCircuitEnabled = latencyCircuitEnabled;
    }

    public boolean isLatencyCircuitCustomTrackerIdEnabled() {
        return latencyCircuitCustomTrackerIdEnabled;
    }

    public void setLatencyCircuitCustomTrackerIdEnabled(boolean latencyCircuitCustomTrackerIdEnabled) {
        this.latencyCircuitCustomTrackerIdEnabled = latencyCircuitCustomTrackerIdEnabled;
    }

    public String getLatencyCircuitTrackerId() {
        return this.latencyCircuitTrackerId;
    }

    public void setLatencyCircuitTrackerId(String latencyCircuitTrackerId) {
        this.latencyCircuitTrackerId = latencyCircuitTrackerId;
    }

    public String getLatencyCircuitRecoveryPeriod() {
        return this.latencyCircuitRecoveryPeriod;
    }

    public void setLatencyCircuitRecoveryPeriod(String latencyCircuitRecoveryPeriod) {
        this.latencyCircuitRecoveryPeriod = latencyCircuitRecoveryPeriod;
    }

    public String getLatencyCircuitMaxFailures() {
        return this.latencyCircuitMaxFailures;
    }

    public void setLatencyCircuitMaxFailures(String latencyCircuitMaxFailures) {
        this.latencyCircuitMaxFailures = latencyCircuitMaxFailures;
    }

    public String getLatencyCircuitSamplingWindow() {
        return this.latencyCircuitSamplingWindow;
    }

    public void setLatencyCircuitSamplingWindow(String latencyCircuitSamplingWindow) {
        this.latencyCircuitSamplingWindow = latencyCircuitSamplingWindow;
    }

    public String getLatencyCircuitMaxLatency() {
        return this.latencyCircuitMaxLatency;
    }

    public void setLatencyCircuitMaxLatency(String latencyCircuitMaxLatency) {
        this.latencyCircuitMaxLatency = latencyCircuitMaxLatency;
    }

    @Override
    public boolean permitsEmpty() {
        return true;
    }

    public String[] getVariablesUsed() {
        List<String> vars = new ArrayList<>();
        vars.add(policyFailureCircuitMaxFailures);
        vars.add(policyFailureCircuitRecoveryPeriod);
        vars.add(policyFailureCircuitSamplingWindow);
        if (StringUtils.isNotEmpty(policyFailureCircuitTrackerId)) {
            vars.add(policyFailureCircuitTrackerId);
        }
        vars.add(latencyCircuitMaxFailures);
        vars.add(latencyCircuitRecoveryPeriod);
        vars.add(latencyCircuitSamplingWindow);
        vars.add(latencyCircuitMaxLatency);
        if (StringUtils.isNotEmpty(latencyCircuitTrackerId)) {
            vars.add(latencyCircuitTrackerId);
        }
        return Syntax.getReferencedNames(vars.toArray(new String[vars.size()]));
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
        meta.put(POLICY_NODE_NAME_FACTORY, NODE_NAME_FACTORY);

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
