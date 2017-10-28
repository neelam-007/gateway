package com.l7tech.gateway.common.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.solutionkit.SolutionKitManagerCallback;
import com.l7tech.policy.solutionkit.SolutionKitManagerContext;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.util.*;
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
     * Test solution kit install without committing the work.
     * @param doTestInstall test install callback
     * @throws Throwable SolutionKitException, ForbiddenException
     */
    //TODO: implement multibundle import for install
    public void testInstall(@NotNull final Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Throwable> doTestInstall) throws Throwable{
        final Collection<SolutionKit> selectedSolutionKits = solutionKitsConfig.getSelectedSolutionKits();
        final SolutionKit parentSolutionKitLoaded = solutionKitsConfig.getParentSolutionKitLoaded();

        for (final SolutionKit solutionKit: selectedSolutionKits) {
            invokeCustomCallback(solutionKit);
            // Update resolved mapping target IDs.
            solutionKitsConfig.setMappingTargetIdsFromResolvedIds(solutionKit);
            validateSolutionKitForInstall(solutionKit, parentSolutionKitLoaded);
            doTestInstall.call(getAsSolutionKitTriple(solutionKit));
        }
    }

    /**
     * Test solution kit upgrade without committing the work.
     * @param doTestUpgrade test upgrade callback
     * @throws Throwable SolutionKitException, ForbiddenException
     */
    public void testUpgrade(@NotNull final Functions.UnaryVoidThrows<SolutionKitInfo, Throwable> doTestUpgrade) throws Throwable {
        //selectedSolutionKits should be ordered because it is a treeset
        final List<SolutionKit> selectedSolutionKits = new ArrayList<>(solutionKitsConfig.getSelectedSolutionKits());
        final SolutionKitInfo solutionKitInfo = collectSolutionKitInformation(selectedSolutionKits);

        for (final SolutionKit solutionKit: selectedSolutionKits) {
            invokeCustomCallback(solutionKit);
            // Update resolved mapping target IDs.
            solutionKitsConfig.setMappingTargetIdsFromResolvedIds(solutionKit);
        }
        doTestUpgrade.call(solutionKitInfo);
    }

    /**
     * Gathers solution kit information to send to the SolutionKitManager so that it can import bundles.
     * @param selectedSolutionKits The list of solution kits selected for install
     * @return SolutionKitInfo containing delete bundles + SK metadata, install bundles + SK metadata, parent SK
     * @throws SolutionKitException Occurs when
     */
    private SolutionKitInfo collectSolutionKitInformation(List<SolutionKit> selectedSolutionKits) throws SolutionKitException{
        final SolutionKit parentSolutionKit = solutionKitsConfig.getParentSolutionKitLoaded();
        final Map<SolutionKit, String> solutionKitWithInstallBundle= new TreeMap<>();
        for (final SolutionKit selectedSolutionKit : selectedSolutionKits) {
            solutionKitWithInstallBundle.put(selectedSolutionKit, solutionKitsConfig.getBundleAsString(selectedSolutionKit));
        }
        final Map<SolutionKit, String> deleteBundles = SolutionKitUtils.generateListOfDeleteBundles(solutionKitsConfig.getSolutionKitsToUpgrade());
        return new SolutionKitInfo(deleteBundles, solutionKitWithInstallBundle, parentSolutionKit);
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
     * @throws SolutionKitException Exception thrown when there are exceptions in finding the solution kit or that when
     * it already exists.
     */
    private void validateSolutionKitForInstall(
            @NotNull final SolutionKit sourceSK,
            @Nullable final SolutionKit parentSKLoaded) throws SolutionKitException {
        final String sourceGuid = sourceSK.getSolutionKitGuid();
        final String sourceIM = sourceSK.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY);
        final String sourceIMDisplayName = InstanceModifier.getDisplayName(sourceIM);

        // Check install
        final SolutionKit solutionKitOnDB;
        try {
            solutionKitOnDB = solutionKitAdmin.get(sourceGuid, sourceIM);
        } catch (FindException e) {
            logger.log(Level.FINE, ExceptionUtils.getMessage(e));
            throw new SolutionKitException("Internal error while retrieving Solution Kit with guid '" + sourceGuid + "' " +
                    "with instance modifier '" + sourceIMDisplayName + "' from database.");
        }

        if (solutionKitOnDB != null) {
            throw new SolutionKitConflictException("This Solution Kit already exists. Please install again with a different instance modifier.");
        }

        if (parentSKLoaded != null) {
            validateParentSolutionKitOnInstall(parentSKLoaded, sourceIM);
        }
    }

    /**
     * During solution kit install, verify that the parent with an IM of another child has the same metadata
     * as the parent that is loaded for install, otherwise the loaded parent should be installed with a different IM, or
     * upgraded instead.
     * @param parentSKLoaded the parent solution kit to check
     * @param sourceIM the Child solution kit instance modifier
     * @throws SolutionKitException Exception thrown when there are exceptions in finding the solution kit or when
     * it already exists.
     */
    private void validateParentSolutionKitOnInstall(@NotNull final SolutionKit parentSKLoaded,
                                                    @Nullable final String sourceIM) throws SolutionKitException {
        final String sourceIMDisplayName = InstanceModifier.getDisplayName(sourceIM);
        // Check install
        final SolutionKit parentSolutionKitOnDb;
        final String parentGuid = parentSKLoaded.getSolutionKitGuid();
        try {
            parentSolutionKitOnDb = solutionKitAdmin.get(parentGuid, sourceIM);
        } catch (FindException e) {
            logger.info(ExceptionUtils.getMessage(e));
            throw new SolutionKitException("Internal error while retrieving Solution Kit with guid '" + parentGuid + "' " +
                    "with instance modifier '" + sourceIMDisplayName + "' from database.");
        }

        if (parentSolutionKitOnDb != null) {
            //validate parent solution kit has same details
            if (SolutionKitUtils.hasSameMetaData(parentSKLoaded, parentSolutionKitOnDb)) {
                logger.log(Level.FINE, "Adding additional child Solution Kits onto parent Solution Kit with guid '"
                        + parentGuid + "' with instance modifier '" + sourceIMDisplayName + "'.");
            } else {
                throw new SolutionKitConflictException("<html>Install failure: Install process attempts to overwrite an " +
                        "existing parent Solution Kit ('" + parentGuid + "' with instance modifier '" +
                        sourceIMDisplayName + "')<br/> with a new Solution Kit that has different properties. " +
                        "Please install again with a different instance modifier.</html>");
            }
        }
    }

    /**
     * Process solution kit install.  Can optionally skip error and continue to the next solution kit.
     * @param errorKitList Optional list to capture processing error instead of immediately throwing exception.
     * @param doAsyncInstall Optional callback to override admin install (e.g. override with AdminGuiUtils.doAsyncAdmin() for console UI)
     * @throws Exception includes: SolutionKitException, FindException, UpdateException, SaveException
     */
    //TODO: update this method to use multibundle import
    public void install(@Nullable final List<Pair<String, SolutionKit>> errorKitList,
                        @Nullable final Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Exception> doAsyncInstall) throws Exception {

        // Check if the loaded skar is a collection of skars.  If so process parent solution kit first.
        final SolutionKit parentSKFromLoad = solutionKitsConfig.getParentSolutionKitLoaded(); // Note: The parent solution kit has a dummy default GOID.
        final Map<String, Goid> instanceModifierWithParentGoid = new HashMap<>();

        // install or upgrade skars
        // After processing the parent, process selected solution kits if applicable.
        for (final SolutionKit solutionKit : solutionKitsConfig.getSelectedSolutionKits()) {
            if (parentSKFromLoad != null) {
                defineParentGoid(errorKitList, parentSKFromLoad, instanceModifierWithParentGoid, solutionKit);
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
     * Process solution kit(s) for upgrade.
     * @param doAsyncUpgrade Optional callback to override admin upgrade (e.g. override with AdminGuiUtils.doAsyncAdmin() for console UI)
     * @throws Exception includes: SolutionKitException, FindException, UpdateException, SaveException
     */
    public void upgrade(@Nullable final Functions.UnaryVoidThrows<SolutionKitInfo, Exception> doAsyncUpgrade) throws Exception {
        //selectedSolutionKits should be ordered because it is a treeset
        final List<SolutionKit> selectedSolutionKits = new ArrayList<>(solutionKitsConfig.getSelectedSolutionKits());

        // Check if the loaded skar is a collection of skars.  If so process parent solution kit first.
        final SolutionKit parentSKFromLoad = solutionKitsConfig.getParentSolutionKitLoaded(); // Note: The parent solution kit has a dummy default GOID.
        final Map<String, Goid> instanceModifierWithParentGoid = new HashMap<>();

        for (final SolutionKit solutionKit : solutionKitsConfig.getSelectedSolutionKits()) {
            if (parentSKFromLoad != null) {
                defineParentGoid(null, parentSKFromLoad, instanceModifierWithParentGoid, solutionKit);
            }

            // Update resolved mapping target IDs.
            solutionKitsConfig.setMappingTargetIdsFromResolvedIds(solutionKit);
        }

        final SolutionKitInfo solutionKitInfo = collectSolutionKitInformation(selectedSolutionKits);

        //upgrade the bundles
        if (doAsyncUpgrade == null) {
            solutionKitAdmin.upgrade(solutionKitInfo);
        } else {
            doAsyncUpgrade.call(solutionKitInfo);
        }
    }

    /**
     * This method determines what the parent goid is depending on if it was saved or updated from the database
     * @param errorKitList The error kit list to accumulate install Gui errors. if null then caller is Restman or upgrade
     * @param parentSKFromLoad Parent solution kit of sskar file
     * @param instanceModifierWithParentGoid The map containing parents created/seen so far
     * @param solutionKit solution kit in question
     * @throws Exception includes: SolutionKitException, FindException, UpdateException, SaveException
     */
    private void defineParentGoid(@Nullable List<Pair<String, SolutionKit>> errorKitList,
                                  SolutionKit parentSKFromLoad,
                                  Map<String, Goid> instanceModifierWithParentGoid,
                                  SolutionKit solutionKit) throws Exception {
        final String solutionKitIM = solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY);
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
                            solutionKitAdmin.update(SolutionKitUtils.copyParentSolutionKit(parentSKFromLoad, parentSKFromDB));
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
            solutionKitAdmin.update(SolutionKitUtils.copyParentSolutionKit(parentSolutionKit, solutionKitOnGateway));
        }
        // save if new
        else {
            final SolutionKit solutionKitCopy = SolutionKitUtils.copyParentSolutionKit(parentSolutionKit, new SolutionKit());
            solutionKitCopy.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, newInstanceModifier);
            goid = solutionKitAdmin.save(solutionKitCopy);
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
                //TODO: this method should also copy instance modifier?
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