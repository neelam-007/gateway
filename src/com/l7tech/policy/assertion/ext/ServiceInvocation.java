package com.l7tech.policy.assertion.ext;

/**
 * The abstract class <code>ServiceInvocation</code>  is extended by custom
 * policy elements that are invoked during the policy processing.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class ServiceInvocation {
    /**
     * Create the <code>ServiceInvocation</code> isntance with the custom
     * assertion.
     * @param customAssertion
     */
    public ServiceInvocation(CustomAssertion customAssertion) {
        this.customAssertion = customAssertion;
    }

    public abstract void onRequest(ServiceRequest request);
    public abstract void onResponse(ServiceResponse response);

    protected CustomAssertion customAssertion;
}