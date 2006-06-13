package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.logging.Logger;

/**
 * Wizard step in the PublishPolicyToUDDIWizard wizard pertaining
 * to getting the UDDI target urls and account credentials.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 12, 2006<br/>
 */
public class UDDITargetWizardStep extends WizardStepPanel {
    private JPanel mainPanel;
    private JTextField uddiURLField;
    private JTextField uddiAccountNameField;
    private JTextField uddiAccountPasswdField;
    private static final Logger logger = Logger.getLogger(UDDITargetWizardStep.class.getName());

    public UDDITargetWizardStep(WizardStepPanel next) {
        super(next);
        initialize();
    }

    public String getDescription() {
        return "Provide the UDDI registry URL and account information to publish this policy";
    }

    public String getStepLabel() {
        return "UDDI Target";
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
        // todo, read the previous uddi URL used
        // todo, populate the previous admin account used
    }

    public boolean onNextButton() {
        // make sure values are legal
        String tmp = uddiURLField.getText();
        if (tmp == null || tmp.length() < 1) {
            showError("UDDI URL cannot be empty");
            return false;
        } else {
            try {
                new URL(tmp);
            } catch (MalformedURLException e) {
                showError("Invalid UDDI URL: " + tmp);
                return false;
            }
        }
        tmp = uddiAccountNameField.getText();
        if (tmp == null || tmp.length() < 1) {
            showError("UDDI account name cannot be empty");
            return false;
        }
        tmp = uddiAccountPasswdField.getText();
        if (tmp == null || tmp.length() < 1) {
            showError("UDDI account password cannot be empty");
            return false;
        }
        return true;
    }

    public void storeSettings(Object settings) throws IllegalArgumentException {
        PublishPolicyToUDDIWizard.Data data = (PublishPolicyToUDDIWizard.Data)settings;
        data.setUddiurl(uddiURLField.getText());
        data.setAccountName(uddiAccountNameField.getText());
        data.setAccountPasswd(uddiAccountPasswdField.getText());
    }

    private void showError(String err) {
        // todo
        logger.warning(err);
    }
}
