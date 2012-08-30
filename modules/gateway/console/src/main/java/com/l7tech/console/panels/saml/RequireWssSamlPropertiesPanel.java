package com.l7tech.console.panels.saml;

import com.l7tech.gui.util.Utilities;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.LegacyAssertionPropertyDialog;
import com.l7tech.policy.assertion.xmlsec.RequireSaml;

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
// TODO [Donal] - rename panel
public class RequireWssSamlPropertiesPanel<AT extends RequireSaml> extends LegacyAssertionPropertyDialog {
    private JTabbedPane tabbedPane;
    private JButton buttonOk;
    private JButton buttonCancel;
    private JPanel mainPanel;
    private RequireSaml assertion;
    private boolean assertionChanged;
    private WizardStepPanel[] wizardPanels;
    private ChangeListener wizardPanelChangeListener;
    private final boolean hasOptionalSignature;

    /**
     * Creates new wizard
     */
    public RequireWssSamlPropertiesPanel(AT assertion, Frame parent, boolean modal, boolean readOnly, boolean hasOptionalSignature) {
        super(parent, assertion, modal);
        this.assertion = assertion;
        this.hasOptionalSignature = hasOptionalSignature;
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        /** Set content pane */
        contentPane.add(mainPanel, BorderLayout.CENTER);
        initialize(readOnly);
    }

    /**
     * @return true if the assertion was changed by this dialog
     */
    public boolean hasAssertionChanged() {
        return assertionChanged;
    }

    private void initialize(final boolean readOnly) {
        setResizable(true);

        final Border emptyBorder = BorderFactory.createEmptyBorder(10, 10, 5, 10);
        Collection panels = new ArrayList();

        VersionWizardStepPanel versionWizardStepPanel = new VersionWizardStepPanel(null, false, this);
        versionWizardStepPanel.setBorder(emptyBorder);
        panels.add(versionWizardStepPanel);
        tabbedPane.add(versionWizardStepPanel.getStepLabel(), versionWizardStepPanel);        

        if (assertion.getAuthenticationStatement() != null) {
            AuthenticationMethodsNewWizardStepPanel authenticationMethodsWizardStepPanel = new AuthenticationMethodsNewWizardStepPanel(null, false, this);
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
        SubjectConfirmationWizardStepPanel subjectConfirmationWizardStepPanel = new SubjectConfirmationWizardStepPanel(null, false, this, false);
        subjectConfirmationWizardStepPanel.setBorder(emptyBorder);
        panels.add(subjectConfirmationWizardStepPanel);

        SubjectConfirmationNameIdentifierWizardStepPanel subjectConfirmationNameIdentifierWizardStepPanel = new SubjectConfirmationNameIdentifierWizardStepPanel(null, false, this);
        subjectConfirmationNameIdentifierWizardStepPanel.setBorder(emptyBorder);
        panels.add(subjectConfirmationNameIdentifierWizardStepPanel);

        final RequireEmbeddedSignatureWizardStepPanel signatureWizardStepPanel;

        // create early as may or may not need to show it and need to set up preceding step correctly if it should be shown.
        if (hasOptionalSignature) {
            signatureWizardStepPanel = new RequireEmbeddedSignatureWizardStepPanel(null, false, this);
        } else {
            signatureWizardStepPanel = null;
        }

        ConditionsWizardStepPanel conditionsWizardStepPanel = new ConditionsWizardStepPanel(signatureWizardStepPanel, false, this);
        conditionsWizardStepPanel.setBorder(emptyBorder);
        panels.add(conditionsWizardStepPanel);

        tabbedPane.add(subjectConfirmationWizardStepPanel.getStepLabel(), subjectConfirmationWizardStepPanel);
        tabbedPane.add(subjectConfirmationNameIdentifierWizardStepPanel.getStepLabel(), subjectConfirmationNameIdentifierWizardStepPanel);
        tabbedPane.add(conditionsWizardStepPanel.getStepLabel(), conditionsWizardStepPanel);

        if (hasOptionalSignature) {
            tabbedPane.add(signatureWizardStepPanel.getStepLabel(), signatureWizardStepPanel);
            panels.add(signatureWizardStepPanel);
        }

        wizardPanels = (WizardStepPanel[])panels.toArray(new WizardStepPanel[]{});

        // Save / restore when changing tabs
        tabbedPane.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                AT workingCopy = (AT) assertion.clone();
                for (int i = 0; i < wizardPanels.length; i++) {
                    WizardStepPanel wizardPanel = wizardPanels[i];
                    wizardPanel.storeSettings(workingCopy);
                }
                ((WizardStepPanel)tabbedPane.getSelectedComponent()).readSettings(workingCopy);
            }
        });

        wizardPanelChangeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                for (int j = 0; j < wizardPanels.length; j++) {
                    WizardStepPanel panel = wizardPanels[j];
                    if (!panel.canAdvance()) {
                        buttonOk.setEnabled(false);
                        return;
                    }
                }
                buttonOk.setEnabled(!readOnly);
            }
        };
        for (int i = 0; i < wizardPanels.length; i++) {
            WizardStepPanel wizardPanel = wizardPanels[i];
            wizardPanel.readSettings(assertion);
            wizardPanel.addChangeListener(wizardPanelChangeListener);
        }
        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        buttonOk.setEnabled(!readOnly);
        buttonOk.addActionListener(new ActionListener() {
            @Override
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
        pack();
    }
}
