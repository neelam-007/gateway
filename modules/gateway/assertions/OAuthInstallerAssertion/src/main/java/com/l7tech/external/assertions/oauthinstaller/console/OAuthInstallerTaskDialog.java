package com.l7tech.external.assertions.oauthinstaller.console;

import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.oauthinstaller.BundleInfo;
import com.l7tech.external.assertions.oauthinstaller.OAuthInstallerAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

import static com.l7tech.console.util.AdminGuiUtils.doAsyncAdmin;

public class OAuthInstallerTaskDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox oAuth10;
    private JCheckBox oAuth20;
    private JCheckBox oAuthManager;
    private JCheckBox oAuthStorage;
    private JCheckBox prefixResolutionURIsAndCheckBox;
    private JTextField ma1TextField;
    private JTextField oAuthTextField;
    private JLabel otkVersionLabel;
    private JLabel parentFolderLabel;
    private JCheckBox oAuthOvp;
    private JPanel componentsToInstallPanel;
    private long selectedFolderOid;
    private final Map<String, Pair<JCheckBox, BundleInfo>> availableBundles = new HashMap<String, Pair<JCheckBox, BundleInfo>>();

    public OAuthInstallerTaskDialog(Frame owner) {
        super(owner, "OAuth Toolkit Installer", true);
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
        ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        final TreePath selectionPath = tree.getSelectionPath();

        String folderPath = null;
        Long parentFolderOid = null;
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

        selectedFolderOid = parentFolderOid;
        parentFolderLabel.setText(folderPath);

        final OAuthInstallerAdmin admin = Registry.getDefault().getExtensionInterface(OAuthInstallerAdmin.class, null);
        try {
            final String oAuthToolkitVersion = admin.getOAuthToolkitVersion();
            otkVersionLabel.setText(oAuthToolkitVersion);

            final List<BundleInfo> allAvailableBundles = admin.getAllOtkComponents();

            componentsToInstallPanel.setLayout(new BoxLayout(componentsToInstallPanel, BoxLayout.Y_AXIS));

            for (BundleInfo bundleInfo : allAvailableBundles) {
                // create panel
                BundleComponent bundleComp = new BundleComponent(bundleInfo.getName(), bundleInfo.getDescription(), bundleInfo.getVersion());
                componentsToInstallPanel.add(bundleComp.getBundlePanel());
                componentsToInstallPanel.add(Box.createRigidArea(new Dimension(10, 10)));
                final Pair<JCheckBox, BundleInfo> checkBoxBundleInfoPair =
                        new Pair<JCheckBox, BundleInfo>(bundleComp.getInstallCheckBox(), bundleInfo);

                availableBundles.put(bundleInfo.getId(), checkBoxBundleInfoPair);
            }
        } catch (OAuthInstallerAdmin.OAuthToolkitInstallationException e) {
            DialogDisplayer.showConfirmDialog(this, "Initialization problem: " + ExceptionUtils.getMessage(e),
                    "Initialization problem", JOptionPane.WARNING_MESSAGE, JOptionPane.ERROR_MESSAGE, null);
        }
    }

    private void onOK() {
        ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);

        final List<String> bundlesToInstall = new ArrayList<String>();
        for (Map.Entry<String, Pair<JCheckBox, BundleInfo>> entry : availableBundles.entrySet()) {
            final JCheckBox checkBox = entry.getValue().left;
            if (checkBox.isSelected()) {
                bundlesToInstall.add(entry.getKey());
            }
        }

        if (bundlesToInstall.isEmpty()) {
            DialogDisplayer.showMessageDialog(this, "No components were selected for installation", "No component selected", JOptionPane.WARNING_MESSAGE, null);
            return;
        }

        final OAuthInstallerAdmin admin = Registry.getDefault().getExtensionInterface(OAuthInstallerAdmin.class, null);

        try {
            final ArrayList right = doAsyncAdmin(
                    admin,
                    OAuthInstallerTaskDialog.this,
                    "OAuth Toolkit Installation",
                    "The selected components of the OAuth toolkit are being installed.",
                    admin.installOAuthToolkit(bundlesToInstall, selectedFolderOid, oAuthTextField.getText().trim())).right();

            final StringBuilder sb = new StringBuilder();
            if (right.isEmpty()) {
                sb.append("No components were installed.");
            } else {
                sb.append("<html>Components installed: <br />");
                for (Object bundleObj : right) {
                    final String guid = bundleObj.toString();
                    final Pair<JCheckBox, BundleInfo> bundleInfo = availableBundles.get(guid);
                    sb.append("<br />");
                    sb.append(bundleInfo.right.getName());
                    sb.append(" - Version: ");
                    sb.append(bundleInfo.right.getVersion());
                }
                sb.append("<br /></html>");
            }

            final JLabel resultsLabel = new JLabel(sb.toString());

            DialogDisplayer.showMessageDialog(this, resultsLabel, "Installation Completed",
                    right.isEmpty() ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE, null);


        } catch (InterruptedException e) {
            // do nothing, user cancelled

        } catch (InvocationTargetException e) {
            DialogDisplayer.showMessageDialog(this, "Could not invoke installation on Gateway",
                    "Installation Problem",
                    JOptionPane.WARNING_MESSAGE, null);
        } catch (RuntimeException e) {
            DialogDisplayer.showMessageDialog(this, "Unexpected error occured during installation: \\n" + ExceptionUtils.getMessage(e),
                    "Installation Problem",
                    JOptionPane.WARNING_MESSAGE, null);
        } catch (OAuthInstallerAdmin.OAuthToolkitInstallationException e) {
            DialogDisplayer.showMessageDialog(this, "Error during installation: " + ExceptionUtils.getMessage(e),
                    "Installation Problem",
                    JOptionPane.WARNING_MESSAGE, null);
        }

        tree.refresh();
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public static void main(String[] args) {
        OAuthInstallerTaskDialog dialog = new OAuthInstallerTaskDialog(null);
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
