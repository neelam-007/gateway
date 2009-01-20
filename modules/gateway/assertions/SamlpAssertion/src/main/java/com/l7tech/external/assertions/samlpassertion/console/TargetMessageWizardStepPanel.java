package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.TargetMessagePanel;
import com.l7tech.external.assertions.samlpassertion.SamlProtocolAssertion;

import javax.swing.*;
import java.awt.*;

/**
 * The SAML version selection <code>WizardStepPanel</code>
 *
 * @author $Author: alex $
 * @version $Revision: 17497 $
 */
public class TargetMessageWizardStepPanel extends SamlpWizardStepPanel {
    private JPanel mainPanel;
    private JPanel soapPanel;
    private JLabel titleLabel;
    private boolean showTitleLabel;
    private JRadioButton radioButtonVersionDefault;
    private JRadioButton radioButtonVersion11;
    private JRadioButton radioButtonVersion12;
    private JPanel targetMessagePanelHolder;
    private TargetMessagePanel targetMessagePanel = new TargetMessagePanel();


    private static final String REQUEST_TITLE = "Target Message and SOAP version";
    private static final String RESPONSE_TITLE = "Target Message";

    /**
     * Creates new form Version WizardPanel
     */
    public TargetMessageWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, AssertionMode mode) {
        super(next, mode);
        this.showTitleLabel = showTitleLabel;
        initialize();
    }

    private void customizeForMode() {
        switch (getMode()) {
            case REQUEST:
                soapPanel.setVisible(true);
                titleLabel.setText(REQUEST_TITLE);
                break;
            case RESPONSE:
                soapPanel.setVisible(false);
                titleLabel.setText(RESPONSE_TITLE);
                break;
        }
    }

    /**
     * Creates new form Version WizardPanel
     */
    public TargetMessageWizardStepPanel(WizardStepPanel next, AssertionMode mode) {
        this(next, true, mode);
    }

    /**
     * Creates new form Version WizardPanel
     */
    public TargetMessageWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, AssertionMode mode, JDialog owner) {
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
        SamlProtocolAssertion assertion = (SamlProtocolAssertion) settings;

        Integer version = assertion.getSoapVersion();

        if (version == null) {
            version = 0;
        }

        switch(version) {
            case 0:
                radioButtonVersionDefault.setSelected(true);
                break;
            case 1:
                radioButtonVersion11.setSelected(true);
                break;
            case 2:
                radioButtonVersion12.setSelected(true);
                break;
        }

        // init targetMessage panel
        targetMessagePanel.setModel(assertion);
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
        SamlProtocolAssertion assertion = (SamlProtocolAssertion) settings;

        if (isRequestMode()) {
            // SOAP version only used for request
            if (radioButtonVersionDefault.isSelected()) {
                assertion.setSoapVersion(0);
            } else if(radioButtonVersion11.isSelected()) {
                assertion.setSoapVersion(1);
            } else if(radioButtonVersion12.isSelected()) {
                assertion.setSoapVersion(2);
            }
        }

        // update targetMessage
        targetMessagePanel.updateModel(assertion);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);

        ButtonGroup versionGroup = new ButtonGroup();
        versionGroup.add(radioButtonVersionDefault);
        versionGroup.add(radioButtonVersion11);
        versionGroup.add(radioButtonVersion12);

        if (showTitleLabel) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.getParent().remove(titleLabel);
        }

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        targetMessagePanelHolder.add(targetMessagePanel);

        customizeForMode();
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {

        if (isRequestMode()) {
            return REQUEST_TITLE;
        }
        return RESPONSE_TITLE;
    }

    public String getDescription() {
        if (isRequestMode()) {
            return "<html>Specify the target location to set the SAMLP query and the SOAP version used to build the message.</html>";
        } else {
            return "<html>Specify the location of the SAMLP response message for the evaluator to parse.</html>";
        }
    }
}