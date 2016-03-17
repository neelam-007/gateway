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
import org.xml.sax.SAXException;

import javax.swing.*;
import java.util.Map;

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
    public static void populateSolutionKitManagerContext(@NotNull final SolutionKitsConfig solutionKitsConfig,
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

                    // make context available to custom UI (as read-only); test for null b/c implementer can optionally null the context
                    SolutionKitManagerContext skContext = customUi.getContext();
                    if (skContext != null) {
                        populateSolutionKitManagerContext(settings, skContext, selectedSolutionKit);
                    }

                    // call button create logic
                    final JButton createButton = customUi.createButton(customizableButtonPanel);
                    if (createButton != null) {
                        customizableButtonPanel.add(createButton);
                    }
                }
            }
        }
    }
}
