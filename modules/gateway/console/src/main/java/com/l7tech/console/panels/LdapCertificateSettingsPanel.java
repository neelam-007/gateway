package com.l7tech.console.panels;

import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.UserMappingConfig;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.TreeSet;
import java.util.Set;
import java.awt.*;

/**
 * Wizard step panel for LDAP Identity Provider certificate settings.
 */
public class LdapCertificateSettingsPanel extends IdentityProviderStepPanel {

    //- PUBLIC

    public LdapCertificateSettingsPanel( final WizardStepPanel next ) {
        this(next, false);
    }

    public LdapCertificateSettingsPanel( final WizardStepPanel next, final boolean readOnly ) {
        super(next);
        initResources();
        initComponents(readOnly);
    }

    @Override
    public boolean canAdvance() {
        String indexFilter = customIndexSearchFilterTextField.getText();
        return !scanAndIndexCertificatesWithFilterRadioButton.isSelected() || (indexFilter!=null && !indexFilter.trim().isEmpty());
    }

    @Override
    public boolean canTest() {
        return true;
    }

    /**
     * Populate the configuration data from the wizard input object to the visual components of the panel.
     *
     * @param settings  The current value of configuration items in the wizard input object.
     */
    @Override
    public void readSettings( final Object settings ) {
        if ( settings instanceof LdapIdentityProviderConfig  ) {
            final LdapIdentityProviderConfig ldapSettings = (LdapIdentityProviderConfig) settings;

            LdapIdentityProviderConfig.UserCertificateUseType certUseType = ldapSettings.getUserCertificateUseType();
            switch ( certUseType ) {
                case INDEX:
                    scanAndIndexCertificatesRadioButton.setSelected(true);
                    break;
                case INDEX_CUSTOM:
                    scanAndIndexCertificatesWithFilterRadioButton.setSelected(true);
                    customIndexSearchFilterTextField.setText(ldapSettings.getUserCertificateIndexSearchFilter());
                    break;
                case NONE:
                default:
                    doNotUseCertificatesRadioButton.setSelected(true);
                    break;
                case SEARCH:
                    searchForCertificatesInRadioButton.setSelected(true);
                    issuerSerialSearchTextField.setText(ldapSettings.getUserCertificateIssuerSerialSearchFilter());
                    subjectKeyIdentifierSearchTextField.setText(ldapSettings.getUserCertificateSKISearchFilter());
                    break;
            }
            enableDisableUserCertOptions();

            if ( customIndexSearchFilterTextField.getText()==null || customIndexSearchFilterTextField.getText().isEmpty() ) {
                Set<String> userMappings = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
                for ( UserMappingConfig config : ldapSettings.getUserMappings() ) {
                    if ( config.getUserCertAttrName()!=null && !config.getUserCertAttrName().trim().isEmpty() ) {
                        userMappings.add(config.getUserCertAttrName().trim());
                    }
                }
                if ( userMappings.size() > 0 ) {
                    boolean multiple = userMappings.size() > 1;

                    StringBuilder builder = new StringBuilder();
                    if (multiple) builder.append("(|");
                    for ( String attr : userMappings ) {
                        builder.append( '(' );
                        builder.append( attr );
                        builder.append( "=*)" );
                    }
                    if (multiple) builder.append(')');
                    customIndexSearchFilterTextField.setText( builder.toString() );
                }
            }

            validationOptionComboBox.setSelectedItem(ldapSettings.getCertificateValidationType());
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
            final LdapIdentityProviderConfig ldapSettings = (LdapIdentityProviderConfig) settings;

            ldapSettings.setUserCertificateIndexSearchFilter(null);
            ldapSettings.setUserCertificateIssuerSerialSearchFilter(null);
            ldapSettings.setUserCertificateSKISearchFilter(null);

            if ( doNotUseCertificatesRadioButton.isSelected() ) {
                ldapSettings.setUserCertificateUseType( LdapIdentityProviderConfig.UserCertificateUseType.NONE );
            } else if ( scanAndIndexCertificatesRadioButton.isSelected() ) {
                ldapSettings.setUserCertificateUseType( LdapIdentityProviderConfig.UserCertificateUseType.INDEX );
            } else if ( scanAndIndexCertificatesWithFilterRadioButton.isSelected() ) {
                ldapSettings.setUserCertificateUseType( LdapIdentityProviderConfig.UserCertificateUseType.INDEX_CUSTOM );
                ldapSettings.setUserCertificateIndexSearchFilter(customIndexSearchFilterTextField.getText());
            } else if ( searchForCertificatesInRadioButton.isSelected() ) {
                ldapSettings.setUserCertificateUseType( LdapIdentityProviderConfig.UserCertificateUseType.SEARCH );
                ldapSettings.setUserCertificateIssuerSerialSearchFilter(issuerSerialSearchTextField.getText());
                ldapSettings.setUserCertificateSKISearchFilter(subjectKeyIdentifierSearchTextField.getText());
            }

            ldapSettings.setCertificateValidationType((CertificateValidationType) validationOptionComboBox.getSelectedItem());
        }
    }

