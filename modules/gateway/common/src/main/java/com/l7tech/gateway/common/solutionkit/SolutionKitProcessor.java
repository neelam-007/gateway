package com.l7tech.gateway.common.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.solutionkit.SolutionKitManagerCallback;
import com.l7tech.policy.solutionkit.SolutionKitManagerContext;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.*;
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
        final SolutionKit parentSolutionKitLoaded = solutionKitsConfig.getParentSolutionKitLoaded();

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

            if (isUpgrade) {
                doTestInstall.call(getAsSolutionKitTriple(solutionKit));
            } else {
                final Pair<Boolean, String> result = validateSolutionKitForInstall(solutionKit, parentSolutionKitLoaded);
                if (result.left) {
                    doTestInstall.call(getAsSolutionKitTriple(solutionKit));
                } else {
                    throw new SolutionKitConflictException(result.right);
                }
            }
        }
    }

    /**
     * Validate if a target solution kit is good for install.
     *
     * Install: if an existing solution kit is found and is the same as the target solution kit, fail the install.
     * This is the requirement from the note in the story SSG-10996, Upgrade Solution Kit.
     * - disable install if SK is already installed (upgrade only)
     *
     * @param sourceSK: the solution kit to be validated
     * @param parentSKLoaded: the parent solution kit to be validated
     * @return true if the solutionKit for install or upgrade is valid, false otherwise. String is the error message and is null for valid cases
     * @throws BadRequestException Exception throw when there are exceptions in finding the solution kit.
     */
    private Pair<@NotNull Boolean, @Nullable String> validateSolutionKitForInstall(
            @NotNull final SolutionKit sourceSK,
            @Nullable final SolutionKit parentSKLoaded) throws BadRequestException {
        final String sourceGuid = sourceSK.getSolutionKitGuid();
        final String sourceIM = InstanceModifier.getInstanceModifier(sourceSK);

        // Check install
        final SolutionKit solutionKitOnDB;
        try {
            solutionKitOnDB = solutionKitAdmin.get(sourceGuid, sourceIM);
        } catch (FindException e) {
            throw new BadRequestException(ExceptionUtils.getMessage(e));
        }

        if (solutionKitOnDB != null) {
            return new Pair<>(false, "This solution kit already exists. To install it again, specify a unique instance modifier");
        }

        if (parentSKLoaded != null) {
            return validateParentSolutionKitOnInstall(parentSKLoaded, sourceIM);
        }
        return new Pair<>(true, null);
    }

    /**
     * During solution kit install, verify that the parent with an IM of another child has the same metadata
     * as the parent that is loaded for install, otherwise the loaded parent should be installed with a different IM, or
     * upgraded instead.
     * @param parentSKLoaded the parent solution kit to check
     * @param sourceIM the Child solution kit instance modifier
     *  @return true if the parent guid for install is valid, false otherwise. String is the error message and is null for valid cases
     * @throws BadRequestException Exception throw when there are exceptions in finding the solution kit.
     */
    private Pair<@NotNull Boolean, @Nullable String> validateParentSolutionKitOnInstall(@NotNull final SolutionKit parentSKLoaded,
                                                                                        @NotNull final String sourceIM) throws BadRequestException {
        final String sourceIMDisplayName = InstanceModifier.getDisplayName(sourceIM);
        // Check install
        final SolutionKit parentSolutionKitOnDb;
        final String parentGuid = parentSKLoaded.getSolutionKitGuid();
        try {
            parentSolutionKitOnDb = solutionKitAdmin.get(parentGuid, sourceIM);
        } catch (FindException e) {
            throw new BadRequestException(ExceptionUtils.getMessage(e));
        }

        if (parentSolutionKitOnDb != null) {
            //validate parent solution kit has same details
            if (SolutionKitUtils.hasSameMetaData(parentSKLoaded, parentSolutionKitOnDb)) {
                logger.log(Level.FINE, "Adding additional child solution kits onto parent solution kit with guid '" + parentGuid + "' with instance modifier '" +
                        sourceIMDisplayName + "'.");
                return new Pair<>(true, null);
            } else {
                return new Pair<>(false, "Install failure: Install process attempts to overwrite an existing parent solution kit ('" + parentGuid + "' with instance modifier '" +
                        sourceIMDisplayName + "') with a new solution kit that has different properties. To install again, specify a different instance modifier.");
            }
        }
        return new Pair<>(true, null);
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
        final Map<String, Goid> instanceModifierWithParentGoid = new HashMap<>();

        // install or upgrade skars
        // After processing the parent, process selected solution kits if applicable.
        for (SolutionKit solutionKit : solutionKitsConfig.getSelectedSolutionKits()) {
            if (parentSKFromLoad != null) {
                final String solutionKitIM = InstanceModifier.getInstanceModifier(solutionKit);
                final Goid parentGoid;
                //saveOrUpdate parent if the instance modifier has not been seen so far
                if (!instanceModifierWithParentGoid.containsKey(solutionKitIM)) {
                    parentGoid = saveOrUpdateParentSolutionKit(parentSKFromLoad, solutionKitIM, errorKitList);
                    instanceModifierWithParentGoid.put(solutionKitIM, parentGoid);
                } else {
                    //just retrieve the parent goid for an instance modifier
                    parentGoid = instanceModifierWithParentGoid.get(solutionKitIM);
                }
                // If the solution kit is under a parent solution kit, then set its parent goid before it gets saved.
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

    /**
     * If the user is installing a solution kit, then save the parent solution kit
     * If the user selected upgrade, then update the parent solution kit
     * @param parentSKFromLoad The parent solution kit from the .SSKAR file
     * @param newInstanceModifier The new instanceModifier that is specified on install.
     *                           On upgrade scenarios, newInstanceModifier is always the same as the current instance
     *                           modifier that exists for the parent on the database
     * @param errorKitList Accumulation of all errors during install/upgrade
     * @return the Goid of the saved or updated parent solution kit
     * @throws Exception includes: SolutionKitException, FindException, UpdateException, SaveException
     */
    @Nullable
    private Goid saveOrUpdateParentSolutionKit(@NotNull final SolutionKit parentSKFromLoad, @Nullable String newInstanceModifier, @Nullable final List<Pair<String, SolutionKit>> errorKitList) throws Exception {
        Goid parentGoid = null;
            try {
                // Case 1: Parent for upgrade
                if (solutionKitsConfig.isUpgrade()) {
                    final SolutionKit parentSKFromDB = solutionKitsConfig.getSolutionKitToUpgrade(parentSKFromLoad.getSolutionKitGuid());
                    if (parentSKFromDB == null) {
                        throw new SolutionKitException("Cannot upgrade because parent solution kit not found.");
                    }
                    final String originalParentIM = parentSKFromDB.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY);
                    //Set the parentSkFromLoad to have the same IM as the parentSK from DB since this is upgrade
                    parentSKFromLoad.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, originalParentIM);

                    if (isSameGuid(parentSKFromLoad, parentSKFromDB)) {
                        if (InstanceModifier.isSame(originalParentIM, newInstanceModifier)) {
                            // Update the parent solution kit attributes
                            parentGoid = parentSKFromDB.getGoid();
                            solutionKitAdmin.update(SolutionKitUtils.copyParentSolutionKitWithIM(parentSKFromLoad, parentSKFromDB, originalParentIM));
                        } else {
                            //Restrict users from assigning different instance modifiers on upgrade
                            final String msg = "Unable to change the instance modifier on upgrade. Please install the Solution Kit and specify a unique instance modifier instead.";
                            logger.info(msg);
                            throw new SolutionKitException(msg);
                        }
                    } else if (isConversionToCollection(parentSKFromLoad, parentSKFromDB)) {
                        // parentSKFromDB is single and parent-less
                        // it's is being changed to be a child of the new uploaded parent (parentSKFromLoad)
                        parentGoid = saveOrUpdate(parentSKFromLoad, newInstanceModifier);
                    } else {
                        String msg = "Unexpected:  GUID (" + parentSKFromLoad.getSolutionKitGuid() + ") from the database does not match the GUID (" + parentSKFromDB.getSolutionKitGuid() + ") of the loaded solution kit from file.";
                        logger.info(msg);
                        throw new SolutionKitException(msg);
                    }
                }
                // Case 2: Parent for install
                else {
                    parentGoid = saveOrUpdate(parentSKFromLoad, newInstanceModifier);
                }
            } catch (Exception e) {
                addToErrorListOrRethrowException(e, errorKitList, parentSKFromLoad);
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
    private Goid saveOrUpdate(@NotNull SolutionKit parentSolutionKit, @Nullable String newInstanceModifier) throws FindException, SaveException, UpdateException {
        final Goid goid;
        final SolutionKit solutionKitOnGateway = solutionKitAdmin.get(parentSolutionKit.getSolutionKitGuid(), newInstanceModifier);
        // update if already installed
        if (solutionKitOnGateway != null) {
            goid = solutionKitOnGateway.getGoid();
            solutionKitAdmin.update(SolutionKitUtils.copyParentSolutionKitWithIM(parentSolutionKit, solutionKitOnGateway, newInstanceModifier));
        }
        // save if new
        else {
            goid = solutionKitAdmin.save(SolutionKitUtils.copyParentSolutionKitWithIM(parentSolutionKit, new SolutionKit(), newInstanceModifier));
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