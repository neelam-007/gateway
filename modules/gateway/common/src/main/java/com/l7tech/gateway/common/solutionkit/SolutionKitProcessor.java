package com.l7tech.gateway.common.solutionkit;

import com.l7tech.objectmodel.Goid;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.util.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Solution Kit processing logic that's common and accessible from both the console UI and in the server headless interface.
 * SolutionKitProcessor is an abstraction above SkarProcessor, working with solution kits *after* conversion from a .sskar (SkarProcessor responsible for the actual conversion from .sskar).
 * Goal is to reuse common code, at the same time, allow for slight differences using callbacks.
 */
public class SolutionKitProcessor {
    private static final Logger logger = Logger.getLogger(SolutionKitProcessor.class.getName());

    final SolutionKitsConfig solutionKitsConfig;
    final SolutionKitAdmin solutionKitAdmin;
    final SkarProcessor skarProcessor;

    public SolutionKitProcessor(@NotNull final SolutionKitsConfig solutionKitsConfig, @NotNull final SolutionKitAdmin solutionKitAdmin, @NotNull final SkarProcessor skarProcessor) {
        this.solutionKitsConfig = solutionKitsConfig;
        this.solutionKitAdmin = solutionKitAdmin;
        this.skarProcessor = skarProcessor;
    }

    // TODO (TL refactor) license check?

    // TODO (TL refactor) getSolutionKitsToUpgrade() ?

    /**
     * Test solution kit install or upgrade without committing the work.
     * @param skipOverrideCheck skip only in specific cases (e.g. resolve target ids of previously install for test upgrade via UI)
     * @param doTestInstall test install callback
     * @throws Throwable
     */
    public void testInstallOrUpgrade(boolean skipOverrideCheck, @NotNull final Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Throwable> doTestInstall) throws Throwable {
        final Collection<SolutionKit> selectedSolutionKits = solutionKitsConfig.getSelectedSolutionKits();
        final boolean isUpgrade = solutionKitsConfig.isUpgrade();

        for (final SolutionKit solutionKit: selectedSolutionKits) {

            // Check if the solution kit is upgradable.  If the solution kit attempts for upgrade, but its skar does not
            // contain UpgradeBundle.xml, then throw exception with warning
            if (isUpgrade && !solutionKitsConfig.isUpgradeInfoProvided(solutionKit)) {
                throw new BadRequestException("Solution kit '" + solutionKit.getName() + "' cannot be used for upgrade due to that its SKAR file does not include UpgradeBundle.xml.");
            }

            // invoke custom callbacks
            skarProcessor.invokeCustomCallback(solutionKit);

            // Update resolved mapping target IDs.
            solutionKitsConfig.updateResolvedMappingsIntoBundle(solutionKit, skipOverrideCheck);

            doTestInstall.call(skarProcessor.getAsSolutionKitTriple(solutionKit));
        }
    }

    /**
     * Test solution kit install or upgrade without committing the work.
     * @param doTestInstall test install callback
     * @throws Throwable
     */
    public void testInstallOrUpgrade(@NotNull final Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Throwable> doTestInstall) throws Throwable {
        testInstallOrUpgrade(false, doTestInstall);
    }

