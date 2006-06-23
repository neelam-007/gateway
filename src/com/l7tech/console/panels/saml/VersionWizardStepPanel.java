package com.l7tech.console.panels.saml;

import java.awt.*;
import javax.swing.*;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;

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

    /**
     * Creates new form Version WizardPanel
     */
    public VersionWizardStepPanel(WizardStepPanel next, boolean showTitleLabel) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        initialize();
    }

    /**
     * Creates new form Version WizardPanel
     */
    public VersionWizardStepPanel(WizardStepPanel next) {
        this(next, true);
    }

    /**
     * Creates new form Version WizardPanel
     */
    public VersionWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, JDialog owner) {
        super(next);
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
        RequestWssSaml requestWssSaml = (RequestWssSaml)settings;

        Integer version = requestWssSaml.getVersion();

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
        RequestWssSaml requestWssSaml = (RequestWssSaml) settings;

        if (radioButtonAnyVersion.isSelected()) {
            requestWssSaml.setVersion(Integer.valueOf(0));
        }
        else if(radioButtonVersion1.isSelected()) {
            requestWssSaml.setVersion(Integer.valueOf(1));
        }
        else if(radioButtonVersion2.isSelected()) {
            requestWssSaml.setVersion(Integer.valueOf(2));
        }
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

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));

        ButtonGroup versionGroup = new ButtonGroup();
        versionGroup.add(radioButtonAnyVersion);
        versionGroup.add(radioButtonVersion1);
        versionGroup.add(radioButtonVersion2);
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "SAML Version";
    }

    public String getDescription() {
        return
        "<html>Specify the SAML Version(s) that will be accepted by the gateway.</html>";
    }
}
