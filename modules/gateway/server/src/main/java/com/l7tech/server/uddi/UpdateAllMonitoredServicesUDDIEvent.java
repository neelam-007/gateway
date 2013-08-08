package com.l7tech.server.uddi;

import com.l7tech.objectmodel.Goid;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * UDDIEvent for triggering forced update events for all services monitoring a specific registry
 *
 * @author darmstrong
 */
public class UpdateAllMonitoredServicesUDDIEvent extends UDDIEvent{

    //- PACKAGE

    UpdateAllMonitoredServicesUDDIEvent(Goid registryGoid) {
        this.registryGoid = registryGoid;
    }

    Goid getRegistryGoid() {
        return registryGoid;
    }

    //- PRIVATE

    final private Goid registryGoid;
}
