package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.identity.ldap.BindOnlyLdapIdentityProviderConfig;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.l7tech.identity.ldap.LdapUrlBasedIdentityProviderConfig.PROP_LDAP_RECONNECT_TIMEOUT;

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
    private JTextField reconnectTimeoutField;
    private Long ldapReconnectTimeout;

    private boolean finishAllowed = false;
    private final Pattern millisecondPattern;

    public BindOnlyLdapGeneralPanel(WizardStepPanel next, boolean readOnly) {
        super(next, readOnly);
        millisecondPattern = Pattern.compile("\\d+");
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        initGui();
    }

    private void initGui() {
        try {
            ClusterProperty clusterProperty = Registry.getDefault().getClusterStatusAdmin()
                    .findServerConfigPropertyByName(PROP_LDAP_RECONNECT_TIMEOUT);
            if (clusterProperty == null || clusterProperty.getValue() == null) {
                ldapReconnectTimeout = DEFAULT_RECONNECT_TIMEOUT;
            } else {
                ldapReconnectTimeout = Long.parseLong(clusterProperty.getValue());
            }
        } catch (FindException e) {
            ldapReconnectTimeout = DEFAULT_RECONNECT_TIMEOUT;
        }

        RunOnChangeListener listener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                updateControlButtonState();
            }
        });

        ldapUrlListPanel.addPropertyChangeListener(LdapUrlListPanel.PROP_DATA, listener);
        providerNameField.getDocument().addDocumentListener(listener);

        validationRules.add(new InputValidator.ComponentValidationRule(reconnectTimeoutField) {
            @Override
            public String getValidationError() {
                Matcher matcher = millisecondPattern.matcher(reconnectTimeoutField.getText());
                if (!matcher.matches()) {
                    return "Reconnect Timeout should be a number of milliseconds between 1 and " + Long.MAX_VALUE;
                }
                return null;
            }
        });
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
        reconnectTimeoutField.setText(ldapReconnectTimeout.toString());

        if (!(settings instanceof BindOnlyLdapIdentityProviderConfig)) {
            return;
        }

        BindOnlyLdapIdentityProviderConfig config = (BindOnlyLdapIdentityProviderConfig) settings;
        providerNameField.setText(config.getName());
        ldapUrlListPanel.setUrlList(config.getLdapUrl());
        ldapUrlListPanel.setClientAuthEnabled(config.isClientAuthEnabled());
        ldapUrlListPanel.selectPrivateKey(config.getKeystoreId(), config.getKeyAlias());
        dnPrefixField.setText(config.getBindPatternPrefix());
        dnSuffixField.setText(config.getBindPatternSuffix());
        if (config.getReconnectTimeout() != null) {
            reconnectTimeoutField.setText(config.getReconnectTimeout().toString());
        }

        // select name field for clone
        if(Goid.isDefault(config.getGoid())) {
            providerNameField.requestFocus();
            providerNameField.selectAll();
            zoneControl.configure(OperationType.CREATE, config);
        } else {
            zoneControl.configure(isReadOnly() ? OperationType.READ : OperationType.UPDATE, config);
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
            config.setReconnectTimeout(Long.parseLong(reconnectTimeoutField.getText()));
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
