/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.Actions;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

/**
 * The <code>RequestWssSamlPropertiesPanel</code> edits the
 * configuration of the SAML authentication statement constraints.
 *
 * @author emil
 * @version Jan 18, 2005
 */
public class RequestWssSamlPropertiesPanel extends JDialog {
    private JTabbedPane tabbedPane;
    private JButton buttonOk;
    private JButton buttonCancel;
    private JPanel mainPanel;
    private RequestWssSaml assertion;
    private boolean assertionChanged;
    private WizardStepPanel[] wizardPanels;
    private ChangeListener wizardPanelChangeListener;

    /**
     * Creates new wizard
     */
    public RequestWssSamlPropertiesPanel(RequestWssSaml assertion, Frame parent, boolean modal) {
        super(parent);
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
        this.assertion = assertion;
        setTitle("SAML Constraints");
        setModal(modal);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        /** Set content pane */
        contentPane.add(mainPanel, BorderLayout.CENTER);
        initialize();
    }

    /**
     * @return true if the assertion was changed by this dialog
     */
    public boolean hasAssertionChanged() {
        return assertionChanged;
    }

    private void initialize() {
        final Border emptyBorder = BorderFactory.createEmptyBorder(10, 10, 5, 10);
        Collection panels = new ArrayList();
        if (assertion.getAuthenticationStatement() != null) {
            AuthenticationMethodsWizardStepPanel authenticationMethodsWizardStepPanel = new AuthenticationMethodsWizardStepPanel(null, false, this);
            authenticationMethodsWizardStepPanel.setBorder(emptyBorder);
            panels.add(authenticationMethodsWizardStepPanel);
            tabbedPane.add(authenticationMethodsWizardStepPanel.getStepLabel(), authenticationMethodsWizardStepPanel);
        }
        if (assertion.getAuthorizationStatement() != null) {
            AuthorizationStatementWizardStepPanel authorizationStatementWizardStepPanel = new AuthorizationStatementWizardStepPanel(null, false, this);
            authorizationStatementWizardStepPanel.setBorder(emptyBorder);
            panels.add(authorizationStatementWizardStepPanel);
            tabbedPane.add(authorizationStatementWizardStepPanel.getStepLabel(), authorizationStatementWizardStepPanel);
        }
        if (assertion.getAttributeStatement() != null) {
            AttributeStatementWizardStepPanel attributeMethodsWizardStepPanel = new AttributeStatementWizardStepPanel(null, false, this);
            attributeMethodsWizardStepPanel.setBorder(emptyBorder);
            panels.add(attributeMethodsWizardStepPanel);
            tabbedPane.add(attributeMethodsWizardStepPanel.getStepLabel(), attributeMethodsWizardStepPanel);
        }
        SubjectConfirmationWizardStepPanel subjectConfirmationWizardStepPanel = new SubjectConfirmationWizardStepPanel(null, false, this);
        subjectConfirmationWizardStepPanel.setBorder(emptyBorder);
        panels.add(subjectConfirmationWizardStepPanel);

        SubjectConfirmationNameIdentifierWizardStepPanel subjectConfirmationNameIdentifierWizardStepPanel = new SubjectConfirmationNameIdentifierWizardStepPanel(null, false, this);
        subjectConfirmationNameIdentifierWizardStepPanel.setBorder(emptyBorder);
        panels.add(subjectConfirmationNameIdentifierWizardStepPanel);

        ConditionsWizardStepPanel conditionsWizardStepPanel = new ConditionsWizardStepPanel(null, false, this);
        conditionsWizardStepPanel.setBorder(emptyBorder);
        panels.add(conditionsWizardStepPanel);
        wizardPanels = (WizardStepPanel[])panels.toArray(new WizardStepPanel[]{});

        tabbedPane.add(subjectConfirmationWizardStepPanel.getStepLabel(), subjectConfirmationWizardStepPanel);
        tabbedPane.add(subjectConfirmationNameIdentifierWizardStepPanel.getStepLabel(), subjectConfirmationNameIdentifierWizardStepPanel);
        tabbedPane.add(conditionsWizardStepPanel.getStepLabel(), conditionsWizardStepPanel);


        wizardPanelChangeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                for (int j = 0; j < wizardPanels.length; j++) {
                    WizardStepPanel panel = wizardPanels[j];
                    if (!panel.canAdvance()) {
                        buttonOk.setEnabled(false);
                        return;
                    }
                }
                buttonOk.setEnabled(true);
            }
        };
        for (int i = 0; i < wizardPanels.length; i++) {
            WizardStepPanel wizardPanel = wizardPanels[i];
            wizardPanel.readSettings(assertion);
            wizardPanel.addChangeListener(wizardPanelChangeListener);
        }
        Actions.setEscKeyStrokeDisposes(this);
        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        buttonOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < wizardPanels.length; i++) {
                    WizardStepPanel wizardPanel = wizardPanels[i];
                    wizardPanel.storeSettings(assertion);
                }
                assertionChanged = true;
                dispose();
            }
        });
        Utilities.equalizeButtonSizes(new JButton[]{buttonCancel, buttonOk});
    }
}