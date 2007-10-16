package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.SamlPolicyAssertion;
import com.l7tech.policy.assertion.SamlIssuerAssertion;

import javax.swing.*;
import java.awt.*;

/**
 * The SAML version selection <code>WizardStepPanel</code>
 *
 * @author $Author$
 * @version $Revision$
 */
public class VersionWizardStepPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JLabel titleLabel;
    private boolean showTitleLabel;
    private JRadioButton radioButtonVersion1;
    private JRadioButton radioButtonVersion2;
    private JRadioButton radioButtonAnyVersion;
    private JLabel versionLabel;
    private JCheckBox signAssertionCheckBox;
    private boolean issueMode;

    /**
     * Creates new form Version WizardPanel
     */
    public VersionWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, boolean issueMode) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        this.issueMode = issueMode;
        initialize();
    }

    /**
     * Creates new form Version WizardPanel
     */
    public VersionWizardStepPanel(WizardStepPanel next, boolean issueMode) {
        this(next, true, issueMode);
    }

    /**
     * Creates new form Version WizardPanel
     */
    public VersionWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, boolean issueMode, JDialog owner) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        this.issueMode = issueMode;
        setOwner(owner);
        initialize();
    }

    public VersionWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, JDialog parent) {
        this(next, showTitleLabel, false, parent);
    }

    public VersionWizardStepPanel(WizardStepPanel next) {
        this(next, true, false);
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
        SamlPolicyAssertion spa = (SamlPolicyAssertion) settings;

        Integer version = spa.getVersion();

        if (version == null) {
            version = Integer.valueOf(1);
        }

        switch(version.intValue()) {
            case 0:
                radioButtonAnyVersion.setSelected(true);
                break;
            case 1:
                radioButtonVersion1.setSelected(true);
                break;
            case 2:
                radioButtonVersion2.setSelected(true);
                break;
        }

        if (issueMode && spa instanceof SamlIssuerAssertion) {
            SamlIssuerAssertion sia = (SamlIssuerAssertion) spa;
            signAssertionCheckBox.setSelected(sia.isSignAssertion());
        }
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
        SamlPolicyAssertion spa = (SamlPolicyAssertion) settings;

        if (radioButtonAnyVersion.isSelected()) {
            spa.setVersion(Integer.valueOf(0));
        } else if(radioButtonVersion1.isSelected()) {
            spa.setVersion(Integer.valueOf(1));
        } else if(radioButtonVersion2.isSelected()) {
            spa.setVersion(Integer.valueOf(2));
        }

        if (issueMode && spa instanceof SamlIssuerAssertion) {
            SamlIssuerAssertion sia = (SamlIssuerAssertion) spa;
            sia.setSignAssertion(signAssertionCheckBox.isSelected());
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);

        ButtonGroup versionGroup = new ButtonGroup();
        versionGroup.add(radioButtonVersion1);
        versionGroup.add(radioButtonVersion2);
        if (issueMode) {
            radioButtonAnyVersion.setVisible(false);
            versionLabel.setText("Issue a SAML assertion with the following version");
            radioButtonVersion1.setText("Version 1.1");
            radioButtonVersion1.setToolTipText("Issue a Version 1.1 SAML Assertion");
            radioButtonVersion2.setText("Version 2.0");
            radioButtonVersion2.setToolTipText("Issue a Version 2.0 SAML Assertion");
            signAssertionCheckBox.setVisible(true);
        } else {
            versionGroup.add(radioButtonAnyVersion);
            signAssertionCheckBox.setVisible(false);
        }

        if (showTitleLabel) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.getParent().remove(titleLabel);
        }

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "SAML Version";
    }

    public String getDescription() {
        if (issueMode) {
            return "<html>Specify the version of the SAML assertion that will be issued.</html>";
        } else {
            return "<html>Specify the SAML Version(s) that will be accepted by the gateway.</html>";
        }
    }
}
