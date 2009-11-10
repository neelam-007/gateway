package com.l7tech.server.uddi;

/**
 * UDDIEvent for subscription notifications.
 */
public class NotificationUDDIEvent extends UDDIEvent {

    //- PUBLIC

    public NotificationUDDIEvent( final long serviceOid,
                                  final String message,
                                  final String remoteAddress ) {
        super(false);
        this.serviceOid = serviceOid;
        this.message = message;
        this.remoteAddress = remoteAddress;
    }

    public long getServiceOid() {
        return serviceOid;
    }

    public String getMessage() {
        return message;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    //- PRIVATE

    private final long serviceOid;
    private final String message;
    private final String remoteAddress;

}
