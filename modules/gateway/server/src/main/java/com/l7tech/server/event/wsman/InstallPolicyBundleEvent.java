package com.l7tech.server.event.wsman;

import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.server.policy.bundle.PreBundleSavePolicyCallback;
import org.jetbrains.annotations.Nullable;

/**
 * Event used to request the appropriate handler to install a policy bundle.
 */
public class InstallPolicyBundleEvent extends PolicyBundleEvent {

    public InstallPolicyBundleEvent(final Object source,
                                    final BundleResolver bundleResolver,
                                    final PolicyBundleInstallerContext context,
                                    final PreBundleSavePolicyCallback preBundleSavePolicyCallback) {
        super(source, bundleResolver, context);
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
