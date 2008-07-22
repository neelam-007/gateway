package com.l7tech.policy.assertion.ext;

import javax.security.auth.Subject;
import java.security.GeneralSecurityException;

/**
 * The security context is available to custom assertions through ServiceResponse.getSecurityContext()
 * and ServiceRequest.getSecurityContext().
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public interface SecurityContext {
    /**
     * Returns a <code>javax.security.auth.Subject</code> object containing the name
     * of the current requestor. Note that this does not imply that it has been authenticated.
     * Use {@link SecurityContext#isAuthenticated()} to determine the authentication
     * status.
     * <p/>
     * If this is an anonymous request, the method returns null.
     *
     * @return the current principal or <b>null</b> if not available
     */
    Subject getSubject();

    /**
     * Whether or not this request has been authenticated.
     *
     * @return true if authenticated, false otherwise
     */
    boolean isAuthenticated();

    /**
     * Set the security context as authenticated
     *
     * @throws GeneralSecurityException if this is being invoked through ServiceResponse (only makes sense in ServiceRequest)
     */
    void setAuthenticated() throws GeneralSecurityException;
}