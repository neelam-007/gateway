package com.l7tech.server.cluster;

import com.ca.apim.gateway.extension.sharedstate.cluster.ClusterInfoService;
import com.ca.apim.gateway.extension.sharedstate.cluster.ClusterNodeSharedInfo;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

public class LocalClusterInfoService implements ClusterInfoService {

    private ClusterNodeInfo localNodeInfo;
    public static final String KEY = "local";

    public LocalClusterInfoService() {
        localNodeInfo = new ClusterNodeInfo();
        localNodeInfo.setLastUpdateTimeStamp(System.currentTimeMillis());
        localNodeInfo.setNodeIdentifier(UUID.randomUUID().toString().replace("-", ""));
        localNodeInfo.setName("gateway");
    }

    @Override
    public Collection<ClusterNodeSharedInfo> getActiveNodes() {
        localNodeInfo.setLastUpdateTimeStamp(System.currentTimeMillis());
        return Arrays.asList(new ClusterNodeSharedInfo() {
            @Override
            public String getNodeIdentifier() {
                return localNodeInfo.getNodeIdentifier();
            }

            @Override
            public String getName() {
                return localNodeInfo.getName();
            }

            @Override
            public long getLastUpdateTimeStamp() {
                return localNodeInfo.getLastUpdateTimeStamp();
            }
        });
    }
}
