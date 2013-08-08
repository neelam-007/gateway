package com.l7tech.server.uddi;

import com.l7tech.objectmodel.Goid;

/**
 * UDDIEvent for ad-hoc polling.
 */
public class PollUDDIEvent extends UDDIEvent {

    //- PUBLIC

    public PollUDDIEvent( final Goid registryGoid,
                          final long startTime,
                          final long endTime ) {
        super( false );
        this.registryGoid = registryGoid;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Goid getRegistryGoid() {
        return registryGoid;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    //- PRIVATE

    private final Goid registryGoid;
    private final long startTime;
    private final long endTime;
}
