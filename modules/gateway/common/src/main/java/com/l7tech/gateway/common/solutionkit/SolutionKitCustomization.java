package com.l7tech.gateway.common.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.solutionkit.SolutionKitManagerCallback;
import com.l7tech.policy.solutionkit.SolutionKitManagerContext;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.util.MissingRequiredElementException;
import com.l7tech.util.Pair;
import com.l7tech.util.TooManyChildElementsException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.util.*;

/**
 * Holder classes required for customizations.
 */
public class SolutionKitCustomization {
    @NotNull
    private final SolutionKitCustomizationClassLoader classLoader;
    @Nullable
    private final SolutionKitManagerCallback customCallback;
    @Nullable
    private final SolutionKitManagerUi customUi;

    public SolutionKitCustomization(@NotNull final SolutionKitCustomizationClassLoader classLoader,
                                    @Nullable final SolutionKitManagerUi customUi,
                                    @Nullable final SolutionKitManagerCallback customCallback) {
        this.classLoader = classLoader;
        this.customUi = customUi;
        this.customCallback = customCallback;
    }

    @NotNull
    public SolutionKitCustomizationClassLoader getClassLoader() {
        return classLoader;
    }

    @Nullable
    public SolutionKitManagerCallback getCustomCallback() {
        return customCallback;
    }

    @Nullable
    public SolutionKitManagerUi getCustomUi() {
        return customUi;
    }

