package com.l7tech.server.policy.module;

import org.springframework.context.ApplicationEvent;

/**
 * Application event fired by ServerAssertionRegistry whenever a modular assertion scan pass has completed.
 */
public class AssertionModuleScanCompletedEvent extends ApplicationEvent {

    private final boolean changesMade;

    public AssertionModuleScanCompletedEvent( Object source, boolean changesMade ) {
        super( source );
        this.changesMade = changesMade;
    }

    /**
     * @return true if at least one module was loaded, unloaded, or supplanted as a result of the scan that just completed.
     */
    public boolean isChangesMade() {
        return changesMade;
    }
}
