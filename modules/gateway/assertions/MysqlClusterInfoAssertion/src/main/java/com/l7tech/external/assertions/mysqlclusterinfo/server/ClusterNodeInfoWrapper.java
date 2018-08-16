package com.l7tech.external.assertions.mysqlclusterinfo.server;

import com.ca.apim.gateway.extension.sharedstate.cluster.ClusterNodeSharedInfo;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;

public class ClusterNodeInfoWrapper implements ClusterNodeSharedInfo {
    private ClusterNodeInfo info;

    public ClusterNodeInfoWrapper(ClusterNodeInfo info) {
        this.info = info;
    }

    @Override
    public String getNodeIdentifier() {
        return info.getNodeIdentifier();
    }

    @Override
    public String getName() {
        return info.getName();
    }

    @Override
    public long getLastUpdateTimeStamp() {
        return info.getLastUpdateTimeStamp();
    }
}
