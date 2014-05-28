package com.l7tech.console.panels.bundles;

import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SquigglyFieldUtils;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.PolicyBundleInstallerAdmin;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

import static com.l7tech.console.util.AdminGuiUtils.doAsyncAdmin;
import static com.l7tech.gateway.common.AsyncAdminMethods.JobId;
import static com.l7tech.gateway.common.admin.PolicyBundleInstallerAdmin.PolicyBundleInstallerException;
import static com.l7tech.policy.bundle.BundleInfo.getPrefixedUrlErrorMsg;
import static com.l7tech.policy.bundle.BundleMapping.EntityType.JDBC_CONNECTION;

/**
 * Provide reusable logic for Bundle Installer dialogs (e.g. handles extracting bundle information, folder selection,
 * button events, component panel and prefix logic.)
 */
public abstract class BundleInstallerDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(BundleInstallerDialog.class.getName());

    private final String installFolder;
    private final String extensionInterfaceInstanceIdentifier;
    protected final Map<String, Pair<BundleComponent, BundleInfo>> availableBundles = new HashMap<>();

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JLabel versionLabel;
    private JLabel installToLabel;
    private JScrollPane componentsToInstallScrollPane;
    private JCheckBox prefixResolutionURIsAndCheckBox;
    private SquigglyTextField installationPrefixTextField;
    private JLabel exampleRoutingUrlLabel;
    private Goid selectedFolderGoid;
    private String folderPath;

    protected JPanel customizableButtonPanel;

    public BundleInstallerDialog(@NotNull final Frame owner, @NotNull final String installFolder, @Nullable final String extensionInterfaceInstanceIdentifier) {
        super(owner, installFolder + " Installer", true);
        this.installFolder = installFolder;
        this.extensionInterfaceInstanceIdentifier = extensionInterfaceInstanceIdentifier;
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

    private void initialize() {
        final Pair<String, Goid> selectedFolderAndGoid = getSelectedFolderAndGoid();
        folderPath = selectedFolderAndGoid.left;
        selectedFolderGoid = selectedFolderAndGoid.right;

        final PolicyBundleInstallerAdmin admin = getExtensionInterface(extensionInterfaceInstanceIdentifier);
        try {
            final String version = admin.getVersion();
            versionLabel.setText(version);

            final List<BundleInfo> allAvailableBundles = admin.getAllComponents();

            JPanel componentsToInstallPanel = new JPanel();
            componentsToInstallPanel.setLayout(new BoxLayout(componentsToInstallPanel, BoxLayout.Y_AXIS));
            componentsToInstallScrollPane.getViewport().add(componentsToInstallPanel);

            for (BundleInfo bundleInfo : allAvailableBundles) {
                // create panel
                BundleComponent bundleComp = new BundleComponent(bundleInfo);
                componentsToInstallPanel.add(bundleComp.getBundlePanel());
                componentsToInstallPanel.add(Box.createRigidArea(new Dimension(10, 10)));
                final Pair<BundleComponent, BundleInfo> checkBoxBundleInfoPair = new Pair<>(bundleComp, bundleInfo);

                // initialize the bundle component's extra panel as required
                initializeExtraPanel(bundleInfo, bundleComp.getExtraPanel());

                availableBundles.put(bundleInfo.getId(), checkBoxBundleInfoPair);
            }
        } catch (PolicyBundleInstallerException e) {
            DialogDisplayer.showConfirmDialog(this, "Initialization problem: " + ExceptionUtils.getMessage(e),
                    "Initialization problem", JOptionPane.WARNING_MESSAGE, JOptionPane.ERROR_MESSAGE, null);
            logger.warning(e.getMessage());
        }

        prefixResolutionURIsAndCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableComponents();
            }
        });

        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(installationPrefixTextField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                SquigglyFieldUtils.validateSquigglyTextFieldState(installationPrefixTextField, new Functions.Unary<String, String>() {
                    @Override
                    public String call(String s) {
                        return getPrefixedUrlErrorMsg(s);
                    }
                });
            }
        }, 500);

        installationPrefixTextField.getDocument().addDocumentListener(new RunOnChangeListener(){
            @Override
            protected void run() {
                final String prefix = installationPrefixTextField.getText().trim();
                setInstallToFolderText(prefix);
                setExamplePrefixLabelText();
                DialogDisplayer.pack(BundleInstallerDialog.this);
                installationPrefixTextField.requestFocus();
            }
        });

        setInstallToFolderText(null);

        setExamplePrefixLabelText();

        enableDisableComponents();
    }

    /**
     * Get the currently selected folder and it's id. If a policy or service is selected, then the folder which
     * contains it will be returned.
     *
     * @return Pair, never null, but contents may be null. If one side is null, both are null.
     */
    public static Pair<String, Goid> getSelectedFolderAndGoid(){
        ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);

        String folderPath = null;
        Goid parentFolderGoid = null;

        if (tree != null) {
            final TreePath selectionPath = tree.getSelectionPath();
            if (selectionPath != null) {
                final Object[] path = selectionPath.getPath();

                if (path.length > 0) {
                    StringBuilder builder = new StringBuilder("");

                    // skip the root node, it is captured as /
                    RootNode rootFolder = (RootNode) path[0];
                    Goid lastParentGoid = rootFolder.getGoid();
                    for (int i = 1, pathLength = path.length; i < pathLength; i++) {
                        Object o = path[i];
                        if (o instanceof FolderNode) {
                            FolderNode folderNode = (FolderNode) o;
                            builder.append("/");
                            builder.append(folderNode.getName());
                            lastParentGoid = folderNode.getGoid();
                        }
                    }
                    builder.append("/");  // if only root node then this captures that with a single /
                    folderPath = builder.toString();
                    parentFolderGoid = lastParentGoid;
                }
            }

            if (parentFolderGoid == null) {
                parentFolderGoid = Folder.ROOT_FOLDER_ID;
                folderPath = "/";
            }
        }

        return new Pair<>(folderPath, parentFolderGoid);
    }

    protected PolicyBundleInstallerAdmin getExtensionInterface(@Nullable final String instanceIdentifier) {
        // interfaceClass and instanceIdentifier must match registered in assertion (for example see SimplePolicyBundleInstallerAssertion)
        return Registry.getDefault().getExtensionInterface(PolicyBundleInstallerAdmin.class, instanceIdentifier);
    }

    protected void initializeExtraPanel(final BundleInfo bundleInfo, final JPanel extraPanel) {
        // do nothing, override in sub class if required
    }

    protected JobId<PolicyBundleDryRunResult> adminDryRunInstall(@NotNull Collection<String> componentIds,
                                                                 @NotNull Map<String, BundleMapping> bundleMappings,
                                                                 @Nullable String installationPrefix) {
        return getExtensionInterface(extensionInterfaceInstanceIdentifier).dryRunInstall(componentIds, bundleMappings, installationPrefix);
    }

    protected JobId<ArrayList> adminInstall(@NotNull Collection<String> componentIds,
                                            @NotNull Goid folderGoid,
                                            @NotNull Map<String, BundleMapping> bundleMappings,
                                            @Nullable String installationPrefix) throws PolicyBundleInstallerException {
        return getExtensionInterface(extensionInterfaceInstanceIdentifier).install(componentIds, folderGoid, bundleMappings, installationPrefix);
    }

    private void setExamplePrefixLabelText(){
        final String prefix = installationPrefixTextField.getText().trim();
        if (prefix.isEmpty()) {
            exampleRoutingUrlLabel.setText("Example prefixed URL:");
        } else {
            String exampleUrl = "https://yourgateway.com:8443/" + prefix + "/query";
            exampleRoutingUrlLabel.setText("Example prefixed URL: " + exampleUrl);
        }
    }

    private void enableDisableComponents() {
        if (prefixResolutionURIsAndCheckBox.isSelected()) {
            installationPrefixTextField.setEnabled(true);
            exampleRoutingUrlLabel.setEnabled(true);
        } else {
            installationPrefixTextField.setEnabled(false);
            exampleRoutingUrlLabel.setEnabled(false);
        }
    }

    private void setInstallToFolderText(@Nullable final String versionPrefix) {
        if (versionPrefix == null || versionPrefix.isEmpty()) {
            installToLabel.setText(folderPath + installFolder);
        } else {
            installToLabel.setText(folderPath + installFolder + " " + versionPrefix);
        }
    }

    private void onOK() {
        final List<String> bundlesToInstall = new ArrayList<>();
        final List<BundleInfo> bundlesSelected = new ArrayList<>();
        final Map<String, BundleMapping> bundleMappings = new HashMap<>();
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

//                onOkExtraPanel();
            }
        }

        if (bundlesToInstall.isEmpty()) {
            DialogDisplayer.showMessageDialog(this, "No components were selected for installation", "No component selected", JOptionPane.WARNING_MESSAGE, null);
            return;
        }

        final String prefix;
        if (prefixResolutionURIsAndCheckBox.isSelected()) {
            final String tempPrefix = installationPrefixTextField.getText().trim();
            if (tempPrefix.isEmpty()) {
                DialogDisplayer.showMessageDialog(this, "No installation prefix was entered", "Enter Installation Prefix", JOptionPane.WARNING_MESSAGE, null);
                return;
            }
            // validate installation prefix
            final String prefixedUrlErrorMsg = getPrefixedUrlErrorMsg(tempPrefix);
            if (prefixedUrlErrorMsg != null) {
                DialogDisplayer.showMessageDialog(this, prefixedUrlErrorMsg, "Invalid installation prefix", JOptionPane.WARNING_MESSAGE, null);
                return;
            }

            prefix = tempPrefix;
        } else {
            prefix = null;
        }

        final PolicyBundleInstallerAdmin admin = getExtensionInterface(extensionInterfaceInstanceIdentifier);

        try {
            final Either<String, PolicyBundleDryRunResult> dryRunEither = doAsyncAdmin(admin,
                    BundleInstallerDialog.this,
                    "Pre Installation Check",
                    "The gateway is being checked for conflicts for the selected components",
                    adminDryRunInstall(bundlesToInstall, bundleMappings, prefix));

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
                    DialogDisplayer.pack(conflictDialog);
                    Utilities.centerOnParentWindow(conflictDialog);
                    DialogDisplayer.display(conflictDialog, new Runnable() {
                        @Override
                        public void run() {
                            if (conflictDialog.wasOKed()) {
                                try {
                                    doInstall(bundlesToInstall, bundleMappings, admin, prefix);
                                } catch (Exception e) {
                                    // this may execute after the code below completes as it's a callback
                                    handleException(e);
                                }
                            }
                        }
                    });
                    DialogDisplayer.pack(conflictDialog);
                } else {
                    doInstall(bundlesToInstall, bundleMappings, admin, prefix);
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
            logger.info("User cancelled installation.");
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
        } else if (e instanceof PolicyBundleInstallerException) {
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
                           final PolicyBundleInstallerAdmin admin,
                           @Nullable final String prefixToUse)
            throws FindException, InterruptedException, InvocationTargetException, PolicyBundleInstallerException {
        final Either<String, ArrayList> resultEither = doAsyncAdmin(
                admin,
                BundleInstallerDialog.this,
                installFolder + " Installation",
                "The selected components of the " + installFolder + " are being installed.",
                adminInstall(bundlesToInstall, selectedFolderGoid, bundleMappings, prefixToUse));

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

            final ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
            tree.refresh();
            TopComponents.getInstance().getEncapsulatedAssertionRegistry().updateEncapsulatedAssertions();
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
        BundleInstallerDialog dialog = new BundleInstallerDialog(new Frame(), "", null) {
            @Override
            protected PolicyBundleInstallerAdmin getExtensionInterface(@Nullable final String instanceIdentifier) {
                return null;
            }
        };
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

    /**
     * Called by IDEA's UI initialization when "Custom Create" is checked for any UI component.
     */
    private void createUIComponents() {
        customizableButtonPanel = new JPanel();
        customizableButtonPanel.setLayout(new BorderLayout());
    }
}
