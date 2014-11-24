package com.l7tech.server.policy.bundle;

import com.l7tech.policy.bundle.BundleInfo;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides an opportunity for the caller to configure the contents of the Layer 7 installer bundle during supported
 * event in the bundle install lifecycle on the Gateway.  The event supported may grow over time.
 */
public abstract class PolicyBundleInstallerCallback {

    /**
     * Wsman only.  Provides an opportunity for the caller to do ad-hoc configuration of the contents of the Layer 7
     * Policy contents before a Service or Policy is created on the gateway (wsman).
     * <p/>
     * Note: the document may have already been updated for built in mapping like for JDBC Connections.
     *
     *
     *
     * @param bundleInfo         the BundleInfo layer 7 policy xml document came from
     * @param entityDetailElmReadOnly    the ServiceDetail or PolicyDetail element for the Gateway Mgmt Api Policy or Service element.
     *                           This provides any necessary context information regarding the policy being saved e.g.
     *                           the service or policy it belongs to. Any changes made to this element will be ignored.
     * @param writeablePolicyDoc Layer7 Policy Document any changes made to this document will be persisted when the
     *                           entity is saved by the BundleInstaller
     * @throws CallbackException if the impl encounters an error and wants the installation to halt.
     *
     */
    public void prePolicySave(@NotNull final BundleInfo bundleInfo,
                              @NotNull final Element entityDetailElmReadOnly,
                              @NotNull final Document writeablePolicyDoc) throws CallbackException {
        // do nothing by default
    }


    /**
     * Restman only.  Provides a callback opportunity to configure the contents of the Layer7 Migration Bundle Document before it's sent to Restman.
     *
     * @param bundleInfo the BundleInfo layer 7 migration bundle xml document came from
     * @param restmanMessageDocument Layer7 Restman Migration Bundle Document, any changes made to this document will be
     *                       persisted when the entities are imported via restman
     * @throws CallbackException if the implementation encounters an error and wants the installation to halt
     */
    public void preMigrationBundleImport(@NotNull final BundleInfo bundleInfo,
                                         @NotNull final Document restmanMessageDocument) throws CallbackException {
        // do nothing by default
    }

    public static class CallbackException extends Exception {
        public CallbackException(String message) {
            super(message);
        }
        public CallbackException(Throwable t) {
            super(t);
        }
    }
}
