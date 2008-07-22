package com.l7tech.console.security.rbac;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DocumentSizeFilter;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.console.panels.PermissionFlags;
import com.l7tech.console.panels.identity.finder.FindIdentitiesDialog;
import com.l7tech.console.panels.identity.finder.Options;
import com.l7tech.console.panels.identity.finder.SearchType;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
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
    private JTextArea roleDescription;
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
    private boolean shouldAllowEdits = RbacUtilities.isEnableRoleEditing();

    private static final String[] COL_NAMES = new String[]{"Operation", "Applies To"};

    private final PermissionFlags flags;

    public EditRoleDialog(Role role, Dialog parent) {
        super(parent, true);
        this.role = role;
        flags = PermissionFlags.get(EntityType.RBAC_ROLE);
        initialize();
    }

    public EditRoleDialog(Role role, Frame parent) {
        super(parent, true);
        this.role = role;
        flags = PermissionFlags.get(EntityType.RBAC_ROLE);
        initialize();
    }

    private void initialize() {
        final SecurityProvider provider = Registry.getDefault().getSecurityProvider();
        if (provider == null) {
            throw new IllegalStateException("Could not instantiate security provider");
        }

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

        ((AbstractDocument)roleDescription.getDocument()).setDocumentFilter(new DocumentSizeFilter(255));
        if (role.getOid() == Role.DEFAULT_OID) {
            setTitle(resources.getString("editRoleDialog.newTitle"));
        } else {
            setTitle(MessageFormat.format(resources.getString(flags.canUpdateSome()?"editRoleDialog.existingTitle":"editRoleDialog.readOnlyExistingTitle"), role.getName()));
            roleName.setText(role.getName());
            roleDescription.setText(RbacUtilities.getDescriptionString(role, false));
            roleDescription.getCaret().setDot(0);
        }
        setupButtonListeners();
        setupActionListeners();
        applyFormSecurity();
        updateButtonStates();
        pack();
    }

    /**
     * Apply form security to elements that are not dynamically enabled disabled.
     *
     * <p>Disables buttons that are dynamically set</p>
     *
     * @see #updateButtonStates
     */
    private void applyFormSecurity() {
        boolean canEdit = flags.canUpdateSome();

        addPermission.setVisible(shouldAllowEdits);
        editPermission.setVisible(shouldAllowEdits);
        removePermission.setVisible(shouldAllowEdits);

        roleName.setEditable(canEdit && shouldAllowEdits);
        roleDescription.setEditable(canEdit && shouldAllowEdits);
        addPermission.setEnabled(canEdit && shouldAllowEdits);
        addAssignment.setEnabled(flags.canUpdateSome());

        // these are enabled later if selections / permissions allow
        editPermission.setEnabled(false);
        removePermission.setEnabled(false);
        removeAssignment.setEnabled(false);        
    }

    private void enablePermissionEditDeleteButtons() {
        boolean validRowSelected = permissionsTable.getModel().getRowCount() != 0 &&
                getSelectedPermission() != null;

        boolean hasEditPermission = flags.canUpdateSome();

        //we should only enable the permission edit/remove buttons if a valid row is selected AND if we are allowing
        // edits because of the mode AND if the user has permission in the first place.
        editPermission.setEnabled(validRowSelected && hasEditPermission && shouldAllowEdits);
        removePermission.setEnabled(validRowSelected && hasEditPermission && shouldAllowEdits);
    }

    private void enableAssignmentDeleteButton() {
        int index = userAssignmentList.getSelectedIndex();
        boolean validRowSelected = assignmentListModel.getSize() != 0 &&
                index < userAssignmentList.getModel().getSize() &&
                index > -1;

        boolean hasEditPermission = flags.canUpdateSome();

        //we should only enable the assignment remove button if a valid row is selected AND if the user has permission
        //in the first place.
        removeAssignment.setEnabled(validRowSelected && hasEditPermission);
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

        public AssignmentListModel() throws FindException {
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

        public synchronized void add(UserRoleAssignment ura) throws FindException, DuplicateObjectException {
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
        userAssignmentList.getSelectionModel().addListSelectionListener(listListener);

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
                opts.setAdminOnly(true);

                FindIdentitiesDialog fid = new FindIdentitiesDialog(TopComponents.getInstance().getTopParent(), true, opts);
                fid.pack();
                Utilities.centerOnScreen(fid);
                FindIdentitiesDialog.FindResult result = fid.showDialog();

                long providerId = result.providerConfigOid;
                for (EntityHeader header : result.entityHeaders) {
                    try {
                        User user = identityAdmin.findUserByID(providerId, header.getStrId());
                        assignmentListModel.add(new UserRoleAssignment(role, user.getProviderId(), user.getId()));
                    } catch (DuplicateObjectException dup) {
                        JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(), dup.getMessage(), "Could not add assignment", JOptionPane.ERROR_MESSAGE);
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
        dlg.pack();
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
        if (flags.canUpdateSome()) {
            role.setName(roleName.getText());
            role.setDescription(roleDescription.getText());
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
            }
        } else {
            role = null;
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
