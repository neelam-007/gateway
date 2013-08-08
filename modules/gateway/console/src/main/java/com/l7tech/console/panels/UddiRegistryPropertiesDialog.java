package com.l7tech.console.panels;

import com.l7tech.console.util.PasswordGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.Goid;
import com.l7tech.uddi.UDDIException;
import com.l7tech.uddi.UDDIRegistryInfo;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.l7tech.console.panels.UddiRegistryPropertiesDialog.UDDI_URL_TYPE.*;

public class UddiRegistryPropertiesDialog extends JDialog {

    private static final String DEFAULT_METRICS_INTERVAL = "10";
    private static final String DEFAULT_SUBSCRIPTION_INTERVAL = "10";

    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField registryNameTextField;
    private JCheckBox enabledCheckBox;
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
    private JCheckBox showPasswordCheckBox;
    private JLabel plaintextPasswordWarningLabel;
    private JLabel publishUrlLabel;
    private JLabel registryAccountLabel;
    private JLabel passwordLabel;
    private JLabel metricsPublishFrequencyLabel;
    private JButton resetUrlButton;
    private JButton testUDDIRegistryButton;
    private JLabel uddiRegistryNameLabel;
    private JLabel uddiRegistryTypeLabel;
    private SecurityZoneWidget zoneControl;

    private UDDIRegistry uddiRegistry;
    private boolean subscriptionServiceAvailable;
    private boolean confirmed;
    private static final String DIALOG_TITLE = "UDDI Registry Properties";
    private Map<String, UDDIRegistryInfo> registryToInfoMap;
    private final boolean readOnly;

    //only used by main in this classs
    private UddiRegistryPropertiesDialog(boolean readOnly) {
        this.readOnly = readOnly;
        setContentPane(contentPane);
        setModal(true);
        this.uddiRegistry = new UDDIRegistry();
        initialize();
    }

    public UddiRegistryPropertiesDialog(Window owner, UDDIRegistry uddiRegistry, boolean readOnly) {
        super(owner, DIALOG_TITLE, UddiRegistryPropertiesDialog.DEFAULT_MODALITY_TYPE);
        this.uddiRegistry = uddiRegistry;
        this.readOnly = readOnly;
        this.subscriptionServiceAvailable = isSubscriptionServiceAvailable(uddiRegistry);
        initialize();
    }

    public void selectNameField() {
        registryNameTextField.selectAll();
    }

    enum UDDI_URL_TYPE {INQUIRY, PUBLISH, SECURITY, SUBSCRIPTION}

