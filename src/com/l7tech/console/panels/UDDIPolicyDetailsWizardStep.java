package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URL;
import java.net.MalformedURLException;

import com.l7tech.common.util.TextUtils;
import com.l7tech.common.uddi.UDDIClient;
import com.l7tech.common.uddi.UDDIException;

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
    private JButton registerButton;
    private PublishPolicyToUDDIWizard.Data data;

    public UDDIPolicyDetailsWizardStep(WizardStepPanel next) {
        super(next);
        initialize();
    }

    public String getDescription() {
        return "Provide UDDI details for this policy and create a tModel for it";
    }

    public String getStepLabel() {
        return "Policy Details";
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        KeyListener validValuesPolice = new KeyListener() {
            public void keyTyped(KeyEvent e) {
                registerButton.setEnabled(validateValues(true));
            }
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {}
        };
        policyNameField.addKeyListener(validValuesPolice);
        policyURLField.addKeyListener(validValuesPolice);
        policyDescField.addKeyListener(validValuesPolice);
        registerButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                if (validateValues(false)) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            UDDIPolicyDetailsWizardStep.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            try {
                                publishPolicyReferenceToUDDIDirectory();
                            } finally {
                                UDDIPolicyDetailsWizardStep.this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                            }
                        }
                    });
                }
            }
        });
    }

    public boolean validateValues(boolean silent) {
        // make sure values are legal
        String tmp = policyNameField.getText();
        if (tmp == null || tmp.length() < 1) {
            if (!silent) showError("Policy name cannot be empty");
            return false;
        }
        tmp = policyDescField.getText();
        if (tmp == null || tmp.length() < 1) {
            if (!silent) showError("Policy description cannot be empty");
            return false;
        }
        tmp = policyURLField.getText();
        if (tmp == null || tmp.length() < 1) {
            if (!silent) showError("Policy URL cannot be empty");
            return false;
        } else {
            try {
                new URL(tmp);
            } catch (MalformedURLException e) {
                if (!silent) showError(tmp + " is not a valid URL");
                return false;
            }
        }
        return true;
    }

    public void storeSettings(Object settings) throws IllegalArgumentException {
        PublishPolicyToUDDIWizard.Data data = (PublishPolicyToUDDIWizard.Data)settings;
        data.setPolicyName(policyNameField.getText());
        data.setPolicyDescription(policyDescField.getText());
        data.setCapturedPolicyURL(policyURLField.getText());
    }

    public boolean canAdvance() {
        return data.getPolicytModelKey() != null && data.getPolicytModelKey().length() > 0;
    }

    private void showError(String err) {
        JOptionPane.showMessageDialog(this, TextUtils.breakOnMultipleLines(err, 30), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void publishPolicyReferenceToUDDIDirectory() {
        // create a tmodel to save
        try {
            UDDIClient uddi = data.getUddi();

            String policyUrl = policyURLField.getText();

            String tModelKey = uddi.publishPolicy(policyNameField.getText(), policyDescField.getText(), policyUrl);
            data.setPolicytModelKey(tModelKey);

            String msg = "Publication successful, policy tModel key: " + data.getPolicytModelKey() +
                         " choose 'Next' below to associate this policy tModel to " +
                         "a business service or 'Finish' to end.";
            JOptionPane.showConfirmDialog(this, TextUtils.breakOnMultipleLines(msg, 30), "Success", JOptionPane.DEFAULT_OPTION);

            notifyListeners();
            registerButton.setEnabled(false);

            logger.info("Published policy with key: " + data.getPolicytModelKey());
        } catch (UDDIException e) {
            logger.log(Level.WARNING, "Cannot publish policy model.", e);
            showError("ERROR cannot publish policy model. " + e.getMessage());
            return;
        } 
    }

    public static String getRootCauseMsg(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null) root = root.getCause();
        return root.getMessage();
    }

    public void readSettings(Object settings) throws IllegalArgumentException {
        data = (PublishPolicyToUDDIWizard.Data)settings;

        policyNameField.setText(data.getPolicyName());
        policyDescField.setText(data.getPolicyDescription());
        policyURLField.setText( data.getCapturedPolicyURL());

        if (canAdvance()) {
            registerButton.setEnabled(false);            
        }
    }
}
