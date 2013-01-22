package com.l7tech.external.assertions.oauthinstaller.console;

import com.l7tech.console.panels.bundles.BundleComponent;
import com.l7tech.console.panels.bundles.ConflictDisplayerDialog;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SquigglyFieldUtils;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.oauthinstaller.OAuthInstallerAssertion;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.external.assertions.oauthinstaller.OAuthInstallerAdmin;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.util.*;
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
import static com.l7tech.policy.bundle.BundleInfo.getPrefixedUrlErrorMsg;
import static com.l7tech.policy.bundle.BundleMapping.EntityType.JDBC_CONNECTION;

public class OAuthInstallerTaskDialog extends JDialog {
    public static final String OAUTH_FOLDER = "OAuth";
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox oAuth10;
    private JCheckBox oAuth20;
    private JCheckBox oAuthManager;
    private JCheckBox oAuthStorage;
    private JCheckBox prefixResolutionURIsAndCheckBox;
    private SquigglyTextField installationPrefixTextField;
    private JLabel otkVersionLabel;
    private JLabel installToLabel;
    private JCheckBox oAuthOvp;
    private JPanel componentsToInstallPanel;
    private JLabel exampleRoutingUrlLabel;
    private JButton manageSecureZoneDatabaseButton;
    private long selectedFolderOid;
    private final Map<String, Pair<BundleComponent, BundleInfo>> availableBundles = new HashMap<String, Pair<BundleComponent, BundleInfo>>();
    private static final Logger logger = Logger.getLogger(OAuthInstallerTaskDialog.class.getName());
    private String folderPath;
    private boolean integrateApiPortal = false;
    private final ActionListener integrateApiListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            final JCheckBox source = (JCheckBox) e.getSource();
            // record users current selection for easy access if installation is chosen
            integrateApiPortal = source.isSelected();
        }
    };


    public OAuthInstallerTaskDialog(Frame owner) {
        super(owner, OAUTH_FOLDER + " Toolkit Installer", true);
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

    /**
     * Get the currently selected folder and it's id. If a policy or service is selected, then the folder which
     * contains it will be returned.
     *
     * @return Pair, never null, but contents may be null. If one side is null, both are null.
     */
    public static Pair<String, Long> getSelectedFolderAndOid(){
        ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);

        String folderPath = null;
        Long parentFolderOid = null;

        if (tree != null) {
            final TreePath selectionPath = tree.getSelectionPath();
            if (selectionPath != null) {
                final Object[] path = selectionPath.getPath();

                if (path.length > 0) {
                    StringBuilder builder = new StringBuilder("");

                    // skip the root node, it is captured as /
                    RootNode rootFolder = (RootNode) path[0];
                    long lastParentOid = rootFolder.getOid();
                    for (int i = 1, pathLength = path.length; i < pathLength; i++) {
                        Object o = path[i];
                        if (o instanceof FolderNode) {
                            FolderNode folderNode = (FolderNode) o;
                            builder.append("/");
                            builder.append(folderNode.getName());
                            lastParentOid = folderNode.getOid();
                        }
                    }
                    builder.append("/");  // if only root node then this captures that with a single /
                    folderPath = builder.toString();
                    parentFolderOid = lastParentOid;
                }
            }

            if (parentFolderOid == null) {
                final RootNode rootNode = tree.getRootNode();
                parentFolderOid = rootNode.getOid();
                folderPath = "/";
            }
        }
        return new Pair<String, Long>(folderPath, parentFolderOid);
    }

    private void initialize(){
        final Pair<String, Long> selectedFolderAndOid = getSelectedFolderAndOid();
        folderPath = selectedFolderAndOid.left;
        selectedFolderOid = selectedFolderAndOid.right;

        final OAuthInstallerAdmin admin = Registry.getDefault().getExtensionInterface(OAuthInstallerAdmin.class, null);
        try {
            final String oAuthToolkitVersion = admin.getOAuthToolkitVersion();
            otkVersionLabel.setText(oAuthToolkitVersion);

            final List<BundleInfo> allAvailableBundles = admin.getAllOtkComponents();

            componentsToInstallPanel.setLayout(new BoxLayout(componentsToInstallPanel, BoxLayout.Y_AXIS));

            for (BundleInfo bundleInfo : allAvailableBundles) {
                // create panel
                BundleComponent bundleComp = new BundleComponent(bundleInfo);
                componentsToInstallPanel.add(bundleComp.getBundlePanel());
                if (OAuthInstallerAssertion.SECURE_ZONE_STORAGE_COMP_ID.equals(bundleInfo.getId())) {
                    // Secure Zone Storage - add in API Portal Integration
                    final JPanel extraPanel = bundleComp.getExtraPanel();
                    extraPanel.setLayout(new BoxLayout(extraPanel, BoxLayout.X_AXIS));
                    JCheckBox integrateCheckBox = new JCheckBox("Integrate with API Portal version 2.1 or 2.2");
                    integrateCheckBox.addActionListener(integrateApiListener);
                    extraPanel.add(integrateCheckBox);
                    extraPanel.add(Box.createHorizontalGlue());
                }
                componentsToInstallPanel.add(Box.createRigidArea(new Dimension(10, 10)));
                final Pair<BundleComponent, BundleInfo> checkBoxBundleInfoPair =
                        new Pair<BundleComponent, BundleInfo>(bundleComp, bundleInfo);

                availableBundles.put(bundleInfo.getId(), checkBoxBundleInfoPair);
            }
        } catch (OAuthInstallerAdmin.OAuthToolkitInstallationException e) {
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
            }
        });

        setInstallToFolderText(null);

        setExamplePrefixLabelText();

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
        enableDisableComponents();
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
            installToLabel.setText(folderPath + OAUTH_FOLDER);
        } else {
            installToLabel.setText(folderPath + OAUTH_FOLDER + " " + versionPrefix);
        }
    }

    private void onOK() {

        final List<String> bundlesToInstall = new ArrayList<String>();
        final List<BundleInfo> bundlesSelected = new ArrayList<BundleInfo>();
        final Map<String, BundleMapping> bundleMappings = new HashMap<String, BundleMapping>();
        boolean doApiIntegration = false;
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
                if (OAuthInstallerAssertion.SECURE_ZONE_STORAGE_COMP_ID.equals(componentId)) {
                    doApiIntegration = integrateApiPortal;
                }
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

        final OAuthInstallerAdmin admin = Registry.getDefault().getExtensionInterface(OAuthInstallerAdmin.class, null);

        try {
            final Either<String, PolicyBundleDryRunResult> dryRunEither = doAsyncAdmin(admin,
                    OAuthInstallerTaskDialog.this,
                    "OAuth Toolkit Pre Installation Check",
                    "The gateway is being checked for conflicts for the selected components",
                    admin.dryRunOtkInstall(bundlesToInstall, bundleMappings, prefix, doApiIntegration));

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
                    final boolean doApiIntegrationFinal = doApiIntegration;
                    DialogDisplayer.display(conflictDialog, new Runnable() {
                        @Override
                        public void run() {
                            if (conflictDialog.wasOKed()) {
                                try {
                                    doInstall(bundlesToInstall, bundleMappings, admin, prefix, doApiIntegrationFinal);
                                } catch (Exception e) {
                                    // this may execute after the code below completes as it's a callback
                                    handleException(e);
                                }
                            }
                        }
                    });
                } else {
                    doInstall(bundlesToInstall, bundleMappings, admin, prefix, doApiIntegration);
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
            logger.info("User cancelled installation of the OAuth Toolkit.");
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
        } else if (e instanceof OAuthInstallerAdmin.OAuthToolkitInstallationException) {
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
                           final OAuthInstallerAdmin admin,
                           final String prefixToUse,
                           final boolean doApiIntegration)
            throws InterruptedException, InvocationTargetException, OAuthInstallerAdmin.OAuthToolkitInstallationException {
        final Either<String, ArrayList> resultEither = doAsyncAdmin(
                admin,
                OAuthInstallerTaskDialog.this,
                OAUTH_FOLDER + " Toolkit Installation",
                "The selected components of the " + OAUTH_FOLDER + " toolkit are being installed.",
                admin.installOAuthToolkit(
                        bundlesToInstall,
                        selectedFolderOid,
                        bundleMappings,
                        prefixToUse,
                        doApiIntegration));

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
        OAuthInstallerTaskDialog dialog = new OAuthInstallerTaskDialog(null);
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
