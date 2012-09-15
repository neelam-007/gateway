package com.l7tech.external.assertions.oauthinstaller.console;

import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.oauthinstaller.OAuthInstallerAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

import static com.l7tech.console.util.AdminGuiUtils.doAsyncAdmin;

public class OAuthInstallerTaskDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox comboBox1;
    private JButton manageJDBCConnectionsButton;
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
    private long selectedFolderOid;
    private List<Pair<String,String>> allBundles;

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
        JTree tree = (JTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        final TreePath selectionPath = tree.getSelectionPath();

        final String folderPath;
        final Long parentFolderOid;
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

                final OAuthInstallerAdmin admin = Registry.getDefault().getExtensionInterface(OAuthInstallerAdmin.class, null);
                try {
                    allBundles = admin.getAllAvailableBundles();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                folderPath = null;
                parentFolderOid = null;
            }
        } else {
            folderPath = null;
            parentFolderOid = null;
        }

        //todo refactor this is not idomatic and prone for errors
        if (folderPath == null || parentFolderOid == null) {
            parentFolderLabel.setText("Please select a folder.");
            buttonOK.setEnabled(false);
        } else {
            selectedFolderOid = parentFolderOid;
            parentFolderLabel.setText(folderPath);
        }

    }

    private void onOK() {

        try {

            ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);

            List<String> bundlesToInstall = new ArrayList<String>();
            if (oAuth10.isSelected()) {
                bundlesToInstall.add("OAuth_1_0");
            }
            if (oAuth20.isSelected()) {
                bundlesToInstall.add("OAuth_2_0");
            }
            if (oAuthManager.isSelected()) {
                bundlesToInstall.add("StorageManager");
            }
            if (oAuthOvp.isSelected()) {
                bundlesToInstall.add("SecureZone_OVP");
            }
            if (oAuthStorage.isSelected()) {
                bundlesToInstall.add("SecureZone_Storage");
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
                        admin.installBundles(bundlesToInstall, selectedFolderOid, oAuthTextField.getText().trim())).right();

                final StringBuilder sb = new StringBuilder();
                if (right.isEmpty()) {
                    sb.append("No components were installed.");
                } else {
                    sb.append("Components installed: ");
                    for (Object bundleObj : right) {
                        sb.append(bundleObj.toString());
                        sb.append("\\n");
                    }
                }

                DialogDisplayer.showMessageDialog(this, "OAuth Toolkit Installation Completed", sb.toString(),
                        right.isEmpty() ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE, null);


            } catch (InterruptedException e) {
                // do nothing, user cancelled

            } catch (InvocationTargetException e) {
                DialogDisplayer.showMessageDialog(this, "Installation Problem",
                        "Could not invoke installation on Gateway",
                        JOptionPane.WARNING_MESSAGE, null);
            } catch (RuntimeException e) {
                DialogDisplayer.showMessageDialog(this, "Installation Problem",
                        "Unexpected error occured during installation: \\n" + ExceptionUtils.getMessage(e),
                        JOptionPane.WARNING_MESSAGE, null);
            }

            tree.refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
