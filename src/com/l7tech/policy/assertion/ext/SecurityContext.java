package com.l7tech.policy.assertion.ext;

import java.security.Principal;

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public interface SecurityContext {
    /**
     * Returns a <code>java.security.Principal</code> object containing the name
     * of the current requestor. Note that this does not imply that the principal
     * has been authenticated. Use {@link SecurityContext#isAuthenticated()} to
     * determine the authentication status.
     *
     * If this is an anonymous request, the method returns null.
     *
     * @return the current principal or <b>null</b> if not available
     */
    Principal getUserPrincipal();

    /**
     * Returns <code>true</code> if this request has been authenticated.
     *
     * @return true if authenticated, fale otherwise
     */
    boolean isAuthenticated();
}