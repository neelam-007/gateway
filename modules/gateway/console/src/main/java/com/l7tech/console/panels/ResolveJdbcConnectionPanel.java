package com.l7tech.console.panels;

import com.l7tech.console.util.EntityUtils;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.exporter.JdbcConnectionReference;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
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

/**
 * @author ghuang
 */
public class ResolveJdbcConnectionPanel extends WizardStepPanel {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.ResolveJdbcConnectionPanel");
    private static final Logger logger = Logger.getLogger(ResolveJdbcConnectionPanel.class.getName());

    private JPanel mainPanel;
    private JComboBox connectionComboBox;
    private JButton manageConnectionsButton;
    private JRadioButton changeRadioButton;
    private JRadioButton removeRadioButton;
    private JRadioButton ignoreRadioButton;
    private JTextField nameTextField;
    private JTextField driverClassTextField;
    private JTextField jdbcUrlTextField;

    private JdbcConnectionReference connectionReference;

    public ResolveJdbcConnectionPanel(WizardStepPanel next, JdbcConnectionReference connectionReference) {
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
        return MessageFormat.format(resources.getString("label.unresolved.jdbc.conn"), connectionReference.getConnectionName());
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

        nameTextField.setText( connectionReference.getConnectionName() );
        nameTextField.setCaretPosition( 0 );
        driverClassTextField.setText( connectionReference.getDriverClass() );
        driverClassTextField.setCaretPosition( 0 );
        jdbcUrlTextField.setText( connectionReference.getJdbcUrl() );
        jdbcUrlTextField.setCaretPosition( 0 );

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
                doManageJdbcConnections();
            }
        });

        populateConnectionCombobox();
        enableAndDisableComponents();
    }

    private void doManageJdbcConnections() {
        final JdbcConnection newConnection = new JdbcConnection();
        newConnection.setName(connectionReference.getConnectionName());
        newConnection.setDriverClass(connectionReference.getDriverClass());
        newConnection.setJdbcUrl(connectionReference.getJdbcUrl());
        newConnection.setUserName(connectionReference.getUserName());
        newConnection.setAdditionalProperties(connectionReference.getAdditionalProps());

        EntityUtils.resetIdentity(newConnection);
        editAndSave(newConnection);
    }

    private void editAndSave(final JdbcConnection connection){
        final JdbcConnectionPropertiesDialog dlg = new JdbcConnectionPropertiesDialog(TopComponents.getInstance().getTopParent(), connection);
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
                    JdbcAdmin admin = getJdbcConnectionAdmin();
                    if (admin == null) return;
                    try {
                       admin.saveJdbcConnection(connection);
                    } catch (UpdateException e) {
                        showErrorMessage(resources.getString("errors.saveFailed.title"),
                                resources.getString("errors.saveFailed.message") + " " + ExceptionUtils.getMessage(e),
                                e,
                                reedit);
                        return;
                    }

                    // refresh controls
                    populateConnectionCombobox();
                    enableAndDisableComponents();
                    if ( changeRadioButton.isEnabled() && !changeWasEnabled ) {
                        if (removeRadioButton.isSelected())
                            changeRadioButton.setSelected( true );
                        connectionComboBox.setSelectedIndex(0);
                    }

                }
            }});
    }

    private void showErrorMessage(String title, String msg, Throwable e, Runnable continuation) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, continuation);
    }

    private void populateConnectionCombobox() {
        final Object selectedItem = connectionComboBox.getSelectedItem();
        java.util.List<String> connNameList;
        JdbcAdmin admin = getJdbcConnectionAdmin();
        if (admin == null) return;
        else {
            try {
                connNameList = admin.getAllJdbcConnectionNames();
            } catch (FindException e) {
                logger.warning("Error getting JDBC connection names");
                return;
            }
        }

        // Sort all default driver classes
        Collections.sort(connNameList);

        // Add all items into the combox box.
        connectionComboBox.removeAllItems();
        for (String driverClass: connNameList) {
            connectionComboBox.addItem(driverClass);
        }

        if ( selectedItem != null && connectionComboBox.getModel().getSize() > 0 ) {
            connectionComboBox.setSelectedItem( selectedItem );
            if ( connectionComboBox.getSelectedIndex() == -1 ) {
                connectionComboBox.setSelectedIndex( 0 );
            }
        }
    }

    private JdbcAdmin getJdbcConnectionAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent()) {
            logger.warning("Cannot get JDBC Connection Admin due to no Admin Context present.");
            return null;
        }
        return reg.getJdbcConnectionAdmin();
    }

    private void enableAndDisableComponents() {
        final boolean enableSelection = connectionComboBox.getModel().getSize() > 0;
        changeRadioButton.setEnabled( enableSelection );

        if ( !changeRadioButton.isEnabled() && changeRadioButton.isSelected() ) {
            removeRadioButton.setSelected( true );    
        }
    }
}
