package com.ca.apim.gateway.extension.sharedstate.cluster;

import com.ca.apim.gateway.extension.Extension;

import java.util.Collection;

/**
 * Provides information about a Gateway cluster.
 * In general, any methods may throw an unchecked exception.
 */
public interface ClusterInfoService extends Extension {
    Collection<ClusterNodeSharedInfo> getActiveNodes();
}
