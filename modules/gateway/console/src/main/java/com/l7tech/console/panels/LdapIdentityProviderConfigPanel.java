package com.l7tech.console.panels;

import com.l7tech.console.util.PasswordGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.ConnectException;


/**
 * This class is a panel for users to input the configuration data of an LDAP identity provider.
 *
 * <p> Copyright (C) 2003-2006 Layer 7 Technologies Inc.</p>
 *
 */

public class LdapIdentityProviderConfigPanel extends IdentityProviderStepPanel {
    static final Logger log = Logger.getLogger(LdapIdentityProviderConfigPanel.class.getName());

    private JPanel mainPanel;
    private JTextField providerNameTextField;
    private JPasswordField ldapBindPasswordField;
    private JTextField ldapBindDNTextField;
    private JTextField ldapSearchBaseTextField;
    private JComboBox providerTypesCombo;
    private JPanel configPanel;
    private JCheckBox showPasswordCheckBox;
    private JLabel plaintextPasswordWarningLabel;
    private JCheckBox adminEnabledCheckbox;
    private JPanel hostUrlPanel;
    private SecurityZoneWidget zoneControl;

    private LdapUrlListPanel ldapUrlListPanel;

    private boolean providerTypeSelectable;
    private boolean finishAllowed = false;
    private boolean advanceAllowed = false;
    private ResourceBundle resources = null;

    // Creates new form ServicePanel
    public LdapIdentityProviderConfigPanel(WizardStepPanel next, boolean providerTypeSelectable) {
        super(next);
        this.providerTypeSelectable = providerTypeSelectable;
        initResources();
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        initGui();
    }

