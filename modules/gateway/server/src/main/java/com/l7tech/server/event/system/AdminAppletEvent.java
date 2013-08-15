package com.l7tech.server.event.system;

import com.l7tech.gateway.common.Component;
import com.l7tech.objectmodel.Goid;

import java.util.logging.Level;

/**
 * Event fired when there is an access to the admin applet HTML page or one of its downloadable components.
 */
public class AdminAppletEvent extends SystemEvent {

    /**
     * Create an event representing a request accessing the admin applet or a component of same.
     *
     * @param source   object that is emitting this event
     * @param level    log level of this event
     * @param ip       ip making the request to access the admin applet (or component of same)
     * @param message  message to use for the audit record
     * @param identityProviderOid   identity provider OID; set to -1 if user was not authenticated
     * @param userName              admin username, or null if user does not have one or was not authenticated
     * @param userId                admin unique user identifier, or null if user was not authenticated
     */
    public AdminAppletEvent(Object source,
                            Level level,
                            String ip,
                            String message,
                            Goid identityProviderOid,
                            String userName,
                            String userId)
    {
        super(source, Component.GW_ADMINAPPLET, ip, level, message, identityProviderOid, userName, userId);
    }

    public String getAction() {
        return NAME;
    }

    private static final String NAME = "Admin Applet Request";
}
