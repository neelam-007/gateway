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

    private static final String POLICY_FAILURE_CIRCUIT_MAX_FAILURES = "policyFailureCircuitMaxFailures";
    private static final String LATENCY_CIRCUIT_MAX_FAILURES = "latencyCircuitMaxFailures";
    private static final String POLICY_FAILURE_CIRCUIT_SAMPLING_WINDOW = "policyFailureCircuitSamplingWindow";
    private static final String POLICY_FAILURE_CIRCUIT_RECOVERY_PERIOD = "policyFailureCircuitRecoveryPeriod";
    private static final String LATENCY_CIRCUIT_RECOVERY_PERIOD = "latencyCircuitRecoveryPeriod";
    private static final String LATENCY_CIRCUIT_SAMPLING_WINDOW = "latencyCircuitSamplingWindow";
    private static final String LATENCY_CIRCUIT_MAX_LATENCY = "latencyCircuitMaxLatency";
    private static final String POLICY_FAILURE_CIRCUIT_ENABLED = "policyFailureCircuitEnabled";
    private static final String LATENCY_CIRCUIT_ENABLED = "latencyCircuitEnabled";

    public CircuitBreakerAssertionTypeMapping() {
        super(new CircuitBreakerAssertion(), "CircuitBreaker");
    }
    
    @Override
    protected void populateElement(WspWriter wspWriter, Element element, TypedReference object) throws InvalidPolicyTreeException {
        CircuitBreakerAssertion ass = (CircuitBreakerAssertion) object.target;

        element.setAttribute(POLICY_FAILURE_CIRCUIT_ENABLED, String.valueOf(ass.isPolicyFailureCircuitEnabled()));
        element.setAttribute(LATENCY_CIRCUIT_ENABLED, String.valueOf(ass.isLatencyCircuitEnabled()));

        element.setAttribute(POLICY_FAILURE_CIRCUIT_MAX_FAILURES, String.valueOf(ass.getPolicyFailureCircuitMaxFailures()));
        element.setAttribute(POLICY_FAILURE_CIRCUIT_SAMPLING_WINDOW, String.valueOf(ass.getPolicyFailureCircuitSamplingWindow()));
        element.setAttribute(POLICY_FAILURE_CIRCUIT_RECOVERY_PERIOD, String.valueOf(ass.getPolicyFailureCircuitRecoveryPeriod()));

        element.setAttribute(LATENCY_CIRCUIT_MAX_FAILURES, String.valueOf(ass.getLatencyCircuitMaxFailures()));
        element.setAttribute(LATENCY_CIRCUIT_MAX_LATENCY, String.valueOf(ass.getLatencyCircuitMaxLatency()));
        element.setAttribute(LATENCY_CIRCUIT_SAMPLING_WINDOW, String.valueOf(ass.getLatencyCircuitSamplingWindow()));
        element.setAttribute(LATENCY_CIRCUIT_RECOVERY_PERIOD, String.valueOf(ass.getLatencyCircuitRecoveryPeriod()));

        super.populateElement(wspWriter, element, object);
    }

    @Override
    protected void populateObject(CompositeAssertion cass, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        CircuitBreakerAssertion ass = (CircuitBreakerAssertion) cass;

        ass.setPolicyFailureCircuitEnabled(Boolean.valueOf(source.getAttribute(POLICY_FAILURE_CIRCUIT_ENABLED)));
        ass.setLatencyCircuitEnabled(Boolean.valueOf(source.getAttribute(LATENCY_CIRCUIT_ENABLED)));

        ass.setPolicyFailureCircuitMaxFailures(stringToInt(source.getAttribute(POLICY_FAILURE_CIRCUIT_MAX_FAILURES)));
        ass.setPolicyFailureCircuitSamplingWindow(stringToInt(source.getAttribute(POLICY_FAILURE_CIRCUIT_SAMPLING_WINDOW)));
        ass.setPolicyFailureCircuitRecoveryPeriod(stringToInt(source.getAttribute(POLICY_FAILURE_CIRCUIT_RECOVERY_PERIOD)));

        ass.setLatencyCircuitMaxFailures(stringToInt(source.getAttribute(LATENCY_CIRCUIT_MAX_FAILURES)));
        ass.setLatencyCircuitMaxLatency(stringToInt(source.getAttribute(LATENCY_CIRCUIT_MAX_LATENCY)));
        ass.setLatencyCircuitSamplingWindow(stringToInt(source.getAttribute(LATENCY_CIRCUIT_SAMPLING_WINDOW)));
        ass.setLatencyCircuitRecoveryPeriod(stringToInt(source.getAttribute(LATENCY_CIRCUIT_RECOVERY_PERIOD)));

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

    private int stringToInt(String attribute) {
        try {
            return Integer.parseInt(attribute);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}