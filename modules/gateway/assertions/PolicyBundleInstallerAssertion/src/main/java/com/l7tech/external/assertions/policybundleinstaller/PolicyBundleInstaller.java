package com.l7tech.external.assertions.policybundleinstaller;

import com.l7tech.external.assertions.policybundleinstaller.installer.restman.MigrationBundleInstaller;
import com.l7tech.external.assertions.policybundleinstaller.installer.wsman.*;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.event.bundle.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerCallback;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.server.policy.bundle.ssgman.GatewayManagementInvoker;
import com.l7tech.server.service.ServiceManager;
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
    private final MigrationBundleInstaller migrationBundleInstaller;

    public PolicyBundleInstaller(@NotNull final GatewayManagementInvoker wsmanInvoker,
                                 @NotNull final GatewayManagementInvoker restmanInvoker,
                                 @NotNull final PolicyBundleInstallerContext context,
                                 @NotNull final ServiceManager serviceManager,
                                 @NotNull final Nullary<Boolean> cancelledCallback) {
        this.context = context;

        folderInstaller = new FolderInstaller(context, cancelledCallback, wsmanInvoker);
        policyInstaller = new PolicyInstaller(context, cancelledCallback, wsmanInvoker);
        encapsulatedAssertionInstaller = new EncapsulatedAssertionInstaller(context, cancelledCallback, wsmanInvoker);
        serviceInstaller = new ServiceInstaller(context, cancelledCallback, wsmanInvoker, serviceManager);
        trustedCertificateInstaller = new TrustedCertificateInstaller(context, cancelledCallback, wsmanInvoker);
        jdbcConnectionInstaller = new JdbcConnectionInstaller(context, cancelledCallback, wsmanInvoker);
        assertionInstaller = new AssertionInstaller(context, cancelledCallback, wsmanInvoker);
        migrationBundleInstaller = new MigrationBundleInstaller(context, cancelledCallback, restmanInvoker);
    }

     public void setPolicyBundleInstallerCallback(@Nullable PolicyBundleInstallerCallback policyBundleInstallerCallback) {
         policyInstaller.setPolicyBundleInstallerCallback(policyBundleInstallerCallback);
         migrationBundleInstaller.setPolicyBundleInstallerCallback(policyBundleInstallerCallback);
    }

    public void setAuthenticatedUser(@Nullable final UserBean authenticatedUser) {
        folderInstaller.getManagementClient().setAuthenticatedUser(authenticatedUser);
        policyInstaller.getManagementClient().setAuthenticatedUser(authenticatedUser);
        encapsulatedAssertionInstaller.getManagementClient().setAuthenticatedUser(authenticatedUser);
        serviceInstaller.getManagementClient().setAuthenticatedUser(authenticatedUser);
        trustedCertificateInstaller.getManagementClient().setAuthenticatedUser(authenticatedUser);
        jdbcConnectionInstaller.getManagementClient().setAuthenticatedUser(authenticatedUser);
        assertionInstaller.getManagementClient().setAuthenticatedUser(authenticatedUser);
        migrationBundleInstaller.getManagementClient().setAuthenticatedUser(authenticatedUser);
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
     * @throws PolicyBundleInstallerCallback.CallbackException
     */
    public void dryRunInstallBundle(@NotNull final DryRunInstallPolicyBundleEvent dryRunEvent)
            throws BundleResolver.BundleResolverException,
            BundleResolver.UnknownBundleException,
            BundleResolver.InvalidBundleException,
            InterruptedException,
            AccessDeniedManagementResponse,
            PolicyBundleInstallerCallback.CallbackException {

        logger.fine("Conflict checking bundle: " + context.getBundleInfo().getId());

        assertionInstaller.dryRunInstall(dryRunEvent);
        final Map<String, String> conflictingPolicyIdsNames = new HashMap<>();
        final Map<String, String> policyIdsNames =  policyInstaller.dryRunInstall(dryRunEvent, conflictingPolicyIdsNames);
        encapsulatedAssertionInstaller.dryRunInstall(dryRunEvent, policyIdsNames, conflictingPolicyIdsNames);
        serviceInstaller.dryRunInstall(dryRunEvent);
        trustedCertificateInstaller.dryRunInstall(dryRunEvent);
        jdbcConnectionInstaller.dryRunInstall(dryRunEvent);
        migrationBundleInstaller.dryRunInstall(dryRunEvent);

        logger.fine("Finished conflict checking bundle: " + context.getBundleInfo());
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
        final Map<String, String> oldToNewPolicyGuids = new HashMap<>();

        // install from prerequisites first
        for (String prerequisiteFolder : context.getBundleInfo().getPrerequisiteFolders()) {
            policyInstaller.install(prerequisiteFolder, oldToNewFolderIds, oldToNewPolicyIds, oldToNewPolicyGuids);
            encapsulatedAssertionInstaller.install(prerequisiteFolder, oldToNewPolicyIds);
            migrationBundleInstaller.install(prerequisiteFolder);
        }

        policyInstaller.install(oldToNewFolderIds, oldToNewPolicyIds, oldToNewPolicyGuids);
        encapsulatedAssertionInstaller.install(oldToNewPolicyIds);
        serviceInstaller.install(oldToNewFolderIds, oldToNewPolicyGuids, policyInstaller);
        trustedCertificateInstaller.install();
        migrationBundleInstaller.install();

        logger.info("Finished installing bundle: " + context.getBundleInfo());
    }

    public FolderInstaller getFolderInstaller() {
        return folderInstaller;
    }

    public ServiceInstaller getServiceInstaller() {
        return serviceInstaller;
    }

    public PolicyInstaller getPolicyInstaller() {
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
