package com.l7tech.console.panels;

import com.l7tech.console.util.Preferences;

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
    public static final String UDDI_ACCOUNT_NAME = "UDDI.ACCOUNT.NAME";
    public static final String UDDI_URL = "UDDI.URL";
    private String panelDescription = "Provide the UDDI registry URL and account information to publish this policy";
    private JLabel descLabel;

    public UDDITargetWizardStep(WizardStepPanel next) {
        super(next);
        initialize();
    }

    public String getDescription() {
        return panelDescription;
    }

    public void setPanelDescription(String panelDescription) {
        this.panelDescription = panelDescription;
        if (descLabel != null) descLabel.setText(panelDescription);
    }

    public String getStepLabel() {
        return "UDDI Target";
    }

    public boolean canFinish() {
        return false;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
        String tmp = Preferences.getPreferences().getString(UDDI_ACCOUNT_NAME, "");
        uddiAccountNameField.setText(tmp);
        tmp = Preferences.getPreferences().getString(UDDI_URL, "");
        uddiURLField.setText(tmp);
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
        Preferences.getPreferences().putProperty(UDDI_URL, tmp);
        tmp = uddiAccountNameField.getText();
        if (tmp == null || tmp.length() < 1) {
            showError("UDDI account name cannot be empty");
            return false;
        }
        Preferences.getPreferences().putProperty(UDDI_ACCOUNT_NAME, tmp);
        tmp = uddiAccountPasswdField.getText();
        if (tmp == null || tmp.length() < 1) {
            showError("UDDI account password cannot be empty");
            return false;
        }
        return true;
    }

    public void storeSettings(Object settings) throws IllegalArgumentException {
        Data data = (Data)settings;
        data.setUddiurl(uddiURLField.getText());
        data.setAccountName(uddiAccountNameField.getText());
        data.setAccountPasswd(uddiAccountPasswdField.getText());
    }

    private void showError(String err) {
        JOptionPane.showMessageDialog(this, err, "Invalid Input", JOptionPane.ERROR_MESSAGE);
    }

    public interface Data {
        void setUddiurl(String in);
        void setAccountName(String in);
        void setAccountPasswd(String in);
    }
}
