package com.l7tech.server.policy;

import org.springframework.context.ApplicationEvent;

/**
 * Event fired when an assertion module is registered.
 */
public class AssertionModuleRegistrationEvent extends ApplicationEvent {
    private final AssertionModule module;

    /**
     * Create an assertion module registration event.
     *
     * @param source  the event source; usually not relevant.
     * @param module  the module that was just registered.
     */
    public AssertionModuleRegistrationEvent(Object source, AssertionModule module) {
        super(source);
        if (module == null) throw new IllegalArgumentException("module must be provided");
        this.module = module;
    }

    /** @return the assertion module that was just unregistered.  Never null. */
    public AssertionModule getModule() {
        return module;
    }
}
