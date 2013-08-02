package com.l7tech.server.uddi;

import com.l7tech.objectmodel.Goid;

/**
 * UDDIEvent for subscription notifications.
 */
public class NotificationUDDIEvent extends UDDIEvent {

    //- PUBLIC

    public NotificationUDDIEvent( final Goid serviceGoid,
                                  final String message,
                                  final String remoteAddress ) {
        super(false);
        this.serviceGoid = serviceGoid;
        this.message = message;
        this.remoteAddress = remoteAddress;
    }

    public Goid getServiceGoid() {
        return serviceGoid;
    }

    public String getMessage() {
        return message;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    //- PRIVATE

    private final Goid serviceGoid;
    private final String message;
    private final String remoteAddress;

}
