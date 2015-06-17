package com.l7tech.gateway.common.solutionkit;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

/**
 * Entity header for {@link com.l7tech.gateway.common.solutionkit.SolutionKit} entity.
 */
public class SolutionKitHeader extends EntityHeader {
    private final String solutionKitGuid;
    private final String solutionKitVersion;
    private final String instanceModifier;
    private final long lastUpdateTime;

    public SolutionKitHeader(SolutionKit solutionKit) {
        this(solutionKit.getId(),
            solutionKit.getName(),
            solutionKit.getProperty(SolutionKit.SK_PROP_DESC_KEY),
            solutionKit.getVersion(),
            solutionKit.getSolutionKitGuid(),
            solutionKit.getSolutionKitVersion(),
            solutionKit.getInstanceModifier(),
            solutionKit.getLastUpdateTime());
    }

    public SolutionKitHeader(String id, String name, String description, Integer version, String solutionKitGuid, String solutionKitVersion, String instanceModifier, long lastUpdateTime) {
        super(id, EntityType.SOLUTION_KIT, name, description, version);
        this.solutionKitGuid = solutionKitGuid;
        this.solutionKitVersion = solutionKitVersion;
        this.instanceModifier = instanceModifier;
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getSolutionKitGuid() {
        return solutionKitGuid;
    }

    public String getSolutionKitVersion() {
        return solutionKitVersion;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public String getInstanceModifier() {
        return instanceModifier;
    }
}