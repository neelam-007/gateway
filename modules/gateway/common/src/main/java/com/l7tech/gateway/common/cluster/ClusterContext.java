package com.l7tech.gateway.common.cluster;

import com.l7tech.gateway.common.log.LogAccessAdmin;

/**
 * Context used to access remote objects from other nodes in the cluster.
 */
public interface ClusterContext {

    LogAccessAdmin getLogAccessAdmin() throws SecurityException;
}
