package com.l7tech.console.panels.bundles;

import com.l7tech.console.panels.JdbcConnectionPropertiesDialog;
import com.l7tech.console.panels.NewPrivateKeyDialog;
import com.l7tech.console.panels.SecurePasswordComboBox;
import com.l7tech.console.panels.SecurePasswordPropertiesDialog;
import com.l7tech.console.util.ActiveKeypairJob;
import com.l7tech.console.util.KeystoreComboEntry;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.console.panels.bundles.BundleConflictComponent.ERROR_ACTION_NEW_OR_UPDATE;
import static com.l7tech.console.panels.bundles.BundleConflictComponent.ERROR_TYPE_TARGET_EXISTS;
import static com.l7tech.console.panels.bundles.BundleConflictComponent.ERROR_TYPE_TARGET_NOT_FOUND;
import static com.l7tech.console.util.AdminGuiUtils.doAsyncAdmin;

/**
 *  A class holds a panel containing migration target details such as
 *  (1) entity ID
 *  (2) entity name, and
 *  (3) creating a new entity if error type is "Target Not Found", or
 *      choosing an option to set "Use Existing" or "Update" if error type is "Target Exists"
 */
public class MigrationEntityDetailPanel {
    private static final Logger logger = Logger.getLogger(MigrationEntityDetailPanel.class.getName());

    private JPanel contentPane;
    private JLabel nameLabel;
    private JLabel idLabel;
    private JRadioButton useExistRadioButton;
    private JRadioButton updateRadioButton;
    private JButton createEntityButton;
    private JComboBox entitiesComboBox;
    private String targetType;

    private JDialog parent;
    private Registry registry = Registry.getDefault();

