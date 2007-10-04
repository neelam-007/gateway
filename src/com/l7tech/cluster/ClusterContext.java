package com.l7tech.cluster;

import com.l7tech.logging.GenericLogAdmin;

/**
 * Context used to access remote objects from other nodes in the cluster.
 *
 * @author $Author$
 * @version $Revision$
 */
public interface ClusterContext {

    public GenericLogAdmin getLogAdmin() throws SecurityException;

}
