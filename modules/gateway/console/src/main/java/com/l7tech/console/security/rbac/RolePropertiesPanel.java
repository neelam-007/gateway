package com.l7tech.console.security.rbac;

import com.l7tech.gateway.common.security.rbac.Role;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Panel which displays the properties of a Role including its Permissions.
 */
public class RolePropertiesPanel extends JPanel {
    private JPanel contentPanel;
    private RolePermissionsPanel permissionsPanel;
    private BasicRolePropertiesPanel basicPropertiesPanel;

    public void configure(@Nullable final Role role, @Nullable final String roleName) {
        basicPropertiesPanel.configure(role, roleName);
        permissionsPanel.configure(role);
    }
}
