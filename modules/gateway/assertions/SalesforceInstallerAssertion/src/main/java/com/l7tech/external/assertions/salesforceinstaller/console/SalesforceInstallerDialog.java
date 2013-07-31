package com.l7tech.external.assertions.salesforceinstaller.console;

import com.l7tech.console.panels.bundles.BundleComponent;
import com.l7tech.console.panels.bundles.ConflictDisplayerDialog;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.salesforceinstaller.SalesforceInstallerAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.l7tech.console.util.AdminGuiUtils.doAsyncAdmin;
import static com.l7tech.policy.bundle.BundleMapping.EntityType.JDBC_CONNECTION;

public class SalesforceInstallerDialog extends JDialog {
    public static final String SALESFORCE_FOLDER = "Required for Salesforce";
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JLabel otkVersionLabel;
    private JPanel componentsToInstallPanel;
    private final Map<String, Pair<BundleComponent, BundleInfo>> availableBundles = new HashMap<String, Pair<BundleComponent, BundleInfo>>();
    private static final Logger logger = Logger.getLogger(SalesforceInstallerDialog.class.getName());

    public SalesforceInstallerDialog(Frame owner) {
        super(owner, SALESFORCE_FOLDER + " Toolkit Installer", true);
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        initialize();
    }

    private void initialize(){
        final SalesforceInstallerAdmin admin = Registry.getDefault().getExtensionInterface(SalesforceInstallerAdmin.class, null);
        try {
            final String version = admin.getVersion();
            otkVersionLabel.setText(version);

            final List<BundleInfo> allAvailableBundles = admin.getAllComponents();

            componentsToInstallPanel.setLayout(new BoxLayout(componentsToInstallPanel, BoxLayout.Y_AXIS));

            for (BundleInfo bundleInfo : allAvailableBundles) {
                // create panel
                BundleComponent bundleComp = new BundleComponent(bundleInfo);
                componentsToInstallPanel.add(bundleComp.getBundlePanel());
                componentsToInstallPanel.add(Box.createRigidArea(new Dimension(10, 10)));
                final Pair<BundleComponent, BundleInfo> checkBoxBundleInfoPair =
                        new Pair<BundleComponent, BundleInfo>(bundleComp, bundleInfo);

                availableBundles.put(bundleInfo.getId(), checkBoxBundleInfoPair);
            }
        } catch (SalesforceInstallerAdmin.SalesforceInstallationException e) {
            DialogDisplayer.showConfirmDialog(this, "Initialization problem: " + ExceptionUtils.getMessage(e),
                    "Initialization problem", JOptionPane.WARNING_MESSAGE, JOptionPane.ERROR_MESSAGE, null);
            logger.warning(e.getMessage());
        }

        enableDisableComponents();
    }


    private void enableDisableComponents() {
        // nothing to change now
    }

