package com.l7tech.server.event.bundle;

import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.server.policy.bundle.PreBundleSavePolicyCallback;
import org.jetbrains.annotations.Nullable;

/**
 * Event used to request the appropriate handler to install a policy bundle.
 */
public class InstallPolicyBundleEvent extends PolicyBundleInstallerEvent {

    public InstallPolicyBundleEvent(final Object source,
                                    final PolicyBundleInstallerContext context,
                                    final PreBundleSavePolicyCallback preBundleSavePolicyCallback) {
        super(source, context);
        this.preBundleSavePolicyCallback = preBundleSavePolicyCallback;
    }

    @Nullable
    public PreBundleSavePolicyCallback getPreBundleSavePolicyCallback() {
        return preBundleSavePolicyCallback;
    }

    // - PRIVATE

    @Nullable
    final PreBundleSavePolicyCallback preBundleSavePolicyCallback;

}
