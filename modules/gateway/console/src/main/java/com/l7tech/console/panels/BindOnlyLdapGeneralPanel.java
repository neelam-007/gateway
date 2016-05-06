package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.identity.ldap.BindOnlyLdapIdentityProviderConfig;
import com.l7tech.objectmodel.Goid;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    private JTextField reconnectTimeoutTextField;
    private JCheckBox useDefaultReconnectCheckbox;


    /** Cluster (serverconfig_override) value from server - a.k.a. "system default" */
    private Long reconnectTimeoutFromServer;

    /** When a user enters a value for this provider, then check system default, then unchecks it,
     * the value they had typed before is restored from here */
    private Long reconnectTimeoutFromBefore;

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
        final String reconnectTimeoutString = Registry.getDefault().getIdentityAdmin()
                .findServerConfigPropertyByName(PROP_LDAP_RECONNECT_TIMEOUT);
        if (StringUtils.isBlank(reconnectTimeoutString)) {
            reconnectTimeoutFromServer = DEFAULT_RECONNECT_TIMEOUT;
        } else {
            reconnectTimeoutFromServer = Long.parseLong(reconnectTimeoutString);
        }
        reconnectTimeoutFromBefore = reconnectTimeoutFromServer;

        RunOnChangeListener listener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                updateControlButtonState();
            }
        });

        ldapUrlListPanel.addPropertyChangeListener(LdapUrlListPanel.PROP_DATA, listener);
        providerNameField.getDocument().addDocumentListener(listener);

        useDefaultReconnectCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (useDefaultReconnectCheckbox.isSelected()) {
                    try {
                        reconnectTimeoutFromBefore = Long.parseLong(reconnectTimeoutTextField.getText());
                    } catch (NumberFormatException e1) {
                        // if the user entered something that doesn't parse, it makes no sense to interrupt the flow at this point
                        // we just leave the previously saved value alone, and the nonsensical value will just be lost
                    }
                    reconnectTimeoutTextField.setText(reconnectTimeoutFromServer.toString());
                    reconnectTimeoutTextField.setEnabled(false);
                } else {
                    reconnectTimeoutTextField.setText(reconnectTimeoutFromBefore.toString());
                    reconnectTimeoutTextField.setEnabled(true);
                }
            }
        });

        validationRules.add(new InputValidator.ComponentValidationRule(reconnectTimeoutTextField) {
            @Override
            public String getValidationError() {
                Matcher matcher = millisecondPattern.matcher(reconnectTimeoutTextField.getText());
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
        reconnectTimeoutTextField.setText(reconnectTimeoutFromServer.toString());

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
        if (config.getReconnectTimeout() == null) {
            reconnectTimeoutTextField.setText(reconnectTimeoutFromServer.toString());
            reconnectTimeoutTextField.setEnabled(false);
            useDefaultReconnectCheckbox.setSelected(true);
        } else {
            reconnectTimeoutTextField.setText(config.getReconnectTimeout().toString());
            reconnectTimeoutTextField.setEnabled(true);
            useDefaultReconnectCheckbox.setSelected(false);
            reconnectTimeoutFromBefore = config.getReconnectTimeout();
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

            if (useDefaultReconnectCheckbox.isSelected()) {
                config.setReconnectTimeout(null);
            } else {
                config.setReconnectTimeout(Long.parseLong(reconnectTimeoutTextField.getText()));
            }

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
