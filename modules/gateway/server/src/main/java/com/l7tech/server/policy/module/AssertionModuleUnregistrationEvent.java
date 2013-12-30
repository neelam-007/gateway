package com.l7tech.server.policy.module;

import org.springframework.context.ApplicationEvent;

/**
 * Event fired when an assertion module is unregistered.
 */
public class AssertionModuleUnregistrationEvent extends ApplicationEvent {
    private final BaseAssertionModule module;

    /**
     * Create an assertion module un-registration event.
     *
     * @param source the source of the event; usually not relevant.
     * @param module the module that was just unregistered.  Must not be null.
     */
    public AssertionModuleUnregistrationEvent(Object source, BaseAssertionModule module) {
        super(source);
        if (module == null) throw new IllegalArgumentException("a module must be provided");
        this.module = module;
    }

    /** @return the assertion module that was just unregistered.  Never null. */
    public BaseAssertionModule getModule() {
        return module;
    }
}
