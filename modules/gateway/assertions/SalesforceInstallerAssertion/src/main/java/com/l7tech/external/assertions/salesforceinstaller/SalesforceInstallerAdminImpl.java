package com.l7tech.external.assertions.salesforceinstaller;

import com.l7tech.gateway.common.admin.PolicyBundleInstallerAdmin;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerAdminAbstractImpl;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Salesforce Installer Admin implementation, which is exposed via an admin extension interface.
 */
public class SalesforceInstallerAdminImpl extends PolicyBundleInstallerAdminAbstractImpl implements PolicyBundleInstallerAdmin {

    public SalesforceInstallerAdminImpl (final String bundleBaseName, final String bundleInfoFileName, final String namespaceInstallerVersion, final ApplicationEventPublisher appEventPublisher) throws PolicyBundleInstallerException {
        super(bundleBaseName, bundleInfoFileName, namespaceInstallerVersion, appEventPublisher);
    }

    @NotNull
    @Override
    protected String getInstallerName() {
        return "Execute Salesforce Operation Assertion";
    }
}