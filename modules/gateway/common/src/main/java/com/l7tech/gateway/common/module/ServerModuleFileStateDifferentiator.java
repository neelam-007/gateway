package com.l7tech.gateway.common.module;

import com.l7tech.util.CollectionUpdate;
import com.l7tech.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;

/**
 * Provides means to differentiate two same {@link ServerModuleFile server modules} having different state.
 * Used in {@link com.l7tech.gateway.common.cluster.ClusterStatusAdmin#getServerModuleFileUpdate(int) getServerModuleFileUpdate(...)}
 * to detect server module state changes. <br/>
 * In addition to {@link ServerModuleFile#equals}, this differentiator also compares module by state.
 */
public class ServerModuleFileStateDifferentiator implements CollectionUpdate.Differentiator<ServerModuleFile> {
    @Override
    public boolean equals(final ServerModuleFile a, final ServerModuleFile b) {
        if (a == b) return true;
        if (a == null || b == null) {
            return false;
        }

        if (!a.equals(b)) {
            return false;
        }

        // check against the states as well
        final Collection<ServerModuleFileState> statesA = Collections.unmodifiableCollection(a.getStates());
        final Collection<ServerModuleFileState> statesB = Collections.unmodifiableCollection(b.getStates());
        return CollectionUtils.containsAll(statesA, statesB) && CollectionUtils.containsAll(statesB, statesA);
    }
}