    private void initGui() {
        ldapUrlListPanel = new LdapUrlListPanel();
        hostUrlPanel.setLayout(new BorderLayout());
        hostUrlPanel.add(ldapUrlListPanel, BorderLayout.CENTER);
        ldapUrlListPanel.addPropertyChangeListener(LdapUrlListPanel.PROP_DATA, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateControlButtonState();
            }
        });

        providerNameTextField.addKeyListener(keyListener);

        ldapSearchBaseTextField.addKeyListener(keyListener);
        PasswordGuiUtils.configureOptionalSecurePasswordField(ldapBindPasswordField, showPasswordCheckBox, plaintextPasswordWarningLabel);

        providerTypesCombo.setEnabled(providerTypeSelectable);
        LdapIdentityProviderConfig[] templates;
        try {
            templates = getIdentityAdmin().getLdapTemplates();
        } catch (FindException e) {
            log.log(Level.WARNING, "cannot retrieve templates", e);
            templates = new LdapIdentityProviderConfig[0];
        } catch (Exception e) {
            if (ExceptionUtils.causedBy(e, ConnectException.class)) {
                log.log(Level.WARNING, "the connection to the Gateway is lost during getting identity provider types.", e);
                throw new RuntimeException(e);
            }
            log.log(Level.WARNING, "cannot retrieve templates", e);
            templates = new LdapIdentityProviderConfig[0];
        }
        Object[] providerTypeItems = new Object[1 + templates.length];
        providerTypeItems[0] = "Select the provider type";
        System.arraycopy( templates, 0, providerTypeItems, 1, templates.length );
        providerTypesCombo.setModel(new DefaultComboBoxModel(providerTypeItems));
        providerTypesCombo.setRenderer(providerTypeRenderer);
        providerTypesCombo.setToolTipText(resources.getString("providerTypeTextField.tooltip"));
        providerTypesCombo.setPreferredSize(new Dimension(217, 20));
        providerTypesCombo.setMinimumSize(new Dimension(217, 20));

        providerTypesCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object o = providerTypesCombo.getSelectedItem();

                if (o instanceof LdapIdentityProviderConfig) {
                    configPanel.setVisible(true);
                    updateControlButtonState();
                } else {
                    configPanel.setVisible(false);
                    advanceAllowed = false;
                    finishAllowed = false;
                }

                // notify the wizard to update the state of the control buttons
                notifyListeners();
            }
        });

        configPanel.setVisible(false);
    }

    /**
     * Loads locale-specific resources: strings  etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.IdentityProviderDialog", locale);
    }

    @Override
    public String getDescription() {
        return  resources.getString("configstep.description");
    }

    /** @return the wizard step label    */
    @Override
    public String getStepLabel() {
        return "Provider Configuration";
    }


    private IdentityAdmin getIdentityAdmin()
            throws RuntimeException {
        return Registry.getDefault().getIdentityAdmin();
    }

    /**
     * Indicate this panel is not the last one. The user must go to the panel.
     *
     * @return false
     */
    @Override
    public boolean canFinish() {
        return finishAllowed;
    }

    /**
     * Test whether the step is finished and it is safe to advance to the next one.  This method
     * should return quickly.
     *
     * @return true if the panel is valid, false otherwis
     */
    @Override
    public boolean canAdvance() {
        return advanceAllowed;
    }


    private void updateControlButtonState() {
        if (providerNameTextField.getText().length() > 0 &&
                //getLdapHostTextField().getText().length() > 0 &&
                !ldapUrlListPanel.isUrlListEmpty() &&
                ldapSearchBaseTextField.getText().length() > 0) {
            // can advance to next panel only when the above three fields are not empty
            advanceAllowed = true;
            finishAllowed = true;
        } else {
            advanceAllowed = false;
            finishAllowed = false;
        }

        // notify the wizard to update the state of the control buttons
        notifyListeners();
    }

    /** populate the form from the provider beans */
    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        readSettings(settings, false);
    }

    /** populate the form from the provider beans, possibly accepting new beans */
    @Override
    public void readSettings(Object settings, boolean acceptNewProvider) {
        if (settings != null) {

            if (settings instanceof LdapIdentityProviderConfig) {

                LdapIdentityProviderConfig iProviderConfig = (LdapIdentityProviderConfig) settings;

                if (acceptNewProvider || iProviderConfig.getOid() != -1) {

                    providerNameTextField.setText(iProviderConfig.getName());
                    ldapBindPasswordField.setText(iProviderConfig.getBindPasswd());
                    ldapBindDNTextField.setText(iProviderConfig.getBindDN());
                    ldapSearchBaseTextField.setText(iProviderConfig.getSearchBase());
                    adminEnabledCheckbox.setSelected(iProviderConfig.isAdminEnabled());
                    final boolean clientAuthEnabled = iProviderConfig.isClientAuthEnabled();
                    ldapUrlListPanel.setClientAuthEnabled(clientAuthEnabled);
                    ldapUrlListPanel.selectPrivateKey(iProviderConfig.getKeystoreId(), iProviderConfig.getKeyAlias());

                    // populate host list based on what is in the iProviderConfig
                    String[] ldapUrls = iProviderConfig.getLdapUrl();
                    ldapUrlListPanel.setUrlList(ldapUrls);
                }

                for (int i = providerTypesCombo.getModel().getSize() - 1; i >= 0; i--) {
                    Object toto = providerTypesCombo.getModel().getElementAt(i);
                    if (toto instanceof LdapIdentityProviderConfig) {
                        if (((LdapIdentityProviderConfig) toto).getName().equals(iProviderConfig.getTemplateName())) {
                            providerTypesCombo.setSelectedIndex(i);
                            break;
                        }
                    }
                }

                updateControlButtonState();

                // select name field for clone
                if( iProviderConfig.getOid() == LdapIdentityProviderConfig.DEFAULT_OID)
                {
                    providerNameTextField.requestFocus();
                    providerNameTextField.selectAll();
                }
                zoneControl.configure(iProviderConfig);
            }
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
    public void storeSettings(Object settings) {

        if (settings != null) {

            Object selectedType = providerTypesCombo.getSelectedItem();

            if (selectedType instanceof LdapIdentityProviderConfig) {
                LdapIdentityProviderConfig ldapType = (LdapIdentityProviderConfig) selectedType;

                final LdapIdentityProviderConfig ldapSettings = (LdapIdentityProviderConfig) settings;

                // stores the default mappings only when the config is a new object or
                // when the selection of the template is changed
                if ( ldapSettings.getTemplateName() == null ||
                        (ldapSettings.getTemplateName() != null &&
                        !ldapSettings.getTemplateName().equals(ldapType.getTemplateName()))) {

                    ldapSettings.setGroupMappings(ldapType.getGroupMappings());
                    ldapSettings.setUserMappings(ldapType.getUserMappings());
                }

                ldapSettings.setTemplateName(ldapType.getTemplateName());
                String[] newlist = ldapUrlListPanel.getUrlList();
                ldapSettings.setLdapUrl(newlist);
                ldapSettings.setName(providerNameTextField.getText());
                ldapSettings.setSearchBase(ldapSearchBaseTextField.getText());
                ldapSettings.setBindDN(ldapBindDNTextField.getText());
                ldapSettings.setBindPasswd(String.valueOf(ldapBindPasswordField.getPassword()));
                ldapSettings.setAdminEnabled(adminEnabledCheckbox.isSelected());

                boolean clientAuth = ldapUrlListPanel.isClientAuthEnabled();
                ldapSettings.setClientAuthEnabled(clientAuth);
                if (clientAuth) {
                    ldapSettings.setKeystoreId(ldapUrlListPanel.getSelectedKeystoreId());
                    ldapSettings.setKeyAlias(ldapUrlListPanel.getSelectedKeyAlias());
                } else {
                    ldapSettings.setKeystoreId(null);
                    ldapSettings.setKeyAlias(null);
                }
                ldapSettings.setSecurityZone(zoneControl.getSelectedZone());
            }
        }
    }

    /**
     * Test whether the step panel allows testing the settings.
     *
     * @return true if the panel is valid, false otherwis
     */
    @Override
    public boolean canTest() {
        return advanceAllowed;
    }

    @Override
    public boolean onNextButton() {
        if(providerNameTextField.getText().length() < 1 || providerNameTextField.getText().length() > 128) {
            JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(), resources.getString("providerNameTextField.length.error"),
                            resources.getString("providerNameTextField.error.title"),
                            JOptionPane.ERROR_MESSAGE);

            return false;
        }
        return true;
    }

    private KeyListener keyListener = new KeyListener() {
        @Override
        public void keyPressed(KeyEvent ke) {
            // don't care
        }

        @Override
        public void keyReleased(KeyEvent ke) {
            updateControlButtonState();
        }

        @Override
        public void keyTyped(KeyEvent ke) {
            // don't care
        }
    };

    private ListCellRenderer
            providerTypeRenderer = new DefaultListCellRenderer() {
                /**
                 * Return a component that has been configured to display the identity provider
                 * type value.
                 *
                 * @param list The JList we're painting.
                 * @param value The value returned by list.getModel().getElementAt(index).
                 * @param index The cells index.
                 * @param isSelected True if the specified cell was selected.
                 * @param cellHasFocus True if the specified cell has the focus.
                 * @return A component whose paint() method will render the specified value.
                 *
                 * @see JList
                 * @see ListSelectionModel
                 * @see ListModel
                 */
                @Override
                public Component getListCellRendererComponent(JList list,
                                                              Object value,
                                                              int index,
                                                              boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (!(value instanceof LdapIdentityProviderConfig)) {
                        setText(value.toString());
                    } else {
                        LdapIdentityProviderConfig type = (LdapIdentityProviderConfig) value;
                        setText(type.getName());
                    }

                    return this;
                }

            };


}
