package com.l7tech.server.event.bundle;

import com.l7tech.server.policy.bundle.PolicyBundleInstallerCallback;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event used to request the appropriate handler to install a policy bundle.
 */
public class InstallPolicyBundleEvent extends PolicyBundleInstallerEvent {

    public InstallPolicyBundleEvent(@NotNull final Object source,
                                    @NotNull final PolicyBundleInstallerContext context,
                                    @Nullable final PolicyBundleInstallerCallback policyBundleInstallerCallback) {
        super(source, context, policyBundleInstallerCallback);
    }
}