    private void onOK() {

        final List<String> bundlesToInstall = new ArrayList<String>();
        final List<BundleInfo> bundlesSelected = new ArrayList<BundleInfo>();
        final Map<String, BundleMapping> bundleMappings = new HashMap<String, BundleMapping>();
        for (Map.Entry<String, Pair<BundleComponent, BundleInfo>> entry : availableBundles.entrySet()) {
            final BundleComponent bundleComponent = entry.getValue().left;
            if (bundleComponent.getInstallCheckBox().isSelected()) {
                final String componentId = entry.getKey();
                bundlesSelected.add(entry.getValue().right);
                bundlesToInstall.add(componentId);
                // does it contain any mappings?
                final Map<String, String> mappedJdbcConnections = bundleComponent.getMappedJdbcConnections();
                final BundleMapping bundleMapping = new BundleMapping();
                for (Map.Entry<String, String> mappedEntry : mappedJdbcConnections.entrySet()) {
                    bundleMapping.addMapping(JDBC_CONNECTION, mappedEntry.getKey(), mappedEntry.getValue());
                }
                bundleMappings.put(componentId, bundleMapping);
            }
        }

        if (bundlesToInstall.isEmpty()) {
            DialogDisplayer.showMessageDialog(this, "No components were selected for installation", "No component selected", JOptionPane.WARNING_MESSAGE, null);
            return;
        }

        final SalesforceInstallerAdmin admin = Registry.getDefault().getExtensionInterface(SalesforceInstallerAdmin.class, null);

        try {
            final Either<String, PolicyBundleDryRunResult> dryRunEither = doAsyncAdmin(admin,
                    SalesforceInstallerDialog.this,
                    "Toolkit Pre Installation Check",
                    "The gateway is being checked for conflicts for the selected components",
                    admin.dryRunInstall(bundlesToInstall, bundleMappings, null));

            if (dryRunEither.isRight()) {
                boolean areConflicts = false;
                final PolicyBundleDryRunResult dryRunResult = dryRunEither.right();
                for (String bundleId : bundlesToInstall) {
                    try {
                        if (dryRunResult.anyConflictsForBundle(bundleId)) {
                            areConflicts = true;
                        }
                    } catch (PolicyBundleDryRunResult.UnknownBundleIdException e) {
                        DialogDisplayer.showMessageDialog(this, "Could not check for conflicts: " + e.getMessage(),
                                "Pre check installation problem",
                                JOptionPane.WARNING_MESSAGE, null);
                        logger.warning(e.getMessage());
                        return;
                    }
                }

                if (areConflicts) {
                    final ConflictDisplayerDialog conflictDialog = new ConflictDisplayerDialog(this, bundlesSelected, dryRunResult);
                    conflictDialog.pack();
                    Utilities.centerOnParentWindow(conflictDialog);
                    DialogDisplayer.display(conflictDialog, new Runnable() {
                        @Override
                        public void run() {
                            if (conflictDialog.wasOKed()) {
                                try {
                                    doInstall(bundlesToInstall, bundleMappings, admin, null);
                                } catch (Exception e) {
                                    // this may execute after the code below completes as it's a callback
                                    handleException(e);
                                }
                            }
                        }
                    });
                } else {
                    doInstall(bundlesToInstall, bundleMappings, admin, null);
                }
            } else {
                // error occurred
                DialogDisplayer.showMessageDialog(this, dryRunEither.left(),
                        "Pre installation check problem",
                        JOptionPane.WARNING_MESSAGE, null);
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void handleException(Exception e) {
        if (e instanceof InterruptedException) {
            // do nothing, user cancelled
            logger.info("User cancelled installation of the Toolkit.");
        } else if (e instanceof InvocationTargetException) {
            DialogDisplayer.showMessageDialog(this, "Could not invoke installation on Gateway",
                    "Installation Problem",
                    JOptionPane.WARNING_MESSAGE, null);
            logger.warning(e.getMessage());
        } else if (e instanceof RuntimeException) {
            DialogDisplayer.showMessageDialog(this, "Unexpected error occurred during installation: \n" + ExceptionUtils.getMessage(e),
                    "Installation Problem",
                    JOptionPane.WARNING_MESSAGE, null);
            logger.warning(e.getMessage());
        } else if (e instanceof SalesforceInstallerAdmin.SalesforceInstallationException) {
            DialogDisplayer.showMessageDialog(this, "Error during installation: " + ExceptionUtils.getMessage(e),
                    "Installation Problem",
                    JOptionPane.WARNING_MESSAGE, null);
            logger.warning(e.getMessage());
        } else {
            DialogDisplayer.showMessageDialog(this, "Unexpected error occurred during installation: \n" + ExceptionUtils.getMessage(e),
                    "Installation Problem",
                    JOptionPane.WARNING_MESSAGE, null);
            logger.warning(e.getMessage());
        }
    }

    private void doInstall(final List<String> bundlesToInstall,
                           final Map<String, BundleMapping> bundleMappings,
                           final SalesforceInstallerAdmin admin,
                           @Nullable final String prefixToUse)
            throws InterruptedException, InvocationTargetException, SalesforceInstallerAdmin.SalesforceInstallationException {
        final Either<String, ArrayList> resultEither = doAsyncAdmin(
                admin,
                SalesforceInstallerDialog.this,
                SALESFORCE_FOLDER + " Toolkit Installation",
                "The selected components of the " + SALESFORCE_FOLDER + " toolkit are being installed.",
                admin.install(
                        bundlesToInstall,
                        bundleMappings,
                        prefixToUse));

        if (resultEither.isRight()) {
            final ArrayList right = resultEither.right();

            final StringBuilder sb = new StringBuilder();
            if (right.isEmpty()) {
                sb.append("No components were installed.");
            } else {
                sb.append("<html>Components installed: <br />");
                for (Object bundleObj : right) {
                    final String guid = bundleObj.toString();
                    final Pair<BundleComponent, BundleInfo> bundleInfo = availableBundles.get(guid);
                    sb.append("<br />");
                    sb.append(bundleInfo.right.getName());
                    sb.append(" - Version: ");
                    sb.append(bundleInfo.right.getVersion());
                }
                sb.append("<br /></html>");
            }

            final JLabel resultsLabel = new JLabel(sb.toString());

            DialogDisplayer.showMessageDialog(this, resultsLabel, "Installation completed",
                    right.isEmpty() ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE, null);

            final ServicesAndPoliciesTree tree =
                    (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
            tree.refresh();
            dispose();

        } else {
            // error occurred
            DialogDisplayer.showMessageDialog(this, resultEither.left(),
                    "Installation problem",
                    JOptionPane.WARNING_MESSAGE, null);
        }
    }

    private void onCancel() {
        final ServicesAndPoliciesTree tree =
                (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        tree.refresh();
        dispose();
    }

    public static void main(String[] args) {
        SalesforceInstallerDialog dialog = new SalesforceInstallerDialog(null);
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
