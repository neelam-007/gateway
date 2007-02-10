package com.l7tech.server.policy;

import org.springframework.context.ApplicationEvent;

/**
 * Event fired when an assertion module is unregistered.
 */
public class AssertionModuleUnregistrationEvent extends ApplicationEvent {
    private final AssertionModule module;

    /**
     * Create an assertio module unregistration event.
     *
     * @param source the source of the event; usually not relevant.
     * @param module the module that was just unregistered.  Must not be null.
     */
    public AssertionModuleUnregistrationEvent(Object source, AssertionModule module) {
        super(source);
        if (module == null) throw new IllegalArgumentException("a module must be provided");
        this.module = module;
    }

    /** @return the assertion module that was just unregistered.  Never null. */
    public AssertionModule getModule() {
        return module;
    }
}