    public MigrationEntityDetailPanel(final JDialog parent, final String errorType, final String targetType, final String name,
                                      final String id, final Map<String, String> selectedMigrationResolutions) {
        this.targetType = targetType;
        this.parent = parent;

        $$$setupUI$$$();

        nameLabel.setText(name);
        idLabel.setText(id);

        useExistRadioButton.setVisible(ERROR_TYPE_TARGET_EXISTS.equals(errorType));
        updateRadioButton.setVisible(ERROR_TYPE_TARGET_EXISTS.equals(errorType));

        entitiesComboBox.setVisible(ERROR_TYPE_TARGET_NOT_FOUND.equals(errorType));
        createEntityButton.setVisible(ERROR_TYPE_TARGET_NOT_FOUND.equals(errorType));

        useExistRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (useExistRadioButton.isSelected()) {
                    selectedMigrationResolutions.remove(idLabel.getText());
                }
            }
        });

        updateRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (updateRadioButton.isSelected()) {
                    selectedMigrationResolutions.put(idLabel.getText(), ERROR_ACTION_NEW_OR_UPDATE);
                }
            }
        });

        entitiesComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (EntityType.JDBC_CONNECTION.toString().equals(targetType)) {
                    String selectedJdbcConnName = (String) entitiesComboBox.getSelectedItem();
                    JdbcAdmin admin = getJdbcConnectionAdmin();
                    if (admin == null) return;

                    try {
                        selectedMigrationResolutions.put(idLabel.getText(), admin.getJdbcConnection(selectedJdbcConnName).getGoid().toString());
                    } catch (FindException e1) {
                        showErrorMessage("Error Resolving Migration Issues", "Cannot find a JDBC Connection.", e1, null);

                    }
                } else if (EntityType.SSG_KEY_ENTRY.toString().equals(targetType)) {
                    final KeystoreComboEntry keystoreEntry = (KeystoreComboEntry) entitiesComboBox.getSelectedItem();

                    selectedMigrationResolutions.put(idLabel.getText(), keystoreEntry.getKeystoreid().toString() + ":" + keystoreEntry.getAlias());
                } else if (EntityType.SECURE_PASSWORD.toString().equals(targetType)) {
                    final SecurePassword securePassword = ((SecurePasswordComboBox) entitiesComboBox).getSelectedSecurePassword();

                    if (securePassword != null) {
                        selectedMigrationResolutions.put(idLabel.getText(), securePassword.getGoid().toString());
                    }
                }
            }
        });

        if (ERROR_TYPE_TARGET_NOT_FOUND.equals(errorType)) {
            initializeEntityComponents();
        }
    }

    /**
     * Do not remove this empty method.
     *
     * Since targetType must be initialized before the method createUIComponents() is called. As long as $$$setupUI$$$ is
     * defined and called, then targetType will be initialized in the constructor before createUIComponents() is called.
      */
    private void $$$setupUI$$$() {
    }

    private void createUIComponents() {
        if (EntityType.SECURE_PASSWORD.toString().equals(targetType)) {
            entitiesComboBox = new SecurePasswordComboBox();
        } else {
            entitiesComboBox = new JComboBox();
        }
    }

    public JPanel getContentPane() {
        return contentPane;
    }

    private void initializeEntityComponents() {
        if (targetType == null || targetType.trim().isEmpty()) return;

        populateEntitiesList();
        initializeCreatingEntityButtons();
    }

    private void populateEntitiesList() {
        if (EntityType.JDBC_CONNECTION.toString().equals(targetType)) {
            populateJdbcConnectionComboBox();
        } else if (EntityType.SSG_KEY_ENTRY.toString().equals(targetType)) {
            populatePrivateKeyComboBox();
        } else if (EntityType.SECURE_PASSWORD.toString().equals(targetType)) {
            populateSecurePasswordComboBox();
        }
    }

    private void populateJdbcConnectionComboBox() {
        final Object selectedItem = entitiesComboBox.getSelectedItem();
        java.util.List<String> connNameList;
        JdbcAdmin admin = getJdbcConnectionAdmin();
        if (admin == null) return;
        else {
            try {
                connNameList = admin.getAllJdbcConnectionNames();
            } catch (FindException e) {
                showErrorMessage("Error Resolving Migration Issues", "Cannot get JDBC Connection names.", e, null);
                return;
            }
        }

        // Sort all default driver classes
        Collections.sort(connNameList);

        // Add all items into the combox box.
        entitiesComboBox.removeAllItems();
        for (String name: connNameList) {
            entitiesComboBox.addItem(name);
        }

        if (selectedItem != null && entitiesComboBox.getModel().getSize() > 0) {
            entitiesComboBox.setSelectedItem(selectedItem);
            if (entitiesComboBox.getSelectedIndex() == -1) {
                entitiesComboBox.setSelectedIndex(0);
            }
        }
    }

    private void populatePrivateKeyComboBox() {
        TrustedCertAdmin trustedCertAdmin = getTrustedCertAdmin();
        if (trustedCertAdmin == null) return;

        try {
            final java.util.List<KeystoreFileEntityHeader> keystores = trustedCertAdmin.findAllKeystores(true);
            if ( keystores != null ) {
                final java.util.List<KeystoreComboEntry> comboEntries = new ArrayList<>();
                final KeystoreComboEntry previousSelection = (KeystoreComboEntry) entitiesComboBox.getSelectedItem();
                for ( final KeystoreFileEntityHeader header : keystores ) {
                    for ( final SsgKeyEntry entry : trustedCertAdmin.findAllKeys(header.getGoid(), true) ) {
                        final KeystoreComboEntry KeystoreComboEntry = new KeystoreComboEntry(header.getGoid(), header.getName(), entry.getAlias());
                        comboEntries.add(KeystoreComboEntry);
                    }
                }

                Collections.sort(comboEntries);

                entitiesComboBox.setModel(new DefaultComboBoxModel(comboEntries.toArray()));

                if ( previousSelection != null && comboEntries.contains(previousSelection) ) {
                    entitiesComboBox.setSelectedItem( previousSelection );
                } else {
                    entitiesComboBox.setSelectedIndex(0);
                }
            }
        } catch (Exception e) {
            showErrorMessage("Error Resolving Migration Issues", "Problem populating keystore info", e, null);
        }
    }

    private void populateSecurePasswordComboBox() {
        if (entitiesComboBox instanceof SecurePasswordComboBox) {
            ((SecurePasswordComboBox)entitiesComboBox).reloadPasswordList();
        }
    }

    private void initializeCreatingEntityButtons() {
        if (EntityType.JDBC_CONNECTION.toString().equals(targetType)) {
            createEntityButton.setText("Create JDBC Connection");
            createEntityButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    createJdbcConnection(new JdbcConnection());
                }
            });
        } else if (EntityType.SSG_KEY_ENTRY.toString().equals(targetType)) {
            createEntityButton.setText("Create Private Key");
            createEntityButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    createPrivateKey();
                }
            });
        } else if (EntityType.SECURE_PASSWORD.toString().equals(targetType)) {
            createEntityButton.setText("Create Stored Password");
            createEntityButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    createSecurePassword(new SecurePassword());
                }
            });
        } else {
            showErrorMessage("Error Resolving Migration Issues", "Initialize Target Entity Error: Invalid target type: " + targetType, null, null);
        }
    }

    private void createJdbcConnection(final JdbcConnection connection) {
        final JdbcConnectionPropertiesDialog jdbcConnPropDialog = new JdbcConnectionPropertiesDialog(parent, connection);
        jdbcConnPropDialog.pack();
        Utilities.centerOnParentWindow(jdbcConnPropDialog);
        DialogDisplayer.display(jdbcConnPropDialog, new Runnable() {
            @Override
            public void run() {
                if (jdbcConnPropDialog.isConfirmed()) {
                    Runnable redo = new Runnable() {
                        public void run() {
                            createJdbcConnection(connection);
                        }
                    };
                    // Save the connection
                    JdbcAdmin admin = getJdbcConnectionAdmin();
                    if (admin == null) return;
                    try {
                        admin.saveJdbcConnection(connection);
                    } catch (UpdateException e) {
                        showErrorMessage("Error Resolving Migration Issues", "Failed to save a JDBC connection: " + ExceptionUtils.getMessage(e), e, redo);
                        return;
                    }
                    // refresh the list
                    populateJdbcConnectionComboBox();

                    entitiesComboBox.setSelectedItem(connection.getName());
                }
            }
        });
    }

    private void createPrivateKey() {
        TrustedCertAdmin trustedCertAdmin = getTrustedCertAdmin();
        if (trustedCertAdmin == null) return;

        KeystoreFileEntityHeader mutableKeystore = null;
        try {
            for (KeystoreFileEntityHeader keystore : trustedCertAdmin.findAllKeystores(true)) {
                if (!keystore.isReadonly()) {
                    mutableKeystore = keystore;
                    break;
                }
            }
        } catch (Exception e) {
            showErrorMessage("Error Resolving Migration Issues", "Cannot retrieve key stores", e, null);
            return;
        }

        final NewPrivateKeyDialog privateKeyDialog = new NewPrivateKeyDialog(parent, mutableKeystore);
        privateKeyDialog.setModal(true);
        privateKeyDialog.pack();
        Utilities.centerOnParentWindow(privateKeyDialog);
        DialogDisplayer.display(privateKeyDialog, new Runnable() {
            @Override
            public void run() {
                if (privateKeyDialog.isConfirmed()) {
                    final ActiveKeypairJob activeKeypairJob = TopComponents.getInstance().getActiveKeypairJob();
                    activeKeypairJob.setKeypairJobId(privateKeyDialog.getKeypairJobId());
                    activeKeypairJob.setActiveKeypairJobAlias(privateKeyDialog.getNewAlias());

                    int minPoll = (privateKeyDialog.getSecondsToWaitForJobToFinish() * 1000) / 30;
                    if (minPoll < ActiveKeypairJob.DEFAULT_POLL_INTERVAL)
                        minPoll = ActiveKeypairJob.DEFAULT_POLL_INTERVAL;
                    activeKeypairJob.setMinJobPollInterval(minPoll);

                    populatePrivateKeyComboBox();

                    for (int i = 0; i < entitiesComboBox.getItemCount(); i++) {
                        KeystoreComboEntry entry = (KeystoreComboEntry) entitiesComboBox.getItemAt(i);
                        if (entry.getAlias().equals(privateKeyDialog.getNewAlias())) {
                            entitiesComboBox.setSelectedItem(entry);
                            break;
                        }
                    }
                }
            }
        });
    }

    private void createSecurePassword(final SecurePassword securePassword) {
        final SecurePasswordPropertiesDialog passwordPropertiesDialog = new SecurePasswordPropertiesDialog(parent, securePassword, false, true);
        passwordPropertiesDialog.pack();
        Utilities.centerOnParentWindow(passwordPropertiesDialog);
        DialogDisplayer.display(passwordPropertiesDialog, new Runnable() {
            @Override
            public void run() {
                if (passwordPropertiesDialog.isConfirmed()) {
                    Runnable redo = new Runnable() {
                        public void run() {
                            createSecurePassword(securePassword);
                        }
                    };
                    // Save the secure password
                    TrustedCertAdmin admin = getTrustedCertAdmin();
                    if (admin == null) return;

                    final Goid savedId;
                    try {
                        savedId = admin.saveSecurePassword(securePassword);

                        // Update password field, if necessary
                        char[] newpass = passwordPropertiesDialog.getEnteredPassword();
                        if (newpass != null) admin.setSecurePassword(savedId, newpass);

                        int keybits = passwordPropertiesDialog.getGenerateKeybits();
                        if (keybits > 0) {
                            doAsyncAdmin(admin, parent, "Generating Key", "Generating PEM private key ...", admin.setGeneratedSecurePassword(savedId, keybits));
                        }
                    } catch (Exception e) {
                        showErrorMessage("Error Resolving Migration Issues", "Failed to save a secure password: " + ExceptionUtils.getMessage(e), e, redo);
                        return;
                    }
                    // refresh the list
                    populateSecurePasswordComboBox();

                    ((SecurePasswordComboBox) entitiesComboBox).setSelectedSecurePassword(savedId);
                }
            }
        });
    }

    private void showErrorMessage(String title, String msg, @Nullable Throwable e, @Nullable Runnable continuation) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(parent, msg, title, JOptionPane.ERROR_MESSAGE, continuation);
    }

    private JdbcAdmin getJdbcConnectionAdmin() {
        if (! registry.isAdminContextPresent()) {
            logger.warning("Cannot get JDBC Connection Admin due to no Admin Context present.");
            return null;
        }
        return registry.getJdbcConnectionAdmin();
    }

    private TrustedCertAdmin getTrustedCertAdmin() {
        if (! registry.isAdminContextPresent()) {
            logger.warning("Cannot get Trusted Cert Manager due to no Admin Context present.");
            return null;
        }
        return registry.getTrustedCertManager();
    }
}