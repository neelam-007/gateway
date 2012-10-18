/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

import static com.l7tech.console.util.AdminGuiUtils.doAsyncAdmin;

/**
 *
 */
public class ExternalAuditStoreConfigDatabase extends WizardStepPanel {
    private JPanel mainPanel;
    private JTextField auditRecordTextField;
    private JTextField auditDetailTextField;

    private static final Logger logger = Logger.getLogger(ExternalAuditStoreConfigDatabase.class.getName());

    /**
     * Creates new form AttributeStatementWizardStepPanel
     * @param next the panel that follows this one. Null means this is the last step.
     */
    public ExternalAuditStoreConfigDatabase(WizardStepPanel next) {
        super(next);
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
        ExternalAuditStoreConfigWizard.ExternalAuditStoreWizardConfig config = (ExternalAuditStoreConfigWizard.ExternalAuditStoreWizardConfig)settings;
        config.auditRecordTableName = auditRecordTextField.getText();
        config.auditDetailTableName = auditDetailTextField.getText();
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
        ExternalAuditStoreConfigWizard.ExternalAuditStoreWizardConfig config = (ExternalAuditStoreConfigWizard.ExternalAuditStoreWizardConfig)settings;

        if(config.auditRecordTableName !=null){
            auditRecordTextField.setText(config.auditRecordTableName);
        }
        if(config.auditDetailTableName != null){
            auditDetailTextField.setText(config.auditDetailTableName);
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());


        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);

        // populate jdbc connections
        final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                notifyListeners();
            }
        });

        auditRecordTextField.getDocument().addDocumentListener(changeListener);
        auditDetailTextField.getDocument().addDocumentListener(changeListener);
    }

    /**
     * @return the wizard step label
     */
    @Override
    public String getStepLabel() {
        return "Configure database";
    }


    @Override
    public String getDescription() {
        return
            "<html>Define the names of the tables that will be used in the database for the external audit store. The following table names are required:<br><br>" +
                    "Audit Record Table: Default name is \"audit_main\"<br>" +
                    "Audit Detail Table: Default name is \"audit_detail\"</html>";
    }

    @Override
    public boolean canAdvance() {
        return !auditDetailTextField.getText().isEmpty() && !auditRecordTextField.getText().isEmpty();
    }
}