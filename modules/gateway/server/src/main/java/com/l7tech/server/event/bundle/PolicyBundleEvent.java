package com.l7tech.server.event.bundle;

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

    private final static String BUNDLE_VERSION_SEPT_12 = "http://ns.l7tech.com/2012/09/policy-bundle";
    protected String policyBundleVersionNs = BUNDLE_VERSION_SEPT_12;
}
