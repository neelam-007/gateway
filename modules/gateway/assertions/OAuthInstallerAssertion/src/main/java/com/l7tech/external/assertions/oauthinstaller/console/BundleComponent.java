package com.l7tech.external.assertions.oauthinstaller.console;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;

/**
 * UI Component for showing bundle meta data
 */
public class BundleComponent extends JPanel{
    private JPanel bundlePanel;
    private JCheckBox installCheckBox;

    public BundleComponent(@NotNull String bundleName, @NotNull String bundleDesc, @NotNull String version) {

        final TitledBorder titledBorder = BorderFactory.createTitledBorder(bundleName);
        bundlePanel.setBorder(titledBorder);

        installCheckBox.setText(bundleDesc + " (v" + version + ")");
    }

    public JPanel getBundlePanel() {
        return bundlePanel;
    }

    public JCheckBox getInstallCheckBox() {
        return installCheckBox;
    }
}
