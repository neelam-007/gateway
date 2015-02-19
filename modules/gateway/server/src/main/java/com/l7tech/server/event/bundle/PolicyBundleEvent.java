package com.l7tech.server.event.bundle;

import com.l7tech.server.policy.bundle.BundleUtils;

public abstract class PolicyBundleEvent extends GatewayManagementRequestEvent {
    public PolicyBundleEvent(final Object source) {
        super(source);
    }

    public String getPolicyBundleVersionNs() {
        return policyBundleVersionNs;
    }

    public void setPolicyBundleVersionNs(String policyBundleVersionNs) {
        this.policyBundleVersionNs = policyBundleVersionNs;
    }

    // - PRIVATE

    protected String policyBundleVersionNs = BundleUtils.NS_BUNDLE;
}
