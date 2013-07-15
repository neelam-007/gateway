package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.*;
import com.l7tech.identity.InvalidIdProviderCfgException;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.util.Functions;
import com.l7tech.util.NameValuePair;
import com.l7tech.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 5/2/12
 */
public class LdapNtlmConfigurationPanel extends IdentityProviderStepPanel {

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.IdentityProviderDialog");

    private static final String RES_STEP_TITLE = "ntlmConfiguration.step.label";
    private static final String RES_STEP_DESCRIPTION = "ntlmConfiguration.step.description";
    public static final String RES_NTLM_PROPERTY_TABLE_NAME_COL = "ntlmConfiguration.ntlm.propertyTable.col.0";
    public static final String RES_NTLM_PROPERTY_TABLE_VALUE_COL = "ntlmConfiguration.ntlm.propertyTable.col.1";
    public static final String RES_SERVICE_PASSWORD_EMPTY = "ntlmConfiguration.service.password.errors.empty";

    private static final Pattern hostPattern = Pattern.compile("^[a-zA-Z0-9-\\\\.]+$");

    private JPanel mainPanel;
    private JCheckBox enableNtlmCheckBox;
    private JTextField domainDnsTextField;
    private JTextField domainNetbiosTextField;
    private JTextField hostDnsTextField;
    private JTextField serverNameTextField;
    private JTextField serviceAccountTextField;
    private JButton addNtlmPropertyButton;
    private JButton deleteNtlmPropertyButton;
    private JButton editNtlmPropertyButton;
    private JButton testNetlogonConnectionButton;
    private JTable ntlmPropertyTable;
    private JTextField hostNetbiosTextField;
    private JComboBox passwordComboBox;
    private JButton manageSecurePasswordsButton;
    private JLabel serviceAccountLabel;
    private JLabel domainNetbiosNameLabel;
    private JLabel hostNetbiosNameLabel;
    private JLabel serverNameLabel;
    private JLabel servicePasswordLabel;

    private boolean isValid = false;

    private SimpleTableModel<NameValuePair> ntlmPropertiesTableModel;
    private InputValidator inputValidator;

    public LdapNtlmConfigurationPanel( final WizardStepPanel next ) {
        super( next );
        initComponents();
    }

    public LdapNtlmConfigurationPanel( final WizardStepPanel next, final boolean readOnly ) {
        super( next, readOnly );
        initComponents();
    }

    /**
     * Provide the description for the step being taken on this panel.
     *
     * @return The description of the step.
     */
    @Override
    public String getDescription() {
        return resources.getString(RES_STEP_DESCRIPTION);
    }
    /**
     * Get the label for this step.
     *
     * @return the label
     */
    @Override
    public String getStepLabel() {
        return resources.getString(RES_STEP_TITLE);
    }

    @Override
    public boolean canTest() {
        return false;
    }

    @Override
    public boolean canAdvance() {
        return (!enableNtlmCheckBox.isSelected() || inputValidator.isValid());
    }

    /**
     * Populate the configuration data from the wizard input object to the visual components of the panel.
     *
     * @param settings  The current value of configuration items in the wizard input object.
     *
     * @throws IllegalArgumentException   if the data provided by the wizard are not valid.
     */
    @Override
    public void readSettings( final Object settings ) {
        if ( settings instanceof LdapIdentityProviderConfig) {
            readProviderConfig( (LdapIdentityProviderConfig) settings );
        }
        enableAndDisableComponents();
    }

