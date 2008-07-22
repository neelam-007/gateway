package com.l7tech.server.event.system;

import com.l7tech.gateway.common.Component;

/**
 * Event issued on policy reload (e.g. by PolicyCache)
 *
 * @author $Author$
 */
public class PolicyReloadEvent extends SystemEvent implements RoutineSystemEvent {

    //- PUBLIC

    public PolicyReloadEvent( Object source ) {
        super(source, Component.GATEWAY);
    }

    public String getAction() {
        return NAME;
    }

    //- PRIVATE

    private static final String NAME = "Policies reloaded";
}