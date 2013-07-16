package com.l7tech.external.assertions.authandmgmtserviceinstaller.console;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Resolver;
import com.l7tech.util.ResolvingComparator;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.authandmgmtserviceinstaller.ApiPortalAuthAndMgmtServiceInstallerConstants.*;

/**
 * Helper class to manage the UI section for Authentication in the installer UI.
 *
 * @author ghuang
 */
public class ApiPortalAuthenticationConfiguration {
    private static final Logger logger = Logger.getLogger(ApiPortalAuthenticationConfiguration.class.getName());

    private JComboBox ldapComboBox;
    private JLabel searchFilterLabel;
    private JTextField searchFilterTextField;
    private JPanel contentPane;
    private JLabel ldapNameLabel;
    private JTextField userSearchFilterTextField;
    private JTextField groupSearchFilterTextField;
    private JPanel genericLdapAttributesPanel;

    private JDialog parent;
    private boolean enabled = true;
    private long lastSelectedLdapId;

    /**
     * Constructor creates a configuration UI for LDAP Provider Authentication.
     *
     * @param parent: the parent UI component to hold this configure UI.
     * @param enabled: an indicator to determine if all components in the UI are enabled or disabled.
     */
    public ApiPortalAuthenticationConfiguration(JDialog parent, boolean enabled) {
        this.parent = parent;
        this.enabled = enabled;

        initialize();
    }

