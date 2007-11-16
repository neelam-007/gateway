package com.l7tech.console.panels;

import com.l7tech.common.util.TextUtils;
import com.l7tech.common.util.ArrayUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.uddi.UDDIClient;
import com.l7tech.common.uddi.UDDIClientFactory;
import com.l7tech.common.uddi.UDDIException;
import com.l7tech.common.uddi.UDDINamedEntity;
import com.l7tech.common.uddi.UDDIAccessControlException;
import com.l7tech.common.uddi.UDDIRegistryInfo;
import com.l7tech.console.util.SsmPreferences;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;

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
    private static final Logger logger = Logger.getLogger(UDDITargetWizardStep.class.getName());
    private static final String UDDI_TYPE = "UDDI.TYPE";
    private static final String UDDI_URL = "UDDI.URL";
    private static final String UDDI_ACCOUNT_NAME = "UDDI.ACCOUNT.NAME";

    private JPanel mainPanel;
    private JComboBox uddiTypeComboBox;
    private JTextField uddiURLField;
    private JTextField uddiAccountNameField;
    private JTextField uddiAccountPasswdField;
    private JLabel descLabel;

    private final SsmPreferences preferences = TopComponents.getInstance().getPreferences();
    private final boolean requireCredentials;
    private String panelDescription = "Provide the UDDI registry URL and account information to publish this policy";
    private String policyUrl = null;
    private String policyName = null;
    private String policyKey = null;
    private UDDIRegistryInfo[] registryTypeInfo;

    public UDDITargetWizardStep(WizardStepPanel next, boolean requireCredentials) {
        super(next);
        this.requireCredentials = requireCredentials;
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

        String uddiType = preferences.getString(UDDI_TYPE, "");
        String uddiUrl = preferences.getString(UDDI_URL, "");
        String uddiAccount = preferences.getString(UDDI_ACCOUNT_NAME, "");

        uddiAccountNameField.setText(uddiAccount);
        uddiURLField.setText(uddiUrl);

        registryTypeInfo = loadRegistryTypeInfo();
        String[] typeNames = toNames(registryTypeInfo);
        uddiTypeComboBox.setModel(new DefaultComboBoxModel(typeNames));

        if ( ArrayUtils.contains( typeNames, uddiType ) ) {
            uddiTypeComboBox.setSelectedItem(uddiType);
        } else if (typeNames.length>0) {
            uddiTypeComboBox.setSelectedItem(typeNames[0]);
        }
    }

    public boolean onNextButton() {
        // make sure values are legal
        String url = uddiURLField.getText().trim();
        if (url == null || url.length() < 1) {
            showError("UDDI URL cannot be empty");
            return false;
        } else {
            try {
                new URL(url);
            } catch (MalformedURLException e) {
                showError("Invalid UDDI URL: " + url);
                return false;
            }
        }
        uddiURLField.setText(url);

        String name = uddiAccountNameField.getText();
        String password = uddiAccountPasswdField.getText();
        if ( requireCredentials ) {
            if (name == null || name.length() < 1) {
                showError("UDDI account name cannot be empty");
                return false;
            }
        }

        if ( name != null && name.length() > 0 ) {
            if (password == null || password.length() < 1) {
                showError("UDDI account password cannot be empty");
                return false;
            }
        } else {
            if (password != null && password.length() > 0) {
                showError("UDDI account password specified without account name");
                return false;
            }
        }

        String type = (String) uddiTypeComboBox.getSelectedItem();

        // store prefs
        preferences.putProperty(UDDI_TYPE, type);
        preferences.putProperty(UDDI_URL, url);
        preferences.putProperty(UDDI_ACCOUNT_NAME, name);

        return canProceed(type, url, name, password);
    }

    private UDDIRegistryInfo[] loadRegistryTypeInfo() {
        Registry registry = Registry.getDefault();
        return registry.getServiceManager().getUDDIRegistryInfo().toArray(new UDDIRegistryInfo[0]);        
    }

    private UDDIRegistryInfo getRegistryTypeInfo(UDDIRegistryInfo[] registryInfos, String name) {
        UDDIRegistryInfo info = null;

        for ( UDDIRegistryInfo currentInfo : registryInfos ) {
            if ( name.equals(currentInfo.getName()) ) {
                info = currentInfo;
                break;
            }
        }

        return info;
    }

    private String[] toNames(UDDIRegistryInfo[] registryInfos) {
        String[] names = new String[registryInfos.length];

        for (int i=0; i<registryInfos.length; i++) {
            UDDIRegistryInfo info = registryInfos[i];
            names[i] = info.getName();
        }

        return names;
    }

    private UDDIClient newUDDI(String type, String url, String accountName, String accountpasswd) {
        UDDIRegistryInfo registryInfo = null;
        if ( type != null ) {
            registryInfo = getRegistryTypeInfo(registryTypeInfo, type);
        }
        UDDIClientFactory factory = UDDIClientFactory.getInstance();

        return factory.newUDDIClient(url, registryInfo, accountName, accountpasswd, null);
    }

    private boolean canProceed(String type, String url, String accountName, String accountpasswd) {
        boolean nextOk = false;

        UDDIClient uddi = newUDDI(type, url, accountName, accountpasswd);

        try {
            if (policyUrl != null && policyUrl.trim().length() > 0) {
                findExistingPolicyModel(uddi);
            } else {
                // try the credentials
                // (bugzilla #2601 preemptively try the credentials)
                uddi.authenticate();
            }
            nextOk = true;
        }
        catch (UDDIAccessControlException e) {
            String msg = "Authentication failed for user '"+accountName+"'.";
            showError(msg);
        }
        catch (UDDIException e) {
            String msg = "Error when testing credentials.";
            showError(msg + " " + ExceptionUtils.getMessage(e));
            logger.log(Level.WARNING, msg, e);
        }


        return nextOk;
    }

    private void findExistingPolicyModel(UDDIClient uddi) throws UDDIException {
        Collection<UDDINamedEntity> policyInfos = uddi.listPolicies(null, policyUrl);

        if ( !policyInfos.isEmpty() ) {
            if ( policyInfos.size() > 1 ) {
                logger.info("Found multiple policies for url '"+policyUrl+"', using first.");
            }

            UDDINamedEntity info = policyInfos.iterator().next();
            policyName = info.getName();
            policyKey = info.getKey();
        }
    }

    public void readSettings(Object settings) throws IllegalArgumentException {
        Data data = (Data) settings;
        policyUrl = data.getCapturedPolicyURL();
    }

    public void storeSettings(Object settings) throws IllegalArgumentException {
        Data data = (Data) settings;
        data.setUddi(newUDDI(
                (String) uddiTypeComboBox.getSelectedItem(),
                uddiURLField.getText().trim(),
                uddiAccountNameField.getText(),
                uddiAccountPasswdField.getText()));
        data.setPolicytModelKey(policyKey);
        if (policyName != null)
            data.setPolicyName(policyName);
    }

    public interface Data {
        void setUddi(UDDIClient uddi);

        String getCapturedPolicyURL();
        void setCapturedPolicyURL(String capturedPolicyURL);

        String getPolicytModelKey();
        void setPolicytModelKey(String policytModelKey);

        String getPolicyName();
        void setPolicyName(String policyName);
    }

    private void showError(String err) {
        JOptionPane.showMessageDialog(this, TextUtils.breakOnMultipleLines(err, 30),
                                      "Error", JOptionPane.ERROR_MESSAGE);
    }
}
