package com.l7tech.server.policy.bundle;

import com.l7tech.policy.bundle.BundleInfo;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface PreBundleSavePolicyCallback {

    /**
     * Provides an opportunity for the caller to do ad-hoc configuration of the contents of the Layer 7 Policy contents
     * before a Service or Policy is created on the gateway.
     * <p/>
     * Note: the document may have already been updated for built in mapping like for JDBC Connections.
     *
     *
     *
     * @param bundleInfo         the BundleInfo layer 7 policy xml document came from
     * @param entityDetailElmReadOnly    the ServiceDetail or PolicyDetail element for the Gateway Mgmt Api Policy or Service element.
     *                           This provides any neccessary context information regarding the policy being saved e.g.
     *                           the service or policy it belongs to. Any changes made to this element will be ignored.
     * @param writeablePolicyDoc Layer7 Policy Document any changes made to this document will be persisted when the
     *                           entity is saved by the BundleInstaller
     * @throws PreBundleSavePolicyCallback.PolicyUpdateException if the impl encounters an error and wants the installation to halt.
     *
     */
    void prePublishCallback(@NotNull final BundleInfo bundleInfo,
                            @NotNull final Element entityDetailElmReadOnly,
                            @NotNull final Document writeablePolicyDoc) throws PolicyUpdateException;


    public static class PolicyUpdateException extends Exception {
        public PolicyUpdateException(String message) {
            super(message);
        }
    }
}