    /**
     * Process solution kit install or upgrade.  Can optionally skip error and continue to the next solution kit.
     * @throws Exception includes: SolutionKitException, FindException, UpdateException, SaveException
     */
    public void installOrUpgrade() throws Exception {
        installOrUpgrade(null, null);
    }
    /**
     * Process solution kit install or upgrade.  Can optionally skip error and continue to the next solution kit.
     * @param errorKitList Optional list to capture processing error instead of immediately throwing exception.
     * @param doAsyncInstall Optional callback to override admin install (e.g. override with AdminGuiUtils.doAsyncAdmin() for console UI)
     * @throws Exception includes: SolutionKitException, FindException, UpdateException, SaveException
     */
    public void installOrUpgrade(@Nullable final List<Pair<String, SolutionKit>> errorKitList,
                                 @Nullable final Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Exception> doAsyncInstall) throws Exception {

        // Check if the loaded skar is a collection of skars.  If so process parent solution kit first.
        final SolutionKit parentSKFromLoad = solutionKitsConfig.getParentSolutionKitLoaded(); // Note: The parent solution kit has a dummy default GOID.
        Goid parentGoid = saveOrUpdateParentSolutionKit(parentSKFromLoad, errorKitList);

        // install or upgrade skars
        // After processing the parent, process selected solution kits if applicable.
        for (SolutionKit solutionKit : solutionKitsConfig.getSelectedSolutionKits()) {
            // If the solution kit is under a parent solution kit, then set its parent goid before it gets saved.
            if (parentSKFromLoad != null) {
                solutionKit.setParentGoid(parentGoid);
            }

            try {
                // Update resolved mapping target IDs.
                solutionKitsConfig.updateResolvedMappingsIntoBundle(solutionKit);

                Triple<SolutionKit, String, Boolean> loaded = skarProcessor.getAsSolutionKitTriple(solutionKit);
                if (doAsyncInstall == null) {
                    solutionKitAdmin.install(loaded.left, loaded.middle, loaded.right);
                } else {
                    doAsyncInstall.call(loaded);
                }
            } catch (Exception e) {
                addToErrorListOrRethrowException(e, errorKitList, solutionKit);
            }
        }
    }

    @Nullable
    private Goid saveOrUpdateParentSolutionKit(@Nullable final SolutionKit parentSKFromLoad, @Nullable final List<Pair<String, SolutionKit>> errorKitList) throws Exception {
        Goid parentGoid = null;

        if (parentSKFromLoad != null) {
            try {
                // Case 1: Parent for upgrade
                if (solutionKitsConfig.isUpgrade()) {
                    final List<SolutionKit> solutionKitsToUpgrade = solutionKitsConfig.getSolutionKitsToUpgrade();
                    assert solutionKitsToUpgrade.size() > 0; // should always be greater then 0 as check is done above (early fail)
                    final SolutionKit parentSKFromDB = solutionKitsToUpgrade.get(0);

                    if (!parentSKFromLoad.getSolutionKitGuid().equalsIgnoreCase(parentSKFromDB.getSolutionKitGuid())) {
                        String msg = "Unexpected:  GUID (" + parentSKFromLoad.getSolutionKitGuid() + ") from the database does not match the GUID (" + parentSKFromDB.getSolutionKitGuid() + ") of the loaded solution kit from file.";
                        logger.info(msg);
                        throw new SolutionKitException(msg);
                    }

                    // Update the parent solution kit attributes
                    parentSKFromDB.setName(parentSKFromLoad.getName());
                    parentSKFromDB.setSolutionKitVersion(parentSKFromLoad.getSolutionKitVersion());
                    parentSKFromDB.setXmlProperties(parentSKFromLoad.getXmlProperties());

                    parentGoid = parentSKFromDB.getGoid();
                    solutionKitAdmin.update(parentSKFromDB);
                }
                // Case 2: Parent for install
                else {
                    final Collection<SolutionKit> solutionKitsExistingOnGateway = solutionKitAdmin.find(parentSKFromLoad.getSolutionKitGuid());
                    // Case 2.1: Find the parent already installed on gateway
                    if (solutionKitsExistingOnGateway.size() > 0) {
                        final SolutionKit parentExistingOnGateway = solutionKitsExistingOnGateway.iterator().next();
                        parentGoid = parentExistingOnGateway.getGoid();
                        solutionKitAdmin.update(parentExistingOnGateway);
                    }
                    // Case 2.2: No such parent installed on gateway
                    else {
                        parentGoid = solutionKitAdmin.save(parentSKFromLoad);
                    }
                }
            } catch (Exception e) {
                addToErrorListOrRethrowException(e, errorKitList, parentSKFromLoad);
            }
        }

        return parentGoid;
    }

    private void addToErrorListOrRethrowException(@NotNull final Exception e, @Nullable final List<Pair<String, SolutionKit>> errorList, @NotNull final SolutionKit solutionKit) throws Exception {
        if (errorList != null) {
            String msg = ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            errorList.add(new Pair<>(msg, solutionKit));
        } else {
            throw e;
        }
    }
}
