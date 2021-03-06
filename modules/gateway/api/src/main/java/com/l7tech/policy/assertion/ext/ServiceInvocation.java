package com.l7tech.policy.assertion.ext;

import com.l7tech.policy.assertion.ext.message.CustomPolicyContext;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Superclass for the server-side runtime code that implements the behaviour for a custom assertion.
 * <p>
 * Every custom assertion has two major parts: the CustomAssertion bean used to hold its policy configuration,
 * and the {@link ServiceInvocation} instance that performs the actual runtime work inside the SecureSpan Gateway.
 * </p><p>
 * The abstract class <code>ServiceInvocation</code> is extended by custom
 * policy elements that are loaded in the Gateway runtime.  Its methods
 * {@link ServiceInvocation#onRequest} and {@link ServiceInvocation#onResponse}
 * are invoked by the gateway during the service request and/or response processing,
 * depending on it's position in the policy tree.
 * </p><p>
 * Those methods may throw <code>IOException</code> on error during processing the
 * request or <code>GeneralSecurityException</code> or its subclass for security
 * related errors.
 * </p><p>
 * The auditing methods can be used to record related to processing of a request or response.
 * </p>
 */
public abstract class ServiceInvocation {

    //- PUBLIC

    /**
     * Create the <code>ServiceInvocation</code> instance
     */
    public ServiceInvocation() {
    }

    /**
     * Associate the service invocation with the custom assertion.
     * The custom assertion framework sets this.
     *
     * @param customAssertion the {@link CustomAssertion} bean holding the configuration for this assertion instance.  Must not be null.
     * @throws NullPointerException if customAssertion is null
     */
    public void setCustomAssertion(CustomAssertion customAssertion) {
        if (customAssertion == null) throw new NullPointerException("customAssertion must not be null");
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
     *
     * @deprecated replaced by checkRequest(...), when overridden and implemented to return a non null status.
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
     *
     * @deprecated replaced by checkRequest(...), when overridden and implemented to return a non null status.
     */
    public void onResponse(ServiceResponse response)
      throws IOException, GeneralSecurityException {}

    /**
     * Override and implement this method for SSG server-side processing of the input data.
     * Use the MessageTargetable interface to configure assertion to target request, response or other message-typed variable.
     *
     * This method replaces onRequest(...) and onResponse(...), which are obsolete and have been deprecated.
     *
     * For backwards compatibility, the default request and response are now located into the context map.
     * You can access them using the following keys; (request and response)
     *
     * @param customPolicyContext    the policy enforcement context
     * @return result status from processing the Custom Assertion
     *
     * @see #onRequest(ServiceRequest)
     * @see #onResponse(ServiceResponse)
     */
    public CustomAssertionStatus checkRequest(final CustomPolicyContext customPolicyContext) {
        try {
            final ServiceRequest request = (ServiceRequest)customPolicyContext.getContext().get("request");
            final ServiceResponse response = (ServiceResponse)customPolicyContext.getContext().get("response");

            if (customPolicyContext.isPostRouting() && response != null) {
                //noinspection deprecation
                onResponse(response);
            } else {
                //noinspection deprecation
                onRequest(request);
            }
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return CustomAssertionStatus.NONE;
    }

    /**
     * Indicate whether the SSG will go through all message parts and load them in memory,
     * so that they will be available in the context-map as key <code>messageParts</code>.
     * <p/>
     * <b>Override</b> this method and return <b>false</b> in order to avoid unnecessary memory usage.
     * Use {@link com.l7tech.policy.assertion.ext.message.CustomMessage#getKnob(Class) CustomMessage.getKnob(Class)},
     * with {@link com.l7tech.policy.assertion.ext.message.knob.CustomPartsKnob CustomPartsKnob}.class as parameter
     * to access the message parts when needed.
     * <p/>
     * Note that, this functionality is kept for backwards compatibility. Its <b>strongly</b> advised
     * to override this method and return false, in your <code>ServiceInvocation</code> implementation,
     * in order to avoid unnecessary memory usage and potential SSG crashes, due to memory starvation.
     *
     * @return true if the SSG should load all message parts into memory, false otherwise.
     *
     * @see com.l7tech.policy.assertion.ext.message.CustomMessage#getKnob(Class)
     * @see com.l7tech.policy.assertion.ext.message.knob.CustomPartsKnob
     * @see com.l7tech.policy.assertion.ext.message.CustomPolicyContext#getContext()
     */
    public boolean loadsMessagePartsIntoMemory() {
        return true;
    }

    //- PROTECTED

    /**
     * Audit an information message.
     *
     * <p>This is for recording occurrences that effect the outcome of
     * this assertions processing.</p>
     *
     * @param message The text of the audit
     */
    protected void auditInfo(String message) {
        if (auditor != null) auditor.auditInfo(customAssertion, message);
    }

    /**
     * Audit a warning message.
     *
     * <p>This is for recording occurrences that negatively effect the outcome
     * of this assertions processing. This would typically be used to record
     * the reason for any failure of this assertion.</p>
     *
     * @param message The text of the audit
     */
    protected void auditWarn(String message) {
        if (auditor != null) auditor.auditWarning(customAssertion, message);
    }

    protected CustomAssertion customAssertion;

    //- PACKAGE

    void setCustomAuditor(CustomAuditor auditor)  {
        this.auditor = auditor;
    }

    //- PRIVATE

    private CustomAuditor auditor;
}