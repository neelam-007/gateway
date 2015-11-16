package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.*;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.security.signer.SignatureVerifier;
import com.l7tech.util.Background;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.security.SignatureException;
import java.util.Collection;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class SolutionKitAdminImpl extends AsyncAdminMethodsImpl implements SolutionKitAdmin {
    @Inject
    private SolutionKitManager solutionKitManager;
    @Inject
    private LicenseManager licenseManager;
    @Inject
    private IdentityProviderConfigManager identityProviderConfigManager;

    @Inject
    @Named( "signatureVerifier" )
    final void setSignatureVerifier(final SignatureVerifier signatureVerifier) {
        this.signatureVerifier = signatureVerifier;
    }
    private SignatureVerifier signatureVerifier;

    @SuppressWarnings("unused")   // used for spring configuration
    public SolutionKitAdminImpl() {}

    public SolutionKitAdminImpl(LicenseManager licenseManager, SolutionKitManager solutionKitManager, SignatureVerifier signatureVerifier, IdentityProviderConfigManager identityProviderConfigManager) {
        this.licenseManager = licenseManager;
        this.solutionKitManager = solutionKitManager;
        this.signatureVerifier = signatureVerifier;
        this.identityProviderConfigManager = identityProviderConfigManager;
    }

    @Override
    public void verifySkarSignature(@NotNull final byte[] digest, @Nullable final String signatureProperties) throws SignatureException {
        getSolutionKitAdminHelper().verifySkarSignature(digest, signatureProperties);
    }

    @NotNull
    @Override
    public List<SolutionKit> getSolutionKitsToUpgrade(@Nullable SolutionKit solutionKit) throws FindException {  // TODO (TL refactor) this method seems out of place
        return getSolutionKitAdminHelper().getSolutionKitsToUpgrade(solutionKit);
    }

    @NotNull
    @Override
    public String testInstall(@NotNull final SolutionKit solutionKit, @NotNull final String bundle, final boolean isUpgrade) throws Exception {
        return getSolutionKitAdminHelper().testInstall(solutionKit, bundle, isUpgrade);
    }

    @NotNull
    @Override
    public JobId<String> testInstallAsync(@NotNull final SolutionKit solutionKit, @NotNull final String bundle, final boolean isUpgrade) {
        final FutureTask<String> task =
                new FutureTask<>(AdminInfo.find(false).wrapCallable(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        return testInstall(solutionKit, bundle, isUpgrade);
                    }
                }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        }, 0L);

        return registerJob(task, String.class);
    }

    @NotNull
    @Override
    public Goid install(@NotNull final SolutionKit solutionKit, @NotNull final String bundle, final boolean isUpgrade) throws Exception {
        return getSolutionKitAdminHelper().install(solutionKit, bundle, isUpgrade);
    }

    @NotNull
    @Override
    public JobId<Goid> installAsync(@NotNull final SolutionKit solutionKit, @NotNull final String bundle, final boolean isUpgrade) {
        final FutureTask<Goid> task =
                new FutureTask<>(AdminInfo.find(false).wrapCallable(new Callable<Goid>() {
                    @Override
                    public Goid call() throws Exception {
                        return install(solutionKit, bundle, isUpgrade);
                    }
                }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        }, 0L);

        return registerJob(task, Goid.class);
    }

    @NotNull
    @Override
    public String uninstall(@NotNull final Goid goid) throws Exception {
        return getSolutionKitAdminHelper().uninstall(goid);
    }

    @NotNull
    @Override
    public JobId<String> uninstallAsync(@NotNull final Goid goid) {
        final FutureTask<String> task =
                new FutureTask<>(AdminInfo.find(false).wrapCallable(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        return uninstall(goid);
                    }
                }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        }, 0L);

        return registerJob(task, String.class);
    }


    @NotNull
    @Override
    public Collection<SolutionKitHeader> findHeaders() throws FindException {
        return getSolutionKitAdminHelper().findHeaders();
    }

    @NotNull
    @Override
    public Collection<SolutionKitHeader> findHeaders(@NotNull final Goid parentGoid) throws FindException {
        return getSolutionKitAdminHelper().findHeaders(parentGoid);
    }

    @NotNull
    @Override
    public Collection<SolutionKit> find(@NotNull String solutionKitGuid) throws FindException {
        return getSolutionKitAdminHelper().find(solutionKitGuid);   // TODO (TL refactor) converting from list to Collection intentional?  more than once in this class
    }

    @NotNull
    @Override
    public Collection<SolutionKit> find(@NotNull Goid parentGoid) throws FindException {
        return getSolutionKitAdminHelper().find(parentGoid);
    }

    @Override
    public SolutionKit get(@NotNull Goid goid) throws FindException {
        return getSolutionKitAdminHelper().get(goid);
    }

    @NotNull
    @Override
    public Goid save(@NotNull SolutionKit solutionKit) throws SaveException {
        return getSolutionKitAdminHelper().save(solutionKit);
    }

    @Override
    public void update(@NotNull SolutionKit solutionKit) throws UpdateException {
        solutionKitManager.update(solutionKit);
    }

    @Override
    public void delete(@NotNull Goid goid) throws FindException, DeleteException {
        solutionKitManager.delete(goid);
    }


    /**
     * Override for unit tests.
     */
    SolutionKitAdminHelper getSolutionKitAdminHelper() {
        return new SolutionKitAdminHelper(licenseManager, solutionKitManager, signatureVerifier, identityProviderConfigManager);
    }
}