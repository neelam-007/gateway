/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * UDDIEvent task implementation representing the types of publish actions which are available and
 * which the UDDICoordinator supports
 * @author darmstrong
 */
package com.l7tech.server.uddi;


class PublishUDDIEvent extends UDDIEvent{
    //- PACKAGE

    enum Type { CREATE_PROXY /*Publish a service's WSDL to UDDI for the time*/,
        UPDATE_PROXY /*Update a previously published service WSDL in UDDI*/,
        ADD_BINDING /*Add a bindingTemplate to an existing BusinessService in UDDI*/,
        OVERWRITE/*Overwrite an entire BusinessService in UDDI with Gateway info*/}

    PublishUDDIEvent(final Type type,
                     final long uddiProxiedServiceInfo) {
        this.type = type;
        this.uddiProxiedServiceInfo = uddiProxiedServiceInfo;
    }

    Type getType() {
        return type;
    }

    long getUddiProxiedServiceInfo() {
        return uddiProxiedServiceInfo;
    }

    //- PRIVATE

    private final Type type;
    private final long uddiProxiedServiceInfo;
}
