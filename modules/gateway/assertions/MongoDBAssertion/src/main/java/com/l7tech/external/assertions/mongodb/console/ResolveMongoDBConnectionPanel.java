package com.l7tech.external.assertions.mongodb.console;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.mongodb.MongoDBReference;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntity;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntityAdmin;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Collections.emptyList;

/**
 * Created by chaja24 on 9/4/2015.
 */
public class ResolveMongoDBConnectionPanel extends WizardStepPanel  {
    private static final Logger logger = Logger.getLogger(ResolveMongoDBConnectionPanel.class.getName());

    private JPanel mainPanel;
    private JTextField connectionNameField;
    private JLabel hostnameLabel;
    private JTextField serverField;
    private JRadioButton changeRadioButton;
    private JRadioButton removeRadioButton;
    private JRadioButton ignoreRadioButton;
    private JButton createMongoDBConnectionButton;
    private JTextField databasenameField;
    private JTextField portField;
    private JTextField usernameField;
    private JComboBox comboBoxAlternativeConnection;

    private MongoDBReference connectionReference;

    public ResolveMongoDBConnectionPanel(WizardStepPanel next, MongoDBReference connectionReference) {
        super(next);
        this.connectionReference = connectionReference;
        initialize();


    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        connectionNameField.setText(connectionReference.getConnectionName());
        serverField.setText(connectionReference.getServerName());
        databasenameField.setText(connectionReference.getDatabaseName());
        portField.setText(connectionReference.getPortNumber());
        usernameField.setText(connectionReference.getUserName());

        // default is delete
        removeRadioButton.setSelected(true);
        comboBoxAlternativeConnection.setEnabled(false);

        // enable/disable provider selector as per action type selected
        changeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                comboBoxAlternativeConnection.setEnabled(true);
            }
        });
        removeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                comboBoxAlternativeConnection.setEnabled(false);
            }
        });
        ignoreRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                comboBoxAlternativeConnection.setEnabled(false);
            }
        });
        comboBoxAlternativeConnection.setRenderer(new TextListCellRenderer<MongoDBConnectionEntity>(new Functions.Unary<String, MongoDBConnectionEntity>() {
            @Override
            public String call(final MongoDBConnectionEntity ssgActiveMongoDBConnections) {
                return getAlternativeConnectionInfo(ssgActiveMongoDBConnections);
            }
        }));


        changeRadioButton.setEnabled(false);
        removeRadioButton.setEnabled(true);
        removeRadioButton.setSelected(true);

        createMongoDBConnectionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createMongoDBConnection();

            }
        });

        populateConnectionComboBox();

        enableAndDisableComponents();
    }

    @Override
    public boolean onNextButton() {
        if (changeRadioButton.isSelected()) {
            if (comboBoxAlternativeConnection.getSelectedIndex() < 0) return false;

            final MongoDBConnectionEntity connector = (MongoDBConnectionEntity) comboBoxAlternativeConnection.getSelectedItem();
            connectionReference.setLocalizeReplace(connector.getGoid());
        } else if (removeRadioButton.isSelected()) {
            connectionReference.setLocalizeDelete();
        } else if (ignoreRadioButton.isSelected()) {
            connectionReference.setLocalizeIgnore();
        }
        return true;
    }

    @Override
    public void notifyActive() {
        populateConnectionComboBox();
    }

    @Override
    public String getDescription() {
        return getStepLabel();
    }

    @Override
    public boolean canFinish() {
        return !hasNextPanel();
    }

    @Override
    public String getStepLabel() {
        return "Unresolved MongoDB Connection " + connectionReference.getRefId();
    }


    private void createMongoDBConnection() {
        final MongoDBConnectionEntity newConnection = new MongoDBConnectionEntity();
        newConnection.setName(connectionReference.getConnectionName());
        newConnection.setDatabaseName(connectionReference.getDatabaseName());
        newConnection.setUri(connectionReference.getServerName());
        newConnection.setUsername(connectionReference.getUserName());
        newConnection.setPort(connectionReference.getPortNumber());
        newConnection.setAuthType(connectionReference.getEncryption());
        newConnection.setUsesDefaultKeyStore(connectionReference.isUsesDefaultKeyStore());
        newConnection.setUsesNoKey(connectionReference.isUsesNoKey());
        newConnection.setKeyAlias(connectionReference.getPrivateKeyAlias());
        newConnection.setNonDefaultKeystoreId(connectionReference.getNonDefaultKeystoreId());
        newConnection.setReadPreference(connectionReference.getReadPreference());

        EntityUtils.resetIdentity(newConnection);
        editAndSave(newConnection);

        selectNewConnection(connectionReference.getConnectionName());
    }

    private void selectNewConnection(String newConnectionName) {

        if (newConnectionName != null && comboBoxAlternativeConnection.getModel().getSize() > 0) {

            MongoDBConnectionEntity entity;
            for (int i = 0; i < comboBoxAlternativeConnection.getItemCount(); i++) {
                entity = (MongoDBConnectionEntity) comboBoxAlternativeConnection.getItemAt(i);
                if (newConnectionName.compareTo(entity.getName())==0) { // has to be exact string match including case.
                    comboBoxAlternativeConnection.setSelectedIndex(i);
                    break;
                }
            }

            if (comboBoxAlternativeConnection.getSelectedIndex() == -1) {
                comboBoxAlternativeConnection.setSelectedIndex(0);
            }
        }
    }

    private void editAndSave(final MongoDBConnectionEntity connection) {
        final MongoDBConnectionDialog dialog = new MongoDBConnectionDialog(this.getOwner(), connection);
        dialog.setVisible(true);
        Utilities.centerOnParent(dialog);
        if (dialog.isConfirmed()) {
            try {
                dialog.getData(connection);
                Goid savedGoid = getEntityManager().save(connection);
                if (savedGoid==null) {
                    logger.log(Level.SEVERE, "Failed to save the new MongoDB connection.  No Goid returned.");
                }
                populateConnectionComboBox();
                changeRadioButton.setEnabled(true);
                changeRadioButton.setSelected(true);
                comboBoxAlternativeConnection.setEnabled(true);

            } catch (SaveException e) {
                logger.log(Level.INFO, "Failed to save the new MongoDB connection.", e);
            } catch (UpdateException e) {
                logger.log(Level.INFO, "Failed to update the MongoDB connection.", e);
            }
        }
    }


    private static MongoDBConnectionEntityAdmin getEntityManager() {
        return Registry.getDefault().getExtensionInterface(MongoDBConnectionEntityAdmin.class, null);
    }

    private String getAlternativeConnectionInfo(final MongoDBConnectionEntity connection) {
        final StringBuilder builder = new StringBuilder();
        builder.append(connection.getName());
        return builder.toString();
    }


    private java.util.List<MongoDBConnectionEntity> findAllMongoDBConnections() {
        try {
            final MongoDBConnectionEntityAdmin admin = getEntityManager();
            ArrayList<MongoDBConnectionEntity> entities = new ArrayList<MongoDBConnectionEntity>();
            for (MongoDBConnectionEntity entity : admin.findByType()) {
                entities.add(entity);
            }
            return entities;
        } catch (IllegalStateException e) {
            // no admin context available
            logger.info("Unable to access MongoDB Connections from server.");
        } catch (FindException e) {
            ErrorManager.getDefault().notify(Level.WARNING, e, "Error MongoDB Connections");
        }
        return emptyList();
    }

    private void populateConnectionComboBox() {
        MongoDBConnectionEntityAdmin admin = getEntityManager();
        if (admin == null) return;

        final Object selectedItem = comboBoxAlternativeConnection.getSelectedItem();
        final Collection<MongoDBConnectionEntity> connections = findAllMongoDBConnections();

        // Sort connectors by combination name
        Collections.sort((java.util.List<MongoDBConnectionEntity>) connections, new Comparator<MongoDBConnectionEntity>() {
            @Override
            public int compare(MongoDBConnectionEntity o1, MongoDBConnectionEntity o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        // Add all items into the combo box.
        comboBoxAlternativeConnection.setModel(Utilities.comboBoxModel(connections));

        if (selectedItem != null && comboBoxAlternativeConnection.getModel().getSize() > 0) {
            comboBoxAlternativeConnection.setSelectedItem(selectedItem);
            if (comboBoxAlternativeConnection.getSelectedIndex() == -1) {
                comboBoxAlternativeConnection.setSelectedIndex(0);
            }
        }
    }

    private void enableAndDisableComponents() {
        final boolean enableSelection = comboBoxAlternativeConnection.getModel().getSize() > 0;
        changeRadioButton.setEnabled(enableSelection);

        if (!changeRadioButton.isEnabled() && changeRadioButton.isSelected()) {
            removeRadioButton.setSelected(true);
        }
    }
}