    private void readProviderConfig(LdapIdentityProviderConfig config) {

        Map<String, String> props = new TreeMap<String, String>(config.getNtlmAuthenticationProviderProperties());

        if(props.containsKey("enabled")){
            enableNtlmCheckBox.setSelected(Boolean.parseBoolean(props.remove("enabled")));
        }
        if(props.containsKey("server.dns.name")) {
            serverNameTextField.setText(props.remove("server.dns.name"));
        }
        if(props.containsKey("service.account")) {
            serviceAccountTextField.setText(props.remove("service.account"));
        }
        if(props.containsKey("service.passwordOid")) {
            SecurePasswordComboBox securePasswordComboBox = (SecurePasswordComboBox)passwordComboBox;
            long oid = -1L;
            try {
               oid = Long.parseLong(props.remove("service.passwordOid"));
            } catch (NumberFormatException e) {
                //this shouldn't happen; swallow the exception
            }
            if(oid != -1L) {
                securePasswordComboBox.setSelectedSecurePassword(oid);
            }
            else {
                securePasswordComboBox.setSelectedItem(null);
            }
        }
        if(props.containsKey("domain.dns.name")) {
            domainDnsTextField.setText(props.remove("domain.dns.name"));
        }
        if(props.containsKey("domain.netbios.name")) {
            domainNetbiosTextField.setText(props.remove("domain.netbios.name") );
        }
        if(props.containsKey("host.dns.name")) {
            hostDnsTextField.setText(props.remove("host.dns.name"));
        }
        if(props.containsKey("host.netbios.name")) {
            hostNetbiosTextField.setText(props.remove("host.netbios.name"));
        }

        if(props != null){
            List<NameValuePair> propList = new ArrayList<NameValuePair>(props.size());
            for(Map.Entry<String, String> entry : props.entrySet()){
                NameValuePair pair = new NameValuePair(entry.getKey(), entry.getValue());
                propList.add(pair);
            }
            ntlmPropertiesTableModel.setRows(propList);
        }
    }

    /**
     * Store the values of all fields on the panel to the wizard object which is a used for
     * keeping all the modified values. The wizard object will be used for providing the
     * updated values when updating the server.
     *
     * @param settings the object representing wizard panel state
     */
    @Override
    public void storeSettings( final Object settings ) {
        if ( settings instanceof LdapIdentityProviderConfig ) {
            storeProviderConfig( (LdapIdentityProviderConfig) settings );
        }
    }

    private void storeProviderConfig( final LdapIdentityProviderConfig config ) {

        TreeMap<String, String> props = new TreeMap<String, String>();
        props.put("enabled", Boolean.toString(enableNtlmCheckBox.isSelected()));
        props.put("server.dns.name", serverNameTextField.getText().trim());
        props.put("service.account", serviceAccountTextField.getText().trim());
        SecurePassword securePassword = ((SecurePasswordComboBox) passwordComboBox).getSelectedSecurePassword();
        if(securePassword != null) {
            Long  passwordOid = securePassword.getOidAsLong();
            props.put("service.passwordOid", passwordOid.toString());
        }
        props.put("domain.dns.name", domainDnsTextField.getText().trim());
        props.put("domain.netbios.name", domainNetbiosTextField.getText().trim());
        props.put("host.dns.name", hostDnsTextField.getText().trim());
        props.put("host.netbios.name", hostNetbiosTextField.getText().trim());
        
        List<NameValuePair> ntlmAdvancedProperties = ntlmPropertiesTableModel.getRows();
        for(NameValuePair pair : ntlmAdvancedProperties){
            props.put(pair.getKey().trim(), pair.getValue().trim());
        }
        
        config.setNtlmAuthenticationProviderProperties(props);    
    }

