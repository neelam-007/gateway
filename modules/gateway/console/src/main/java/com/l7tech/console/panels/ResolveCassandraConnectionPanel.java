package com.l7tech.console.panels;

import com.l7tech.console.util.EntityUtils;
import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.gateway.common.cassandra.CassandraConnectionManagerAdmin;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.exporter.CassandraConnectionReference;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.MessageFormat;


public class ResolveCassandraConnectionPanel extends WizardStepPanel {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.ResolveCassandraConnectionPanel");
    private static final Logger logger = Logger.getLogger(ResolveCassandraConnectionPanel.class.getName());

    private JPanel mainPanel;
    private JComboBox connectionComboBox;
    private JButton manageConnectionsButton;
    private JRadioButton changeRadioButton;
    private JRadioButton removeRadioButton;
    private JRadioButton ignoreRadioButton;
    private JTextField nameTextField;
    private JTextField keyspaceTextField;
    private JTextField contactPointsTextField;
    private JTextField compressionTextField;
    private JTextField portTextField;

    private CassandraConnectionReference connectionReference;

    @SuppressWarnings("unchecked")
    public ResolveCassandraConnectionPanel(WizardStepPanel next, CassandraConnectionReference connectionReference) {
        super(next);
        this.connectionReference = connectionReference;
        initialize();
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
        return MessageFormat.format(resources.getString("label.unresolved.cassandra.conn"), connectionReference.getConnectionName());
    }

    @Override
    public boolean onNextButton() {
        if (changeRadioButton.isSelected()) {
            if (connectionComboBox.getSelectedIndex() < 0) return false;

            String connectionName = (String) connectionComboBox.getSelectedItem();
            connectionReference.setLocalizeReplaceByName(connectionName);
        } else if (removeRadioButton.isSelected()) {
            connectionReference.setLocalizeDelete();
        } else if (ignoreRadioButton.isSelected()) {
            connectionReference.setLocalizeIgnore();
        }
        return true;
    }

    @Override
    public void notifyActive() {
        populateConnectionCombobox();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        nameTextField.setText(connectionReference.getConnectionName());
        nameTextField.setCaretPosition(0);
        keyspaceTextField.setText(connectionReference.getKeyspace());
        keyspaceTextField.setCaretPosition(0);
        contactPointsTextField.setText(connectionReference.getContactPoints());
        contactPointsTextField.setCaretPosition(0);
        portTextField.setText(connectionReference.getPort());
        portTextField.setCaretPosition(0);
        compressionTextField.setText(connectionReference.getCompression());
        compressionTextField.setCaretPosition(0);

        // default is delete
        removeRadioButton.setSelected(true);
        connectionComboBox.setEnabled(false);

        // enable/disable provider selector as per action type selected
        changeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectionComboBox.setEnabled(true);
            }
        });
        removeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectionComboBox.setEnabled(false);
            }
        });
        ignoreRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectionComboBox.setEnabled(false);
            }
        });

        manageConnectionsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
              doManageCassandraConnections();
            }
        });

        populateConnectionCombobox();
        enableAndDisableComponents();
    }

    private void doManageCassandraConnections() {
        final CassandraConnection newConnection = new CassandraConnection();
        newConnection.setName(connectionReference.getConnectionName());
        newConnection.setKeyspaceName(connectionReference.getKeyspace());
        newConnection.setContactPoints(connectionReference.getContactPoints());
        newConnection.setPort(connectionReference.getPort());
        newConnection.setUsername(connectionReference.getUsername());
        newConnection.setCompression(connectionReference.getCompression());
        newConnection.setProperties(connectionReference.getProperties());

        EntityUtils.resetIdentity(newConnection);
        editAndSave(newConnection);
    }

    private void editAndSave(final CassandraConnection connection) {
        final CassandraConnectionPropertiesDialog dlg =
                new CassandraConnectionPropertiesDialog(TopComponents.getInstance().getTopParent(), connection);
        final boolean changeWasEnabled = changeRadioButton.isEnabled();
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    Runnable reedit = new Runnable() {
                        public void run() {
                            editAndSave(connection);
                        }
                    };

                    // Save the connection
                    CassandraConnectionManagerAdmin admin = getCassandraManagerAdmin();
                    if (admin == null) return;
                    try {
                        admin.saveCassandraConnection(connection);
                    } catch (UpdateException | SaveException e) {
                        showErrorMessage(resources.getString("errors.saveFailed.title"),
                                resources.getString("errors.saveFailed.message") + " " + ExceptionUtils.getMessage(e),
                                e,
                                reedit);
                        return;
                    }

                    // refresh controls
                    populateConnectionCombobox();
                    enableAndDisableComponents();
                    if (changeRadioButton.isEnabled() && !changeWasEnabled) {
                        if (removeRadioButton.isSelected())
                            changeRadioButton.setSelected(true);
                        connectionComboBox.setSelectedIndex(0);
                    }

                }
            }
        });
    }

    private void showErrorMessage(String title, String msg, Throwable e, Runnable continuation) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, continuation);
    }

    @SuppressWarnings("unchecked")
    private void populateConnectionCombobox() {
        final Object selectedItem = connectionComboBox.getSelectedItem();
        java.util.List<String> connNameList;
        CassandraConnectionManagerAdmin admin = getCassandraManagerAdmin();
        if (admin == null) return;
        else {
            try {
                connNameList = admin.getAllCassandraConnectionNames();
            } catch (FindException e) {
                logger.warning("Error getting Cassandra connection names");
                return;
            }
        }

        // Sort all default driver classes
        Collections.sort(connNameList);

        // Add all items into the combox box.
        connectionComboBox.removeAllItems();
        for (String connName : connNameList) {
            connectionComboBox.addItem(connName);
        }

        if (selectedItem != null && connectionComboBox.getModel().getSize() > 0) {
            connectionComboBox.setSelectedItem(selectedItem);
            if (connectionComboBox.getSelectedIndex() == -1) {
                connectionComboBox.setSelectedIndex(0);
            }
        }
    }

    private CassandraConnectionManagerAdmin getCassandraManagerAdmin() {
        CassandraConnectionManagerAdmin admin = null;
        if (Registry.getDefault().isAdminContextPresent()) {
            admin = Registry.getDefault().getCassandraConnectionAdmin();
        } else {
            logger.log(Level.WARNING, "No Admin Context present!");
        }
        return admin;
    }

    private void enableAndDisableComponents() {
        final boolean enableSelection = connectionComboBox.getModel().getSize() > 0;
        changeRadioButton.setEnabled(enableSelection);

        if (!changeRadioButton.isEnabled() && changeRadioButton.isSelected()) {
            removeRadioButton.setSelected(true);
        }
    }
}
