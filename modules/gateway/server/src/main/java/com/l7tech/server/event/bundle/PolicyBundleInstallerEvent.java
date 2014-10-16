package com.l7tech.server.event.bundle;

import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import org.jetbrains.annotations.NotNull;

public abstract class PolicyBundleInstallerEvent extends PolicyBundleEvent {

    public PolicyBundleInstallerEvent(final Object source,
                                      final PolicyBundleInstallerContext context) {
        super(source);
        this.context = context;
    }

    /**
     * The context may be updated by the processor of this event. Any changes must be seen by the producer of this event.
     *
     * @return context
     */
    @NotNull
    public PolicyBundleInstallerContext getContext() {
        return context;
    }

    // - PRIVATE
    @NotNull
    private final PolicyBundleInstallerContext context;

}
