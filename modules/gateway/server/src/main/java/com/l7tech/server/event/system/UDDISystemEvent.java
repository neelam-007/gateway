package com.l7tech.server.event.system;

import com.l7tech.gateway.common.Component;

/**
 * System audit event for UDDI.
 */
public class UDDISystemEvent extends SystemEvent implements RoutineSystemEvent {

    //- PUBLIC
    public enum Action {
        REGISTRY_UPDATE("Registry Update"), CHECKING_ENDPOINTS("Checking Endpoints");

        Action(String action) {
            this.action = action;
        }

        @Override
        public String toString() {
            return action.toString();
        }

        //- PRIVATE
        private final String action;
    }

    public UDDISystemEvent( final Object source,
                            final Component component,
                            final Action action) {
        super( source, component );
        this.action = action;
    }


    @Override
    public String getAction() {
        return action.toString();
    }

    //- PRIVATE

    private final Action action;
}
