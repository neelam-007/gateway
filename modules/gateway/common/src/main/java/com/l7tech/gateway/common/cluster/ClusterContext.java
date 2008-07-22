package com.l7tech.gateway.common.cluster;

import com.l7tech.gateway.common.logging.GenericLogAdmin;

/**
 * Context used to access remote objects from other nodes in the cluster.
 *
 * @author $Author$
 * @version $Revision$
 */
public interface ClusterContext {

    public GenericLogAdmin getLogAdmin() throws SecurityException;

}
