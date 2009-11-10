/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * UDDIEvent task implementation representing the types of publish actions which are available and
 * which the UDDICoordinator supports
 * @author darmstrong
 */
package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIPublishStatus;


class PublishUDDIEvent extends UDDIEvent{
    //- PACKAGE

    PublishUDDIEvent(
                     final UDDIProxiedServiceInfo uddiProxiedServiceInfo,
                     final UDDIPublishStatus uddiPublishStatus) {
        this.uddiProxiedServiceInfo = uddiProxiedServiceInfo;
        this.uddiPublishStatus = uddiPublishStatus;
    }

    UDDIProxiedServiceInfo getUddiProxiedServiceInfo() {
        return uddiProxiedServiceInfo;
    }

    public UDDIPublishStatus getUddiPublishStatus() {
        return uddiPublishStatus;
    }

    //- PRIVATE

    private final UDDIProxiedServiceInfo uddiProxiedServiceInfo;
    private final UDDIPublishStatus uddiPublishStatus;
}
