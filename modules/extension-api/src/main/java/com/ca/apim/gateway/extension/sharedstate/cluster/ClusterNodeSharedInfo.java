package com.ca.apim.gateway.extension.sharedstate.cluster;

public interface ClusterNodeSharedInfo {
    String getNodeIdentifier();

    String getName();

    long getLastUpdateTimeStamp();
}
