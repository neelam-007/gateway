/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
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
public class ExternalAuditStoreConfigSchema extends WizardStepPanel {
    private JPanel mainPanel;
    private JButton testButton;
    private JButton getSchemaButton;
    private JTextArea schemaTextArea;
    private JTextField auditRecordTextField;
    private JTextField auditDetailTextField;

    private static final Logger logger = Logger.getLogger(ExternalAuditStoreConfigSchema.class.getName());
    private String connection;
    private String schema;

    /**
     * Creates new form AttributeStatementWizardStepPanel
     * @param next the panel that follows this one. Null means this is the last step.
     */
    public ExternalAuditStoreConfigSchema(WizardStepPanel next) {
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

        connection = config.connection;
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
        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doTest();
            }
        });
        getSchemaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                schemaTextArea.setText(getSchema());
                schemaTextArea.setCaretPosition(0);
            }
        });

    }

    private void doTest() {
        JdbcAdmin admin = getJdbcAdmin();

        if(admin == null) {
            DialogDisplayer.showMessageDialog(
                    ExternalAuditStoreConfigSchema.this,
                     "Failed  Cannot process testing due to JDBC Conneciton Admin unavailable.",
                    "Testing",
                    JOptionPane.WARNING_MESSAGE,
                    null);

            return ;
        }
        boolean success = false;
        String errorMessage = "";
        try {
            errorMessage = doAsyncAdmin(
                    admin,
                    ExternalAuditStoreConfigSchema.this.getOwner(),
                    "Testing",
                    "Testing",
                    admin.testAuditSinkSchema(connection,auditRecordTextField.getText(),auditDetailTextField.getText())).right();

            success = errorMessage.isEmpty();

        } catch (InterruptedException e) {
            // do nothing, user cancelled operation
        } catch (InvocationTargetException e) {
            success = false;
            errorMessage = e.getMessage();
        }

        DialogDisplayer.showMessageDialog(
            ExternalAuditStoreConfigSchema.this,
            success ? "Passed!" : "Failed" ,
            "Testing",
            success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE,
            null);

        schema = success?"Schema is valid" : getSchema();
        schemaTextArea.setText(schema);
        schemaTextArea.setCaretPosition(0);
    }

    private String getSchema() {
        return getAuditAdmin().getExternalAuditsSchema();
    }

    private AuditAdmin getAuditAdmin() {
        return  Registry.getDefault().getAuditAdmin();
    }

    private JdbcAdmin getJdbcAdmin() {
        return  Registry.getDefault().getJdbcConnectionAdmin();
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
            "<html>Words! More Words!!!</html>";
    }

    @Override
    public boolean canAdvance() {
        return true;
    }

    @Override
    public boolean canFinish() {
        return true;
    }
}