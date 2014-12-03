package com.l7tech.gateway.common.solutionkit;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

/**
 * Entity header for {@link com.l7tech.gateway.common.solutionkit.SolutionKit} entity.
 */
public class SolutionKitHeader extends EntityHeader {
    private String solutionKitGuid;
    private String solutionKitVersion;

    public SolutionKitHeader(SolutionKit solutionKit) {
        this(solutionKit.getId(),
            solutionKit.getName(),
            solutionKit.getProperty(SolutionKit.SK_PROP_DESC_KEY),
            solutionKit.getVersion(),
            solutionKit.getSolutionKitGuid(),
            solutionKit.getSolutionKitVersion());
    }

    public SolutionKitHeader(String id, String name, String description, Integer version, String solutionKitGuid, String solutionKitVersion) {
        super(id, EntityType.SOLUTION_KIT, name, description, version);
        this.solutionKitGuid = solutionKitGuid;
        this.solutionKitVersion = solutionKitVersion;
    }

    public String getSolutionKitGuid() {
        return solutionKitGuid;
    }

    public void setSolutionKitGuid(String solutionKitGuid) {
        this.solutionKitGuid = solutionKitGuid;
    }

    public String getSolutionKitVersion() {
        return solutionKitVersion;
    }

    public void setSolutionKitVersion(String solutionKitVersion) {
        this.solutionKitVersion = solutionKitVersion;
    }
}