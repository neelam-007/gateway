package com.l7tech.console.security.rbac;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.rbac.*;
import com.l7tech.console.util.Registry;
import com.l7tech.console.panels.PermissionFlags;
import com.l7tech.objectmodel.FindException;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

public class RoleManagementDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(RoleManagementDialog.class.getName());

    private JButton buttonOK;
    private JButton buttonCancel;
    private JPanel informationPane;

    private JList roleList;
    private JButton addRole;
    private JButton editRole;
    private JButton removeRole;

    private JPanel mainPanel;
    private JTextPane propertiesPane;

    private final PermissionFlags flags;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RbacGui");

    private final RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();

    private final ActionListener roleActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            try {
                doUpdateRoleAction(e);
            } catch (RemoteException e1) {
                throw new RuntimeException(e1);
            }
        }
    };
    private JScrollPane propertyScroller;

    public RoleManagementDialog(Dialog parent) throws HeadlessException {
        super(parent, resources.getString("manageRoles.title"));
        flags = PermissionFlags.get(EntityType.RBAC_ROLE);
        initialize();
    }

    public RoleManagementDialog(Frame parent) throws HeadlessException {
        super(parent, resources.getString("manageRoles.title"));
        flags = PermissionFlags.get(EntityType.RBAC_ROLE);
        initialize();
    }

    private void initialize() {
        enableRoleManagmentButtons(RbacUtilities.isEnableRoleEditing());

        populateList();
        setupButtonListeners();
        setupActionListeners();

        roleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(mainPanel);

        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        enableEditRemoveButtons();

        pack();
    }

    private void enableRoleManagmentButtons(boolean enable) {
        addRole.setVisible(flags.canCreateSome() && enable);
        removeRole.setVisible(flags.canDeleteSome() && enable);
    }

    private void setupActionListeners() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        informationPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        roleList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                    //disable this code for now since we are not allowing the editing of roles in 3.6.
                    //Uncomment this to allow double click editing and enable/disable of the buttons
                    if (e.getClickCount() == 1)
                        enableEditRemoveButtons();
                    else if (e.getClickCount() >= 2) {
                        showEditDialog(getSelectedRole());
                        try {
                            updatePropertiesSummary();
                        } catch (RemoteException re) {
                            throw new RuntimeException(re);
                        }
                    }
            }

        });

        roleList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableEditRemoveButtons();
                try {
                    updatePropertiesSummary();
                } catch (RemoteException re) {
                    throw new RuntimeException(re);
                }
            }
        });
    }

    private void updatePropertiesSummary() throws RemoteException {
        String message = null;

        Role role = getSelectedRole();
        if (role != null) {
            String roleName = role.getName();
            String roleDescription = role.getDescription();

            StringBuilder sb = new StringBuilder();
            sb.append("<html>");

            if (StringUtils.isNotEmpty(roleDescription)) {
                sb.append(RbacUtilities.getDescriptionString(role, true));
                sb.append("<br>");
            } else {
                sb.append("<strong>Permissions:</strong><br>");
                for (String s : getPermissionList(role)) {
                    sb.append(MessageFormat.format("&nbsp&nbsp&nbsp{0}<br>\n", s));
                }
            }

            sb.append("<br>");

            sb.append("<strong>Assignments:</strong><br>");

            for (String u : getAssignmentList(role)) {
                sb.append(MessageFormat.format("&nbsp&nbsp&nbsp{0}<br>\n", u));
            }

            sb.append("</html>");
            message = sb.toString();
        }
        propertiesPane.setText(message);
        propertiesPane.getCaret().setDot(0);
    }

    private Set<String> getPermissionList(Role role) {
        Set<String> sorted = new TreeSet<String>();
        if (role != null) {
            Set<Permission> permissions = role.getPermissions();
            if (permissions == null || permissions.isEmpty()) {
                sorted.add("   NONE \n");
            } else {
                for (Permission permission : permissions) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("    ").append(permission.getOperation()).append(" ");
                    EntityType etype = permission.getEntityType();
                    switch(permission.getScope().size()) {
                        case 0:
                            sb.append("[Any");
                            if (etype == EntityType.ANY)
                                sb.append(" Object");
                            else {
                                sb.append(" ").append(etype.getName());
                            }
                            sb.append("]");
                            break;
                        case 1:
                            sb.append(etype.getName()).append(" ").append(
                                    permission.getScope().iterator().next().toString());
                            break;
                        default:
                            sb.append("[Complex Scope]");
                    }
                    sorted.add(sb.toString());
                }
            }
        }
        return sorted;
    }

    private Set<String> getAssignmentList(Role role) throws RemoteException {
        Set<String> sorted = new TreeSet<String>();

        if (role != null) {
            Set<UserRoleAssignment> users = role.getUserAssignments();
            if (users == null || users.isEmpty()) {
                sorted.add("   NONE\n");
            } else {
                for (UserRoleAssignment ura : users) {
                    try {
                        UserHolder holder = new UserHolder(ura);
                        sorted.add("   " + holder);
                    } catch (FindException e) {
                        logger.warning("Could not find a user with id=" + ura.getUserId());
                    } catch (UserHolder.NoSuchUserException e) {
                        logger.info("Removing deleted user #" + ura.getUserId());
                    }
                }
            }
        }

        return sorted;
    }

    private void setupButtonListeners() {
        editRole.addActionListener(roleActionListener);
        addRole.addActionListener(roleActionListener);
        removeRole.addActionListener(roleActionListener);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
    }

    private void enableEditRemoveButtons() {
        boolean validRowSelected = roleList.getModel().getSize() != 0 &&
                roleList.getSelectedValue() != null;

        boolean hasUpdatePermissions = flags.canUpdateSome();
        boolean canRemovePermissions = flags.canDeleteSome();

        removeRole.setEnabled(canRemovePermissions && validRowSelected);
        editRole.setEnabled(validRowSelected);
    }

    private void doUpdateRoleAction(ActionEvent e) throws RemoteException {
        Object source = e.getSource();
        if (source == null || !(source instanceof JButton)) {
            return;
        }

        JButton srcButton = (JButton) source;
        if (srcButton == addRole) {
            Role newRole = showEditDialog(new Role());
            if (newRole != null) populateList();
            updatePropertiesSummary();
        } else if (srcButton == editRole) {
            Role r = showEditDialog(getSelectedRole());
            if (r != null) populateList();
            updatePropertiesSummary();
        } else if (srcButton == removeRole) {
            final Role selectedRole = getSelectedRole();
            if (selectedRole == null) return;
            Utilities.doWithConfirmation(
                this,
                resources.getString("manageRoles.deleteTitle"),
                MessageFormat.format(resources.getString("manageRoles.deleteMessage"), selectedRole.getName()),
                new Runnable() {
                    public void run() {
                        try {
                            rbacAdmin.deleteRole(selectedRole);
                            populateList();
                        } catch (Exception e1) {
                            throw new RuntimeException("Couldn't delete Role", e1);
                        }
                    }
                });
            updatePropertiesSummary();
        }
    }

    private Role getSelectedRole() {
        return (Role)roleList.getSelectedValue();
    }

    private Role showEditDialog(Role selectedRole) {
        if (selectedRole == null) return null;

        EditRoleDialog dlg = new EditRoleDialog(selectedRole, this);
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        Role updated = dlg.getRole();
        if (updated != null) {
            Role sel = (Role) roleList.getSelectedValue();
            populateList();
            roleList.setSelectedValue(sel, true);
        }
        return updated;
    }

    private void populateList() {
        try {
            Role[] roles = rbacAdmin.findAllRoles().toArray(new Role[0]);
            Arrays.sort(roles);
            roleList.setModel(new DefaultComboBoxModel(roles));
        } catch (Exception e) {
            throw new RuntimeException("Couldn't get initial list of Roles", e);
        }
    }

    private void onOK() {
        dispose();
    }

    private void onCancel() {
        dispose();
    }
}
