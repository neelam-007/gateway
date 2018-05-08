package com.l7tech.external.assertions.circuitbreaker;

import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.*;
import org.w3c.dom.Element;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
@SuppressWarnings("unused")
public class CircuitBreakerAssertionTypeMapping extends CompositeAssertionMapping {

    private static final String L7_NS_PREFIX = "L7p:";

    private static final String POLICY_FAILURE_CIRCUIT_ENABLED = "policyFailureCircuitEnabled";
    private static final String POLICY_FAILURE_CIRCUIT_CUSTOM_TRACKER_ID_ENABLED = "policyFailureCircuitCustomTrackerIdEnabled";
    private static final String POLICY_FAILURE_CIRCUIT_EVENT_TRACKER_ID = "policyFailureCircuitEventTrackerID";
    private static final String POLICY_FAILURE_CIRCUIT_MAX_FAILURES = "policyFailureCircuitMaxFailures";
    private static final String POLICY_FAILURE_CIRCUIT_SAMPLING_WINDOW = "policyFailureCircuitSamplingWindow";
    private static final String POLICY_FAILURE_CIRCUIT_RECOVERY_PERIOD = "policyFailureCircuitRecoveryPeriod";

    private static final String LATENCY_CIRCUIT_ENABLED = "latencyCircuitEnabled";
    private static final String LATENCY_CIRCUIT_CUSTOM_TRACKER_ID_ENABLED = "latencyCircuitCustomTrackerIdEnabled";
    private static final String LATENCY_CIRCUIT_EVENT_TRACKER_ID = "latencyCircuitEventTrackerID";
    private static final String LATENCY_CIRCUIT_MAX_FAILURES = "latencyCircuitMaxFailures";
    private static final String LATENCY_CIRCUIT_SAMPLING_WINDOW = "latencyCircuitSamplingWindow";
    private static final String LATENCY_CIRCUIT_RECOVERY_PERIOD = "latencyCircuitRecoveryPeriod";
    private static final String LATENCY_CIRCUIT_MAX_LATENCY = "latencyCircuitMaxLatency";

    public CircuitBreakerAssertionTypeMapping() {
        super(new CircuitBreakerAssertion(), "CircuitBreaker");
    }
    
    @Override
    protected void populateElement(WspWriter wspWriter, Element element, TypedReference object) throws InvalidPolicyTreeException {
        CircuitBreakerAssertion assertion = (CircuitBreakerAssertion) object.target;

        element.setAttribute(POLICY_FAILURE_CIRCUIT_ENABLED, String.valueOf(assertion.isPolicyFailureCircuitEnabled()));
        element.setAttribute(POLICY_FAILURE_CIRCUIT_EVENT_TRACKER_ID, assertion.getPolicyFailureCircuitTrackerId());
        element.setAttribute(POLICY_FAILURE_CIRCUIT_MAX_FAILURES, assertion.getPolicyFailureCircuitMaxFailures());
        element.setAttribute(POLICY_FAILURE_CIRCUIT_SAMPLING_WINDOW, String.valueOf(assertion.getPolicyFailureCircuitSamplingWindow()));
        element.setAttribute(POLICY_FAILURE_CIRCUIT_RECOVERY_PERIOD, String.valueOf(assertion.getPolicyFailureCircuitRecoveryPeriod()));
        element.setAttribute(POLICY_FAILURE_CIRCUIT_CUSTOM_TRACKER_ID_ENABLED, String.valueOf(assertion.isPolicyFailureCircuitCustomTrackerIdEnabled()));

        element.setAttribute(LATENCY_CIRCUIT_ENABLED, String.valueOf(assertion.isLatencyCircuitEnabled()));
        element.setAttribute(LATENCY_CIRCUIT_EVENT_TRACKER_ID, assertion.getLatencyCircuitTrackerId());
        element.setAttribute(LATENCY_CIRCUIT_MAX_FAILURES, String.valueOf(assertion.getLatencyCircuitMaxFailures()));
        element.setAttribute(LATENCY_CIRCUIT_MAX_LATENCY, String.valueOf(assertion.getLatencyCircuitMaxLatency()));
        element.setAttribute(LATENCY_CIRCUIT_SAMPLING_WINDOW, String.valueOf(assertion.getLatencyCircuitSamplingWindow()));
        element.setAttribute(LATENCY_CIRCUIT_RECOVERY_PERIOD, String.valueOf(assertion.getLatencyCircuitRecoveryPeriod()));
        element.setAttribute(LATENCY_CIRCUIT_CUSTOM_TRACKER_ID_ENABLED, String.valueOf(assertion.isLatencyCircuitCustomTrackerIdEnabled()));

        super.populateElement(wspWriter, element, object);
    }

    @Override
    protected void populateObject(CompositeAssertion cass, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        CircuitBreakerAssertion ass = (CircuitBreakerAssertion) cass;

        ass.setPolicyFailureCircuitEnabled(Boolean.valueOf(source.getAttribute(POLICY_FAILURE_CIRCUIT_ENABLED)));
        ass.setPolicyFailureCircuitTrackerId(source.getAttribute(POLICY_FAILURE_CIRCUIT_EVENT_TRACKER_ID));
        ass.setPolicyFailureCircuitMaxFailures(source.getAttribute(POLICY_FAILURE_CIRCUIT_MAX_FAILURES));
        ass.setPolicyFailureCircuitSamplingWindow(source.getAttribute(POLICY_FAILURE_CIRCUIT_SAMPLING_WINDOW));
        ass.setPolicyFailureCircuitRecoveryPeriod(source.getAttribute(POLICY_FAILURE_CIRCUIT_RECOVERY_PERIOD));
        ass.setPolicyFailureCircuitCustomTrackerIdEnabled(Boolean.valueOf(source.getAttribute(POLICY_FAILURE_CIRCUIT_CUSTOM_TRACKER_ID_ENABLED)));

        ass.setLatencyCircuitEnabled(Boolean.valueOf(source.getAttribute(LATENCY_CIRCUIT_ENABLED)));
        ass.setLatencyCircuitTrackerId(source.getAttribute(LATENCY_CIRCUIT_EVENT_TRACKER_ID));
        ass.setLatencyCircuitMaxFailures(source.getAttribute(LATENCY_CIRCUIT_MAX_FAILURES));
        ass.setLatencyCircuitMaxLatency(source.getAttribute(LATENCY_CIRCUIT_MAX_LATENCY));
        ass.setLatencyCircuitSamplingWindow(source.getAttribute(LATENCY_CIRCUIT_SAMPLING_WINDOW));
        ass.setLatencyCircuitRecoveryPeriod(source.getAttribute(LATENCY_CIRCUIT_RECOVERY_PERIOD));
        ass.setLatencyCircuitCustomTrackerIdEnabled(Boolean.valueOf(source.getAttribute(LATENCY_CIRCUIT_CUSTOM_TRACKER_ID_ENABLED)));

        super.populateObject(cass, source, visitor);
    }

    @Override
    protected String getNsPrefix() {
        return L7_NS_PREFIX;
    }

    @Override
    protected String getNsUri() {
        return WspConstants.L7_POLICY_NS;
    }
}