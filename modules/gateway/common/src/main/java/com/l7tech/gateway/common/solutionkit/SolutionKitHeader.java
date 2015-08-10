package com.l7tech.gateway.common.solutionkit;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Entity header for {@link com.l7tech.gateway.common.solutionkit.SolutionKit} entity.
 */
public class SolutionKitHeader extends EntityHeader {
    private final String solutionKitGuid;
    private final String solutionKitVersion;
    private final String instanceModifier;
    private final long lastUpdateTime;
    private final Goid parentGoid;

    public SolutionKitHeader(SolutionKit solutionKit) {
        this(solutionKit.getId(),
            solutionKit.getName(),
            solutionKit.getProperty(SolutionKit.SK_PROP_DESC_KEY),
            solutionKit.getVersion(),
            solutionKit.getSolutionKitGuid(),
            solutionKit.getParentGoid(),
            solutionKit.getSolutionKitVersion(),
            solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY),
            solutionKit.getLastUpdateTime());
    }

    public SolutionKitHeader(String id, String name, String description, Integer version, String solutionKitGuid, Goid parentGoid, String solutionKitVersion, String instanceModifier, long lastUpdateTime) {
        super(id, EntityType.SOLUTION_KIT, name, description, version);
        this.solutionKitGuid = solutionKitGuid;
        this.parentGoid = parentGoid;
        this.solutionKitVersion = solutionKitVersion;
        this.instanceModifier = instanceModifier;
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getSolutionKitGuid() {
        return solutionKitGuid;
    }

    public Goid getParentGoid() {
        return parentGoid;
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

    @Override
    public int compareTo(@NotNull Object o) {
        final SolutionKitHeader other = (SolutionKitHeader)o;

        // Compare name
        final int compareName = String.CASE_INSENSITIVE_ORDER.compare(getName(), other.getName());
        if (compareName != 0) {
            return compareName;
        }
        // Compare sk_version
        else {
            final int compareSKVersion = String.CASE_INSENSITIVE_ORDER.compare(getSolutionKitVersion(), other.getSolutionKitVersion());
            if (compareSKVersion != 0) {
                return compareSKVersion;
            }
            // Compare instance modifier
            else {
                // Instance modifier could be null, so reassign it as an empty string before passing it into the compare method.
                String instanceModifier = getInstanceModifier();
                if (instanceModifier == null) instanceModifier = "";

                String otherIM = other.getInstanceModifier();
                if (otherIM == null) otherIM = "";

                final int compareInstanceModifier = String.CASE_INSENSITIVE_ORDER.compare(instanceModifier, otherIM);
                if (compareInstanceModifier != 0) {
                    return compareInstanceModifier;
                }
                // Compare description
                else {
                    final int compareDescription = String.CASE_INSENSITIVE_ORDER.compare(getDescription(), other.getDescription());
                    if (compareDescription != 0) {
                        return compareDescription;
                    }
                    // Compare update time
                    else {
                        return (int) (getLastUpdateTime() - other.getLastUpdateTime());
                    }
                }
            }
        }
    }
}