    private void initComponents() {
        this.setLayout( new BorderLayout() );
        this.add( mainPanel, BorderLayout.CENTER );

        RunOnChangeListener validationListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                if( isValid != canAdvance()) {
                    isValid = canAdvance();
                    enableAndDisableComponents();
                    notifyListeners();
                }
            }
        });

        RunOnChangeListener serviceAccountEmptyListener = new RunOnChangeListener(new Runnable(){
            @Override
            public void run() {
                if(!serviceAccountTextField.getText().isEmpty()) {
                    passwordComboBox.setEnabled(true);
                    manageSecurePasswordsButton.setEnabled(true);
                }
                else{
                    passwordComboBox.setEnabled(false);
                    manageSecurePasswordsButton.setEnabled(false);
                }
            }
        });

        serverNameTextField.getDocument().addDocumentListener(validationListener);
        serviceAccountTextField.getDocument().addDocumentListener(validationListener);
        serviceAccountTextField.getDocument().addDocumentListener(serviceAccountEmptyListener);
        domainNetbiosTextField.getDocument().addDocumentListener(validationListener);
        hostNetbiosTextField.getDocument().addDocumentListener(validationListener);

        inputValidator = new InputValidator(this, getStepLabel());
        InputValidator.ComponentValidationRule hostNameRule = new InputValidator.ComponentValidationRule(serverNameTextField) {
            final String errorMsg = "Invalid characters entered";

            @Override
            public String getValidationError() {
                Matcher m = hostPattern.matcher(serverNameTextField.getText().trim());
                if (!m.matches()) {
                    return errorMsg;
                }
                return null;
            }
        };
        inputValidator.constrainTextFieldToBeNonEmpty(serverNameLabel.getText(), serverNameTextField, hostNameRule);
        inputValidator.constrainTextFieldToBeNonEmpty(serviceAccountLabel.getText(), serviceAccountTextField, null) ;
        inputValidator.constrainTextFieldToBeNonEmpty(domainNetbiosNameLabel.getText(), domainNetbiosTextField, null);
        inputValidator.constrainTextFieldToBeNonEmpty(hostNetbiosNameLabel.getText(), hostNetbiosTextField, null);
        inputValidator.ensureComboBoxSelection(servicePasswordLabel.getText(), passwordComboBox);

        passwordComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableAndDisableComponents();
            }
        });

        enableNtlmCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableAndDisableComponents();
            }
        });

        manageSecurePasswordsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doManagePasswords();
            }
        });

        addNtlmPropertyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editNtlmProperty(null);
            }
        });

        editNtlmPropertyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int viewRow = ntlmPropertyTable.getSelectedRow();
                if(viewRow > -1) {
                    editNtlmProperty(ntlmPropertiesTableModel.getRowObject(ntlmPropertyTable.convertRowIndexToModel(viewRow)));
                }
            }
        });

        deleteNtlmPropertyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int viewRow = ntlmPropertyTable.getSelectedRow();
                if ( viewRow > -1 ) {
                    ntlmPropertiesTableModel.removeRowAt(ntlmPropertyTable.convertRowIndexToModel(viewRow));
                }
            }
        });

        testNetlogonConnectionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Map<String, String> props = new HashMap<String, String>();
                props.put("server.dns.name", serverNameTextField.getText().trim());
                props.put("service.account", serviceAccountTextField.getText().trim());
                SecurePassword securePassword = ((SecurePasswordComboBox) passwordComboBox).getSelectedSecurePassword();
                if(securePassword != null) {
                    Long  passwordOid = securePassword.getOidAsLong();
                    props.put("service.passwordOid", passwordOid.toString());
                }
                props.put("domain.dns.name", domainDnsTextField.getText().trim());
                props.put("domain.netbios.name", domainNetbiosTextField.getText().trim());
                props.put("host.dns.name", hostDnsTextField.getText().trim());
                props.put("host.netbios.name", hostNetbiosTextField.getText().trim());

                java.util.List<NameValuePair> propertiesList  = ntlmPropertiesTableModel.getRows();

                for(NameValuePair pair : propertiesList) {
                    props.put(pair.getKey().trim(), pair.getValue().trim());
                }
                try {
                    getIdentityAdmin().testNtlmConfig(props);
                    JOptionPane.showMessageDialog(getOwner(), "Connection to Netlogon service was successful");
                } catch (InvalidIdProviderCfgException e1) {
                    JOptionPane.showMessageDialog(getOwner(), "Failed to connect to Netlogon Service!\nPlease check NTLM properties", "Netlogon Connection Error", JOptionPane.ERROR_MESSAGE);
                }

            }
        });

        ntlmPropertiesTableModel = TableUtil.configureTable(
                ntlmPropertyTable,
                TableUtil.column(resources.getString(RES_NTLM_PROPERTY_TABLE_NAME_COL), 50, 100, 100000, property("key"), String.class),
                TableUtil.column(resources.getString(RES_NTLM_PROPERTY_TABLE_VALUE_COL), 50, 100, 100000, property("value"), String.class)
        );
        ntlmPropertyTable.getSelectionModel().setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        ntlmPropertyTable.getTableHeader().setReorderingAllowed( false );
    }

    private void enableAndDisableComponents() {
        boolean isEnabled = enableNtlmCheckBox.isSelected() && !isReadOnly();
        domainDnsTextField.setEnabled(isEnabled);
        domainNetbiosTextField.setEnabled(isEnabled);
        hostDnsTextField.setEnabled(isEnabled);
        hostNetbiosTextField.setEnabled(isEnabled);
        serverNameTextField.setEnabled(isEnabled);
        serviceAccountTextField.setEnabled(isEnabled);
        if(!serviceAccountTextField.getText().isEmpty()) {
            passwordComboBox.setEnabled(isEnabled);
            manageSecurePasswordsButton.setEnabled(isEnabled);
        }
        else{
            passwordComboBox.setEnabled(false);
            manageSecurePasswordsButton.setEnabled(false);
        }
        addNtlmPropertyButton.setEnabled(isEnabled);
        editNtlmPropertyButton.setEnabled(isEnabled);
        deleteNtlmPropertyButton.setEnabled(isEnabled);
        ntlmPropertyTable.setEnabled(isEnabled);
        testNetlogonConnectionButton.setEnabled(isEnabled & inputValidator.isValid());
        notifyListeners();
    }

    private void editNtlmProperty( final NameValuePair nameValuePair ) {
        final SimplePropertyDialog dlg = nameValuePair == null ?
                new SimplePropertyDialog(getOwner()) :
                new SimplePropertyDialog(getOwner(), new Pair<String,String>( nameValuePair.getKey(), nameValuePair.getValue() ) );
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    final Pair<String, String> property = dlg.getData();
                    for (final NameValuePair pair : new ArrayList<NameValuePair>(ntlmPropertiesTableModel.getRows())) {
                        if (pair.getKey().equals(property.left)) {
                            ntlmPropertiesTableModel.removeRow(pair);
                        }
                    }
                    if (nameValuePair != null) ntlmPropertiesTableModel.removeRow(nameValuePair);

                    ntlmPropertiesTableModel.addRow(new NameValuePair(property.left, property.right));
                }
            }
        });
    }

    private static Functions.Unary<String,NameValuePair> property(final String propName) {
        return Functions.propertyTransform(NameValuePair.class, propName);
    }

    private IdentityAdmin getIdentityAdmin()
            throws RuntimeException {
        IdentityAdmin admin = Registry.getDefault().getIdentityAdmin();
        if (admin == null) {
            throw new RuntimeException("Could not find registered " + IdentityAdmin.class);
        }

        return admin;
    }

    private void doManagePasswords() {
        final SecurePasswordComboBox passwordComboBox = (SecurePasswordComboBox)this.passwordComboBox;
        final SecurePassword password = passwordComboBox.getSelectedSecurePassword();

        final SecurePasswordManagerWindow securePasswordManagerWindow = new SecurePasswordManagerWindow(getOwner());

        securePasswordManagerWindow.pack();
        Utilities.centerOnParentWindow(securePasswordManagerWindow);
        DialogDisplayer.display(securePasswordManagerWindow, new Runnable() {
            @Override
            public void run() {
                passwordComboBox.reloadPasswordList();

                if ( password != null ) {
                    password.getName();
                    passwordComboBox.setSelectedSecurePassword( password.getOid() );
                }
                else {
                    passwordComboBox.setSelectedItem(null);
                }
            }
        });
    }

    private void createUIComponents() {
        passwordComboBox = new SecurePasswordComboBox();
    }
}
