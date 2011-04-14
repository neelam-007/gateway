package com.l7tech.server.event.system;

import com.l7tech.gateway.common.Component;

/**
 * Event issued when archiving audit records.
 *
 * @author jbufu
 */
public class AuditArchiverEvent extends SystemEvent implements RoutineSystemEvent {
    //- PUBLIC

    public AuditArchiverEvent( Object source ) {
        super(source, Component.GATEWAY);
    }

    @Override
    public String getAction() {
        return NAME;
    }

    //- PRIVATE

    private static final String NAME = "Audit Archiver";
}
