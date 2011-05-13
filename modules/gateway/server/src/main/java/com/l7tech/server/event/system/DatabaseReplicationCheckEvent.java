package com.l7tech.server.event.system;

import com.l7tech.gateway.common.Component;

/**
 * Routine system event for database replication delay checking
 */
public class DatabaseReplicationCheckEvent extends SystemEvent implements RoutineSystemEvent {

    public DatabaseReplicationCheckEvent( final Object source, final Component component ) {
        super( source, component );
    }

    @Override
    public String getAction() {
        return "Database Replication Check";
    }
}
