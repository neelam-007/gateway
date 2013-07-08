package com.l7tech.console.panels;

import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.identity.ldap.BindOnlyLdapIdentityProviderConfig;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import java.awt.*;

/**
 *
 */
public class BindOnlyLdapGeneralPanel extends IdentityProviderStepPanel {
    private JPanel mainPanel;
    private LdapUrlListPanel ldapUrlListPanel;
    private JTextField dnPrefixField;
    private JTextField dnSuffixField;
    private JTextField providerNameField;
    private SecurityZoneWidget zoneControl;

    private boolean finishAllowed = false;

    public BindOnlyLdapGeneralPanel(WizardStepPanel next, boolean readOnly) {
        super(next, readOnly);
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        initGui();
    }

    private void initGui() {
        RunOnChangeListener listener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                updateControlButtonState();
            }
        });

        ldapUrlListPanel.addPropertyChangeListener(LdapUrlListPanel.PROP_DATA, listener);
        providerNameField.getDocument().addDocumentListener(listener);
    }

    @Override
    public String getDescription() {
        return
            "<html>This Wizard allows you to configure a Simplified LDAP Identity Provider. Fields \n" +
            "marked with an asterisk \"*\" are required.\n" +
            "<p>Click [Test] at any time to test the LDAP configuration.</p></html>";
    }

    @Override
    public String getStepLabel() {
        return "Simple LDAP Identity Provider Configuration";
    }

    @Override
    public boolean canFinish() {
        return finishAllowed;
    }

    @Override
    public boolean canAdvance() {
        return canFinish();
    }

    @Override
    public boolean canTest() {
        return canFinish();
    }

    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        readSettings(settings, false);
    }

    @Override
    public void readSettings(Object settings, boolean acceptNewProvider) throws IllegalArgumentException {
        if (settings instanceof BindOnlyLdapIdentityProviderConfig) {
            BindOnlyLdapIdentityProviderConfig config = (BindOnlyLdapIdentityProviderConfig) settings;
            providerNameField.setText(config.getName());
            ldapUrlListPanel.setUrlList(config.getLdapUrl());
            ldapUrlListPanel.setClientAuthEnabled(config.isClientAuthEnabled());
            ldapUrlListPanel.selectPrivateKey(config.getKeystoreId(), config.getKeyAlias());
            dnPrefixField.setText(config.getBindPatternPrefix());
            dnSuffixField.setText(config.getBindPatternSuffix());

            // select name field for clone
            if( config.getOid() == BindOnlyLdapIdentityProviderConfig.DEFAULT_OID) {
                providerNameField.requestFocus();
                providerNameField.selectAll();
                zoneControl.configure(OperationType.CREATE, config);
            } else {
                zoneControl.configure(isReadOnly() ? OperationType.READ : OperationType.UPDATE, config);
            }
        }
    }

    @Override
    public void storeSettings(Object settings) throws IllegalArgumentException {
        if (settings instanceof BindOnlyLdapIdentityProviderConfig) {
            BindOnlyLdapIdentityProviderConfig config = (BindOnlyLdapIdentityProviderConfig) settings;
            config.setName(providerNameField.getText());
            config.setLdapUrl(ldapUrlListPanel.getUrlList());
            config.setClientAuthEnabled(ldapUrlListPanel.isClientAuthEnabled());
            config.setBindPatternPrefix(dnPrefixField.getText());
            config.setBindPatternSuffix(dnSuffixField.getText());
            config.setSecurityZone(zoneControl.getSelectedZone());
            boolean clientAuth = ldapUrlListPanel.isClientAuthEnabled();
            config.setClientAuthEnabled(clientAuth);
            if (clientAuth) {
                config.setKeystoreId(ldapUrlListPanel.getSelectedKeystoreId());
                config.setKeyAlias(ldapUrlListPanel.getSelectedKeyAlias());
            } else {
                config.setKeystoreId(null);
                config.setKeyAlias(null);
            }
        }
    }

    private void updateControlButtonState() {
        finishAllowed = providerNameField.getText().length() > 0 && !ldapUrlListPanel.isUrlListEmpty();

        // notify the wizard to update the state of the control buttons
        notifyListeners();
    }
}
