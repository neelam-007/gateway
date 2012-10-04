package com.l7tech.server.event.wsman;

import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.server.policy.bundle.PreBundleSavePolicyCallback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event used to request the appropriate handler to install a policy bundle.
 */
public class InstallPolicyBundleEvent extends WSManagementRequestEvent {

    public InstallPolicyBundleEvent(final Object source,
                                    final BundleResolver bundleResolver,
                                    final PolicyBundleInstallerContext context,
                                    final PreBundleSavePolicyCallback preBundleSavePolicyCallback) {
        super(source);
        this.bundleResolver = bundleResolver;
        this.context = context;
        this.preBundleSavePolicyCallback = preBundleSavePolicyCallback;
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

    @NotNull
    public BundleResolver getBundleResolver() {
        return bundleResolver;
    }

    @Nullable
    public PreBundleSavePolicyCallback getPreBundleSavePolicyCallback() {
        return preBundleSavePolicyCallback;
    }

    public String getPolicyBundleVersionNs() {
        return policyBundleVersionNs;
    }

    public void setPolicyBundleVersionNs(String policyBundleVersionNs) {
        this.policyBundleVersionNs = policyBundleVersionNs;
    }

    // - PRIVATE

    @NotNull
    final BundleResolver bundleResolver;
    @NotNull
    final PolicyBundleInstallerContext context;
    @Nullable
    final PreBundleSavePolicyCallback preBundleSavePolicyCallback;

    private final static String BUNDLE_VERSION_SEPT_12 = "http://ns.l7tech.com/2012/09/policy-bundle";

    private String policyBundleVersionNs = BUNDLE_VERSION_SEPT_12;

}
