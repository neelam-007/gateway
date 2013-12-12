package com.l7tech.console.panels;

import com.l7tech.console.security.rbac.RoleSelectionDialog;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.identity.external.PolicyBackedIdentityProviderConfig;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    private SecurityZoneWidget zoneControl;
    private JCheckBox defaultRoleCheckBox;
    private JLabel authPoliciesWarningLabel;
    private JTextField roleTextField;
    private JButton roleSelectButton;

    private java.util.List<PolicyHeader> policies;
    private Role selectedRole;

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
        }

        if (policies == null || policies.isEmpty()) {
            authPoliciesWarningLabel.setText("No identity provider policies exist or are available to the current admin");
            authPoliciesWarningLabel.setVisible(true);
        } else {
            authPoliciesWarningLabel.setText("");
            authPoliciesWarningLabel.setVisible(false);
        }

        adminEnabledCheckbox.addActionListener(listener);
        defaultRoleCheckBox.addActionListener(listener);
        policyComboBox.addActionListener(listener);

        roleSelectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final RoleSelectionDialog selectDialog = new RoleSelectionDialog(TopComponents.getInstance().getTopParent(), "Select Default Role", Collections.<Role>emptyList(), false);
                selectDialog.pack();
                DialogDisplayer.display(selectDialog, new Runnable() {
                    @Override
                    public void run() {
                        if (selectDialog.isConfirmed()) {
                            final Role selected = selectDialog.getSelectedRoles().iterator().next();
                            if (selected != null) {
                                try {
                                    // retrieve role with attached entities
                                    selectedRole = Registry.getDefault().getRbacAdmin().findRoleByPrimaryKey(selected.getGoid());
                                } catch (final FindException | PermissionDeniedException ex) {
                                    logger.log(Level.WARNING, "Unable to retrieve selected role: " + ExceptionUtils.getMessage(ex), ExceptionUtils.getDebugException(ex));
                                    DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), "Unable to retrieve selected role", "Error", JOptionPane.ERROR_MESSAGE, null);
                                }
                            } else {
                                selectedRole = null;
                            }
                            roleTextField.setText(selectedRole == null ? StringUtils.EMPTY : getNameForRole(selectedRole));
                            updateControlButtonState();
                        }
                    }
                });
            }
        });

        updateControlButtonState();
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
            return new ArrayList<>(Registry.getDefault().getPolicyAdmin().findPolicyHeadersWithTypes(EnumSet.of(PolicyType.IDENTITY_PROVIDER_POLICY), false));
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
            roleTextField.setText(StringUtils.EMPTY);
        } else {
            try {
                final Role role = Registry.getDefault().getRbacAdmin().findRoleByPrimaryKey(roleId);
                if (role != null) {
                    selectedRole = role;
                    roleTextField.setText(getNameForRole(role));
                }
            } catch (final FindException | PermissionDeniedException e) {
                logger.log(Level.WARNING, "Unable to retrieve role: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                roleTextField.setText(roleId.toString());
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
                    return;
                }
            }

            // Not found
            policyComboBox.setSelectedIndex(-1);
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

            config.setDefaultRoleId(!defaultRoleCheckBox.isSelected() || selectedRole == null ? null : selectedRole.getGoid());

            config.setSecurityZone(zoneControl.getSelectedZone());
        }
    }

    private void updateControlButtonState() {
        boolean ok = true;

        if (providerNameField.getText().length() < 1)
            ok = false;

        if (policyComboBox.getSelectedItem() == null)
            ok = false;

        if (adminEnabledCheckbox.isSelected() && defaultRoleCheckBox.isSelected() && selectedRole == null)
            ok = false;

        finishAllowed = ok;

        defaultRoleCheckBox.setEnabled(adminEnabledCheckbox.isSelected());
        roleTextField.setEnabled(defaultRoleCheckBox.isSelected());
        roleSelectButton.setEnabled(defaultRoleCheckBox.isSelected());

        // notify the wizard to update the state of the control buttons
        notifyListeners();
    }
}
