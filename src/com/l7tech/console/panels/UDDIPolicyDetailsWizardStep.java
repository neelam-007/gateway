package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Wizard step in the PublishPolicyToUDDIWizard wizard pertaining
 * to describing the policy to publish a reference to.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 12, 2006<br/>
 */
public class UDDIPolicyDetailsWizardStep extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger(UDDIPolicyDetailsWizardStep.class.getName());
    private JPanel mainPanel;
    private JTextField policyNameField;
    private JTextField policyDescField;
    private JTextField policyURLField;
    private final String policyURL;
    private final String serviceName;

    public UDDIPolicyDetailsWizardStep(WizardStepPanel next, String policyURL, String serviceName) {
        super(next);
        this.policyURL = policyURL;
        this.serviceName = serviceName;
        initialize();
    }

    public String getDescription() {
        return "Provide UDDI details for this policy";
    }

    public String getStepLabel() {
        return "Policy Details";
    }

    public boolean canFinish() {
        return false;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
        policyNameField.setText(serviceName);
        policyURLField.setText(policyURL);
        policyDescField.setText("A policy for service " + serviceName);
    }

    public boolean onNextButton() {
        // make sure values are legal
        String tmp = policyNameField.getText();
        if (tmp == null || tmp.length() < 1) {
            showError("Policy name cannot be empty");
            return false;
        }
        tmp = policyDescField.getText();
        if (tmp == null || tmp.length() < 1) {
            showError("Policy description cannot be empty");
            return false;
        }
        tmp = policyURLField.getText();
        if (tmp == null || tmp.length() < 1) {
            showError("Policy URL cannot be empty");
            return false;
        } else {
            try {
                new URL(tmp);
            } catch (MalformedURLException e) {
                showError(tmp + " is not a valid URL");
                return false;
            }
        }
        return true;
    }

    public void storeSettings(Object settings) throws IllegalArgumentException {
        PublishPolicyToUDDIWizard.Data data = (PublishPolicyToUDDIWizard.Data)settings;
        data.setPolicyName(policyNameField.getText());
        data.setPolicyDescription(policyURLField.getText());
        data.setCapturedPolicyURL(policyURLField.getText());
    }

    private void showError(String err) {
        // todo
        logger.warning(err);
    }
}
