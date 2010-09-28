package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.PermissionFlags;
import com.l7tech.console.panels.identity.finder.FindIdentitiesDialog;
import com.l7tech.console.panels.identity.finder.Options;
import com.l7tech.console.panels.identity.finder.SearchType;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gui.util.DocumentSizeFilter;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;
import java.util.*;

public class EditRoleDialog extends JDialog {
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
    private JButton addAssignment;
    private JButton removeAssignment;
    private JTable roleAssigneeTable;
    private RoleAssignmentTableModel roleAssignmentTableModel;

    private PermissionTableModel tableModel;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RbacGui");

    private final ActionListener permissionsListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            doPermissionAction(e);
        }
    };

    private final IdentityAdmin identityAdmin = Registry.getDefault().getIdentityAdmin();
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

        this.tableModel = new PermissionTableModel();
        permissionsTable.setModel(tableModel);
        permissionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Utilities.setRowSorter(permissionsTable, tableModel, new int[] {0}, new boolean[] {true}, new Comparator[] {null});

        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);

        ((AbstractDocument)roleDescription.getDocument()).setDocumentFilter(new DocumentSizeFilter(255));
        if (role.getOid() == Role.DEFAULT_OID) {
            setTitle(resources.getString("editRoleDialog.newTitle"));
        } else {
            setTitle(MessageFormat.format(resources.getString(flags.canUpdateSome()?"editRoleDialog.existingTitle":"editRoleDialog.readOnlyExistingTitle"), role.getName()));
            roleName.setText(role.getName());
            if ( shouldAllowEdits ) {
                roleDescription.setText(role.getDescription()); // then we need to show the actual description with placeholders 
            } else {
                roleDescription.setText(RbacUtilities.getDescriptionString(role, false));
            }
            roleDescription.getCaret().setDot(0);
        }

        setUpAssigneeTable();        

        setupButtonListeners();
        setupActionListeners();
        applyFormSecurity();
        updateButtonStates();
        pack();
    }

    private void setUpAssigneeTable(){
        try{
            roleAssignmentTableModel = new RoleAssignmentTableModel(role);
        }catch(Exception ex){
            throw new RuntimeException("Could not look up assignments for role", ex);
        }
        this.roleAssigneeTable.setModel(roleAssignmentTableModel);
        DefaultListSelectionModel dlsm = new DefaultListSelectionModel();
        dlsm.addListSelectionListener(new RoleAssignmentListSelectionListener(this));
        this.roleAssigneeTable.setSelectionModel(dlsm);
        //don't allow the user to be able to reorder columns in the table
        this.roleAssigneeTable.getTableHeader().setReorderingAllowed(false);

        Utilities.setRowSorter(roleAssigneeTable, roleAssignmentTableModel, new int[]{0,1}, new boolean[]{true, true},
                new Comparator[]{null, RoleAssignmentTableModel.USER_GROUP_COMPARATOR});
        TableColumn tC = roleAssigneeTable.getColumn(RoleAssignmentTableModel.USER_GROUPS);
        tC.setCellRenderer(new UserGroupTableCellRenderer(roleAssigneeTable));
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

    private static class RoleAssignmentListSelectionListener implements ListSelectionListener{
        private EditRoleDialog dialog;

        RoleAssignmentListSelectionListener(EditRoleDialog dialog){
            this.dialog = dialog;                
        }
        @Override
        public void valueChanged(ListSelectionEvent e) {
            dialog.enableAssignmentDeleteButton();
        }
    }

    private void enableAssignmentDeleteButton() {
        int index = this.roleAssigneeTable.getSelectedRow();
        
        boolean validRowSelected = index > -1;

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

        @Override
        public String getColumnName(int column) {
            return COL_NAMES[column];
        }

        @Override
        public int getRowCount() {
            return permissions.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
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
                    EntityType etype = perm.getEntityType();
                    switch(perm.getScope().size()) {
                        case 0:
                            StringBuilder sb = new StringBuilder("<Any");
                            if (etype == EntityType.ANY)
                                sb.append(" Object");
                            else {
                                sb.append(" ").append(etype.getName());
                            }
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

    private void updateButtonStates() {
        buttonOK.setEnabled(StringUtils.isNotEmpty(roleName.getText()));
        enablePermissionEditDeleteButtons();
        enableAssignmentDeleteButton();
    }

    private void setupActionListeners() {
        ListSelectionListener listListener = new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateButtonStates();
            }
        };

        roleName.getDocument().addDocumentListener(
                new RunOnChangeListener(new Runnable() {
                    @Override
                    public void run() {
                        updateButtonStates();
                    }
                })
        );

        permissionsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2 && shouldAllowEdits)
                    showEditPermissionDialog(getSelectedPermission());
            }
        });
        permissionsTable.getSelectionModel().addListSelectionListener(listListener);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void setupButtonListeners() {
        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                    onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        addPermission.addActionListener(permissionsListener);
        editPermission.addActionListener(permissionsListener);
        removePermission.addActionListener(permissionsListener);

        addAssignment.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Options opts = new Options();
                opts.setInitialProvider(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
                opts.setSearchType(SearchType.ALL);
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
                        if(header.getType() == com.l7tech.objectmodel.EntityType.USER){
                            User user = identityAdmin.findUserByID(providerId, header.getStrId());
                             roleAssignmentTableModel.addRoleAssignment(new RoleAssignment(role, user.getProviderId(), user.getId(), EntityType.USER));
                        }else if(header.getType() == com.l7tech.objectmodel.EntityType.GROUP){
                            Group group = identityAdmin.findGroupByID(providerId, header.getStrId());
                            roleAssignmentTableModel.addRoleAssignment(new RoleAssignment(role, group.getProviderId(), group.getId(), EntityType.GROUP));
                        }else{
                            throw new RuntimeException("Identity of type " + header.getType()+" is not supported");
                        }
                    } catch (DuplicateObjectException dup) {
                        JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(), dup.getMessage(), "Could not add assignment", JOptionPane.ERROR_MESSAGE);
                    } catch (Exception e1) {
                        throw new RuntimeException("Couldn't find User", e1);
                    }
                }
            }
        });

        removeAssignment.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utilities.doWithConfirmation(
                    EditRoleDialog.this,
                    resources.getString("manageRoles.removeAssignmentTitle"), resources.getString("manageRoles.removeAssignmentMessage"), new Runnable() {
                    @Override
                    public void run() {
                        int [] selectedRow = roleAssigneeTable.getSelectedRows();
                        Integer [] selectedModel = new Integer[selectedRow.length];
                        for (int i= 0; i< selectedRow.length; i++) {
                            selectedModel[i] = Utilities.convertRowIndexToModel(roleAssigneeTable,selectedRow[i]);
                        }
                        Arrays.sort( selectedModel, Collections.reverseOrder() );
                        for ( int modelRow : selectedModel ) {
                            roleAssignmentTableModel.removeRoleAssignment(modelRow);
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
            Utilities.doWithConfirmation(EditRoleDialog.this, "manageRoles.removePermissionTitle", "removePermissionMessage", new Runnable() {
                @Override
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
            if ( shouldAllowEdits ) {
                role.setName(roleName.getText());
                role.setDescription(roleDescription.getText());
            }
            Set<Permission> perms = role.getPermissions();
            perms.clear();
            for (Permission perm : tableModel.getPermissions()) {
                perms.add(perm);
            }

            role.getRoleAssignments().clear();
            for (RoleAssignment assignment : this.roleAssignmentTableModel.getRoleAssignments()) {
                role.getRoleAssignments().add(assignment);
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
