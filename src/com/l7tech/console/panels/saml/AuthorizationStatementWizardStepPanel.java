/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAuthorizationStatement;

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

    /**
     * Creates new form AuthorizationStatementWizardStepPanel
     */
    public AuthorizationStatementWizardStepPanel(WizardStepPanel next, boolean showTitleLabel) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        initialize();
    }

    /**
     * Creates new form AuthorizationStatementWizardStepPanel
     */
    public AuthorizationStatementWizardStepPanel(WizardStepPanel next) {
        this(next, true);
    }


    /**
     * Creates new form AuthorizationStatementWizardStepPanel
     */
    public AuthorizationStatementWizardStepPanel(WizardStepPanel next,  boolean showTitleLabel, JDialog owner) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        setOwner(owner);
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
    public void storeSettings(Object settings) throws IllegalArgumentException {
        RequestWssSaml assertion = (RequestWssSaml)settings;
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
    public void readSettings(Object settings) throws IllegalArgumentException {
        RequestWssSaml assertion = (RequestWssSaml)settings;
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

        textFieldResource.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                notifyListeners();
            }

            public void insertUpdate(DocumentEvent e) {
                notifyListeners();
            }

            public void removeUpdate(DocumentEvent e) {
                notifyListeners();
            }
        });
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Authorization Statement";
    }


    public String getDescription() {
        return
        "<html>Specify the Resource [required] that the SAML statement MUST describe; the " +
          "Resource Action [optional] and the Action Namespace [optional] " +
          "and whether the message signature is required as the proof material</html>";
    }

    /**
     * Test whether the step is finished and it is safe to advance to the next one.
     * The resource must be specified
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean canAdvance() {
        String resource = textFieldResource.getText();
        return (resource != null && !"".equals(resource.trim()));
    }

    /**
     * Test whether the step is finished and it is safe to finish the wizard.
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean canFinish() {
        return false;
    }
}