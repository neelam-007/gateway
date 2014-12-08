package com.l7tech.server.event.bundle;

import com.l7tech.identity.UserBean;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerCallback;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PolicyBundleInstallerEvent extends PolicyBundleEvent {

    public PolicyBundleInstallerEvent(@NotNull final Object source,
                                      @NotNull final PolicyBundleInstallerContext context) {
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

    @Nullable
    public PolicyBundleInstallerCallback getPolicyBundleInstallerCallback() {
        return policyBundleInstallerCallback;
    }

    @Nullable
    public UserBean getAuthenticatedUser() {
        return authenticatedUser;
    }

    public void setPolicyBundleInstallerCallback(@Nullable PolicyBundleInstallerCallback policyBundleInstallerCallback) {
        this.policyBundleInstallerCallback = policyBundleInstallerCallback;
    }

    public void setAuthenticatedUser(@Nullable UserBean authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    // - PRIVATE
    @NotNull
    private final PolicyBundleInstallerContext context;
    @Nullable
    private PolicyBundleInstallerCallback policyBundleInstallerCallback;
    @Nullable
    private UserBean authenticatedUser;
}
