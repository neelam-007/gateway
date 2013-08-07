package com.l7tech.console.security.rbac;

import com.l7tech.gateway.common.security.rbac.RbacUtilities;
import com.l7tech.gateway.common.security.rbac.Role;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Panel which displays the basic properties of a Role.
 */
public class BasicRolePropertiesPanel extends JPanel {
    public static final String CUSTOM = "Custom";
    public static final String SYSTEM = "System";
    private JPanel contentPanel;
    private JTextField roleTextField;
    private JTextField typeTextField;
    private JTextPane descriptionTextPane;

    public BasicRolePropertiesPanel() {
        descriptionTextPane.setContentType("text/html");
    }

    public void configure(@Nullable final Role role, @Nullable final String roleName) {
        if (role != null) {
            roleTextField.setText(roleName == null ? StringUtils.EMPTY : roleName);
            typeTextField.setText(role.isUserCreated() ? CUSTOM : SYSTEM);
            descriptionTextPane.setText(RbacUtilities.getDescriptionString(role, true));
        } else {
            roleTextField.setText(StringUtils.EMPTY);
            typeTextField.setText(StringUtils.EMPTY);
            descriptionTextPane.setText(StringUtils.EMPTY);
        }
    }
}
