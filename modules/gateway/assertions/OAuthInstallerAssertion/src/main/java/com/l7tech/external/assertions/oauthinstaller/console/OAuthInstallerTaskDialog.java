package com.l7tech.external.assertions.oauthinstaller.console;

import com.l7tech.console.panels.bundles.BundleComponent;
import com.l7tech.console.panels.bundles.BundleInstallerDialog;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.oauthinstaller.OAuthInstallerAdmin;
import com.l7tech.external.assertions.oauthinstaller.OAuthInstallerAssertion;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.PolicyBundleInstallerAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static com.l7tech.gateway.common.admin.PolicyBundleInstallerAdmin.PolicyBundleInstallerException;

public class OAuthInstallerTaskDialog extends BundleInstallerDialog {
    public static final String OAUTH_FOLDER = "OAuth";
    private boolean integrateApiPortal = false;

    public OAuthInstallerTaskDialog(Frame owner) {
        super(owner, OAUTH_FOLDER, null);
        setTitle("OAuth Toolkit Installer");

        JButton manageSecureZoneDatabaseButton = new JButton("Manage OTK Database");
        customizableButtonPanel.add(manageSecureZoneDatabaseButton);
        manageSecureZoneDatabaseButton.setLayout(new BorderLayout());
        manageSecureZoneDatabaseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final OAuthInstallerSecureZoneDatabaseDialog dlg = new OAuthInstallerSecureZoneDatabaseDialog(OAuthInstallerTaskDialog.this);
                dlg.pack();
                Utilities.centerOnScreen(dlg);
                DialogDisplayer.display(dlg, new Runnable() {
                    @Override
                    public void run() {
                        // refresh connections in case one was created
                        for (Map.Entry<String, Pair<BundleComponent, BundleInfo>> entry : availableBundles.entrySet()) {
                            entry.getValue().left.refreshJdbcConnections();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected PolicyBundleInstallerAdmin getExtensionInterface(@Nullable final String instanceIdentifier) {
        // interfaceClass and instanceIdentifier must match registered keys in OAuthInstallerAssertion
        return Registry.getDefault().getExtensionInterface(OAuthInstallerAdmin.class, instanceIdentifier);
    }

    @Override
    protected AsyncAdminMethods.JobId<PolicyBundleDryRunResult> adminDryRunInstall(@NotNull Collection<String> componentIds,
                                                                                   @NotNull Map<String, BundleMapping> bundleMappings,
                                                                                   @NotNull Goid folderGoid,
                                                                                   @Nullable String installationPrefix) {
        return ((OAuthInstallerAdmin) getExtensionInterface(null)).dryRunInstall(componentIds, folderGoid, bundleMappings, installationPrefix, integrateApiPortal);
    }

    @Override
    protected AsyncAdminMethods.JobId<ArrayList> adminInstall(@NotNull Collection<String> componentIds,
                                                              @NotNull Goid folderGoid,
                                                              @NotNull Map<String, BundleMapping> bundleMappings,
                                                              @Nullable String installationPrefix) throws PolicyBundleInstallerException {
        return ((OAuthInstallerAdmin) getExtensionInterface(null)).install(componentIds, folderGoid, bundleMappings, installationPrefix, integrateApiPortal);
    }

    @Override
    protected void initializeExtraPanel(final BundleInfo bundleInfo, final JPanel extraPanel) {
        if (OAuthInstallerAssertion.SECURE_ZONE_STORAGE_COMP_ID.equals(bundleInfo.getId())) {
            extraPanel.setLayout(new BoxLayout(extraPanel, BoxLayout.X_AXIS));
            JCheckBox integrateCheckBox = new JCheckBox("Integrate with the CA API Portal");
            integrateCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final JCheckBox source = (JCheckBox) e.getSource();
                    // record users current selection for easy access if installation is chosen
                    integrateApiPortal = source.isSelected();
                }
            });
            extraPanel.add(integrateCheckBox);
            extraPanel.add(Box.createHorizontalGlue());
        }
    }
}