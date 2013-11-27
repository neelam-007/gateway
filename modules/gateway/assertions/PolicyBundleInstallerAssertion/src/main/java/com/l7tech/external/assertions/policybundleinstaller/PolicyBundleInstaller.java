package com.l7tech.external.assertions.policybundleinstaller;

import com.l7tech.external.assertions.policybundleinstaller.installer.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.event.wsman.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.server.policy.bundle.PreBundleSavePolicyCallback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.AccessDeniedManagementResponse;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.UnexpectedManagementResponse;
import static com.l7tech.util.Functions.Nullary;

public class PolicyBundleInstaller {

    public static class InstallationException extends Exception {
        public InstallationException(String message) {
            super(message);
        }

        public InstallationException(String message, Throwable cause) {
            super(message, cause);
        }

        public InstallationException(Throwable cause) {
            super(cause);
        }
    }

    private static final Logger logger = Logger.getLogger(PolicyBundleInstaller.class.getName());

    @NotNull
    private final PolicyBundleInstallerContext context;

    private final FolderInstaller folderInstaller;
    private final ServiceInstaller serviceInstaller;
    private final PolicyInstaller policyInstaller;
    private final TrustedCertificateInstaller trustedCertificateInstaller;
    private final EncapsulatedAssertionInstaller encapsulatedAssertionInstaller;
    private final JdbcConnectionInstaller jdbcConnectionInstaller;
    private final AssertionInstaller assertionInstaller;

    public PolicyBundleInstaller(@NotNull final GatewayManagementInvoker gatewayManagementInvoker,
                                 @NotNull final PolicyBundleInstallerContext context,
                                 @NotNull final Nullary<Boolean> cancelledCallback) {
        this.context = context;

        folderInstaller = new FolderInstaller(context, cancelledCallback, gatewayManagementInvoker);
        policyInstaller = new PolicyInstaller(context, cancelledCallback, gatewayManagementInvoker);
        encapsulatedAssertionInstaller = new EncapsulatedAssertionInstaller(context, cancelledCallback, gatewayManagementInvoker);
        serviceInstaller = new ServiceInstaller(context, cancelledCallback, gatewayManagementInvoker);
        trustedCertificateInstaller = new TrustedCertificateInstaller(context, cancelledCallback, gatewayManagementInvoker);
        jdbcConnectionInstaller = new JdbcConnectionInstaller(context, cancelledCallback, gatewayManagementInvoker);
        assertionInstaller = new AssertionInstaller(context, cancelledCallback, gatewayManagementInvoker);
    }

    public void setSavePolicyCallback(@Nullable PreBundleSavePolicyCallback savePolicyCallback) {
        policyInstaller.setSavePolicyCallback(savePolicyCallback);
    }

    /**
     * Dry run the installation looking for conflicts. Any conflicts found are updated in the dry run event.
     *
     * @param dryRunEvent event used to capture any conflicts.
     *
     * @throws com.l7tech.server.policy.bundle.BundleResolver.BundleResolverException
     * @throws com.l7tech.server.policy.bundle.BundleResolver.UnknownBundleException
     * @throws com.l7tech.server.policy.bundle.BundleResolver.InvalidBundleException
     * @throws InterruptedException
     * @throws com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.AccessDeniedManagementResponse
     */
    public void dryRunInstallBundle(@NotNull final DryRunInstallPolicyBundleEvent dryRunEvent)
            throws BundleResolver.BundleResolverException,
            BundleResolver.UnknownBundleException,
            BundleResolver.InvalidBundleException,
            InterruptedException,
            AccessDeniedManagementResponse {

        final Map<String, String> conflictingPolicyIdsNames = new HashMap<>();
        final Map<String, String> policyIdsNames =  policyInstaller.dryRunInstall(dryRunEvent, conflictingPolicyIdsNames);
        encapsulatedAssertionInstaller.dryRunInstall(dryRunEvent, policyIdsNames, conflictingPolicyIdsNames);
        serviceInstaller.dryRunInstall(dryRunEvent);
        trustedCertificateInstaller.dryRunInstall(dryRunEvent);
        jdbcConnectionInstaller.dryRunInstall(dryRunEvent);
        assertionInstaller.dryRunInstall(dryRunEvent);
    }

    /**
     * Install the contents of the configured bundle
     */
    public void installBundle()
            throws InstallationException,
            BundleResolver.UnknownBundleException,
            BundleResolver.BundleResolverException,
            InterruptedException,
            BundleResolver.InvalidBundleException,
            IOException,
            UnexpectedManagementResponse,
            AccessDeniedManagementResponse {

        logger.info("Installing bundle: " + context.getBundleInfo().getId());

        final Map<String, Goid> oldToNewFolderIds = folderInstaller.install();
        final Map<String, String> oldToNewPolicyIds = new HashMap<>();
        final Map<String, String> oldToNewPolicyGuids = policyInstaller.install(oldToNewFolderIds, oldToNewPolicyIds);
        encapsulatedAssertionInstaller.install(oldToNewPolicyIds);
        serviceInstaller.install(oldToNewFolderIds, oldToNewPolicyGuids, policyInstaller);
        trustedCertificateInstaller.install();

        logger.info("Finished installing bundle: " + context.getBundleInfo());
    }

    FolderInstaller getFolderInstaller() {
        return folderInstaller;
    }

    ServiceInstaller getServiceInstaller() {
        return serviceInstaller;
    }

    PolicyInstaller getPolicyInstaller() {
        return policyInstaller;
    }

    public EncapsulatedAssertionInstaller getEncapsulatedAssertionInstaller() {
        return encapsulatedAssertionInstaller;
    }

    TrustedCertificateInstaller getTrustedCertificateInstaller() {
        return trustedCertificateInstaller;
    }

    JdbcConnectionInstaller getJdbcConnectionInstaller() {
        return jdbcConnectionInstaller;
    }

    AssertionInstaller getAssertionInstaller() {
        return assertionInstaller;
    }
}
