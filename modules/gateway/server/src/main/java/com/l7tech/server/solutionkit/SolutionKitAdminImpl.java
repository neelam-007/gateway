package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
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

import static com.l7tech.server.event.AdminInfo.find;

public class SolutionKitAdminImpl extends AsyncAdminMethodsImpl implements SolutionKitAdmin {
    @Inject
    private SolutionKitManager solutionKitManager;
    @Inject
    private LicenseManager licenseManager;

    @Inject
    @Named( "signatureVerifier" )
    final void setSignatureVerifier(final SignatureVerifier signatureVerifier) {
        this.signatureVerifier = signatureVerifier;
    }
    private SignatureVerifier signatureVerifier;


    @SuppressWarnings("unused")   // used for spring configuration
    public SolutionKitAdminImpl() {}

    public SolutionKitAdminImpl(LicenseManager licenseManager, SolutionKitManager solutionKitManager, SignatureVerifier signatureVerifier) {
        this.licenseManager = licenseManager;
        this.solutionKitManager = solutionKitManager;
        this.signatureVerifier = signatureVerifier;
    }

    @NotNull
    @Override
    public Collection<SolutionKitHeader> findSolutionKits() throws FindException {
        return solutionKitManager.findAllHeaders();
    }

    @NotNull
    @Override
    public Collection<SolutionKit> findBySolutionKitGuid(@NotNull String solutionKitGuid) throws FindException {
        return solutionKitManager.findBySolutionKitGuid(solutionKitGuid);
    }

    @NotNull
    @Override
    public Collection<SolutionKitHeader> findAllChildrenByParentGoid(Goid parentGoid) throws FindException {
        return getSolutionKitAdminHelper().findAllChildrenByParentGoid(parentGoid);
    }

    @NotNull
    @Override
    public Collection<SolutionKitHeader> findAllExcludingChildren() throws FindException {
        return solutionKitManager.findAllExcludingChildren();
    }

    @NotNull
    @Override
    public Collection<SolutionKitHeader> findParentSolutionKits() throws FindException {
        return solutionKitManager.findParentSolutionKits();
    }

    @Override
    public SolutionKit get(@NotNull Goid goid) throws FindException {
        return getSolutionKitAdminHelper().get(goid);
    }

    @NotNull
    @Override
    public JobId<String> testInstall(@NotNull final SolutionKit solutionKit, @NotNull final String bundle) {
        final FutureTask<String> task =
            new FutureTask<>(find(false).wrapCallable(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return getSolutionKitAdminHelper().testInstall(solutionKit, bundle);
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
    public JobId<Goid> install(@NotNull final SolutionKit solutionKit, @NotNull final String bundle, final boolean isUpgrade) {
        final FutureTask<Goid> task =
            new FutureTask<>(find(false).wrapCallable(new Callable<Goid>() {
                @Override
                public Goid call() throws Exception {
                    return getSolutionKitAdminHelper().install(solutionKit, bundle, isUpgrade);
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
    public JobId<String> uninstall(@NotNull final Goid goid) {
        final FutureTask<String> task =
            new FutureTask<>(find(false).wrapCallable(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return getSolutionKitAdminHelper().uninstall(goid);
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
    public Goid saveSolutionKit(@NotNull SolutionKit solutionKit) throws SaveException {
        return solutionKitManager.save(solutionKit);
    }

    @Override
    public void updateSolutionKit(@NotNull SolutionKit solutionKit) throws UpdateException {
        solutionKitManager.update(solutionKit);
    }

    @Override
    public void deleteSolutionKit(@NotNull Goid goid) throws FindException, DeleteException {
        solutionKitManager.delete(goid);
    }

    @Override
    public void verifySkarSignature(@NotNull final byte[] digest, @Nullable final String signatureProperties) throws SignatureException {
        getSolutionKitAdminHelper().verifySkarSignature(digest, signatureProperties);
    }

    @Override
    public List<SolutionKit> getSolutionKitsToUpgrade(@Nullable SolutionKit solutionKit) {
        return getSolutionKitAdminHelper().getSolutionKitsToUpgrade(solutionKit);
    }
    /**
     * Override for unit tests.
     */
    SolutionKitAdminHelper getSolutionKitAdminHelper() {
        return new SolutionKitAdminHelper(licenseManager, solutionKitManager, signatureVerifier);
    }
}