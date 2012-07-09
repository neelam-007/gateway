/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.gui.widgets.OptionalCredentialsPanel;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.util.Either;

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
    private JButton createDatabaseButton;

    private static final Logger logger = Logger.getLogger(ExternalAuditStoreConfigSchema.class.getName());
    private String connection;
    private String auditRecordTableName;
    private String auditDetailTableName;
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
        // None to store
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
        auditRecordTableName = config.auditRecordTableName;
        auditDetailTableName = config.auditDetailTableName;
    }

    @Override
    public void notifyActive() {
        // clear schema display
        schemaTextArea.setText(null);

        super.notifyActive();
    }

    private void initialize() {
        setLayout(new BorderLayout());


        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);

        // populate jdbc connections
        final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                notifyListeners();
                createDatabaseButton.setEnabled(canFinish());
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

        createDatabaseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCreateDatabase();
            }
        });

    }

    private void doCreateDatabase() {

        final AuditAdmin admin = getAuditAdmin();

        if(admin == null) {
            DialogDisplayer.showMessageDialog(
                    ExternalAuditStoreConfigSchema.this,
                    "Failed  Cannot create databse due to JDBC Conneciton Admin unavailable.",
                    "Creating Database\"",
                    JOptionPane.WARNING_MESSAGE,
                    null);

            return ;
        }

        //get password

        final UsernamePasswordDialog pwdPanel = new UsernamePasswordDialog(this.getOwner(),"Enter credentials",null);

        pwdPanel.pack();
        Utilities.centerOnScreen(pwdPanel);
        DialogDisplayer.display(pwdPanel, new Runnable() {
            @Override
            public void run() {
                if(!pwdPanel.isConfirmed())
                    return;

                String username,password ;
                username = pwdPanel.getUsername();
                password = new String(pwdPanel.getPassword());

                boolean success = false;
                String errorMessage = "";
                try {
                    Either<String,String> result = doAsyncAdmin(
                            admin,
                            ExternalAuditStoreConfigSchema.this.getOwner(),
                            "Creating Database",
                            "Creating Database",
                            getAuditAdmin().createExternalAuditDatabaseTables(connection, auditRecordTableName, auditDetailTableName,username,password));
                    errorMessage = result.isLeft() ? result.left(): result.right();

                    success = errorMessage.isEmpty();

                } catch (InterruptedException e) {
                    // do nothing, user cancelled operation
                } catch (InvocationTargetException e) {
                    success = false;
                    errorMessage = e.getMessage();
                }

                DialogDisplayer.showMessageDialog(
                        ExternalAuditStoreConfigSchema.this,
                        success ? "Database created successfully!" : "Failed to create database \n"+errorMessage ,
                        "Creating Database",
                        success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE,
                        null);
            }
        });


    }

    private void doTest() {
        AuditAdmin admin = getAuditAdmin();

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
                    admin.testAuditSinkSchema(connection,auditRecordTableName,auditDetailTableName)).right();

            success = errorMessage.isEmpty();

        } catch (InterruptedException e) {
            // do nothing, user cancelled operation
        } catch (InvocationTargetException e) {
            success = false;
            errorMessage = e.getMessage();
        }

        DialogDisplayer.showMessageDialog(
            ExternalAuditStoreConfigSchema.this,
            success ? "Passed!" : "Failed \n"+errorMessage ,
            "Testing",
            success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE,
            null);

        schema = success?"Schema is valid" : getSchema();
        schemaTextArea.setText(schema);
        schemaTextArea.setCaretPosition(0);
    }

    private String getSchema() {
        return getAuditAdmin().getExternalAuditsSchema(connection,auditRecordTableName,auditDetailTableName);
    }

    private AuditAdmin getAuditAdmin() {
        return  Registry.getDefault().getAuditAdmin();
    }

    /**
     * @return the wizard step label
     */
    @Override
    public String getStepLabel() {
        return "Test/Create database";
    }


    @Override
    public String getDescription() {
        return
            "<html>The schema that will be used to create audit tables in the specified JDBC connection.<br>" +
                  "Test the existing schema.</html>";
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