package com.l7tech.policy.assertion.ext;

/**
 * The abstract class <code>ServiceInvocation</code> is extended by custom
 * policy elements that are loaded in the gateway runtime. It's methods
 * {@link ServiceInvocation#onRequest} and {@link ServiceInvocation#onResponse}
 * are invoked by the gateway during the service request and/or response processing,
 * depending on it's position in the policy tree.
 * <p/>
 * The methods in this class are empty.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class ServiceInvocation {
    /**
     * Create the <code>ServiceInvocation</code> isntance with the custom
     * assertion.
     *
     * @param customAssertion
     */
    public ServiceInvocation(CustomAssertion customAssertion) {
        this.customAssertion = customAssertion;
    }

    /**
     * Invoked before invoking the protected service
     *
     * @param request the service request associated
     * @see ServiceRequest
     */
    public void onRequest(ServiceRequest request) {}

    /**
     * Invoked before invoking the protected service
     *
     * @param response the service response associated
     * @see ServiceResponse
     */
    public void onResponse(ServiceResponse response) {}

    protected CustomAssertion customAssertion;
}