package com.l7tech.objectmodel;

import org.jetbrains.annotations.NotNull;

/**
 * EntityHeader for ServiceUsage where the 'id' is the composite key of serviceGoid-nodeId.
 */
public class ServiceUsageHeader extends EntityHeader {
    private final Goid serviceGoid;
    private final String nodeId;

    public ServiceUsageHeader(@NotNull final Goid serviceGoid, @NotNull final String nodeId) {
        super(serviceGoid.toHexString() + "-" + nodeId, EntityType.SERVICE_USAGE, null, null);
        this.serviceGoid = serviceGoid;
        this.nodeId = nodeId;
    }

    @NotNull
    public Goid getServiceGoid() {
        return serviceGoid;
    }

    @NotNull
    public String getNodeId() {
        return nodeId;
    }
}