    /**
     * Initialize adds listeners and validators to components where were not created via a custom created.
     * Calls modelToView to set the state of the UI components from the UDDIRegistry instance variable
     */
    private void initialize(){
        setContentPane(contentPane);
//        getRootPane().setDefaultButton(okButton);

        Utilities.setEscKeyStrokeDisposes(this);

        final InputValidator inputValidator = new InputValidator(this, DIALOG_TITLE);
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

        baseUrlTextField.addFocusListener( new FocusListener() {
            @Override
            public void focusGained( FocusEvent e ) {
                //disable the ok button as the default action when ever the base url field gets focus
                //if ok, it will be set when focus is lost
                getRootPane().setDefaultButton( null );
            }

            @Override
            public void focusLost( FocusEvent e ) {
                //If any other url field is currently empty, then auto generate the text for that field
                computeURLField( securityUrlTextField, SECURITY );
                computeURLField( inquiryUrlTextField, INQUIRY );
                computeURLField( publishUrlTextField, PUBLISH );
                computeURLField( subscriptionUrlTextField, SUBSCRIPTION );
                enableOrDisableUrls();
                enableOkButtonAfterBaseURLUpdated();
            }
        } );

        subscriptionUrlTextField.getDocument().addDocumentListener( new RunOnChangeListener( new Runnable() {
            @Override
            public void run() {
                enableOrDisableMonitoringOptions();
            }
        } ) );

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

        testUDDIRegistryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String validate = inputValidator.validate();
                final String testRegistryTitle = "Test UDDI Registry Settings";
                if(validate != null){
                    DialogDisplayer.showMessageDialog(UddiRegistryPropertiesDialog.this,  validate, testRegistryTitle, JOptionPane.ERROR_MESSAGE, null);
                    return;
                }
                final UDDIRegistry testUddiRegistry = new UDDIRegistry();
                viewToModel(testUddiRegistry);
                UDDIRegistryAdmin uddiRegistryAdmin = getUDDIRegistryAdmin();
                try {
                    uddiRegistryAdmin.testUDDIRegistryAuthentication(testUddiRegistry);

                    DialogDisplayer.showMessageDialog(UddiRegistryPropertiesDialog.this, "Connected successfully to UDDI Registry!",
                            testRegistryTitle, JOptionPane.INFORMATION_MESSAGE, null);

                } catch (UDDIException e1) {
                    DialogDisplayer.showMessageDialog(UddiRegistryPropertiesDialog.this,
                            "Could not connect to UDDI Registry: " + e1.getMessage(),
                            testRegistryTitle, JOptionPane.ERROR_MESSAGE, null);                    
                }
            }
        });
        zoneControl.configure(Goid.isDefault(uddiRegistry.getGoid()) ? OperationType.CREATE : !readOnly ? OperationType.UPDATE : OperationType.READ, uddiRegistry);

        //field validation
        inputValidator.constrainTextFieldToBeNonEmpty("UDDI Registry Name", registryNameTextField, null);

        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                if(uddiRegistryTypeComboBox.getSelectedIndex() == -1) return "Please select a UDDI Registry type";
                return null;
            }
        });

        inputValidator.constrainTextFieldToBeNonEmpty("Base URL", baseUrlTextField, null);
        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                return validateUrl( "Base URL: ", baseUrlTextField.getText().trim(), true );
            }
        });
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
                return validateUrl( "Security URL: ", securityUrlTextField.getText().trim(), true );
            }
        });

        inputValidator.constrainTextFieldToBeNonEmpty("Inquiry URL", inquiryUrlTextField, null);
        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                return validateUrl( "Inquiry URL: ", inquiryUrlTextField.getText().trim(), true );
            }
        });

        inputValidator.constrainTextFieldToBeNonEmpty("Publish URL", publishUrlTextField, null);
        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                return validateUrl( "Publish URL: ", publishUrlTextField.getText().trim(), true );
            }
        });

        //Subscription url is not required, but must be valid if supplied
        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                return validateUrl( "Subscription URL: ", subscriptionUrlTextField.getText(), false );
            }
        });

        inputValidator.constrainTextFieldToNumberRange( "Metrics Publish Frequency", metricsPublishFrequencyTextField, 1L, 1440L );
        inputValidator.constrainTextFieldToNumberRange( "Frequency", notificationFrequencyTextField, 1L, 1440L );

        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                String error = null;
                if ( monitoringEnabledCheckBox.isEnabled() &&
                     monitoringEnabledCheckBox.isSelected() &&
                     subscribeForNotificationRadioButton.isSelected() &&
                     !subscriptionServiceAvailable ) {
                    error = "'Subscribe for notifications' requires a published UDDI Notification service.\n" +
                            "Please disable monitoring or choose 'Poll for notifications'.\n\n" +
                            "To allow 'Subscribe for notifications' please create and publish a\n" +
                            "UDDI Notification service to this registry and try again.";
                }
                return error;
            }
        });

        PasswordGuiUtils.configureOptionalSecurePasswordField(passwordTextField, showPasswordCheckBox, plaintextPasswordWarningLabel);

        modelToView( uddiRegistry );
        setMinimumSize( getMinimumSize() );
    }

    /**
     * The only purpose of this method is to stop the ok button's validation from being triggered when the base
     * url is used to trigger an update to the other URLs. This method is called after this event happens. This
     * stops the ok validation code from running when the URL's are empty and it may not be obvious to the user.
     * If the user goes and deletes a value for a url field and clicks ok, it will be obvious what the problem is
     * The default ok button action should have been removed when the base url gains focus
     *
     * See bug 7978
     *
     */
    private void enableOkButtonAfterBaseURLUpdated(){
        if(registryNameTextField.getText().trim().isEmpty()) return;

        if(uddiRegistryTypeComboBox.getSelectedIndex() == -1) return;

        if(baseUrlTextField.getText().trim().isEmpty()) return;

        //just check a single url
        if(inquiryUrlTextField.getText().trim().isEmpty()) return;

        getRootPane().setDefaultButton(okButton);
    }

    private String validateUrl( final String label, final String url, final boolean required ) {
        try {
            if ( !url.isEmpty() ) {
                new URL(url);
            }

            if ( !ValidationUtils.isValidUrl(url, !required, Arrays.asList( "http", "https"))) {
                return label + "Is not a valid absolute HTTP(S) URL.";
            }
        } catch (MalformedURLException e) {
            return label + ExceptionUtils.getMessage(e);
        }

        return null;
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
            case SUBSCRIPTION:
                relativeUrlPart = uddiInfo.getSubscription();
                break;
            default:
                relativeUrlPart =  null;
        }

        if ( relativeUrlPart != null ) {
            textField.setText(defaultText + (!hasTrailingSlash ? "/" : "") + relativeUrlPart);
        } else {
            textField.setText(""); // This registry type does not support this API 
        }
    }
    
    /**
     * Applies the state of the UDDIRegistry onto the UI components.
     * Calls enableOrDisableComponents after doing this to enable / disable components appropriately depending on
     * the UDDIRegistry state
     */
    private void modelToView( final UDDIRegistry uddiRegistry ){
        registryNameTextField.setText(uddiRegistry.getName());
        registryNameTextField.setCaretPosition( 0 );
        enabledCheckBox.setSelected(uddiRegistry.isEnabled());
        String regType = uddiRegistry.getUddiRegistryType();
        if(regType != null){
            try {
                uddiRegistryTypeComboBox.setSelectedItem(UDDIRegistry.UDDIRegistryType.findType(regType));
            } catch ( IllegalStateException ise ) {
                uddiRegistryTypeComboBox.setSelectedIndex(-1);
            }
        }else{
            uddiRegistryTypeComboBox.setSelectedIndex(-1);
        }

        baseUrlTextField.setText(uddiRegistry.getBaseUrl());
        baseUrlTextField.setCaretPosition( 0 );
        securityUrlTextField.setText(uddiRegistry.getSecurityUrl());
        securityUrlTextField.setCaretPosition( 0 );
        inquiryUrlTextField.setText(uddiRegistry.getInquiryUrl());
        inquiryUrlTextField.setCaretPosition( 0 );
        publishUrlTextField.setText(uddiRegistry.getPublishUrl());
        publishUrlTextField.setCaretPosition( 0 );
        subscriptionUrlTextField.setText(uddiRegistry.getSubscriptionUrl()==null ? "" : uddiRegistry.getSubscriptionUrl());
        subscriptionUrlTextField.setCaretPosition( 0 );
        clientAuthenticationCheckBox.setSelected(uddiRegistry.isClientAuth());
        PrivateKeysComboBox privateKeyDropDown = (PrivateKeysComboBox) privateKeyComboBox;
        final Long keyStoreOid = (uddiRegistry.getKeystoreOid() != null)? uddiRegistry.getKeystoreOid(): 0L;
        final String alias = uddiRegistry.getKeyAlias();

        privateKeyDropDown.select(keyStoreOid, alias);
        userNameTextField.setText(uddiRegistry.getRegistryAccountUserName()==null ? "" : uddiRegistry.getRegistryAccountUserName());
        userNameTextField.setCaretPosition( 0 );
        passwordTextField.setText(uddiRegistry.getRegistryAccountPassword()==null ? "" : uddiRegistry.getRegistryAccountPassword());
        passwordTextField.setCaretPosition( 0 );
        metricsEnabledCheckBox.setSelected(uddiRegistry.isMetricsEnabled());
        if ( uddiRegistry.isMetricsEnabled() ) {
            metricsPublishFrequencyTextField.setText(Long.toString(uddiRegistry.getMetricPublishFrequency()/ (long) TimeUnit.MINUTES.getMultiplier() ));
        } else {
            metricsPublishFrequencyTextField.setText( DEFAULT_METRICS_INTERVAL );
        }
        monitoringEnabledCheckBox.setSelected(uddiRegistry.isMonitoringEnabled());
        if ( uddiRegistry.isMonitoringEnabled() ) {
            subscribeForNotificationRadioButton.setSelected(uddiRegistry.isSubscribeForNotifications());
            pollForNotificationsRadioButton.setSelected(!uddiRegistry.isSubscribeForNotifications());
            notificationFrequencyTextField.setText(Long.toString(uddiRegistry.getMonitoringFrequency()/ (long) TimeUnit.MINUTES.getMultiplier() ));
        } else {
            subscribeForNotificationRadioButton.setSelected(true);
            notificationFrequencyTextField.setText( DEFAULT_SUBSCRIPTION_INTERVAL );
        }
        enableOrDisableComponents();
    }

    private void enableOrDisableComponents(){
        if(uddiRegistryTypeComboBox.getSelectedIndex() == -1) enableOrDisableUddiTypeDependentComponents(false);
        else enableOrDisableUddiTypeDependentComponents(!readOnly);

        enableOrDisableUrls();

        enableOrDisableClientAuthOptions();

        enableOrDisableMetricsOptions();

        enableOrDisableMonitoringOptions();

        uddiRegistryTypeComboBox.setEnabled(!readOnly);
        uddiRegistryTypeLabel.setEnabled(!readOnly);
        enabledCheckBox.setEnabled(!readOnly);
        registryNameTextField.setEnabled(!readOnly);
        uddiRegistryNameLabel.setEnabled(!readOnly);
    }

    private void enableOrDisableUrls(){
        final String baseUrlText = baseUrlTextField.getText();
        final boolean enableUrls = baseUrlTextField.isEnabled() && baseUrlText != null && !baseUrlText.trim().isEmpty() && !readOnly;
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
        final String subcriptionUrl = subscriptionUrlTextField.getText();
        final boolean enableMonitoringSelection = subcriptionUrl != null && !subcriptionUrl.trim().isEmpty() && !readOnly;
        final boolean enableMonitoringOptions = monitoringEnabledCheckBox.isSelected() && monitoringEnabledCheckBox.isEnabled();

        monitoringEnabledCheckBox.setEnabled( enableMonitoringSelection );
        subscribeForNotificationRadioButton.setEnabled(enableMonitoringOptions);
        pollForNotificationsRadioButton.setEnabled(enableMonitoringOptions);
        notificationFrequencyLabel.setEnabled(enableMonitoringOptions);
        notificationFrequencyTextField.setEnabled(enableMonitoringOptions);
    }

    private void enableOrDisableMetricsOptions() {
        final boolean enableMetricOptions = metricsEnabledCheckBox.isSelected() && metricsEnabledCheckBox.isEnabled() && !readOnly;
        metricsPublishFrequencyLabel.setEnabled(enableMetricOptions);
        metricsPublishFrequencyTextField.setEnabled(enableMetricOptions);
    }

    private void enableOrDisableClientAuthOptions() {
        final boolean enableClientAuthOptions = clientAuthenticationCheckBox.isSelected()
                && clientAuthenticationCheckBox.isEnabled() && !readOnly;
        keyStoreLabel.setEnabled(enableClientAuthOptions);
        privateKeyComboBox.setEnabled(enableClientAuthOptions);
    }

    public static void main(String[] args) {
        UddiRegistryPropertiesDialog dialog = new UddiRegistryPropertiesDialog(true);
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

    public UDDIRegistry getConnector() {
        return uddiRegistry;
    }

    private void onOk() {
        viewToModel(uddiRegistry);
        confirmed = !readOnly;
        dispose();
    }

    /**
     * Configure the UDDIRegistry instance with information gathered from the GUI control states.
     * Assumes caller has already checked view state against the inputValidator.
     */
    private void viewToModel( final UDDIRegistry uddiRegistry ) {
        uddiRegistry.setName(registryNameTextField.getText().trim());
        uddiRegistry.setEnabled(enabledCheckBox.isSelected());
        UDDIRegistry.UDDIRegistryType regType = (UDDIRegistry.UDDIRegistryType) uddiRegistryTypeComboBox.getSelectedItem();
        uddiRegistry.setUddiRegistryType(regType.toString());

        uddiRegistry.setBaseUrl(baseUrlTextField.getText().trim());
        uddiRegistry.setSecurityUrl(securityUrlTextField.getText().trim());
        uddiRegistry.setInquiryUrl(inquiryUrlTextField.getText().trim());
        uddiRegistry.setPublishUrl(publishUrlTextField.getText().trim());
        if ( subscriptionUrlTextField.getText().trim().isEmpty() ) {
            uddiRegistry.setSubscriptionUrl( null );
        } else {
            uddiRegistry.setSubscriptionUrl(subscriptionUrlTextField.getText().trim());
        }

        uddiRegistry.setClientAuth(clientAuthenticationCheckBox.isSelected());
        PrivateKeysComboBox privateKeyDropDown = (PrivateKeysComboBox) privateKeyComboBox;
        final String keyAlias = privateKeyDropDown.getSelectedKeyAlias();
        uddiRegistry.setKeyAlias(keyAlias);
        final long keyStoreId = privateKeyDropDown.getSelectedKeystoreId();
        uddiRegistry.setKeystoreOid(keyStoreId);

        if ( userNameTextField.getText() != null && !userNameTextField.getText().trim().isEmpty() ) {
            uddiRegistry.setRegistryAccountUserName(userNameTextField.getText().trim());
            uddiRegistry.setRegistryAccountPassword(new String(passwordTextField.getPassword()));
        } else {
            uddiRegistry.setRegistryAccountUserName( null );
            uddiRegistry.setRegistryAccountPassword( null );   
        }

        final boolean metricsEnabled = metricsEnabledCheckBox.isSelected() && metricsEnabledCheckBox.isEnabled();
        uddiRegistry.setMetricsEnabled(metricsEnabled);
        if(metricsEnabled){
            uddiRegistry.setMetricPublishFrequency(Long.parseLong(metricsPublishFrequencyTextField.getText())* (long) TimeUnit.MINUTES.getMultiplier() );
        } else {
            uddiRegistry.setMetricPublishFrequency( 0L );
        }

        final boolean monitoringEnabled = monitoringEnabledCheckBox.isSelected() && monitoringEnabledCheckBox.isEnabled();
        uddiRegistry.setMonitoringEnabled(monitoringEnabled);
        if(monitoringEnabled){
            uddiRegistry.setSubscribeForNotifications(subscribeForNotificationRadioButton.isSelected());
            uddiRegistry.setMonitoringFrequency(Long.parseLong(notificationFrequencyTextField.getText())* (long) TimeUnit.MINUTES.getMultiplier() );
        } else {
            uddiRegistry.setMonitoringFrequency( 0L );
        }
        uddiRegistry.setSecurityZone(zoneControl.getSelectedZone());
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
        privateKeyComboBox = new PrivateKeysComboBox(true, false, false);
        privateKeyComboBox.setRenderer( TextListCellRenderer.<Object>basicComboBoxRenderer() );
        privateKeyComboBox.setMinimumSize( new Dimension( 100, privateKeyComboBox.getPreferredSize().height  ) );
        privateKeyComboBox.setPreferredSize( new Dimension( 100, privateKeyComboBox.getPreferredSize().height ) );

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
        baseUrlTextField.setEnabled(enable);
        clientAuthenticationCheckBox.setEnabled(enable);
        registryAccountLabel.setEnabled(enable);
        userNameLabel.setEnabled(enable);
        userNameTextField.setEnabled(enable);
        passwordLabel.setEnabled(enable);
        passwordTextField.setEnabled(enable);
        keyStoreLabel.setEnabled(enable);
        privateKeyComboBox.setEnabled(enable);
        monitoringEnabledCheckBox.setEnabled(enable);

        boolean metricsEnable = enable && getSelectedRegistryInfo()!=null && getSelectedRegistryInfo().isSupportsMetrics();
        metricsEnabledCheckBox.setEnabled(metricsEnable);
    }

    private UDDIRegistryInfo getSelectedRegistryInfo() {
        UDDIRegistryInfo info = null;

        Object item = uddiRegistryTypeComboBox.getSelectedItem();
        if ( item != null ) {
            info = registryToInfoMap.get( item.toString() );
        }

        return info;
    }

    private boolean isSubscriptionServiceAvailable( final UDDIRegistry registry ) {
        boolean available = false;

        if ( registry != null && !Goid.isDefault(registry.getGoid()) ) {
            UDDIRegistryAdmin admin = getUDDIRegistryAdmin();
            if ( admin != null ) {
                available = admin.subscriptionNotificationsAvailable( registry.getGoid() );
            }
        }

        return available;
    }

    /** @return the UDDIRegistryAdmin interface, or null if not connected or it's unavailable for some other reason */
    private UDDIRegistryAdmin getUDDIRegistryAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent())
            return null;
        return reg.getUDDIRegistryAdmin();
    }
    
}
