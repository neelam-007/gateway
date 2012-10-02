package com.l7tech.server.policy.bundle;

import com.l7tech.policy.bundle.BundleInfo;
import org.w3c.dom.Document;

public interface PreBundleSavePolicyCallback {
    //todo test coverage

    /**
     * Provides an opportunity for the caller to do ad-hoc configuration of the contents of the Layer 7 Policy contents
     * before a Service or Policy is created on the gateway.
     * <p/>
     * Note: the document may have already been updated for built in mapping like for JDBC Connections.
     *
     * @param bundleInfo         the BundleInfo layer 7 policy xml document came from
     * @param resourceType       type of resource: Service or Policy
     * @param writeablePolicyDoc Layer7 Policy Document any changes made to this document will be persisted when the
     *                           entity is saved by the BundleInstaller
     * @throws PreBundleSavePolicyCallback.PolicyUpdateException if the impl encounters an error and wants the installation to halt.
     *
     */
    void prePublishCallback(final BundleInfo bundleInfo, final String resourceType, final Document writeablePolicyDoc) throws PolicyUpdateException;


    public static class PolicyUpdateException extends Exception {
        public PolicyUpdateException(String message) {
            super(message);
        }
    }
}
