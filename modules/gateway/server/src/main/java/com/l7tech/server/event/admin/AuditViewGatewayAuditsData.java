package com.l7tech.server.event.admin;

import java.util.logging.Level;

/**
 * An info level audit message for viewing gateway's audit data.
 *  
 * User: dlee
 * Date: Jul 16, 2008
 */
public class AuditViewGatewayAuditsData extends AdminEvent {
    
    public AuditViewGatewayAuditsData(Object source) {
        super(source, "View audit data");
    }

    @Override
    public Level getMinimumLevel() {
        return Level.INFO;
    }
}
