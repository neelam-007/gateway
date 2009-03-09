package com.l7tech.server.ems.gateway;

import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.identity.User;

/**
 * @author: ghuang
 * @date: Mar 9, 2009
 */
public class MockGatewayClusterClientManager implements GatewayClusterClientManager {
    public GatewayClusterClient getGatewayClusterClient(String clusterId, User user) throws GatewayException {
        return null;
    }

    public GatewayClusterClient getGatewayClusterClient(SsgCluster cluster, User user) throws GatewayException {
        return null;
    }
}
