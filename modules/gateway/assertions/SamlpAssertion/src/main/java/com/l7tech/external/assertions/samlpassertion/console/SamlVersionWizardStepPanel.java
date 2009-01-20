package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.external.assertions.samlpassertion.SamlpRequestBuilderAssertion;
import com.l7tech.external.assertions.samlpassertion.SamlProtocolAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The SAML version selection <code>WizardStepPanel</code>
 *
 * @author $Author: megery $
 * @version $Revision: 21105 $
 */
public class SamlVersionWizardStepPanel extends SamlpWizardStepPanel {
    private JPanel mainPanel;
    private JLabel titleLabel;
    private boolean showTitleLabel;
    private JRadioButton radioButtonVersion1;
    private JRadioButton radioButtonVersion2;
    private JLabel versionLabel;
    private JRadioButton radioButtonRequestIDGenerate;
    private JRadioButton radioButtonRequestIDFromVar;
    private JTextField textFieldRequestIDVariable;
    private JLabel requestIDLabel;
    private JLabel optionalAttributesLabel;
    private JLabel destinationLabel;
    private JLabel consentLabel;
    private JTextField textFieldDestination;
    private JTextField textFieldConsent;
    private JPanel requestIdPanel;
    private JPanel optionalRequesAttrPanel;

    /**
     * Creates new form Version WizardPanel
     */
    public SamlVersionWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, AssertionMode mode) {
        super(next, mode);
        this.showTitleLabel = showTitleLabel;
        initialize();
    }

    /**
     * Creates new form Version WizardPanel
     */
    public SamlVersionWizardStepPanel(WizardStepPanel next, AssertionMode mode) {
        this(next, true, mode);
    }

    /**
     * Creates new form Version WizardPanel
     */
    public SamlVersionWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, AssertionMode mode, JDialog owner) {
        super(next, mode);
        this.showTitleLabel = showTitleLabel;
        setOwner(owner);
        initialize();
    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        SamlProtocolAssertion assertion = SamlProtocolAssertion.class.cast(settings);

        Integer version = assertion.getSamlVersion();
        if (version == null) {
            version = 1;
        }

        switch(version) {
            case 1:
                radioButtonVersion1.setSelected(true);
                break;
            case 2:
                radioButtonVersion2.setSelected(true);
                break;
        }

        if (isRequestMode()) {
            readRequestSpecific(assertion);
        }
    }

    /**
     * Parses the request specific configurations from the assertion and updates the appropriate UI component.
     *
     * @param samlpAssertion the SAMLP assertion instance
     */
    private void readRequestSpecific(SamlProtocolAssertion samlpAssertion) {

        SamlpRequestBuilderAssertion assertion = SamlpRequestBuilderAssertion.class.cast(samlpAssertion);

        if (assertion.getRequestId() != null && assertion.getRequestId() == 1) {
            radioButtonRequestIDFromVar.setSelected(true);
            textFieldRequestIDVariable.setEnabled(true);
            textFieldRequestIDVariable.setText(assertion.getRequestIdVariable());
        } else {
            radioButtonRequestIDGenerate.setSelected(true);
            textFieldRequestIDVariable.setEnabled(false);
        }

        textFieldDestination.setText(assertion.getDestinationAttribute());
        textFieldConsent.setText(assertion.getConsentAttribute());
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

        SamlProtocolAssertion assertion = SamlProtocolAssertion.class.cast(settings);

        if (radioButtonVersion1.isSelected()) {
            assertion.setSamlVersion(1);
        } else if(radioButtonVersion2.isSelected()) {
            assertion.setSamlVersion(2);
        }

        if (isRequestMode()) {
            storeRequestSpecific(assertion);
        }
    }

    /**
     * Updates request specific configurations to the assertion object.
     *
     * @param samlpAssertion the SAMLP assertion instance
     */
    private void storeRequestSpecific(SamlProtocolAssertion samlpAssertion) {

        SamlpRequestBuilderAssertion assertion = SamlpRequestBuilderAssertion.class.cast(samlpAssertion);

        if (radioButtonRequestIDGenerate.isSelected()) {
            assertion.setRequestId(0);
            assertion.setRequestIdVariable(null);
        } else {
            assertion.setRequestId(1);
            assertion.setRequestIdVariable(textFieldRequestIDVariable.getText());
        }
        assertion.setDestinationAttribute(textFieldDestination.getText());
        assertion.setConsentAttribute(textFieldConsent.getText());
    }
    
    private void initialize() {
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);

        ButtonGroup versionGroup = new ButtonGroup();
        versionGroup.add(radioButtonVersion1);
        versionGroup.add(radioButtonVersion2);
        radioButtonVersion1.setText("Version 1.1");
        radioButtonVersion2.setText("Version 2.0");

        if (isRequestMode()) {
            versionLabel.setText("Create a SAMLP query request with the following version");
            radioButtonVersion1.setToolTipText("Create a Version 1.1 SAMLP Request");
            radioButtonVersion2.setToolTipText("Create a Version 2.0 SAMLP Request");
            initializeRequestIDButtons();
            requestIdPanel.setVisible(true);
            optionalRequesAttrPanel.setVisible(true);
        } else {
            versionLabel.setText("Evaluate a SAMLP response with the following version");
            radioButtonVersion1.setToolTipText("Evaluate a Version 1.1 SAMLP Response");
            radioButtonVersion2.setToolTipText("Evaluate a Version 2.0 SAMLP Response");
            requestIdPanel.setVisible(false);
            optionalRequesAttrPanel.setVisible(false);
        }

        if (showTitleLabel) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.getParent().remove(titleLabel);
        }

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

    }

    /**
     * Initializes the Request Identifier radio button group.
     */
    protected void initializeRequestIDButtons() {

        // create RequestID ButtonGroup
        ButtonGroup requestIDGroup = new ButtonGroup();
        requestIDGroup.add(radioButtonRequestIDGenerate);
        requestIDGroup.add(radioButtonRequestIDFromVar);

        radioButtonRequestIDGenerate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (radioButtonRequestIDGenerate.isSelected()) {
                    textFieldRequestIDVariable.setEnabled(false);
                }
            }
        });

        radioButtonRequestIDFromVar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (radioButtonRequestIDFromVar.isSelected()) {
                    textFieldRequestIDVariable.setEnabled(true);
                }
            }
        });
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "SAML Version";
    }

    public String getDescription() {
        if (isRequestMode()) {
            return "<html>Specify the version of the SAMLP query request that will be created.</html>";
        } else {
            return "<html>Specify the version of the SAMLP response that will be evaluated by this assertion.</html>";
        }
    }
}