package com.l7tech.server.uddi;

/**
 * UDDIEvent for ad-hoc polling.
 */
public class PollUDDIEvent extends UDDIEvent {

    //- PUBLIC

    public PollUDDIEvent( final long registryOid,
                          final long startTime,
                          final long endTime ) {
        super( false );
        this.registryOid = registryOid;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public long getRegistryOid() {
        return registryOid;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    //- PRIVATE

    private final long registryOid;
    private final long startTime;
    private final long endTime;
}
