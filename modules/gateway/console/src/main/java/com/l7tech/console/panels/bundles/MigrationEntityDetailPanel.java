package com.l7tech.console.panels.bundles;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.action.ManageJdbcConnectionsAction;
import com.l7tech.console.action.ManagePrivateKeysAction;
import com.l7tech.console.action.ManageSecurePasswordsAction;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.panels.SecurePasswordComboBox;
import com.l7tech.console.panels.policydiff.PolicyDiffWindow;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.*;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.console.panels.bundles.ConflictDisplayerDialog.ErrorType.EntityDeleted;
import static com.l7tech.console.panels.bundles.ConflictDisplayerDialog.ErrorType.TargetExists;
import static com.l7tech.console.panels.bundles.ConflictDisplayerDialog.ErrorType.TargetNotFound;
import static com.l7tech.console.panels.bundles.ConflictDisplayerDialog.MAPPING_TARGET_ID_ATTRIBUTE;
import static com.l7tech.console.panels.bundles.ConflictDisplayerDialog.MappingAction.*;
import static com.l7tech.objectmodel.EntityType.*;

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
    private JButton manageEntityButton;
    private JComboBox entitiesComboBox;
    private JRadioButton createRadioButton;
    private JButton compareEntityButton;
    private JCheckBox deleteEntityCheckBox;
    private EntityType targetType;
    private String policyResourceXml;
    private String existingEntityXml;

    private JDialog parent;
    private Registry registry = Registry.getDefault();

    public MigrationEntityDetailPanel(final JDialog parent, final ConflictDisplayerDialog.ErrorType errorType,
                                      final EntityType targetType, final String name, final String id, final boolean versionModified, final String policyResourceXml,
                                      final Map<String, Pair<ConflictDisplayerDialog.MappingAction, Properties>> selectedMigrationResolutions,
                                      @NotNull final JButton[] selectAllButtons) {
        this.targetType = targetType;
        this.parent = parent;
        this.policyResourceXml = policyResourceXml;

        $$$setupUI$$$();

        nameLabel.setText(name);
        idLabel.setText(id);

        useExistRadioButton.setVisible(errorType == TargetExists);
        updateRadioButton.setVisible(errorType == TargetExists);
        createRadioButton.setVisible(errorType == TargetExists);

        compareEntityButton.setEnabled(compareEntityEnabled(errorType, id));
        compareEntityButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    new PolicyDiffWindow(
                        new Pair<>("Existing " + targetType.toString().toLowerCase() + ": " + name, new PolicyTreeModel(WspReader.getDefault().parsePermissively(getExistingEntityXml(id), WspReader.Visibility.includeDisabled))),
                        new Pair<>("Updated " + targetType.toString().toLowerCase() + ": " + name, new PolicyTreeModel(WspReader.getDefault().parsePermissively(policyResourceXml, WspReader.Visibility.includeDisabled)))
                    ).setVisible(true);
                } catch (IOException ioe) {
                    DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), "Cannot parse the policy XML", "Policy Comparison Error", JOptionPane.WARNING_MESSAGE, null);
                }
            }
        });

        entitiesComboBox.setVisible(errorType == TargetNotFound);
        manageEntityButton.setVisible(errorType == TargetNotFound);
        deleteEntityCheckBox.setVisible(errorType == EntityDeleted);

        // default to "Use Existing", unless there's a version modifier (prefix) default to "Create New"
        if (versionModified) {
            createRadioButton.setSelected(true);
            selectedMigrationResolutions.put(idLabel.getText(), new Pair<>(AlwaysCreateNew, (Properties) null));
        } else {
            useExistRadioButton.setSelected(true);
            selectedMigrationResolutions.put(idLabel.getText(), new Pair<>(NewOrExisting, (Properties) null));
        }

        useExistRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (useExistRadioButton.isSelected()) {
                    selectedMigrationResolutions.put(idLabel.getText(), new Pair<>(NewOrExisting, (Properties) null));
                }
            }
        });

        updateRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (updateRadioButton.isSelected()) {
                    selectedMigrationResolutions.put(idLabel.getText(), new Pair<>(NewOrUpdate, (Properties) null));
                }
            }
        });

        createRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (createRadioButton.isSelected()) {
                    selectedMigrationResolutions.put(idLabel.getText(), new Pair<>(AlwaysCreateNew, (Properties) null));
                }
            }
        });

        entitiesComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (entitiesComboBox.getItemCount() == 0) return;

                final Properties properties = new Properties();
                if (targetType == JDBC_CONNECTION) {
                    String selectedJdbcConnName = (String) entitiesComboBox.getSelectedItem();

                    JdbcAdmin admin = getJdbcConnectionAdmin();
                    if (admin == null) return;

                    try {
                        properties.put(MAPPING_TARGET_ID_ATTRIBUTE, admin.getJdbcConnection(selectedJdbcConnName).getGoid().toString());
                        selectedMigrationResolutions.put(idLabel.getText(), new Pair<>(NewOrExisting, properties));
                    } catch (FindException e1) {
                        showErrorMessage("Error Resolving Migration Issues", "Cannot find a JDBC Connection.", e1, null);
                    }
                } else if (targetType == SSG_KEY_ENTRY) {
                    final KeystoreComboEntry keystoreEntry = (KeystoreComboEntry) entitiesComboBox.getSelectedItem();

                    properties.put(MAPPING_TARGET_ID_ATTRIBUTE, keystoreEntry.getKeystoreid().toString() + ":" + keystoreEntry.getAlias());
                    selectedMigrationResolutions.put(idLabel.getText(), new Pair<>(NewOrExisting, properties));
                } else if (targetType == SECURE_PASSWORD) {
                    final SecurePassword securePassword = ((SecurePasswordComboBox) entitiesComboBox).getSelectedSecurePassword();

                    if (securePassword != null) {
                        properties.put(MAPPING_TARGET_ID_ATTRIBUTE, securePassword.getGoid().toString());
                        selectedMigrationResolutions.put(idLabel.getText(), new Pair<>(NewOrExisting, properties));
                    }
                }
            }
        });

        deleteEntityCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String id = idLabel.getText();

                if (deleteEntityCheckBox.isSelected()) {
                    Properties properties = new Properties();
                    properties.setProperty(id, targetType.toString());
                    selectedMigrationResolutions.put(id, new Pair<>(Delete, properties));
                } else {
                    selectedMigrationResolutions.remove(id);
                }
            }
        });

        if (errorType == TargetNotFound) {
            initializeEntityComponents();
        }

        for (int i = 0; i < selectAllButtons.length; i++) {
            final JButton selectAllButton = selectAllButtons[i];
            final JRadioButton radioButton = i == 0? useExistRadioButton : (i == 1? updateRadioButton : createRadioButton);

            final int index = i;
            selectAllButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    radioButton.setSelected(true);

                    // Don't directly call radioButton.doClick().
                    // Otherwise, the UI shows very slow on selecting all radio buttons, if there are a large amount of radio buttons.
                    selectedMigrationResolutions.put(
                        idLabel.getText(),
                        new Pair<>(index == 0? NewOrExisting : (index == 1? NewOrUpdate : AlwaysCreateNew), (Properties) null)
                    );
                }
            });
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
        if (targetType == SECURE_PASSWORD) {
            entitiesComboBox = new SecurePasswordComboBox();
        } else {
            entitiesComboBox = new JComboBox();
        }
    }

    public JPanel getContentPane() {
        return contentPane;
    }

    private void initializeEntityComponents() {
        if (targetType == null) return;

        populateEntitiesList();
        initializeManagingEntitiesButtons();
    }

    private void populateEntitiesList() {
        if (targetType == JDBC_CONNECTION) {
            populateJdbcConnectionComboBox();
        } else if (targetType == SSG_KEY_ENTRY) {
            populatePrivateKeyComboBox();
        } else if (targetType == SECURE_PASSWORD) {
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

    private void initializeManagingEntitiesButtons() {
        if (targetType != JDBC_CONNECTION && targetType != SSG_KEY_ENTRY && targetType != SECURE_PASSWORD) {
            showErrorMessage("Error Resolving Migration Issues", "Initialize Target Entity Error: Invalid target type: " + targetType, null, null);
            return;
        }

        final SecureAction action =
            targetType == JDBC_CONNECTION? new ManageJdbcConnectionsAction() :
            targetType == SSG_KEY_ENTRY? new ManagePrivateKeysAction() : new ManageSecurePasswordsAction();

        manageEntityButton.setText("Manage " + (targetType == JDBC_CONNECTION? "JDBC Connection" : (targetType == SSG_KEY_ENTRY? "Private Key" : "Stored Password")));
        manageEntityButton.setEnabled(action.isEnabled());
        manageEntityButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.actionPerformed(e);

                if (targetType == JDBC_CONNECTION)
                    populateJdbcConnectionComboBox();
                else if (targetType == SSG_KEY_ENTRY)
                    populatePrivateKeyComboBox();
                else
                    populateSecurePasswordComboBox();
            }
        });
    }

    private boolean compareEntityEnabled(final ConflictDisplayerDialog.ErrorType errorType, final String entityGoid) {
        if (errorType != TargetExists || !(targetType == POLICY || targetType == SERVICE)) {
            logger.fine("The error type is not TargetExists or the entity type is either Service nor Policy, so 'Compare Entity' is disabled.");

            // In this case, also set 'Compare Entity' to be invisible.
            compareEntityButton.setVisible(false);

            return false;
        }

        if (policyResourceXml == null || policyResourceXml.trim().isEmpty()) {
            logger.warning("The updated policy XML is not specified, so 'Compare Entity' is disabled.");
            return false;
        }

        final String currentActiveEntityXml = getExistingEntityXml(entityGoid);
        if (currentActiveEntityXml == null) {
            logger.warning("Cannot get the existing active entity XML, so 'Compare Entity' is disabled.");
            return false;
        }

        if (XmlUtil.reformatXml(policyResourceXml).equals(XmlUtil.reformatXml(currentActiveEntityXml))) {
            String message = "The existing entity is identical to the updated entity, so comparing entity is not necessary.";
            logger.fine(message);
            compareEntityButton.setToolTipText(message);
            return false;
        }

        return true;
    }

    private String getExistingEntityXml(final String entityGoid) {
        if (existingEntityXml != null && !existingEntityXml.trim().isEmpty()) {
            return existingEntityXml;
        }

        try {
            if (targetType == POLICY) {
                final PolicyVersion existingPolicyVersion = Registry.getDefault().getPolicyAdmin().findLatestRevisionForPolicy(Goid.parseGoid(entityGoid));
                existingEntityXml = existingPolicyVersion.getXml();
            } else if (targetType == SERVICE) {
                final PublishedService publishedService = Registry.getDefault().getServiceManager().findServiceByID(entityGoid);
                existingEntityXml = publishedService.getPolicy().getXml();
            } else {
                DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                    "The compared entity is neither a policy nor a service.", "Policy Comparison Error", JOptionPane.WARNING_MESSAGE, null);
            }
        } catch (FindException e) {
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                "Cannot find a published service", "Policy Comparison Error", JOptionPane.WARNING_MESSAGE, null);
        }

        if (existingEntityXml != null) {
            existingEntityXml = existingEntityXml.trim();
        }

        return existingEntityXml;
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