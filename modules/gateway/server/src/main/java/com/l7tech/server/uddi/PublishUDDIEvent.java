/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * UDDIEvent task implementation representing the types of publish actions which are available and
 * which the UDDICoordinator supports
 * @author darmstrong
 */
package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;


class PublishUDDIEvent extends UDDIEvent{
    //- PACKAGE

//    enum Type { CREATE_PROXY /*Publish a service's WSDL to UDDI for the time*/,
//        UPDATE_PROXY /*Update a previously published service WSDL in UDDI*/,
//        ADD_BINDING /*Add a bindingTemplate to an existing BusinessService in UDDI*/,
//        DELETE_BINDING/*Delete a bindingTemplate from a BusinessService*/,
//        OVERWRITE/*Overwrite an entire BusinessService in UDDI with Gateway info*/}

    PublishUDDIEvent(UDDIProxiedServiceInfo.PublishType publishType,
                     final UDDIProxiedServiceInfo uddiProxiedServiceInfo) {
        this.publishType = publishType;
        this.uddiProxiedServiceInfo = uddiProxiedServiceInfo;
    }

    UDDIProxiedServiceInfo.PublishType getPublishType() {
        return publishType;
    }

    UDDIProxiedServiceInfo getUddiProxiedServiceInfo() {
        return uddiProxiedServiceInfo;
    }

    //- PRIVATE

    private final UDDIProxiedServiceInfo.PublishType publishType;
    private final UDDIProxiedServiceInfo uddiProxiedServiceInfo;
}
