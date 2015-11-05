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
 * SolutionKitProcessor is an abstraction above SkarProcessor, to deal with converting a .skar into a solution kit.
 * Reuse common code at the same time allow for slight differences.
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

    // TODO (TL refactor) invokeCustomCallback() ?

    // TODO (TL refactor) license check?

    // TODO (TL refactor) getSolutionKitsToUpgrade() ?

    // TODO (TL refactor) testInstall() ?

    /**
     * Process solution kit install or upgrade.  Can optionally skip error and continue to the next solution kit.
     * @param errorKitList Optional list to capture processing error instead of immediately throwing exception.
     * @param doAsyncInstall Optional callback to override admin install (e.g. override with AdminGuiUtils.doAsyncAdmin() for console UI)
     * @throws Exception includes: SolutionKitException, FindException, UpdateException, SaveException
     */
    public void installOrUpgrade(@Nullable final List<Pair<String, SolutionKit>> errorKitList,
                                 @Nullable final Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Exception> doAsyncInstall) throws Exception {

        // Check if the loaded skar is a collection of skars.  If so process parent solution kit first.
        Goid parentGoid = null;
        final SolutionKit parentSKFromLoad = solutionKitsConfig.getParentSolutionKitLoaded(); // Note: The parent solution kit has a dummy default GOID.
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

        // install or upgrade skars
        // After processing the parent, process selected solution kits if applicable.
        for (SolutionKit solutionKit : solutionKitsConfig.getSelectedSolutionKits()) {
            // If the solution kit is under a parent solution kit, then set its parent goid before it gets saved.
            if (parentSKFromLoad != null) {
                solutionKit.setParentGoid(parentGoid);
            }

            try {
                Triple<SolutionKit, String, Boolean> loaded = skarProcessor.installOrUpgrade(solutionKit);
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
