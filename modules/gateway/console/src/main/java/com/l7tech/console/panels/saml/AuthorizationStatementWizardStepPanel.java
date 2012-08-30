/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.SamlAuthorizationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlPolicyAssertion;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * The SAML Conditions <code>WizardStepPanel</code>
 * @author emil
 * @version Jan 20, 2005
 */
public class AuthorizationStatementWizardStepPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JLabel titleLabel;
    private JTextField textFieldAction;
    private JTextField textFieldActionNamespace;
    private JTextField textFieldResource;
    private boolean showTitleLabel;
    private final boolean issueMode;

    private static final String DESC_PREFIX = "<html>Specify the Resource [required] that the SAML statement ";
    private static final String DESC_SUFFIX = " describe; the Resource Action [required] and the Action Namespace [optional].</html>";

    /**
     * Creates new form AuthorizationStatementWizardStepPanel
     */
    public AuthorizationStatementWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, boolean issueMode) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        this.issueMode = issueMode;
        initialize();
    }

    /**
     * Creates new form AuthorizationStatementWizardStepPanel
     */
    public AuthorizationStatementWizardStepPanel(WizardStepPanel next, boolean issueMode) {
        this(next, true, issueMode);
    }


    /**
     * Creates new form AuthorizationStatementWizardStepPanel
     */
    public AuthorizationStatementWizardStepPanel(WizardStepPanel next,  boolean showTitleLabel, boolean issueMode, JDialog owner) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        this.issueMode = issueMode;
        setOwner(owner);
        initialize();
    }

    public AuthorizationStatementWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, JDialog parent) {
        this(next, showTitleLabel, false, parent);
    }

    public AuthorizationStatementWizardStepPanel(WizardStepPanel next) {
        this(next, true, false);
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
        SamlPolicyAssertion assertion = (SamlPolicyAssertion)settings;
        SamlAuthorizationStatement statement = assertion.getAuthorizationStatement();
         if (statement == null) {
             throw new IllegalArgumentException();
         }
        statement.setAction(textFieldAction.getText());
        statement.setActionNamespace(textFieldActionNamespace.getText());
        statement.setResource(textFieldResource.getText());
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
        SamlPolicyAssertion assertion = (SamlPolicyAssertion)settings;
        SamlAuthorizationStatement statement = assertion.getAuthorizationStatement();
        setSkipped(statement == null);
        if (statement == null) {
            return;
        }

        textFieldAction.setText(statement.getAction());
        textFieldActionNamespace.setText(statement.getActionNamespace());
        textFieldResource.setText(statement.getResource());
    }

    private void initialize() {
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        if (showTitleLabel) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.getParent().remove(titleLabel);
        }

        DocumentListener docListener = new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                notifyListeners();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                notifyListeners();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                notifyListeners();
            }
        };
        textFieldResource.getDocument().addDocumentListener(docListener);
        textFieldAction.getDocument().addDocumentListener(docListener);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                notifyListeners();
            }
        });
    }

    /**
     * @return the wizard step label
     */
    @Override
    public String getStepLabel() {
        return "Authorization Statement";
    }


    @Override
    public String getDescription() {
        if (issueMode) {
            return DESC_PREFIX + "will" + DESC_SUFFIX;
        } else {
            return DESC_PREFIX + "MUST" + DESC_SUFFIX;
        }
    }

    /**
     * Test whether the step is finished and it is safe to advance to the next one.
     * The resource and action must be specified
     *
     * @return true if the panel is valid, false otherwis
     */
    @Override
    public boolean canAdvance() {
        String resource = textFieldResource.getText();
        String action = textFieldAction.getText();
        return (notNullOrEmpty(resource) && notNullOrEmpty(action));
    }

    private boolean notNullOrEmpty(String s) {
        return s != null && !"".equals(s.trim());
    }
}