package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.identity.external.PolicyBackedIdentityProviderConfig;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.comparator.NamedEntityComparator;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Primary configuration wizard step panel for a Policy-backed identity provider configuration.
 */
public class PolicyBackedIdentityGeneralPanel extends IdentityProviderStepPanel {
    private static final Logger logger = Logger.getLogger(PolicyBackedIdentityGeneralPanel.class.getName());

    private JPanel mainPanel;
    private JTextField providerNameField;
    private JComboBox<PolicyHeader> policyComboBox;
    private JCheckBox adminEnabledCheckbox;
    private JComboBox<Pair<Role, String>> roleComboBox;
    private SecurityZoneWidget zoneControl;
    private JCheckBox defaultRoleCheckBox;

    private java.util.List<PolicyHeader> policies;
    private java.util.List<Role> roles;
    private java.util.List<Pair<Role, String>> rolesWithNames;

    private boolean finishAllowed = false;

    public PolicyBackedIdentityGeneralPanel(WizardStepPanel next, boolean readOnly) {
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

        providerNameField.getDocument().addDocumentListener(listener);

        if (Registry.getDefault().isAdminContextPresent()) {
            policies = loadPolicies();
            policyComboBox.setModel(new DefaultComboBoxModel<>(policies.toArray(new PolicyHeader[policies.size()])));

            loadRoles();

            //noinspection unchecked,Convert2Diamond
            roleComboBox.setModel(new DefaultComboBoxModel<Pair<Role, String>>(rolesWithNames.<Pair<Role, String>>toArray(new Pair[rolesWithNames.size()])));
            roleComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    if (value instanceof Pair) {
                        @SuppressWarnings("unchecked")
                        Pair<Role, String> pair = (Pair<Role, String>) value;
                        value = pair.right;
                    }
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            });
        }

        adminEnabledCheckbox.addActionListener(listener);
        defaultRoleCheckBox.addActionListener(listener);
        policyComboBox.addActionListener(listener);
        roleComboBox.addActionListener(listener);

        updateControlButtonState();
    }

    private void loadRoles() {
        try {
            final ArrayList<Role> roles = new ArrayList<>(Registry.getDefault().getRbacAdmin().findAllRoles());
            Collections.sort(roles, new NamedEntityComparator());
            this.roles = roles;
            this.rolesWithNames = new ArrayList<>();
            for (Role role : roles) {
                rolesWithNames.add(new Pair<>(role, getNameForRole(role)));
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to load roles; " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            this.roles = Collections.emptyList();
            this.rolesWithNames = Collections.emptyList();
        }
    }

    private String getNameForRole(Role role) {
        String name = "name unavailable";
        try {
            name = Registry.getDefault().getEntityNameResolver().getNameForEntity(role, true);
            return name;
        } catch (final FindException e) {
            logger.log(Level.WARNING, "Unable to retrieve name for role: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
        return name;
    }

    private static java.util.List<PolicyHeader> loadPolicies() {
        try {
            return new ArrayList<>(Registry.getDefault().getPolicyAdmin().findPolicyHeadersWithTypes(EnumSet.of(PolicyType.INCLUDE_FRAGMENT), false));
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to load policies; " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return Collections.emptyList();
        }
    }

    @Override
    public String getDescription() {
        return "<html>This Wizard allows you to configure a Policy-Backed Identity Provider. Fields \n" +
            "marked with an asterisk \"*\" are required.\n" +
            "\n<p><b>WARNING:</b> A default role assignment allows <b>any user</b> who successfully authenticates with\n" +
            "this provider to <b>administer the Gateway</b> using the specified role, unless\n" +
            "they have some more-specific role assignments specifically for their username.\n" +
            "</html>";
    }

    @Override
    public String getStepLabel() {
        return "Policy-Backed Identity Provider Configuration";
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
        if (settings instanceof PolicyBackedIdentityProviderConfig) {
            PolicyBackedIdentityProviderConfig config = (PolicyBackedIdentityProviderConfig) settings;
            providerNameField.setText(config.getName());
            selectPolicy(config.getPolicyId());
            selectRole(config.getDefaultRoleId());
            adminEnabledCheckbox.setSelected(config.isAdminEnabled());
            defaultRoleCheckBox.setSelected(config.getDefaultRoleId() != null);

            // select name field for clone
            if(Goid.isDefault(config.getGoid())) {
                providerNameField.requestFocus();
                providerNameField.selectAll();
                zoneControl.configure(OperationType.CREATE, config);
            } else {
                zoneControl.configure(isReadOnly() ? OperationType.READ : OperationType.UPDATE, config);
            }
        }

        updateControlButtonState();
    }

    private void selectRole(Goid roleId) {
        if (roleId == null) {
            roleComboBox.setSelectedItem(null);
        } else {
            for (int i = 0; i < roles.size(); i++) {
                Role role = roles.get(i);
                if (roleId.equals(role.getGoid())) {
                    roleComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void selectPolicy(Goid policyId) {
        if (policyId == null) {
            policyComboBox.setSelectedItem(null);
        } else {
            for (int i = 0; i < policies.size(); i++) {
                PolicyHeader header = policies.get(i);
                if (policyId.equals(header.getGoid())) {
                    policyComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    @Override
    public void storeSettings(Object settings) throws IllegalArgumentException {
        if (settings instanceof PolicyBackedIdentityProviderConfig) {
            PolicyBackedIdentityProviderConfig config = (PolicyBackedIdentityProviderConfig) settings;
            config.setName(providerNameField.getText());

            PolicyHeader policyHeader = (PolicyHeader) policyComboBox.getSelectedItem();
            config.setPolicyId(policyHeader == null ? null : policyHeader.getGoid());

            config.setAdminEnabled(adminEnabledCheckbox.isSelected());

            @SuppressWarnings("unchecked")
            Pair<Role, String> pair = (Pair<Role, String>) roleComboBox.getSelectedItem();
            Role role = pair == null ? null : pair.left;
            config.setDefaultRoleId(!defaultRoleCheckBox.isSelected() || role == null ? null : role.getGoid());

            config.setSecurityZone(zoneControl.getSelectedZone());
        }
    }

    private void updateControlButtonState() {
        boolean ok = true;

        if (providerNameField.getText().length() < 1)
            ok = false;

        if (policyComboBox.getSelectedItem() == null)
            ok = false;

        if (adminEnabledCheckbox.isSelected() && defaultRoleCheckBox.isSelected() && roleComboBox.getSelectedItem() == null)
            ok = false;

        finishAllowed = ok;

        defaultRoleCheckBox.setEnabled(adminEnabledCheckbox.isSelected());
        roleComboBox.setEnabled(defaultRoleCheckBox.isSelected());

        // notify the wizard to update the state of the control buttons
        notifyListeners();
    }
}
