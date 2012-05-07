package com.l7tech.console.panels;

import com.l7tech.console.util.PasswordGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.*;
import com.l7tech.identity.InvalidIdProviderCfgException;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.util.Functions;
import com.l7tech.util.NameValuePair;
import com.l7tech.util.Pair;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

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

    private JPanel mainPanel;
    private JCheckBox enableNtlmCheckBox;
    private JTextField domainDnsTextField;
    private JTextField domainNetbiosTextField;
    private JTextField hostDnsTextField;
    private JTextField serverNameTextField;
    private JTextField serviceAccountTextField;
    private JPasswordField passwordField;
    private JButton addNtlmPropertyButton;
    private JButton deleteNtlmPropertyButton;
    private JButton editNtlmPropertyButton;
    private JButton testNetlogonConnectionButton;
    private JTable ntlmPropertyTable;
    private JTextField hostNetbiosTextField;
    private JCheckBox showPasswordCheckBox;
    private JLabel plaintextPasswordWarningLabel;

    private InputValidator inputValidator;

    private SimpleTableModel<NameValuePair> ntlmPropertiesTableModel;

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
        return true;
    }

    @Override
    public boolean canAdvance() {
        return (!enableNtlmCheckBox.isSelected() || 
                !serverNameTextField.getText().isEmpty() && 
                !serviceAccountTextField.getText().isEmpty() &&
                !hostNetbiosTextField.getText().isEmpty() &&
                !domainNetbiosTextField.getText().isEmpty());
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
        if(props.containsKey("service.secure.password")) {
            passwordField.setText(props.remove("service.secure.password"));
        }
        if(props.containsKey("domain.dns.name")) {
            domainDnsTextField.setText(props.remove("domain.dns.name"));
        }
        if(props.containsKey("domain.netbios.name")) {
            domainNetbiosTextField.setText(props.remove("domain.netbios.name") );
        }
        if(props.containsKey("localhost.dns.name")) {
            hostDnsTextField.setText(props.remove("localhost.dns.name"));
        }
        if(props.containsKey("localhost.netbios.name")) {
            hostNetbiosTextField.setText(props.remove("localhost.netbios.name"));
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
        props.put("server.dns.name", serverNameTextField.getText());
        props.put("service.account", serviceAccountTextField.getText());
        props.put("service.secure.password", new String(passwordField.getPassword()));
        props.put("domain.dns.name", domainDnsTextField.getText());
        props.put("domain.netbios.name", domainNetbiosTextField.getText());
        props.put("localhost.dns.name", hostDnsTextField.getText());
        props.put("localhost.netbios.name", hostNetbiosTextField.getText());
        
        List<NameValuePair> ntlmAdvancedProperties = ntlmPropertiesTableModel.getRows();
        for(NameValuePair pair : ntlmAdvancedProperties){
            props.put(pair.getKey(), pair.getValue());
        }
        
        config.setNtlmAuthenticationProviderProperties(props);    
    }


    private void initComponents() {
        this.setLayout( new BorderLayout() );
        this.add( mainPanel, BorderLayout.CENTER );

        inputValidator = new InputValidator(this, resources.getString(RES_STEP_TITLE));
        // Password field must not be empty
        inputValidator.constrainTextFieldToBeNonEmpty("Password", passwordField, new InputValidator.ComponentValidationRule(passwordField) {
            @Override
            public String getValidationError() {
                if( passwordField.getPassword().length==0 ) {
                    return resources.getString(RES_SERVICE_PASSWORD_EMPTY);
                }

                return null;
            }
        });
        inputValidator.validateWhenDocumentChanges(passwordField);
        PasswordGuiUtils.configureOptionalSecurePasswordField(passwordField, showPasswordCheckBox, plaintextPasswordWarningLabel);
        
        enableNtlmCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableAndDisableComponents();
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
                props.put("server.dns.name", serverNameTextField.getText());
                props.put("service.account", serviceAccountTextField.getText());
                props.put("service.secure.password", new String(passwordField.getPassword()));
                props.put("domain.dns.name", domainDnsTextField.getText());
                props.put("domain.netbios.name", domainNetbiosTextField.getText());
                props.put("localhost.dns.name", hostDnsTextField.getText());
                props.put("localhost.netbios.name", hostNetbiosTextField.getText());

                java.util.List<NameValuePair> propertiesList  = ntlmPropertiesTableModel.getRows();

                for(NameValuePair pair : propertiesList) {
                    props.put(pair.getKey(), pair.getValue());
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
        passwordField.setEnabled(isEnabled);
        addNtlmPropertyButton.setEnabled(isEnabled);
        editNtlmPropertyButton.setEnabled(isEnabled);
        deleteNtlmPropertyButton.setEnabled(isEnabled);
        ntlmPropertyTable.setEnabled(isEnabled);
        testNetlogonConnectionButton.setEnabled(isEnabled);
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

}
