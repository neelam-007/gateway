package com.l7tech.server.event.wsman;

import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import org.jetbrains.annotations.NotNull;

public class PolicyBundleEvent extends WSManagementRequestEvent {

    public PolicyBundleEvent(final Object source,
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

    public String getPolicyBundleVersionNs() {
        return policyBundleVersionNs;
    }

    public void setPolicyBundleVersionNs(String policyBundleVersionNs) {
        this.policyBundleVersionNs = policyBundleVersionNs;
    }

    // - PRIVATE

    private final static String BUNDLE_VERSION_SEPT_12 = "http://ns.l7tech.com/2012/09/policy-bundle";
    protected String policyBundleVersionNs = BUNDLE_VERSION_SEPT_12;
    @NotNull
    final PolicyBundleInstallerContext context;

}
