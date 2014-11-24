package com.l7tech.server.event.bundle;

import com.l7tech.server.policy.bundle.PolicyBundleInstallerCallback;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PolicyBundleInstallerEvent extends PolicyBundleEvent {

    public PolicyBundleInstallerEvent(@NotNull final Object source,
                                      @NotNull final PolicyBundleInstallerContext context,
                                      @Nullable final PolicyBundleInstallerCallback policyBundleInstallerCallback) {
        super(source);
        this.context = context;
        this.policyBundleInstallerCallback = policyBundleInstallerCallback;
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

    @Nullable
    public PolicyBundleInstallerCallback getPolicyBundleInstallerCallback() {
        return policyBundleInstallerCallback;
    }

    // - PRIVATE
    @NotNull
    private final PolicyBundleInstallerContext context;
    @Nullable
    private final PolicyBundleInstallerCallback policyBundleInstallerCallback;
}
