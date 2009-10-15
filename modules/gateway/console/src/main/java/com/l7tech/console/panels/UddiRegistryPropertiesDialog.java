package com.l7tech.console.panels;

import com.l7tech.uddi.*;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.console.util.Registry;
import static com.l7tech.console.panels.UddiRegistryPropertiesDialog.UDDI_URL_TYPE.*;
import com.l7tech.gateway.common.uddi.UDDIRegistry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.net.URL;
import java.net.MalformedURLException;

public class UddiRegistryPropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(UddiRegistryPropertiesDialog.class.getName());

    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JLabel uddiRegistryName;
    private JTextField registryNameTextField;
    private JCheckBox enabledCheckBox;
    private JLabel uddiRegistryType;
    private JComboBox uddiRegistryTypeComboBox;
    private JLabel baseUrlLabel;
    private JTextField baseUrlTextField;
    private JLabel inquiryUrlLabel;
    private JTextField inquiryUrlTextField;
    private JTextField publishUrlTextField;
    private JLabel securityUrlLabel;
    private JTextField securityUrlTextField;
    private JLabel subscriptionUrlLabel;
    private JTextField subscriptionUrlTextField;
    private JCheckBox clientAuthenticationCheckBox;
    private JLabel keyStoreLabel;
    private JComboBox privateKeyComboBox;
    private JCheckBox metricsEnabledCheckBox;
    private JTextField metricsPublishFrequencyTextField;
    private JCheckBox monitoringEnabledCheckBox;
    private JRadioButton subscribeForNotificationRadioButton;
    private JRadioButton pollForNotificationsRadioButton;
    private JLabel notificationFrequencyLabel;
    private JTextField notificationFrequencyTextField;
    private JLabel userNameLabel;
    private JTextField userNameTextField;
    private JPasswordField passwordTextField;
    private JLabel publishUrlLabel;
    private JLabel registryAccountLabel;
    private JLabel passwordLabel;
    private JLabel metricsPublishFrequencyLabel;
    private JButton resetUrlButton;

    private UDDIRegistry uddiRegistry;
    private boolean confirmed;
    private static final String DIALOG_TITLE = "UDDI Registry Properties";
    private Map<String, UDDIRegistryInfo> registryToInfoMap;

    private UddiRegistryPropertiesDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        this.uddiRegistry = new UDDIRegistry();
        initialize();
    }

    public UddiRegistryPropertiesDialog(Window owner, UDDIRegistry uddiRegistry) {
        super(owner, DIALOG_TITLE, UddiRegistryPropertiesDialog.DEFAULT_MODALITY_TYPE);
        this.uddiRegistry = uddiRegistry;
        initialize();
    }

    enum UDDI_URL_TYPE {INQUIRY, PUBLISH, SECURITY, SUBSCRIPTION}

    /**
     * Initialize adds listeners and validators to components where were not created via a custom created.
     * Calls modelToView to set the state of the UI components from the UDDIRegistry instance variable
     */
    private void initialize(){
        setContentPane(contentPane);
        getRootPane().setDefaultButton(okButton);

        Utilities.setEscKeyStrokeDisposes(this);

        InputValidator inputValidator = new InputValidator(this, DIALOG_TITLE);
        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        registryToInfoMap = new HashMap<String, UDDIRegistryInfo>();
        Collection<UDDIRegistryInfo> registryInfos = Registry.getDefault().getServiceManager().getUDDIRegistryInfo();
        for(UDDIRegistryInfo info: registryInfos){
            registryToInfoMap.put(info.getName(), info);
        }

        baseUrlTextField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {}

            @Override
            public void focusLost(FocusEvent e) {
                //If any other url field is currently empty, then auto generate the text for that field
                computeURLField(securityUrlTextField, SECURITY);
                computeURLField(inquiryUrlTextField, INQUIRY);
                computeURLField(publishUrlTextField, PUBLISH);
                computeURLField(subscriptionUrlTextField, SUBSCRIPTION);
                enableOrDisableUrls();
            }

        });

        resetUrlButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                securityUrlTextField.setText("");
                inquiryUrlTextField.setText("");
                publishUrlTextField.setText("");
                subscriptionUrlTextField.setText("");
                
                computeURLField(securityUrlTextField, SECURITY);
                computeURLField(inquiryUrlTextField, INQUIRY);
                computeURLField(publishUrlTextField, PUBLISH);
                computeURLField(subscriptionUrlTextField, SUBSCRIPTION);
                enableOrDisableUrls();
            }
        });

        metricsEnabledCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableMetricsOptions();
            }
        });

        monitoringEnabledCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableMonitoringOptions();
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        clientAuthenticationCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                keyStoreLabel.setEnabled(clientAuthenticationCheckBox.isSelected());
                privateKeyComboBox.setEnabled(clientAuthenticationCheckBox.isSelected());
            }
        });

        //field validation
        inputValidator.constrainTextFieldToBeNonEmpty("UDDI Registry Name", registryNameTextField, null);
