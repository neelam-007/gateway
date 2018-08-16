package com.ca.apim.gateway.extension.sharedstate.cluster;

import com.ca.apim.gateway.extension.Extension;

import java.util.Collection;

public interface ClusterInfoService extends Extension {
    Collection<ClusterNodeSharedInfo> getActiveNodes();
}
