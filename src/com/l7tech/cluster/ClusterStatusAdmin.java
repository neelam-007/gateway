package com.l7tech.cluster;

/**
 * Admin interface for the cluster status panel of the SSM
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Dec 22, 2003<br/>
 * $Id$<br/>
 *
 */
public interface ClusterStatusAdmin {
    ClusterInfo[] getClusterStatus();
    ServiceUsage[] getServiceUsage();
}
