package com.l7tech.server.event.system;

import com.l7tech.gateway.common.Component;

/**
 * Event issued on service reload (e.g. by ServiceCache)
 *
 * @author $Author$
 */
public class ServiceReloadEvent extends SystemEvent implements RoutineSystemEvent {

    //- PUBLIC

    public ServiceReloadEvent( Object source ) {
        super(source, Component.GATEWAY);
    }

    public String getAction() {
        return NAME;
    }

    //- PRIVATE

    private static final String NAME = "Services reloaded";
}