    /**
     * Populate the Solution Kit Manager context with the following: metadata, install bundle, uninstall bundle, already installed metadata.
     * This method might fit better as part of SolutionKitManagerContext (public API).  But for now we avoid exposing it as part of our
     * public API so we don't lock ourselves out from future changes.
     */
    private static void populateSolutionKitManagerContext(@NotNull final SolutionKitsConfig solutionKitsConfig,
                                                         @NotNull final SolutionKitManagerContext skmContext,
                                                         @NotNull final SolutionKit solutionKit) throws TooManyChildElementsException, MissingRequiredElementException, SAXException {

        // set to context: metadata, install bundle
        skmContext.setSolutionKitMetadata(SolutionKitUtils.createDocument(solutionKit));
        skmContext.setMigrationBundle(solutionKitsConfig.getBundleAsDocument(solutionKit));

        // set to context: uninstall bundle
        final String uninstallBundle = solutionKit.getUninstallBundle();
        if (StringUtils.isNotBlank(uninstallBundle)) {
            skmContext.setUninstallBundle(XmlUtil.stringToDocument(uninstallBundle));
        }

        // set to context: already installed metadata
        final SolutionKit solutionKitToUpgrade = solutionKitsConfig.getSolutionKitToUpgrade(solutionKit.getSolutionKitGuid());
        if (solutionKitToUpgrade != null) {
            skmContext.setInstalledSolutionKitMetadata(SolutionKitUtils.createDocument(solutionKitToUpgrade));
        }

        // set to context: instance modifier
        skmContext.setInstanceModifier(solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY));
    }

    public static void addCustomUis(@NotNull final JPanel customizableButtonPanel,
                                    @NotNull final SolutionKitsConfig settings,
                                    @Nullable final SolutionKit selectedSolutionKit) throws TooManyChildElementsException, MissingRequiredElementException, SAXException {

        // Initially remove any button from customizableButtonPanel
        customizableButtonPanel.removeAll();

        // If the selected solution kit has customization with a custom UI, then create a button via the custom UI.
        // Otherwise, there is not any button created.
        if (selectedSolutionKit != null) {
            final Map<String, Pair<SolutionKit, SolutionKitCustomization>> customizations = settings.getCustomizations();
            final Pair<SolutionKit, SolutionKitCustomization> customization = customizations.get(selectedSolutionKit.getSolutionKitGuid());
            if (customization != null && customization.right != null) {
                final SolutionKitManagerUi customUi = customization.right.getCustomUi();
                if (customUi != null) {
                    // we have a customUi so populate the context map
                    populateSolutionKitManagerContextMap(customUi.getContextMap(), settings);

                    // call button create logic
                    final JButton createButton = customUi.createButton(customizableButtonPanel);
                    if (createButton != null) {
                        customizableButtonPanel.add(createButton);
                    }
                }
            }
        }
    }

    /**
     * Internal read-only {@code SolutionKitManagerContext}.
     * {@code UnsupportedOperationException} is thrown if a setter is called.
     */
    private static class ReadOnlySolutionKitManagerContext extends SolutionKitManagerContext {
        @NotNull
        private final SolutionKitManagerContext context;

        private ReadOnlySolutionKitManagerContext(@NotNull final SolutionKitManagerContext context) {
            this.context = context;
        }

        /**
         * Returns a clone of the solution kit metadata DOM Document.
         * <p>
         * The trade off here is that creating an expensive copy prevents the programmer from modifying the context meta data
         * allowing us to optimize the code in the future if needed.
         *
         * TODO: in the future return immutable or read-only Document instance.
         * </p>
         */
        @Override
        public Document getSolutionKitMetadata() {
            final Document solutionKitMetadata = context.getSolutionKitMetadata();
            return solutionKitMetadata != null ? (Document)solutionKitMetadata.cloneNode(true) : null;
        }

        @Override
        public void setSolutionKitMetadata(final Document solutionKitMetadata) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getInstanceModifier() {
            return context.getInstanceModifier();
        }

        @Override
        public void setInstanceModifier(final String instanceModifier) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, String> getKeyValues() {
            return Collections.unmodifiableMap(context.getKeyValues());
        }

        /**
         * Returns a clone of the DOM Document representing the restman migration bundle to install or upgrade the {@code SolutionKit}.
         * <p>
         * The trade off here is that creating an expensive copy prevents the programmer from modifying the context meta data
         * allowing us to optimize the code in the future if needed.
         *
         * TODO: in the future return immutable or read-only Document instance.
         * </p>
         */
        @Override
        public Document getMigrationBundle() {
            final Document migrationBundle = context.getMigrationBundle();
            return migrationBundle != null ? (Document)migrationBundle.cloneNode(true) : null;
        }

        @Override
        public void setMigrationBundle(final Document migrationBundle) {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns a clone of the DOM Document representing the restman migration bundle to uninstall the {@code SolutionKit}.
         * <p>
         * The trade off here is that creating an expensive copy prevents the programmer from modifying the context meta data
         * allowing us to optimize the code in the future if needed.
         *
         * TODO: in the future return immutable or read-only Document instance.
         * </p>
         */
        @Override
        public Document getUninstallBundle() {
            final Document uninstallBundle = context.getUninstallBundle();
            return uninstallBundle != null ? (Document)uninstallBundle.cloneNode(true) : null;
        }

        @Override
        public void setUninstallBundle(final Document uninstallBundle) {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns a clone of the DOM Document representing the metadata of the *already installed* {@code SolutionKit}.
         * <p>
         * The trade off here is that creating an expensive copy prevents the programmer from modifying the context meta data
         * allowing us to optimize the code in the future if needed.
         *
         * TODO: in the future return immutable or read-only Document instance.
         * </p>
         */
        @Override
        public Document getInstalledSolutionKitMetadata() {
            final Document installedSolutionKitMetadata = context.getInstalledSolutionKitMetadata();
            return installedSolutionKitMetadata != null ? (Document)installedSolutionKitMetadata.cloneNode(true) : null;
        }

        @Override
        public void setInstalledSolutionKitMetadata(final Document installedSolutionKitMetadata) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Populate all {@code SolutionKit}'s context objects, including the parent, into the specified {@code contextMap}.
     * {@code SolutionKit}'s are mapped by their {@code SolutionKit} id/guid.
     * <p/>
     * Note that the {@code contextMap} will be cleared of any existing values.
     *
     * @param contextMap                the context map to populate.  Required and cannot be {@code null}.
     * @param solutionKitsConfig        solution kit config.  Required and cannot be {@code null}.
     */
    public static void populateSolutionKitManagerContextMap(
            @NotNull final Map<String, SolutionKitManagerContext> contextMap,
            @NotNull final SolutionKitsConfig solutionKitsConfig
    ) throws MissingRequiredElementException, TooManyChildElementsException, SAXException {
        // clear any previous values
        contextMap.clear();

        final SolutionKit parentSolutionKit = solutionKitsConfig.getParentSolutionKitLoaded();
        if (parentSolutionKit != null) {
            contextMap.put(
                    parentSolutionKit.getSolutionKitGuid(),
                    getOrCreateIndividualSolutionKitManagerContext(
                            solutionKitsConfig,
                            parentSolutionKit
                    )
            );
        }
        final Collection<SolutionKit> loadedSolutionKits =  solutionKitsConfig.getLoadedSolutionKits().keySet();
        for (final SolutionKit solutionKit : loadedSolutionKits) {
            contextMap.put(
                    solutionKit.getSolutionKitGuid(),
                    getOrCreateIndividualSolutionKitManagerContext(
                            solutionKitsConfig,
                            solutionKit
                    )
            );
        }
    }

    /**
     * Retrieve specified {@code solutionKit} declared {@code SolutionKitManagerContext} or create a new one is missing or {@code null}.
     * <p/>
     * If the {@code solutionKit} doesn't have customizations or the declared {@code SolutionKitManagerContext} is {@code null},
     * then a new context is created, populated with all meta info.
     * <p/>
     * If {@code readOnly} is set to {@code true}, then the resulting context will be {@code ReadOnlySolutionKitManagerContext}.
     *
     *
     * @param solutionKitsConfig          solution kit config.  Required and cannot be {@code null}.
     * @param solutionKit                 the kit to create the context for.  Required and cannot be {@code null}.
     */
    @NotNull
    private static SolutionKitManagerContext getOrCreateIndividualSolutionKitManagerContext(
            @NotNull final SolutionKitsConfig solutionKitsConfig,
            @NotNull final SolutionKit solutionKit
    ) throws MissingRequiredElementException, TooManyChildElementsException, SAXException {
        final Map<String, Pair<SolutionKit, SolutionKitCustomization>> customizations = solutionKitsConfig.getCustomizations();
        final Pair<SolutionKit, SolutionKitCustomization> customizationPair = customizations.get(solutionKit.getSolutionKitGuid());

        SolutionKitManagerContext skContext = null;
        final SolutionKitCustomization customization = customizationPair != null ? customizationPair.right : null;
        if (customization != null) {
            final SolutionKitManagerUi customUi = customization.getCustomUi();
            skContext = customUi != null ? customUi.getContext() : null;
        }
        // if there is no context declared by the solutionKit then create a default one
        if (skContext == null) {
            // create a default SolutionKitManagerContext containing only the meta info for the skar
            skContext = new SolutionKitManagerContext();
        }
        // populate the context
        populateSolutionKitManagerContext(solutionKitsConfig, skContext, solutionKit);

        return new ReadOnlySolutionKitManagerContext(skContext);
    }

    /**
     * Populate selected {@code SolutionKit} {@code GUID}'s, including the parent, into the specified {@code selectedGuidSet}.
     * <p/>
     * Note that the {@code selectedGuidSet} will be cleared of any existing values.
     *
     * @param selectedGuidSet       the set to populate.  Required and cannot be {@code null}.
     * @param solutionKitsConfig    solution kit config.  Required and cannot be {@code null}.
     */
    public static void populateSelectedSolutionKits(
            @NotNull final Set<String> selectedGuidSet,
            @NotNull final SolutionKitsConfig solutionKitsConfig
    ) {
        selectedGuidSet.clear();

        final SolutionKit parentSolutionKit = solutionKitsConfig.getParentSolutionKitLoaded();
        if (parentSolutionKit != null) {
            selectedGuidSet.add(parentSolutionKit.getSolutionKitGuid());
        }
        final Collection<SolutionKit> selectedSolutionKits = solutionKitsConfig.getSelectedSolutionKits();
        for (final SolutionKit solutionKit : selectedSolutionKits) {
            selectedGuidSet.add(solutionKit.getSolutionKitGuid());
        }
    }
}
