package com.l7tech.policy.assertion.ext;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * The abstract class <code>ServiceInvocation</code> is extended by custom
 * policy elements that are loaded in the gateway runtime. It's methods
 * {@link ServiceInvocation#onRequest} and {@link ServiceInvocation#onResponse}
 * are invoked by the gateway during the service request and/or response processing,
 * depending on it's position in the policy tree.
 * Those methods may throw <code>IOException</code> on error during processing the
 * requiest or <code>GeneralSecurityException</code> or its subclass for security
 * related error.
 * <p/>
 * The methods in this class are empty.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class ServiceInvocation {
    /**
     * Create the <code>ServiceInvocation</code> instance
     */
    public ServiceInvocation() {
    }

    /**
     * Associate the service invocation with the custom assertion.
     * The custom assertion framework sets this.
     *
     * @param customAssertion the
     */
    public void setCustomAssertion(CustomAssertion customAssertion) {
        this.customAssertion = customAssertion;
    }

    /**
     * Invoked before invoking the protected service
     *
     * @param request the service request associated
     * @throws IOException              on error processing the request
     * @throws GeneralSecurityException is thrown on security related
     *                                  error - the nature of the failure may be described by subclass
     * @see ServiceRequest
     */
    public void onRequest(ServiceRequest request)
      throws IOException, GeneralSecurityException {}

    /**
     * Invoked before invoking the protected service
     *
     * @param response the service response associated
     * @throws IOException              on error processing the request
     * @throws GeneralSecurityException is thrown on security related
     *                                  error - the nature of the failure may be described by subclass
     * @see ServiceResponse
     */
    public void onResponse(ServiceResponse response)
      throws IOException, GeneralSecurityException {}

    protected CustomAssertion customAssertion;
}