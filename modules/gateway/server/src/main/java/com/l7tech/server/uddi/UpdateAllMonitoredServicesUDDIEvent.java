package com.l7tech.server.uddi;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * UDDIEvent for triggering forced update events for all services monitoring a specific registry
 *
 * @author darmstrong
 */
public class UpdateAllMonitoredServicesUDDIEvent extends UDDIEvent{

    //- PACKAGE

    UpdateAllMonitoredServicesUDDIEvent(long registryOid) {
        this.registryOid = registryOid;
    }

    long getRegistryOid() {
        return registryOid;
    }

    //- PRIVATE

    final private long registryOid;
}
