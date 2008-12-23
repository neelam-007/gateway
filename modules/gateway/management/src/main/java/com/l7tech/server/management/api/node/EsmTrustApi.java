package com.l7tech.server.management.api.node;

/**
 * API published by a Node for use by the Enterprise Service Manager Server's trust bootstrapping.
 */
public interface EsmTrustApi {
    /**
     * Called on the Node, by the ESM, to assert that the named user wishes to authorize the calling ESM
     * instance to manage this Node on his behalf.
     * <p/>
     * This call must be invoked over an HTTPS connection with a client certificate recognized by this Node
     * as belonging to the specified esmId.
     * <p/>
     * If this call is successful, this Node will establish a mapping from the specified ESM user ID on the specified
     * ESM instance, to the specified internal User.  Future administrative calls from this ESM instance
     * will be accepted if they vouch for the specified esmUserId, and will have all of the access roles of the
     * specified internal user.
     * <p/>
     * This mapping persists until it is explicitly removed or until this Node no longer recognizes
     * the specified ESM instance.
     *
     * @param username     name of a user from the Internal Identity Provider on this Node.  Required.
     * @param userPassword the password for this user.  Required.
     * @param esmId        the ESM ID of the ESM instance that is claiming the right to access this Node as this user.  Required.
     *                     Must match up with the client certificate for the HTTPS connection on which this method invocation arrives.
     * @param esmUserId    this ESM's identifier for this user; opaque to the Node.  The Node will establish a mapping from
     *                     this ESM user ID to the specified username.
     * @throws SecurityException if the mapping cannot be established.
     */
    void grantAccessToEsmAsUser(String username, char[] userPassword, String esmId, String esmUserId) throws SecurityException;
}
