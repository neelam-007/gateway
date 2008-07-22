package com.l7tech.server.event.admin;

import java.util.logging.Level;

/**
 * Event recording the fact that audit signing is turned on or off
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 15, 2007<br/>
 */
public class AuditSigningStatusChange extends AdminEvent {
    public AuditSigningStatusChange(Object source, String onOrOff) {
        super(source, "Audit signing turned " + onOrOff);
    }

    public Level getMinimumLevel() {
        return Level.WARNING;
    }
}
