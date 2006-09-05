package com.l7tech.console.security.rbac;

import com.l7tech.common.gui.util.RunOnChangeListener;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.rbac.*;
import com.l7tech.common.security.rbac.EntityType;
import static com.l7tech.common.security.rbac.EntityType.RBAC_ROLE;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.console.panels.identity.finder.FindIdentitiesDialog;
import com.l7tech.console.panels.identity.finder.Options;
import com.l7tech.console.panels.identity.finder.SearchType;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.security.FormAuthorizationPreparer;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

public class EditRoleDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(EditRoleDialog.class.getName());

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

    private FormAuthorizationPreparer formAuthorizationPreparer;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RbacGui");

    private final ActionListener permissionsListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            doPermissionAction(e);
        }
    };

    private final IdentityAdmin identityAdmin = Registry.getDefault().getIdentityAdmin();
    private final Map<Long, String> idpNames = new HashMap<Long, String>();
    private boolean shouldAllowEdits = RbacUtilities.isEnableRoleEditing();

    private static final String[] COL_NAMES = new String[]{"Operation", "Applies To"};

    public EditRoleDialog(Role role, Dialog parent) {
        super(parent, true);
        this.role = role;
        initialize();
    }

    public EditRoleDialog(Role role, Frame parent) {
        super(parent, true);
        this.role = role;
        initialize();
    }

    private void initialize() {
        final SecurityProvider provider = Registry.getDefault().getSecurityProvider();
        if (provider == null) {
            throw new IllegalStateException("Could not instantiate security provider");
        }
        formAuthorizationPreparer = new FormAuthorizationPreparer(provider, new AttemptedCreate(RBAC_ROLE));


        enablePermissionEdits(shouldAllowEdits);

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
        permissionsTable.setModel(tableModel);
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
        updateButtonStates();
        pack();
    }

    private void applyFormSecurity() {
        // list components that are subject to security (they require the full admin role)
        formAuthorizationPreparer.prepare(new Component[]{
            addAssignment,
            removeAssignment,
        });
    }

    private void enablePermissionEdits(boolean enableRoleEditing) {
        addPermission.setVisible(enableRoleEditing);
        editPermission.setVisible(enableRoleEditing);
        removePermission.setVisible(enableRoleEditing);

        addPermission.setEnabled(enableRoleEditing);
        editPermission.setEnabled(enableRoleEditing);
        removePermission.setEnabled(enableRoleEditing);
    }

    private void enablePermissionEditDeleteButtons() {
        boolean enabled = permissionsTable.getModel().getRowCount() != 0 &&
                getSelectedPermission() != null;

        editPermission.setEnabled(enabled);
        removePermission.setEnabled(enabled);
    }

    private void enableAssignmentDeleteButton() {
        boolean enabled = assignmentListModel.getSize() != 0 &&
                userAssignmentList.getSelectedIndex() < userAssignmentList.getModel().getSize();

        removeAssignment.setEnabled(enabled);
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
            return 2;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            Permission perm = permissions.get(rowIndex);
            switch(columnIndex) {
                case 0:
                    if (perm.getOperation() == OperationType.OTHER) {
                        return perm.getOtherOperationName();
                    } else {
                        return perm.getOperation().getName();
                    }
                case 1:
                    StringBuilder sb = new StringBuilder();
                    EntityType etype = perm.getEntityType();
                    switch(perm.getScope().size()) {
                        case 0:
                            sb.append("<Any");
                            if (etype == EntityType.ANY)
                                sb.append(" Object");
                            else {
                                sb.append(" ").append(etype.getName());
                            }
                            sb.append(">");
                            return sb.toString();
                        case 1:
                            sb.append(etype.getName()).append(" ").append(
                                    perm.getScope().iterator().next().toString());
                            return sb.toString();
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

    private class AssignmentListModel extends AbstractListModel {
        private final List<UserRoleAssignment> assignments = new ArrayList<UserRoleAssignment>();
        private final List<UserHolder> holders = new ArrayList<UserHolder>();

        public AssignmentListModel() throws RemoteException, FindException {
            for (UserRoleAssignment assignment : role.getUserAssignments()) {
                try {
                    UserHolder uh = new UserHolder(assignment);
                    holders.add(uh);
                    assignments.add(assignment);
                } catch (UserHolder.NoSuchUserException e) {
                    logger.info("Removing deleted user #" + assignment.getUserId());
                }
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

        public synchronized void add(UserRoleAssignment ura) throws RemoteException, FindException, DuplicateObjectException {
            try {
                UserHolder holder = null;
                try {
                    holder = new UserHolder(ura);
                } catch (UserHolder.NoSuchUserException e) {
                    throw new FindException("Can't assign deleted user", e);
                }
                if (assignments.contains(ura) || holders.contains(holder)) {
                    throw new DuplicateObjectException("The user \"" + holder.getUser().getName() + "\" is already assigned to this role");
                }
                assignments.add(ura);
                holders.add(holder);
            } finally {
                fireContentsChanged(userAssignmentList, 0, assignments.size());
            }
        }

        public synchronized Object getElementAt(int index) {
            return holders.get(index);
        }
    }

    private void updateButtonStates() {
        buttonOK.setEnabled(StringUtils.isNotEmpty(roleName.getText()));
        enablePermissionEditDeleteButtons();
        enableAssignmentDeleteButton();
        applyFormSecurity();
    }

    private void setupActionListeners() {
        ListSelectionListener listListener = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateButtonStates();
            }
        };

        roleName.getDocument().addDocumentListener(
                new RunOnChangeListener(new Runnable() {
                    public void run() {
                        updateButtonStates();
                    }
                })
        );

        permissionsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2 && shouldAllowEdits)
                    showEditPermissionDialog(getSelectedPermission());
            }
        });
        permissionsTable.getSelectionModel().addListSelectionListener(listListener);

        userAssignmentList.setModel(assignmentListModel);
        userAssignmentList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1)
                    updateButtonStates();
            }
        });

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

        addAssignment.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Options opts = new Options();
                opts.setInitialProvider(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
                opts.setSearchType(SearchType.USER);
                opts.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                opts.setDisposeOnSelect(true);
                opts.setDisableOpenProperties(true);
                opts.setInternalOnly(true);

                FindIdentitiesDialog fid = new FindIdentitiesDialog(TopComponents.getInstance().getMainWindow(), true, opts);
                fid.pack();
                Utilities.centerOnScreen(fid);
                FindIdentitiesDialog.FindResult result = fid.showDialog();

                long providerId = result.providerConfigOid;
                for (EntityHeader header : result.entityHeaders) {
                    try {
                        User user = identityAdmin.findUserByID(providerId, header.getStrId());
                        assignmentListModel.add(new UserRoleAssignment(role, user.getProviderId(), user.getId()));
                    } catch (DuplicateObjectException dup) {
                        JOptionPane.showMessageDialog(TopComponents.getInstance().getMainWindow(), dup.getMessage(), "Could not add assignment", JOptionPane.ERROR_MESSAGE);
                    } catch (Exception e1) {
                        throw new RuntimeException("Couldn't find User", e1);
                    }
                }
            }
        });

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
                        updateButtonStates();
                    }
                });
            }
        });
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
        } catch (ObjectModelException se) {
            JOptionPane.showMessageDialog(this.getParent(),
                    "The Role could not be saved: " + ExceptionUtils.getMessage(se),
                    "Error Saving Role",
                    JOptionPane.ERROR_MESSAGE);
        } catch (RemoteException re) {
            throw new RuntimeException(re);
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
