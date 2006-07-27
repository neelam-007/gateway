package com.l7tech.console.security.rbac;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.RunOnChangeListener;
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

import org.apache.commons.lang.StringUtils;

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
                            StringBuilder sb = new StringBuilder("<Any");
                            if (perm.getEntityType() != EntityType.ANY)
                                sb.append(perm.getEntityType().getName());
                            sb.append(">");
                            return sb.toString();
                        case 1:
                            return perm.getScope().iterator().next().toString();
                        default:
                            return "<Complex Scope>";
                    }
                default:
                    throw new RuntimeException("Unsupported column " + columnIndex);
            }
        }

        public void add(Permission p) {
            permissions.add(p);
            fireTableDataChanged();
        }

        public void remove(Permission p) {
            permissions.remove(p);
            fireTableDataChanged();
        }
    }

    private void inititialize() {
        try {
            EntityHeader[] hs = identityAdmin.findAllIdentityProviderConfig();
            for (EntityHeader h : hs) {
                idpNames.put(h.getOid(), h.getName());
            }

            assignmentListModel = new AssignmentListModel();
        } catch (Exception e) {
            throw new RuntimeException("Couldn't lookup Identity Providers", e);
        }

        this.tableModel = new PermissionTableModel();
        this.permissionsTable.setModel(tableModel);
        permissionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);
        if (role.getOid() == Role.DEFAULT_OID) {
            setTitle(resources.getString("editRoleDialog.newTitle"));
        } else {
            setTitle(MessageFormat.format(resources.getString("editRoleDialog.existingTitle"), role.getName()));
            roleName.setText(role.getName());
        }

        setupButtonListeners();
        setupActionListeners();
        updateButtons();
        pack();
    }

    private class AssignmentListModel extends AbstractListModel {
        private final List<UserRoleAssignment> assignments = new ArrayList<UserRoleAssignment>();
        private final List<UserHolder> holders = new ArrayList<UserHolder>();

        public AssignmentListModel() throws RemoteException, FindException {
            for (UserRoleAssignment assignment : role.getUserAssignments()) {
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
            fireContentsChanged(userAssignmentList, 0, assignments.size());
        }

        public synchronized void add(UserRoleAssignment ura) throws RemoteException, FindException {
            assignments.add(ura);
            holders.add(new UserHolder(ura));
            fireContentsChanged(userAssignmentList, 0, assignments.size());
        }

        public synchronized Object getElementAt(int index) {
            return holders.get(index);
        }
    }

    private void updateButtons() {
        buttonOK.setEnabled(StringUtils.isNotEmpty(roleName.getText()));
    }

    private void setupActionListeners() {
        RunOnChangeListener roleNameListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                updateButtons();
            }
        });

        roleName.getDocument().addDocumentListener(roleNameListener);

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
        if (srcButton == addPermission) {
            Permission p = showEditPermissionDialog(new Permission(role, null, EntityType.ANY));
            if (p != null) tableModel.add(p);
            return;
        }

        final Permission perm = getSelectedPermission();
        if (perm == null) return;

        if (srcButton == editPermission) {
            Permission p = showEditPermissionDialog(perm);
            if (p != null) tableModel.fireTableDataChanged();
        } else if (srcButton == removePermission) {
            Utilities.doWithConfirmation(EditRoleDialog.this, "Remove Permission", "Are you sure you want to remove this permission", new Runnable() {
                public void run() {
                    tableModel.remove(perm);
                }
            });
        }
    }

    private Permission showEditPermissionDialog(Permission perm) {
        EditPermissionsDialog dlg = new EditPermissionsDialog(perm, this);
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        return dlg.getPermission();
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

        role.getUserAssignments().clear();
        for (UserRoleAssignment assignment : assignmentListModel.assignments) {
            role.getUserAssignments().add(assignment);
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
