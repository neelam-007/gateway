/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.console.action.Actions;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;

/**
 * The <code>AuthenticationStatementPropertiesPanel</code> edits the
 * configuration of the SAML authentication statement constraints.
 *
 * @author emil
 * @version Jan 18, 2005
 */
public class AuthenticationStatementPropertiesPanel extends JDialog {
    private JTabbedPane tabbedPane;
    private JButton buttonOk;
    private JButton buttonCancel;
    private JPanel mainPanel;
    private SamlAuthenticationStatement assertion;
    private boolean assertionChanged;
    private WizardStepPanel[] wizardPanels;

    /**
     * Creates new wizard
     */
    public AuthenticationStatementPropertiesPanel(SamlAuthenticationStatement assertion, Frame parent, boolean modal) {
        super(parent);
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
        this.assertion = assertion;
        setTitle("SAML Authentication Statement Constraints");
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
        AuthenticationMethodsWizardStepPanel authenticationMethodsWizardStepPanel = new AuthenticationMethodsWizardStepPanel(null, false);
        authenticationMethodsWizardStepPanel.setBorder(emptyBorder);
        panels.add(authenticationMethodsWizardStepPanel);

        SubjectConfirmationWizardStepPanel subjectConfirmationWizardStepPanel = new SubjectConfirmationWizardStepPanel(null, false);
        subjectConfirmationWizardStepPanel.setBorder(emptyBorder);
        panels.add(subjectConfirmationWizardStepPanel);

        SubjectConfirmationNameIdentifierWizardStepPanel subjectConfirmationNameIdentifierWizardStepPanel = new SubjectConfirmationNameIdentifierWizardStepPanel(null, false);
        subjectConfirmationNameIdentifierWizardStepPanel.setBorder(emptyBorder);
        panels.add(subjectConfirmationNameIdentifierWizardStepPanel);

        ConditionsWizardStepPanel conditionsWizardStepPanel = new ConditionsWizardStepPanel(null, false);
        conditionsWizardStepPanel.setBorder(emptyBorder);
        panels.add(conditionsWizardStepPanel);
        wizardPanels = (WizardStepPanel[])panels.toArray(new WizardStepPanel[]{});

        tabbedPane.add(authenticationMethodsWizardStepPanel.getStepLabel(), authenticationMethodsWizardStepPanel);
        tabbedPane.add(subjectConfirmationWizardStepPanel.getStepLabel(), subjectConfirmationWizardStepPanel);
        tabbedPane.add(subjectConfirmationNameIdentifierWizardStepPanel.getStepLabel(), subjectConfirmationNameIdentifierWizardStepPanel);
        tabbedPane.add(conditionsWizardStepPanel.getStepLabel(), conditionsWizardStepPanel);


        final ChangeListener changeListener = new ChangeListener() {
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
            wizardPanel.addChangeListener(changeListener);
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