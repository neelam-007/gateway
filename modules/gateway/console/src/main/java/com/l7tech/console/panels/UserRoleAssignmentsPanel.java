package com.l7tech.console.panels;

import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Vector;
import java.awt.*;

/**
 * User: megery
 * Date: Oct 16, 2006
 * Time: 4:10:41 PM
 */
public class UserRoleAssignmentsPanel extends JPanel {
    private JList rolesList;
    private JPanel mainPanel;
    private JLabel rolesLabel;
    private static final String genericLabel = "{0} is assigned to the following roles:";

    private User user;

    public UserRoleAssignmentsPanel(User whichUser) throws FindException {
        this.user = whichUser;
        rolesLabel.setText(MessageFormat.format(genericLabel, user.getName()));

        Vector<String> assignmentsModel = new Vector<String>();
        for (Role role : getAssignedRolesForUser()) {
            assignmentsModel.add(role.getName());    
        }
        rolesList.setListData(assignmentsModel);
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

    }

    private Collection<Role> getAssignedRolesForUser() throws FindException {
        RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();
        return rbacAdmin.findRolesForUser(user);
    }
}
