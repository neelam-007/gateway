package com.l7tech.console.panels;

import com.l7tech.console.action.ChangePasswordAction;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.jcalendar.JDateTimeChooser;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.User;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Displays account info for a user and the roles they are in.
 */
public class MyAccountDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(MyAccountDialog.class.getName());
    private static final String MY_ACCOUNT = "My Account";
    private static final String NEVER = "Never";
    private JPanel contentPanel;
    private JTabbedPane tabPanel;
    private JPanel propertiesPanel;
    private JPanel rolesPanel;
    private JLabel usernameLabel;
    private JLabel firstNameLabel;
    private JLabel lastNameLabel;
    private JLabel emailLabel;
    private JLabel expiryLabel;
    private JButton changePasswordButton;
    private IdentityRoleAssignmentsPanel assignedRolesPanel;
    private JButton closeButton;
    private User user;

    public MyAccountDialog(@NotNull Window owner, @NotNull final User user) {
        super(owner, MY_ACCOUNT, DEFAULT_MODALITY_TYPE);
        this.user = user;
        setContentPane(contentPanel);
        initButtons();
        initTextFields(user);
    }

    private void initTextFields(User user) {
        usernameLabel.setText(user.getLogin());
        firstNameLabel.setText(user.getFirstName());
        lastNameLabel.setText(user.getLastName());
        emailLabel.setText(user.getEmail());
        if (user instanceof InternalUser) {
            final String formattedExpiry;
            final long expiry = ((InternalUser) user).getExpiration();
            if (expiry == -1) {
                formattedExpiry = NEVER;
            } else {
                final SimpleDateFormat format = new SimpleDateFormat(new JDateTimeChooser().getDateFormatString());
                formattedExpiry = format.format(new Date(expiry));
            }
            expiryLabel.setText(formattedExpiry);
        } else {
            expiryLabel.setVisible(false);
        }
    }

    private void initButtons() {
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                dispose();
            }
        });
        getRootPane().setDefaultButton(closeButton);
        Utilities.setEscAction(this, closeButton);

        changePasswordButton.setAction( new ChangePasswordAction() );
    }

    private void createUIComponents() {
        final Set<Role> rolesForUser = new HashSet<>();
        Role defaultRole = null;
        try {
            rolesForUser.addAll(Registry.getDefault().getRbacAdmin().findRolesForUser(user));
            defaultRole = Registry.getDefault().getRbacAdmin().findDefaultRoleForIdentityProvider(user.getProviderId());
        } catch (final FindException e) {
            logger.log(Level.WARNING, "Unable to retrieve roles for user: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
        assignedRolesPanel = new IdentityRoleAssignmentsPanel(user, rolesForUser, defaultRole, true);
    }
}
