package com.l7tech.console.security.rbac;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.rbac.*;
import com.l7tech.console.panels.identity.finder.FindIdentitiesDialog;
import com.l7tech.console.panels.identity.finder.Options;
import com.l7tech.console.panels.identity.finder.SearchType;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.rmi.RemoteException;

public class EditRoleDialog extends JDialog {
    private Role role;

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField roleName;
    private JButton addPermission;
    private JButton editPermission;
    private JButton removePermission;
    private JTable permissionsTable;
    private JList userAssignmentList;
    private JButton addAssignment;
    private JButton removeAssignment;

    private AssignmentListModel assignmentListModel;
    private PermissionTableModel tableModel;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RbacGui");

    private final ActionListener permissionsListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            doPermissionAction(e);
        }
    };

    private final IdentityAdmin identityAdmin = Registry.getDefault().getIdentityAdmin();
    private final Map<Long, String> idpNames = new HashMap<Long, String>();

    public EditRoleDialog(Role role, Dialog parent) {
        super(parent, true);
        this.role = role;
        inititialize();
    }

    public EditRoleDialog(Role role, Frame parent) {
        super(parent, true);
        this.role = role;
        inititialize();
    }

    private static final String[] COL_NAMES = new String[]{"Type", "Operation", "Applies To"};

    private class UserHolder {
        private final UserRoleAssignment userRoleAssignment;
        private final User user;
        private final String provName;

        public UserHolder(UserRoleAssignment ura) throws RemoteException, FindException {
            this.userRoleAssignment = ura;
            this.user = identityAdmin.findUserByID(ura.getProviderId(), ura.getUserId());
            String name = idpNames.get(user.getProviderId());
            if (name == null) name = "Unknown Identity Provider #" + user.getProviderId();
            provName = name;
        }

        public UserRoleAssignment getUserRoleAssignment() {
            return userRoleAssignment;
        }

        public User getUser() {
            return user;
        }

        public String toString() {
            String name = user.getName();
            if (name == null) name = user.getLogin();
            if (name == null) name = user.getUniqueIdentifier();
            StringBuilder sb = new StringBuilder(name);
            sb.append(" [").append(provName).append("]");
            return sb.toString();
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UserHolder that = (UserHolder) o;

            if (user != null ? !user.equals(that.user) : that.user != null) return false;

            return true;
        }

        public int hashCode() {
            return (user != null ? user.hashCode() : 0);
        }
    }

    private class PermissionTableModel extends AbstractTableModel {
        private final java.util.List<Permission> permissions;

        public java.util.List<Permission> getPermissions() {
            return permissions;
        }

        private PermissionTableModel() {
            permissions = new ArrayList<Permission>();
            permissions.addAll(role.getPermissions());
        }

        public String getColumnName(int column) {
            return COL_NAMES[column];
        }

        public int getRowCount() {
            return permissions.size();
        }

        public int getColumnCount() {
            return 3;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            Permission perm = permissions.get(rowIndex);
            switch(columnIndex) {
                case 0:
                    return perm.getEntityType().getName();
                case 1:
                    if (perm.getOperation() == OperationType.OTHER) {
                        return perm.getOtherOperationName();
                    } else {
                        return perm.getOperation().getName();
                    }
                case 2:
                    switch(perm.getScope().size()) {
                        case 0:
                            return "<Any " + perm.getEntityType().getName() + ">";
                        case 1:
                            return perm.getScope().iterator().next().toString();
                        default:
                            return "<Complex Scope>";
                    }
                default:
                    throw new RuntimeException("Unsupported column " + columnIndex);
            }
        }
    }

    private void inititialize() {
        try {
            assignmentListModel = new AssignmentListModel();

            EntityHeader[] hs = identityAdmin.findAllIdentityProviderConfig();
            for (EntityHeader h : hs) {
                idpNames.put(h.getOid(), h.getName());
            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't lookup Identity Providers", e);
        }

        this.tableModel = new PermissionTableModel();
        this.permissionsTable.setModel(tableModel);
        permissionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        if (role.getOid() != Role.DEFAULT_OID) {
            try {
                for (UserRoleAssignment ura : role.getUserAssignments()) {
                    assignmentListModel.add(ura);
                }
            } catch (Exception e1) {
                throw new RuntimeException("Couldn't find assigned users", e1);
            }
        }

        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);
        if (role.getOid() == Role.DEFAULT_OID) {
            setTitle(resources.getString("editRole.newTitle"));
        } else {
            setTitle(MessageFormat.format(resources.getString("editRole.existingTitle"), role.getName()));
            roleName.setText(role.getName());
        }

        setupButtonListeners();
        setupActionListeners();
        pack();
    }

    private class AssignmentListModel extends AbstractListModel {
        private final List<UserRoleAssignment> assignments = new ArrayList<UserRoleAssignment>();
        private final List<UserHolder> holders = new ArrayList<UserHolder>();

        public AssignmentListModel() throws RemoteException, FindException {
            for (UserRoleAssignment assignment : assignments) {
                assignments.add(assignment);
                holders.add(new UserHolder(assignment));
            }
        }

        public synchronized int getSize() {
            return assignments.size();
        }

        public synchronized void remove(UserHolder holder) {
            holders.remove(holder);
            assignments.remove(holder.getUserRoleAssignment());
            role.getUserAssignments().remove(holder.getUserRoleAssignment());
            fireContentsChanged(userAssignmentList, 0, assignments.size());
        }

        public synchronized void add(UserRoleAssignment ura) throws RemoteException, FindException {
            assignments.add(ura);
            holders.add(new UserHolder(ura));
            role.getUserAssignments().add(ura);
            fireContentsChanged(userAssignmentList, 0, assignments.size());
        }

        public synchronized Object getElementAt(int index) {
            return holders.get(index);
        }
    }

    private void setupActionListeners() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        addAssignment.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Options opts = new Options();
                opts.setInitialProvider(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
                opts.setSearchType(SearchType.USER);
                opts.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                opts.setDisposeOnSelect(true);
                opts.setDisableOpenProperties(true);

                FindIdentitiesDialog fid = new FindIdentitiesDialog(TopComponents.getInstance().getMainWindow(), true, opts);
                fid.pack();
                Utilities.centerOnScreen(fid);
                FindIdentitiesDialog.FindResult result = fid.showDialog();

                long providerId = result.providerConfigOid;
                for (EntityHeader header : result.entityHeaders) {
                    try {
                        User user = identityAdmin.findUserByID(providerId, header.getStrId());
                        assignmentListModel.add(new UserRoleAssignment(role, user.getProviderId(), user.getUniqueIdentifier()));
                    } catch (Exception e1) {
                        throw new RuntimeException("Couldn't find User", e1);
                    }
                }
            }
        });

        userAssignmentList.setModel(assignmentListModel);

        removeAssignment.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Utilities.doWithConfirmation(
                    EditRoleDialog.this,
                    resources.getString("manageRoles.removeAssignmentTitle"), resources.getString("manageRoles.removeAssignmentMessage"), new Runnable() {
                    public void run() {
                        Object[] selected = userAssignmentList.getSelectedValues();
                        for (Object o : selected) {
                            UserHolder u = (UserHolder)o;
                            assignmentListModel.remove(u);
                            role.getUserAssignments().remove(u.getUserRoleAssignment());
                        }
                    }
                });
            }
        });
    }

    private void setupButtonListeners() {
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

        addPermission.addActionListener(permissionsListener);
        editPermission.addActionListener(permissionsListener);
        removePermission.addActionListener(permissionsListener);
    }

    private void doPermissionAction(ActionEvent e) {
        Object src = e.getSource();
        if ((src == null) || !(src instanceof JButton))
            return;

        JButton srcButton = (JButton) src;
        if (srcButton == addPermission)
            showEditPermissionDialog(new Permission(role, null, null));
        else if (srcButton == editPermission) {
            Permission perm = getSelectedPermission();
            if (perm != null) showEditPermissionDialog(perm);
        } else if (srcButton == removePermission) {
            final Permission perm = getSelectedPermission();
            if (perm != null) {
                Utilities.doWithConfirmation(EditRoleDialog.this, "Remove Permission", "Are you sure you want to remove this permission", new Runnable() {
                    public void run() {
                        tableModel.getPermissions().remove(perm);
                        role.getPermissions().remove(perm);
                    }
                });
            }
        }
    }

    private void deleteWithConfirm(Permission selectedPermission) {
        if (selectedPermission == null)
            return;

        int result = JOptionPane.showConfirmDialog(this, "Remove the \"" + selectedPermission + "\" role from the system?", "Confirm Role Removal", JOptionPane.YES_NO_CANCEL_OPTION);
        if (result == JOptionPane.YES_OPTION) {
//            listModel.remove(selectedPermission);
        }
    }

    private void showEditPermissionDialog(Permission perm) {
        EditPermissionsDialog dlg = new EditPermissionsDialog(perm, this);
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
    }

    private Permission getSelectedPermission() {
        int row = permissionsTable.getSelectedRow();
        if (row == -1) return null;
        return tableModel.getPermissions().get(row);
    }

    private void onOK() {
        role.setName(roleName.getText());
        Set<Permission> perms = role.getPermissions();
        perms.clear();
        for (Permission perm : tableModel.getPermissions()) {
            perms.add(perm);
        }

        try {
            long oid = Registry.getDefault().getRbacAdmin().saveRole(role);
            role.setOid(oid);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't save Role", e);
        }

        dispose();
    }

    public Role getRole() {
        return role;
    }

    private void onCancel() {
        role = null;
        dispose();
    }
}
