package com.l7tech.gateway.common.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.solutionkit.SolutionKitManagerCallback;
import com.l7tech.policy.solutionkit.SolutionKitManagerContext;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Solution Kit processing logic that's common and accessible from both the console UI and in the server headless interface.
 * SolutionKitProcessor is working with solution kits *after* conversion from a .sskar (SkarPayload responsible for the actual conversion from .sskar).
 * Goal is to reuse common code, at the same time, allow for slight differences using callbacks.
 */
public class SolutionKitProcessor {
    private static final Logger logger = Logger.getLogger(SolutionKitProcessor.class.getName());

    final SolutionKitsConfig solutionKitsConfig;
    final SolutionKitAdmin solutionKitAdmin;

    public SolutionKitProcessor(@NotNull final SolutionKitsConfig solutionKitsConfig, @NotNull final SolutionKitAdmin solutionKitAdmin) {
        this.solutionKitsConfig = solutionKitsConfig;
        this.solutionKitAdmin = solutionKitAdmin;
    }

    // TODO (TL refactor) license check?

    // TODO (TL refactor) getSolutionKitsToUpgrade() ?

    /**
     * Test solution kit install or upgrade without committing the work.
     * @param doTestInstall test install callback
     * @throws Throwable
     */
    public void testInstallOrUpgrade(@NotNull final Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Throwable> doTestInstall) throws Throwable {
        final Collection<SolutionKit> selectedSolutionKits = solutionKitsConfig.getSelectedSolutionKits();
        final boolean isUpgrade = solutionKitsConfig.isUpgrade();

        for (final SolutionKit solutionKit: selectedSolutionKits) {

            // Check if the solution kit is upgradable.  If the solution kit attempts for upgrade, but its skar does not
            // contain UpgradeBundle.xml, then throw exception with warning
            if (isUpgrade && !solutionKitsConfig.isUpgradeInfoProvided(solutionKit)) {
                throw new BadRequestException("Solution kit '" + solutionKit.getName() + "' cannot be used for upgrade due to that its SKAR file does not include UpgradeBundle.xml.");
            }

            // invoke custom callbacks
            invokeCustomCallback(solutionKit);

            // Update resolved mapping target IDs.
            solutionKitsConfig.setMappingTargetIdsFromResolvedIds(solutionKit);

            doTestInstall.call(getAsSolutionKitTriple(solutionKit));
        }
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
                solutionKitsConfig.setMappingTargetIdsFromResolvedIds(solutionKit);

                Triple<SolutionKit, String, Boolean> loaded = getAsSolutionKitTriple(solutionKit);
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

                    if (isSameGuid(parentSKFromLoad, parentSKFromDB)) {
                        // Update the parent solution kit attributes
                        parentSKFromDB.setName(parentSKFromLoad.getName());
                        parentSKFromDB.setSolutionKitVersion(parentSKFromLoad.getSolutionKitVersion());
                        parentSKFromDB.setXmlProperties(parentSKFromLoad.getXmlProperties());

                        parentGoid = parentSKFromDB.getGoid();
                        solutionKitAdmin.update(parentSKFromDB);
                    } else if (isConversionToCollection(parentSKFromLoad, parentSKFromDB)) {
                        // parentSKFromDB is single and parent-less
                        // it's is being changed to be a child of the new uploaded parent (parentSKFromLoad)
                        parentGoid = saveOrUpdate(parentSKFromLoad);
                    } else {
                        String msg = "Unexpected:  GUID (" + parentSKFromLoad.getSolutionKitGuid() + ") from the database does not match the GUID (" + parentSKFromDB.getSolutionKitGuid() + ") of the loaded solution kit from file.";
                        logger.info(msg);
                        throw new SolutionKitException(msg);
                    }
                }
                // Case 2: Parent for install
                else {
                    parentGoid = saveOrUpdate(parentSKFromLoad);
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

    private boolean isSameGuid(@NotNull SolutionKit newSolutionKit, @NotNull SolutionKit existingSolutionKit) {
        return newSolutionKit.getSolutionKitGuid().equalsIgnoreCase(existingSolutionKit.getSolutionKitGuid());
    }

    private boolean isConversionToCollection(@NotNull SolutionKit newSolutionKit, @NotNull SolutionKit existingSolutionKit) {
        return SolutionKitUtils.isCollectionOfSkars(newSolutionKit) && !SolutionKitUtils.isCollectionOfSkars(existingSolutionKit);
    }

    @Nullable
    private Goid saveOrUpdate(@NotNull SolutionKit parentSolutionKit) throws FindException, SaveException, UpdateException {
        Goid goid;
        final Collection<SolutionKit> solutionKitsExistingOnGateway = solutionKitAdmin.find(parentSolutionKit.getSolutionKitGuid());
        // update if already installed
        if (solutionKitsExistingOnGateway.size() > 0) {
            final SolutionKit parentExistingOnGateway = solutionKitsExistingOnGateway.iterator().next();
            goid = parentExistingOnGateway.getGoid();
            solutionKitAdmin.update(parentExistingOnGateway);
        }
        // save if new
        else {
            goid = solutionKitAdmin.save(parentSolutionKit);
        }

        return goid;
    }

    /**
     * Invoke custom callback code support read and write of Solution Kit metadata and bundle.
     */
    void invokeCustomCallback(final SolutionKit solutionKit) throws SolutionKitException {
        try {
            SolutionKitManagerContext skContext;

            Pair<SolutionKit, SolutionKitCustomization> customization;
            SolutionKitManagerCallback customCallback;
            SolutionKitManagerUi customUi;

            customization = solutionKitsConfig.getCustomizations().get(solutionKit.getSolutionKitGuid());
            if (customization == null || customization.right == null) return;

            // implementer provides a callback
            customCallback = customization.right.getCustomCallback();
            if (customCallback == null) return;

            // we have a callback so populate the context map
            SolutionKitCustomization.populateSolutionKitManagerContextMap(customCallback.getContextMap(), solutionKitsConfig);

            // we have a callback so populate selected solution kits set
            SolutionKitCustomization.populateSelectedSolutionKits(customCallback.getSelectedSolutionKits(), solutionKitsConfig);

            customUi = customization.right.getCustomUi();

            // if implementer provides a context
            skContext = customUi != null ? customUi.getContext() : null;
            if (skContext != null) {
                // execute callback
                customCallback.preMigrationBundleImport(skContext);

                // copy back metadata from xml version
                SolutionKitUtils.copyDocumentToSolutionKit(skContext.getSolutionKitMetadata(), solutionKit);

                // set (possible) changes made to metadata and bundles (install/upgrade and uninstall)
                solutionKit.setUninstallBundle(skContext.getUninstallBundle() == null ? null : XmlUtil.nodeToString(skContext.getUninstallBundle()));
                solutionKitsConfig.setBundle(solutionKit, skContext.getMigrationBundle());

                solutionKitsConfig.setPreviouslyResolvedIds();  // need to call a second time; already called earlier
                solutionKitsConfig.setMappingTargetIdsFromPreviouslyResolvedIds(solutionKit, solutionKitsConfig.getBundle(solutionKit));
            } else {
                customCallback.preMigrationBundleImport(null);
            }
        } catch (SolutionKitManagerCallback.CallbackException e) {
            String errorMessage = ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, errorMessage, ExceptionUtils.getDebugException(e));
            throw new BadRequestException(e.getMessage(), e);
        } catch (IncompatibleClassChangeError e) {
            String errorMessage = solutionKit.getName() + " was created using an incompatible version of the customization library.";
            logger.log(Level.WARNING, errorMessage, e);
            throw new BadRequestException(errorMessage, e);
        }  catch (IOException | TooManyChildElementsException | MissingRequiredElementException | SAXException e) {
            String errorMessage = ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, errorMessage, ExceptionUtils.getDebugException(e));
            throw new BadRequestException("Unexpected error during custom callback invocation.", e);
        }
    }

    /**
     * Get solution kit from the SKAR for install or upgrade.
     */
    Triple<SolutionKit, String, Boolean> getAsSolutionKitTriple(@NotNull final SolutionKit solutionKit) throws SolutionKitException {
        String bundleXml = solutionKitsConfig.getBundleAsString(solutionKit);
        if (bundleXml == null) {
            throw new BadRequestException("Unexpected error: unable to get Solution Kit bundle.");
        }

        return new Triple<>(solutionKit, bundleXml, solutionKitsConfig.isUpgrade());
    }
}