    /** @return the wizard step label    */
    @Override
    public String getStepLabel() {
        return resources.getString(RES_STEP_TITLE);
    }

    /**
     * Provide the description for the step being taken on this panel.
     *
     * @return  String  The descritpion of the step.
     */
    @Override
    public String getDescription() {
        return resources.getString(RES_STEP_DESCRIPTION);
    }

    // - PRIVATE

    private static final String RES_STEP_TITLE = "certificateSettings.step.label";
    private static final String RES_STEP_DESCRIPTION = "certificateSettings.step.description";
    private static final String RES_VALTYPE_PREFIX = "validation.option.";
    private static final String RES_VALTYPE_DEFAULT = "default";

    private JPanel mainPanel;
    private JComboBox validationOptionComboBox;
    private JRadioButton doNotUseCertificatesRadioButton;
    private JRadioButton scanAndIndexCertificatesRadioButton;
    private JRadioButton scanAndIndexCertificatesWithFilterRadioButton;
    private JRadioButton searchForCertificatesInRadioButton;
    private JTextField issuerSerialSearchTextField;
    private JTextField subjectKeyIdentifierSearchTextField;
    private JTextField customIndexSearchFilterTextField;
    private ResourceBundle resources;

    /**
     * Loads locale-specific resources: strings  etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.IdentityProviderDialog", locale);
    }

    /**
     * Init UI, disable components if read-only
     */
    private void initComponents( final boolean readOnly ) {
        setLayout(new BorderLayout());

        doNotUseCertificatesRadioButton.setEnabled(!readOnly);
        scanAndIndexCertificatesRadioButton.setEnabled(!readOnly);
        scanAndIndexCertificatesWithFilterRadioButton.setEnabled(!readOnly);
        searchForCertificatesInRadioButton.setEnabled(!readOnly);

        RunOnChangeListener listener = new RunOnChangeListener( new Runnable() {
            @Override
            public void run() {
                enableDisableUserCertOptions();
                notifyListeners();
            }
        });
        doNotUseCertificatesRadioButton.addActionListener( listener );
        scanAndIndexCertificatesRadioButton.addActionListener( listener );
        scanAndIndexCertificatesWithFilterRadioButton.addActionListener( listener );
        searchForCertificatesInRadioButton.addActionListener( listener );
        customIndexSearchFilterTextField.getDocument().addDocumentListener( listener );
        enableDisableUserCertOptions();

        DefaultComboBoxModel model = new DefaultComboBoxModel( CertificateValidationType.values());
        model.insertElementAt(null, 0);
        validationOptionComboBox.setModel(model);
        validationOptionComboBox.setRenderer(new TextListCellRenderer<CertificateValidationType>(
                new Functions.Unary<String,CertificateValidationType>(){
                    @Override
                    public String call( final CertificateValidationType type ) {
                        String labelKey = RES_VALTYPE_PREFIX + RES_VALTYPE_DEFAULT;
                        if (type != null) {
                            labelKey = RES_VALTYPE_PREFIX + type.name();
                        }

                        return resources.getString(labelKey);
                    }
                }, null, true));

        validationOptionComboBox.setEnabled(!readOnly);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void enableDisableUserCertOptions() {
        if ( !isReadOnly() ) {
            boolean indexEnabled = scanAndIndexCertificatesWithFilterRadioButton.isSelected();
            boolean searchEnabled = searchForCertificatesInRadioButton.isSelected();

            customIndexSearchFilterTextField.setEnabled( indexEnabled );

            issuerSerialSearchTextField.setEnabled( searchEnabled );
            subjectKeyIdentifierSearchTextField.setEnabled( searchEnabled );
        }
    }
}
