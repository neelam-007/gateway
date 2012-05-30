/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.logging.Logger;

/**
 *
 */
public class ExternalAuditStoreConfigJdbc extends WizardStepPanel {
    private JPanel mainPanel;
    private JComboBox jdbcConnectionComboBox;
    private JButton manageConnectionsButton;

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
       ((ExternalAuditStoreConfigWizard.ExternalAuditStoreWizardConfig)settings).connection = (String)jdbcConnectionComboBox.getSelectedItem();
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
        java.util.List<String> connNameList;
        JdbcAdmin admin = getJdbcConnectionAdmin();
        if (admin == null) {
            return;
        } else {
            try {
                connNameList = admin.getAllJdbcConnectionNames();
            } catch (FindException e) {
                logger.warning("Error getting JDBC connection names");
                return;
            }
        }

        // Sort all default driver classes
        Collections.sort(connNameList);
        // Add an empty driver class at the first position of the list
        connNameList.add(0, "");

        // Add all items into the combox box.
        jdbcConnectionComboBox.removeAllItems();
        for (String driverClass: connNameList) {
            jdbcConnectionComboBox.addItem(driverClass);
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
            "<html>Blah blah blah English is hard!</html>";
    }

    @Override
    public boolean canAdvance() {
        return jdbcConnectionComboBox.getSelectedIndex() > 0; // first entry empty;
    }

    @Override
    public boolean canFinish() {
        return false;
    }
}