package com.l7tech.server.event.system;

import com.l7tech.gateway.common.Component;

/**
 * System audit event for UDDI.
 */
public class UDDISystemEvent extends SystemEvent implements RoutineSystemEvent {

    //- PUBLIC

    public UDDISystemEvent( final Object source,
                            final Component component ) {
        super( source, component );
    }

    @Override
    public String getAction() {
        return action;
    }

    //- PRIVATE

    private final String action = "Registry Update";
}