//        inputValidator.validateWhenDocumentChanges(registryNameTextField);

        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                if(uddiRegistryTypeComboBox.getSelectedIndex() == -1) return "Please select a UDDI Registry type";
                return null;
            }
        });

        inputValidator.constrainTextFieldToBeNonEmpty("UDDI Base URL", baseUrlTextField, null);
//        inputValidator.validateWhenDocumentChanges(baseUrlTextField);
        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                try {
                    new URL(baseUrlTextField.getText().trim());
                } catch (MalformedURLException e) {
                    return "Invalid Base URL: " + e.getMessage();
                }

                return null;
            }
        });

        inputValidator.constrainTextFieldToBeNonEmpty("Security URL", securityUrlTextField, null);
        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                final String baseUrl = baseUrlTextField.getText();
                if (!securityUrlTextField.getText().startsWith(baseUrl)) return "Security URL does not being with base URL";

                try {
                    new URL(securityUrlTextField.getText().trim());
                } catch (MalformedURLException e) {
                    return "Security URL: " + e.getMessage();
                }

                return null;
            }
        });

        inputValidator.constrainTextFieldToBeNonEmpty("Inquiry URL", inquiryUrlTextField, null);
        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                final String baseUrl = baseUrlTextField.getText();
                if (!inquiryUrlTextField.getText().startsWith(baseUrl)) return "Inquiry URL does not being with base URL";

                try {
                    new URL(inquiryUrlTextField.getText().trim());
                } catch (MalformedURLException e) {
                    return "Inquiry URL: " + e.getMessage();
                }

                return null;
            }
        });

        inputValidator.constrainTextFieldToBeNonEmpty("Publish URL", publishUrlTextField, null);
        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                final String baseUrl = baseUrlTextField.getText();
                if (!publishUrlTextField.getText().startsWith(baseUrl)) return "Publish URL does not being with base URL";

                try {
                    new URL(publishUrlTextField.getText().trim());
                } catch (MalformedURLException e) {
                    return "Publish URL: " + e.getMessage();
                }

                return null;
            }
        });

        //Subscription url is not required, but must be valid if supplied
        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                final String baseUrl = baseUrlTextField.getText();
                final String subUrl = subscriptionUrlTextField.getText();
                if(subUrl == null || subUrl.trim().isEmpty()) return null;

                if (!subscriptionUrlTextField.getText().startsWith(baseUrl)) return "Subscription URL does not being with base URL";

                try {
                    new URL(subscriptionUrlTextField.getText().trim());
                } catch (MalformedURLException e) {
                    return "Subscription URL: " + e.getMessage();
                }

                return null;
            }
        });

        inputValidator.constrainTextFieldToBeNonEmpty("UDDI Registry Account Username", userNameTextField, null);
        modelToView();
    }

    /**
     * Update the text field with a URL based on the base url and the registry type chosen.
     * If the text field contains any text it is not updated
     * If the uddi registry chosen, if any, is pre configured with values, then we can also add the correct
     * relative URL
     * @param textField the text field to update with an automatic url based on the base url
     * @param urlType what type of url is contained in the text field
     */
    private void computeURLField(final JTextField textField, UDDI_URL_TYPE urlType){
        //return null if the text field has a value
        if(textField.getText() != null && !textField.getText().trim().isEmpty()) return;

        final String defaultText = baseUrlTextField.getText().trim();
        if(uddiRegistryTypeComboBox.getSelectedIndex() == -1){
            //If no uddi type info, just return the value from the text field
            textField.setText(defaultText);
            return;
        }

        final Object uddiType = uddiRegistryTypeComboBox.getSelectedItem();
        //not all registries have info configured
        if(!registryToInfoMap.containsKey(uddiType.toString())) {
            textField.setText(defaultText);
            return;
        }

        boolean hasTrailingSlash = (defaultText.lastIndexOf("/") == defaultText.length() - 1
                || defaultText.lastIndexOf("\\") == defaultText.length() - 1);

        UDDIRegistryInfo uddiInfo = registryToInfoMap.get(uddiType.toString());
        String relativeUrlPart;
        switch(urlType){
            case INQUIRY:
                relativeUrlPart = uddiInfo.getInquiry();
                break;
            case PUBLISH:
                relativeUrlPart = uddiInfo.getPublication();
                break;
            case SECURITY:
                relativeUrlPart = uddiInfo.getSecurityPolicy();
                break;
            default:
                relativeUrlPart =  null;
        }

        textField.setText(defaultText + ((!hasTrailingSlash) ? "/" : "") + ((relativeUrlPart != null)? relativeUrlPart: ""));
    }
    
    /**
     * Applies the state of the UDDIRegistry onto the UI components.
     * Calls enableOrDisableComponents after doing this to enable / disable components appropriately depending on
     * the UDDIRegistry state
     */
    public void modelToView(){
        registryNameTextField.setText(uddiRegistry.getName());
        enabledCheckBox.setSelected(uddiRegistry.isEnabled());
        uddiRegistryTypeComboBox.setSelectedItem(UDDIRegistry.UDDIRegistryType.findType(uddiRegistry.getUddiRegistryType()));
        baseUrlTextField.setText(uddiRegistry.getBaseUrl());
        securityUrlTextField.setText(uddiRegistry.getSecurityUrl());
        inquiryUrlTextField.setText(uddiRegistry.getInquiryUrl());
        publishUrlTextField.setText(uddiRegistry.getPublishUrl());
        subscriptionUrlTextField.setText(uddiRegistry.getSubscriptionUrl());
        clientAuthenticationCheckBox.setSelected(uddiRegistry.isClientAuth());
        PrivateKeysComboBox privateKeyDropDown = (PrivateKeysComboBox) privateKeyComboBox;
        final Long keyStoreOid = (uddiRegistry.getKeystoreOid() != null)? uddiRegistry.getKeystoreOid(): 0;
        final String alias = uddiRegistry.getKeyAlias();

        privateKeyDropDown.select(keyStoreOid, alias);
        userNameTextField.setText(uddiRegistry.getRegistryAccountUserName());
        passwordTextField.setText(uddiRegistry.getRegistryAccountPassword());
        metricsEnabledCheckBox.setSelected(uddiRegistry.isMetricsEnabled());
        metricsPublishFrequencyTextField.setText(Long.toString(uddiRegistry.getMetricPublishFrequency()));
        monitoringEnabledCheckBox.setSelected(uddiRegistry.isMonitoringEnabled());
        subscribeForNotificationRadioButton.setSelected(uddiRegistry.isSubscribeForNotifications());
        pollForNotificationsRadioButton.setSelected(!uddiRegistry.isSubscribeForNotifications());
        notificationFrequencyTextField.setText(Long.toString(uddiRegistry.getMonitoringFrequency()));
        enableOrDisableComponents();
    }

    public void enableOrDisableComponents(){
        if(uddiRegistryTypeComboBox.getSelectedIndex() == -1) enableOrDisableUddiTypeDependentComponents(false);
        else enableOrDisableUddiTypeDependentComponents(true);

        enableOrDisableUrls();

        enableOrDisableClientAuthOptions();

        enableOrDisableMetricsOptions();

        enableOrDisableMonitoringOptions();
    }

    private void enableOrDisableUrls(){
        final String baseUrlText = baseUrlTextField.getText();
        final boolean enableUrls = baseUrlTextField.isEnabled() && baseUrlText != null && !baseUrlText.trim().isEmpty();
        securityUrlLabel.setEnabled(enableUrls);
        securityUrlTextField.setEnabled(enableUrls);
        inquiryUrlLabel.setEnabled(enableUrls);
        inquiryUrlTextField.setEnabled(enableUrls);
        publishUrlLabel.setEnabled(enableUrls);
        publishUrlTextField.setEnabled(enableUrls);
        subscriptionUrlLabel.setEnabled(enableUrls);
        subscriptionUrlTextField.setEnabled(enableUrls);
        resetUrlButton.setEnabled(enableUrls);
    }

    private void enableOrDisableMonitoringOptions() {
        final boolean enableMonitoringOptions = monitoringEnabledCheckBox.isSelected() && monitoringEnabledCheckBox.isEnabled();

        subscribeForNotificationRadioButton.setEnabled(enableMonitoringOptions);
        pollForNotificationsRadioButton.setEnabled(enableMonitoringOptions);
        notificationFrequencyLabel.setEnabled(enableMonitoringOptions);
        notificationFrequencyTextField.setEnabled(enableMonitoringOptions);
    }

    private void enableOrDisableMetricsOptions() {
        final boolean enableMetricOptions = metricsEnabledCheckBox.isSelected() && metricsEnabledCheckBox.isEnabled();
        metricsPublishFrequencyLabel.setEnabled(enableMetricOptions);
        metricsPublishFrequencyTextField.setEnabled(enableMetricOptions);
    }

    private void enableOrDisableClientAuthOptions() {
        final boolean enableClientAuthOptions = clientAuthenticationCheckBox.isSelected()
                && clientAuthenticationCheckBox.isEnabled();
        keyStoreLabel.setEnabled(enableClientAuthOptions);
        privateKeyComboBox.setEnabled(enableClientAuthOptions);
    }

    public static void main(String[] args) {
        UddiRegistryPropertiesDialog dialog = new UddiRegistryPropertiesDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

    public UDDIRegistry getConnector() {
        return uddiRegistry;
    }

    private void onOk() {
        viewToModel();
        confirmed = true;
        dispose();
    }

    /**
     * Configure the UDDIRegistry instance with information gathered from the GUI control states.
     * Assumes caller has already checked view state against the inputValidator.
     */
    private void viewToModel() {
        uddiRegistry.setName(registryNameTextField.getText());
        uddiRegistry.setEnabled(enabledCheckBox.isSelected());
        UDDIRegistry.UDDIRegistryType regType = (UDDIRegistry.UDDIRegistryType) uddiRegistryTypeComboBox.getSelectedItem();
        uddiRegistry.setUddiRegistryType(regType.toString());

        uddiRegistry.setBaseUrl(baseUrlTextField.getText());
        uddiRegistry.setSecurityUrl(securityUrlTextField.getText());
        uddiRegistry.setInquiryUrl(inquiryUrlTextField.getText());
        uddiRegistry.setPublishUrl(publishUrlTextField.getText());
        uddiRegistry.setSubscriptionUrl(subscriptionUrlTextField.getText());

        uddiRegistry.setClientAuth(clientAuthenticationCheckBox.isSelected());
        PrivateKeysComboBox privateKeyDropDown = (PrivateKeysComboBox) privateKeyComboBox;
        final String keyAlias = privateKeyDropDown.getSelectedKeyAlias();
        uddiRegistry.setKeyAlias(keyAlias);
        final long keyStoreId = privateKeyDropDown.getSelectedKeystoreId();
        uddiRegistry.setKeystoreOid(keyStoreId);
        
        uddiRegistry.setRegistryAccountUserName(userNameTextField.getText());
        uddiRegistry.setRegistryAccountPassword(new String(passwordTextField.getPassword()));

        final boolean metricsEnabled = metricsEnabledCheckBox.isSelected() && metricsEnabledCheckBox.isEnabled();
        uddiRegistry.setMetricsEnabled(metricsEnabled);
        if(metricsEnabled){
            uddiRegistry.setMetricPublishFrequency(Long.parseLong(metricsPublishFrequencyTextField.getText()));
        }

        final boolean monitoringEnabled = monitoringEnabledCheckBox.isSelected() && monitoringEnabledCheckBox.isEnabled();
        uddiRegistry.setMonitoringEnabled(monitoringEnabled);
        if(monitoringEnabled){
            uddiRegistry.setSubscribeForNotifications(subscribeForNotificationRadioButton.isSelected());
            uddiRegistry.setMonitoringFrequency(Long.parseLong(notificationFrequencyTextField.getText()));
        }
    }

    @Override
    public void setVisible(boolean b) {
        if (b && !isVisible()) confirmed = false;
        super.setVisible(b);
    }

    /** @return true if the dialog has been dismissed with the ok button */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Code called by Idea form code
     */
    private void createUIComponents() {
        privateKeyComboBox = new PrivateKeysComboBox();
        System.out.println("Created private key combo box");

        //Registry type
        UDDIRegistry.UDDIRegistryType[] types = UDDIRegistry.UDDIRegistryType.values();
        uddiRegistryTypeComboBox = new JComboBox(types);
        uddiRegistryTypeComboBox.setSelectedIndex(-1);
        uddiRegistryTypeComboBox.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableComponents();
            }
        });
    }

    private void enableOrDisableUddiTypeDependentComponents(boolean enable) {
        baseUrlLabel.setEnabled(enable);
        baseUrlTextField.setEditable(enable);
        clientAuthenticationCheckBox.setEnabled(enable);
        registryAccountLabel.setEnabled(enable);
        userNameLabel.setEnabled(enable);
        userNameTextField.setEnabled(enable);
        passwordLabel.setEnabled(enable);
        passwordTextField.setEnabled(enable);
        keyStoreLabel.setEnabled(enable);
        privateKeyComboBox.setEnabled(enable);
        metricsEnabledCheckBox.setEnabled(enable);
        monitoringEnabledCheckBox.setEnabled(enable);
    }

}
