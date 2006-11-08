package com.l7tech.console.panels;

import com.l7tech.common.util.TextUtils;
import com.l7tech.console.util.SsmPreferences;
import com.l7tech.console.util.TopComponents;
import org.systinet.uddi.InvalidParameterException;
import org.systinet.uddi.client.v3.UDDIException;
import org.systinet.uddi.client.v3.UDDISecurityStub;
import org.systinet.uddi.client.v3.UDDI_Security_PortType;
import org.systinet.uddi.client.v3.struct.Get_authToken;

import javax.swing.*;
import javax.xml.soap.SOAPException;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
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
    private final SsmPreferences preferences = TopComponents.getInstance().getPreferences();

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
        String tmp = preferences.getString(UDDI_ACCOUNT_NAME, "");
        uddiAccountNameField.setText(tmp);
        tmp = preferences.getString(UDDI_URL, "");
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
        String url = normalizeURL(tmp);
        uddiURLField.setText(url);
        preferences.putProperty(UDDI_URL, url);

        tmp = uddiAccountNameField.getText();
        if (tmp == null || tmp.length() < 1) {
            showError("UDDI account name cannot be empty");
            return false;
        }
        String name = tmp;
        preferences.putProperty(UDDI_ACCOUNT_NAME, name);

        tmp = uddiAccountPasswdField.getText();
        if (tmp == null || tmp.length() < 1) {
            showError("UDDI account password cannot be empty");
            return false;
        }

        // try the credentials
        // (bugzilla #2601 preemptively try the credentials)
        try {
            testAuthInfo(url, name, tmp);
        } catch (SOAPException e) {
            String msg = "Could not get UDDI auth_token using this target and these credentials.";
            Throwable t = e;
            while (t.getCause() != null) t = t.getCause();
            showError(msg + " " + catchDispositionReport(t.getMessage()));
            logger.log(Level.WARNING, msg, e);
            return false;
        } catch (UDDIException e) {
            String msg = "Could not get UDDI auth_token using this target and these credentials.";
            Throwable t = e;
            while (t.getCause() != null) t = t.getCause();
            showError(msg + " " + catchDispositionReport(t.getMessage()));
            logger.log(Level.WARNING, msg, e);
            return false;
        } catch (InvalidParameterException e) {
            String msg = "Could not get UDDI auth_token using this target and these credentials.";
            Throwable t = e;
            while (t.getCause() != null) t = t.getCause();
            showError(msg + " " + catchDispositionReport(t.getMessage()));
            logger.log(Level.WARNING, msg, e);
            return false;
        } catch (Exception e) {
            String msg = "Could not get UDDI auth_token using this target and these credentials.";
            Throwable t = e;
            while (t.getCause() != null) t = t.getCause();
            showError(msg + " " + catchDispositionReport(t.getMessage()));
            logger.log(Level.WARNING, msg, e);
            return false;
        }
        return true;
    }

    private String catchDispositionReport(String msg) {
        String output = msg;
        if (msg != null && msg.startsWith("<dispositionReport")) {
            int end = msg.indexOf("</errInfo>");
            if (end > 0) {
                int start = end-1;
                while (start > 0) {
                    if (msg.charAt(start) == '>') break;
                    start--;
                }
                output = msg.substring(start+1, end);
            }
        }
        return output;
    }

    private String normalizeURL(String uddiurl) {
        if (uddiurl.indexOf("/uddi") < 1) {
            if (uddiurl.endsWith("/")) {
                uddiurl = uddiurl + "uddi/";
            } else {
                uddiurl = uddiurl + "/uddi/";
            }
        }
        if (!uddiurl.endsWith("/")) {
            uddiurl = uddiurl + "/";
        }
        return uddiurl;
    }

    private String testAuthInfo(String url, String accountName, String accountpasswd) throws SOAPException, UDDIException, InvalidParameterException {
        String authInfo;
        UDDI_Security_PortType security = UDDISecurityStub.getInstance(url + "security");
        authInfo = security.get_authToken(new Get_authToken(accountName, accountpasswd)).getAuthInfo();
        return authInfo;
    }

    public void storeSettings(Object settings) throws IllegalArgumentException {
        Data data = (Data)settings;
        data.setUddiurl(uddiURLField.getText());
        data.setAccountName(uddiAccountNameField.getText());
        data.setAccountPasswd(uddiAccountPasswdField.getText());
    }

    public interface Data {
        void setUddiurl(String in);
        void setAccountName(String in);
        void setAccountPasswd(String in);
    }

    private void showError(String err) {
        JOptionPane.showMessageDialog(this, TextUtils.breakOnMultipleLines(err, 30),
                                      "Error", JOptionPane.ERROR_MESSAGE);
    }
}
