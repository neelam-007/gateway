package com.l7tech.console.security.rbac;

import com.l7tech.gateway.common.security.rbac.RbacUtilities;
import com.l7tech.gateway.common.security.rbac.Role;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Panel which displays the properties of a Role.
 */
public class RolePropertiesPanel extends JPanel {
    private static final String CUSTOM = "Custom";
    private static final String SYSTEM = "System";
    private JPanel contentPanel;
    private JTextField roleTextField;
    private JTextPane descriptionTextPane;
    private JTextField typeTextField;
    private RolePermissionsPanel permissionsPanel;
    private Role role;
    private String roleName;

    public RolePropertiesPanel() {
        descriptionTextPane.setContentType("text/html");
    }

    public void configure(@Nullable final Role role, @Nullable final String roleName) {
        this.role = role;
        this.roleName = roleName;
        loadTextFields();
        permissionsPanel.configure(role);
    }

    private void loadTextFields() {
        if (role != null) {
            roleTextField.setText(roleName);
            typeTextField.setText(role.isUserCreated() ? CUSTOM : SYSTEM);
            descriptionTextPane.setText(RbacUtilities.getDescriptionString(role, true));
        } else {
            roleTextField.setText(StringUtils.EMPTY);
            typeTextField.setText(StringUtils.EMPTY);
            descriptionTextPane.setText(StringUtils.EMPTY);
        }
    }
}
