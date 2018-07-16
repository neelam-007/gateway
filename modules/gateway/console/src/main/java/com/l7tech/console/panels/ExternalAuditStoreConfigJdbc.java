/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Logger;

/**
 *
 */
public class ExternalAuditStoreConfigJdbc extends WizardStepPanel {
    private JPanel mainPanel;
    private JComboBox jdbcConnectionComboBox;
    private JButton manageConnectionsButton;
    private JRadioButton createDefaultAuditSinkRadioButton;
    private JRadioButton createCustomAuditSInkRadioButton;
    private JPanel defaultSinkConfigurationPanel;

    private java.util.List<JdbcConnection> connList;
    private static final Logger logger = Logger.getLogger(ExternalAuditStoreConfigJdbc.class.getName());

    /**
     * Creates new form AttributeStatementWizardStepPanel
     * @param next the panel that follows this one. Null means this is the last step.
     */
    public ExternalAuditStoreConfigJdbc(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        initialize();
    }

    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     * <p/>
     * This is a noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    @Override
    public void storeSettings(Object settings) throws IllegalArgumentException {
        String connectionName = (String)jdbcConnectionComboBox.getSelectedItem();
       ((ExternalAuditStoreConfigWizard.ExternalAuditStoreWizardConfig)settings).connection = connectionName;

        for(JdbcConnection conn : connList){
            if(conn.getName().equals(connectionName)){
                ((ExternalAuditStoreConfigWizard.ExternalAuditStoreWizardConfig)settings).connectionDriverClass = conn.getDriverClass();
            }
        }
       ((ExternalAuditStoreConfigWizard.ExternalAuditStoreWizardConfig)settings).custom = createCustomAuditSInkRadioButton.isSelected();
    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings. This is a
     * noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        populateConnectionCombobox();
        String connection =  ((ExternalAuditStoreConfigWizard.ExternalAuditStoreWizardConfig)settings).connection;
        jdbcConnectionComboBox.setSelectedItem(connection);
    }

    private void initialize() {
        // populate jdbc connections
        final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                defaultSinkConfigurationPanel.setEnabled(createDefaultAuditSinkRadioButton.isSelected());
                notifyListeners();
            }
        });
        ((JTextField)jdbcConnectionComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(changeListener);
        manageConnectionsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object selectedConnection = jdbcConnectionComboBox.getSelectedItem();
                JdbcConnectionManagerWindow connMgrWindow = new JdbcConnectionManagerWindow(TopComponents.getInstance().getTopParent());
                connMgrWindow.pack();
                Utilities.centerOnScreen(connMgrWindow);
                DialogDisplayer.display(connMgrWindow);

                populateConnectionCombobox();
                jdbcConnectionComboBox.setSelectedItem(selectedConnection);
            }
        });
        jdbcConnectionComboBox.addItemListener(changeListener);
        populateConnectionCombobox();

        createDefaultAuditSinkRadioButton.addChangeListener(changeListener);
        createCustomAuditSInkRadioButton.addChangeListener(changeListener);

        notifyListeners();

    }

    private JdbcAdmin getJdbcConnectionAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent()) {
            logger.warning("Cannot get JDBC Connection Admin due to no Admin Context present.");
            return null;
        }
        return reg.getJdbcConnectionAdmin();
    }

    private void populateConnectionCombobox() {
        JdbcAdmin admin = getJdbcConnectionAdmin();
        if (admin == null) {
            return;
        } else {
            try {
                connList = admin.getAllJdbcConnections();
            } catch (FindException e) {
                logger.warning("Error getting JDBC connection names");
                return;
            }
        }

        // Sort all default driver classes
        Collections.sort(connList,new Comparator<JdbcConnection>() {
            @Override
            public int compare(JdbcConnection o1, JdbcConnection o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        // Add an empty driver class at the first position of the list
        jdbcConnectionComboBox.addItem("");

        // Add all items into the combox box.
        jdbcConnectionComboBox.removeAllItems();
        for (JdbcConnection connection: connList) {
            if(ExternalAuditStoreConfigWizard.STRICT_CONNECTION_NAME_PATTERN.matcher(connection.getName()).matches())
                jdbcConnectionComboBox.addItem(connection.getName());
        }

        jdbcConnectionComboBox.setSelectedItem(0);
    }
    /**
     * @return the wizard step label
     */
    @Override
    public String getStepLabel() {
        return "Select JDBC Connection";
    }


    @Override
    public String getDescription() {
        return
            "<html>Create the policies for audit storage and lookup</html>";
    }

    @Override
    public boolean canAdvance() {
        String selectedConnection =(String)jdbcConnectionComboBox.getSelectedItem();
        return createDefaultAuditSinkRadioButton.isSelected() && selectedConnection!=null && !selectedConnection.isEmpty();
    }

    @Override
    public boolean canFinish() {
        return createCustomAuditSInkRadioButton.isSelected();
    }
}