    private void initialize() {
        ldapComboBox.setModel(new DefaultComboBoxModel(populateLdapProviders()));

        ldapComboBox.addItemListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                updateComponents();
            }
        }));

        updateComponents();
    }

    public JPanel getContentPane() {
        return contentPane;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        updateComponents();
    }

    private void updateComponents() {
        ldapNameLabel.setEnabled(enabled);
        ldapComboBox.setEnabled(enabled);

        searchFilterLabel.setEnabled(enabled);
        searchFilterTextField.setEnabled(enabled);
        for (Component c: genericLdapAttributesPanel.getComponents()) {
            c.setEnabled(enabled);
        }

        boolean visible = ldapComboBox.getSelectedIndex() != 0;
        if (visible) {
            if (isMsadLdapSelectedFromComboBox(ldapComboBox)) {
                searchFilterLabel.setVisible(true);
                searchFilterTextField.setVisible(true);
                genericLdapAttributesPanel.setVisible(false);
            } else {
                searchFilterLabel.setVisible(false);
                searchFilterTextField.setVisible(false);
                genericLdapAttributesPanel.setVisible(true);
            }
        } else {
            searchFilterLabel.setVisible(false);
            searchFilterTextField.setVisible(false);
            genericLdapAttributesPanel.setVisible(false);
        }

        long selectedLdapId = getSelectedLdapId(ldapComboBox);
        if (isMsadLdapSelectedFromComboBox(ldapComboBox) && selectedLdapId != lastSelectedLdapId) {
            searchFilterTextField.setText(AUTH_MSAD_LDAP_DEFAULT_SEARCH_FILTER);
        }
        else if (isOpenLdapSelectedFromComboBox(ldapComboBox) && selectedLdapId != lastSelectedLdapId) {
            userSearchFilterTextField.setText(AUTH_OPEN_LDAP_DEFAULT_USER_SEARCH_FILTER);

            LdapIdentityProviderConfig lipc = ((LdapRowInfo) ldapComboBox.getSelectedItem()).lipc;
            groupSearchFilterTextField.setText(MessageFormat.format(AUTH_OPEN_LDAP_DEFAULT_GROUP_SEARCH_FILTER, lipc.getSearchBase()));
        }
        lastSelectedLdapId = selectedLdapId;

        DialogDisplayer.pack(parent);
    }

    public String getSearchFilter() {
        return searchFilterTextField.getText().trim();
    }

    public String getUserSearchFilter() {
        return userSearchFilterTextField.getText().trim();
    }

    public String getGroupSearchFilter() {
        return groupSearchFilterTextField.getText().trim();
    }

    public JComboBox getLdapComboBox() {
        return ldapComboBox;
    }

    /**
     * Check if the selected LDAP Provider is a Open LDAP type.
     *
     * @param ldapComboBox: the combo box lists all configured LDAP Providers.
     * @return true if the selected LDAP Provider is a Open LDAP.  Otherwise, return false.
     */
    public static boolean isOpenLdapSelectedFromComboBox(JComboBox ldapComboBox) {
        LdapIdentityProviderConfig ldapSelected = ((LdapRowInfo) ldapComboBox.getSelectedItem()).lipc;
        if (ldapSelected == null) return false;

        return GENERIC_LDAP_TEMPLATE_NAME.equals(ldapSelected.getTemplateName());
    }

    /**
     * Check if the selected LDAP Provider is MSAD LDAP type.
     *
     * @param ldapComboBox: the combo box lists all configured LDAP Providers.
     * @return true if the selected LDAP Provider is a MSAD LDAP.  Otherwise, return false.
     */
    public static boolean isMsadLdapSelectedFromComboBox(JComboBox ldapComboBox) {
        LdapIdentityProviderConfig ldapSelected = ((LdapRowInfo) ldapComboBox.getSelectedItem()).lipc;
        if (ldapSelected == null) return false;

        return MSAD_LDAP_TEMPLATE_NAME.equals(ldapSelected.getTemplateName());
    }

    /**
     * A help method gets the object ID of the selected  LDAP Provider from the combo box.
     *
     * @param ldapComboBox: the combo box lists all configured LDAP Providers.
     * @return a long integer for the selected LDAP Provider ID
     */
    public static long getSelectedLdapId(JComboBox ldapComboBox) {
        EntityHeader ldapSelected = ((LdapRowInfo) ldapComboBox.getSelectedItem()).entityHeader;
        if (ldapSelected == null) throw new IllegalArgumentException("Could not find any LDAP Providers.");
        return ldapSelected.getOid();
    }

    /**
     * A helper method gets all configured LDAP Providers in the gateway.
     *
     * @return a list of configured LDAP Providers in the gateway
     */
    public static LdapRowInfo[] populateLdapProviders() {
        Collection<LdapRowInfo> LdapRowInfoList = new ArrayList<LdapRowInfo>();
        LdapRowInfoList.add(new LdapRowInfo(new EntityHeader(-1, EntityType.ANY, "Select LDAP", "First row in the ComboBox"), null));

        final IdentityAdmin admin = Registry.getDefault().getIdentityAdmin();
        if (admin == null) {
            logger.warning("Cannot get the IdentityAdmin");
        } else {
            try {
                EntityHeader[] providerHeaders = admin.findAllIdentityProviderConfig();
                Arrays.sort(providerHeaders, new ResolvingComparator<EntityHeader, String>(new Resolver<EntityHeader, String>() {
                    @Override
                    public String resolve(final EntityHeader key) {
                        return key.getName() == null ? "" : key.getName().toLowerCase();
                    }
                }, false));

                for (EntityHeader header : providerHeaders) {
                    IdentityProviderConfig ipc = admin.findIdentityProviderConfigByID(header.getOid());
                    if (ipc != null && ipc instanceof LdapIdentityProviderConfig) {
                        LdapIdentityProviderConfig lipc = (LdapIdentityProviderConfig)ipc;
                        String templateName = lipc.getTemplateName();

                        if (MSAD_LDAP_TEMPLATE_NAME.equals(templateName) || GENERIC_LDAP_TEMPLATE_NAME.equals(templateName)) {
                            LdapRowInfoList.add(new LdapRowInfo(header,lipc));
                        }
                    }
                }
            } catch (FindException e) {
                logger.warning("Cannot find IdentityProviderConfig");
            }
        }

        return LdapRowInfoList.toArray(new LdapRowInfo[] {});
    }

    /**
     * A class holds an EntityHeader object (entityHeader) and a corresponding LdapIdentityProviderConfig (lipc), where
     * entityHeader provides the name of Ldap Providers and lipc provides LDAP Provider type etc properties.
     */
    public static class LdapRowInfo {
        protected EntityHeader entityHeader;
        protected LdapIdentityProviderConfig lipc;

        protected LdapRowInfo(EntityHeader entityHeader, LdapIdentityProviderConfig lipc) {
            this.entityHeader = entityHeader;
            this.lipc = lipc;
        }

        @Override
        public String toString() {
            return entityHeader != null? entityHeader.toString() : "";
        }
    }
}