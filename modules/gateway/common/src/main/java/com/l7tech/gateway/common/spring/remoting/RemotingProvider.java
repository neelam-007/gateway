package com.l7tech.gateway.common.spring.remoting;

import java.security.Principal;

/**
 * The remoting provider interface implemented for the server side of remoting.
 *
 * @author steve
 */
public interface RemotingProvider {

    //TODO [steve] most of this interface is backwards, we should suport checking of facility (from config?) and annotations, not specifics like licensing, cluster vs admin, etc.

    /**
     * Enforce licensing for the given invocation.
     *
     * <p>A runtime exception is thrown if not licensed.</p>
     *
     * @param className The name of the class
     * @param methodName The name of the method
     */
    void enforceLicensed( String className, String methodName );

    /**
     * Enforce administration permission for the given invocation.
     *
     * <p>A runtime exception is thrown if not permitted.</p>
     *
     * @see com.l7tech.gateway.common.spring.remoting.RemoteUtils#getHttpServletRequest()
     */
    void enforceAdminEnabled();

    /**
     * Enforce cluster permission for the given invocation.
     *
     * <p>A runtime exception is thrown if not permitted.</p>
     *
     * @see com.l7tech.gateway.common.spring.remoting.RemoteUtils#getHttpServletRequest()
     */
    void enforceClusterEnabled();

    /**
     * Get the Principal that is associated with the given cookie.
     *
     * <p>If the cookie is not related to any principal then NULL is returned.</p>
     *
     * @param cookie The cookie
     * @return The associated Principal or null.
     */
    Principal getPrincipalForCookie( String cookie );
}
