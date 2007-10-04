package com.l7tech.cluster;

import javax.security.auth.login.LoginException;

/**
 * Login to a another node in the cluster.
 *
 * <p>The credentials are expected to be present at the transport level.</p>
 *
 * @author $Author$
 * @version $Revision$
 */
public interface ClusterLogin {

    /**
     * Login for remote nodes.
     *
     * @return the context to use.
     * @throws LoginException on login error
     */
    public ClusterContext login() throws LoginException;
}
