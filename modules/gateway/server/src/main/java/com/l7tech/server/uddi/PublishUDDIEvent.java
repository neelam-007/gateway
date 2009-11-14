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
import com.l7tech.gateway.common.uddi.UDDIServiceControl;


class PublishUDDIEvent extends UDDIEvent{
    //- PACKAGE

    PublishUDDIEvent(final UDDIProxiedServiceInfo uddiProxiedServiceInfo,
                     final UDDIPublishStatus uddiPublishStatus,
                     final UDDIServiceControl serviceControl) {
        this.uddiProxiedServiceInfo = uddiProxiedServiceInfo;
        this.uddiPublishStatus = uddiPublishStatus;
        this.serviceControl = serviceControl;
    }

    UDDIProxiedServiceInfo getUddiProxiedServiceInfo() {
        return uddiProxiedServiceInfo;
    }

    UDDIPublishStatus getUddiPublishStatus() {
        return uddiPublishStatus;
    }

    UDDIServiceControl getServiceControl() {
        return serviceControl;
    }

    //- PRIVATE

    private final UDDIProxiedServiceInfo uddiProxiedServiceInfo;
    private final UDDIPublishStatus uddiPublishStatus;
    private final UDDIServiceControl serviceControl;
}
