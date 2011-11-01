package com.l7tech.console.panels;

import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.LogonInfo;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.User;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Oct 16, 2006
 * Time: 4:10:41 PM
 */
public class UserRoleAssignmentsPanel extends JPanel {
    private JList rolesList;
    private JPanel mainPanel;
    private JLabel rolesLabel;
    private JButton statusButton;
    private JLabel statusLabel;
    private JLabel statusTextLabel;
    private boolean canUpdate;
    private boolean isAdminEnabled;
    private static final String genericLabel = "{0} is assigned to the following roles:";
    static Logger log = Logger.getLogger(UserRoleAssignmentsPanel.class.getName());

    private User user;

    public UserRoleAssignmentsPanel(User whichUser, boolean isAdminEnabled, boolean canUpdate) throws FindException {
        this.user = whichUser;
        this.canUpdate = canUpdate;
        this.isAdminEnabled = isAdminEnabled;
        rolesLabel.setText(MessageFormat.format(genericLabel, user.getName()));

        Vector<String> assignmentsModel = new Vector<String>();
        for (Role role : getAssignedRolesForUser()) {
            assignmentsModel.add(role.getName());
        }
        rolesList.setListData(assignmentsModel);
        setEnabled(!assignmentsModel.isEmpty());  // disable for non admin user
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        populateStatus();
        statusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Registry.getDefault().getIdentityAdmin().activateUser(user);
                    populateStatus();
                } catch (FindException e1) {
                    JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(), e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    log.log(Level.SEVERE, "Error updating User: " + e.toString(), e);
                } catch (UpdateException e1) {
                    JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(), e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    log.log(Level.SEVERE, "Error updating User: " + e.toString(), e);
                }
            }
        });
    }

    public void enableDisableState(boolean isUserEnabledAndNotExpired) {
        if (statusTextLabel.isVisible()) {
            statusLabel.setEnabled(isUserEnabledAndNotExpired);
            statusTextLabel.setEnabled(isUserEnabledAndNotExpired);
            statusButton.setEnabled(isUserEnabledAndNotExpired);
        }
    }


    private void populateStatus() throws FindException {
        if (user instanceof LdapUser ||
                user instanceof InternalUser) {

            LogonInfo.State userState = Registry.getDefault().getIdentityAdmin().getLogonState(user);
            if (userState == null) {
                userState = LogonInfo.State.ACTIVE;
            }

            statusLabel.setText(getStateString(userState));

            statusButton.setVisible(canUpdate && userState != LogonInfo.State.ACTIVE);

            boolean isUserEnabledAndNotExpired = true;
            if (user instanceof InternalUser) {
                isUserEnabledAndNotExpired = ((InternalUser) user).isEnabled();
                long expiry = ((InternalUser) user).getExpiration();

                isUserEnabledAndNotExpired = isUserEnabledAndNotExpired && (expiry < 0 || expiry > System.currentTimeMillis());
            }
            enableDisableState(isAdminEnabled && isUserEnabledAndNotExpired);

            String statusButtonText = null;
            if (userState == LogonInfo.State.INACTIVE)
                statusButtonText = "Activate";
            else if (userState == LogonInfo.State.EXCEED_ATTEMPT)
                statusButtonText = "Unlock";
            statusButton.setText(statusButtonText);
        } else {
            statusTextLabel.setVisible(false);
            statusLabel.setVisible(false);
            statusButton.setVisible(false);
        }

    }

    public String getStateString(LogonInfo.State state) {
        switch (state) {
            case ACTIVE:
                return "Active";
            case INACTIVE:
                return "Inactive";
            case EXCEED_ATTEMPT:
                return "Locked";
            default:
                return null;
        }
    }

    private Collection<Role> getAssignedRolesForUser() throws FindException {
        if(!isAdminEnabled)
            return Collections.emptySet();
        RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();
        return rbacAdmin.findRolesForUser(user);
    }

    public void reloadUserLogonState() {
        try {
            populateStatus();
        } catch (FindException e) {
            log.log(Level.SEVERE, "Error loading user logon state", e);
            e.printStackTrace();
        }
    }
}
