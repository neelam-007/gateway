package com.l7tech.policy.assertion.ext;

import javax.security.auth.Subject;
import java.security.GeneralSecurityException;

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public interface SecurityContext {
    /**
     * Returns a <code>javax.security.auth.Subject</code> object containing the name
     * of the current requestor. Note that this does not imply that it has been
     * Use {@link SecurityContext#isAuthenticated()} to determine the authentication
     * status.
     * <p/>
     * If this is an anonymous request, the method returns null.
     *
     * @return the current principal or <b>null</b> if not available
     */
    Subject getSubject();

    /**
     * Returns <code>true</code> if this request has been authenticated.
     *
     * @return true if authenticated, fale otherwise
     */
    boolean isAuthenticated();

    /**
     * set the security context as authenticated
     *
     * @throws GeneralSecurityException thrown if the current state does not allow
     *                                  method invoking
     */
    void setAuthenticated() throws GeneralSecurityException;
}