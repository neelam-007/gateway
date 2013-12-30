package com.l7tech.external.assertions.simplepolicybundleinstaller;

import com.l7tech.gateway.common.admin.PolicyBundleInstallerAdmin;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerAdminAbstractImpl;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Simple Policy Bundle Installer Admin implementation, which is exposed via an admin extension interface.
 */
public class SimplePolicyBundleInstallerAdminImpl extends PolicyBundleInstallerAdminAbstractImpl implements PolicyBundleInstallerAdmin {

    public SimplePolicyBundleInstallerAdminImpl(final String bundleBaseName, final String bundleInfoFileName, final String namespaceInstallerVersion, final ApplicationEventPublisher appEventPublisher) throws PolicyBundleInstallerException {
        super(bundleBaseName, bundleInfoFileName, namespaceInstallerVersion, appEventPublisher);
    }

    @NotNull
    @Override
    protected String getInstallerName() {
        return "Simple Policy Bundle";
    }
}
