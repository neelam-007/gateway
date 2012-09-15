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
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DocumentSizeFilter;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;
import java.util.*;

public class EditRoleDialog extends JDialog {
    private Role role;
    private boolean confirmed = false;

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

    private SimpleTableModel<Permission> permissionsTableModel;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RbacGui");

    private final ActionListener permissionsListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            doPermissionAction(e);
        }
    };

    private final IdentityAdmin identityAdmin = Registry.getDefault().getIdentityAdmin();
    private boolean shouldAllowEdits;

    private final PermissionFlags flags;

    public EditRoleDialog(Role role, Window parent) {
        super(parent, ModalityType.DOCUMENT_MODAL);
        this.role = role;
        flags = PermissionFlags.get(EntityType.RBAC_ROLE);
        shouldAllowEdits = RbacUtilities.isEnableRoleEditing() && (role.isUserCreated() || RbacUtilities.isEnableBuiltInRoleEditing());
        initialize();
    }

    private void initialize() {
        final SecurityProvider provider = Registry.getDefault().getSecurityProvider();
        if (provider == null) {
            throw new IllegalStateException("Could not instantiate security provider");
        }

        this.permissionsTableModel = TableUtil.configureTable(permissionsTable,
            TableUtil.column("Operation", 20, 70, 120, operationCellGetter),
            TableUtil.column("Applies To", 150, 300, 99999, appliesToCellGetter));
        permissionsTableModel.setRows(new ArrayList<Permission>(role.getPermissions()));
        permissionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Utilities.setRowSorter(permissionsTable, permissionsTableModel, new int[] {0}, new boolean[] {true}, null);

        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);

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

        // set limit after populating.  default description could be longer then 255
        ((AbstractDocument)roleDescription.getDocument()).setDocumentFilter(new DocumentSizeFilter(255));
        
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
        boolean canEdit = flags.canUpdateSome() || flags.canCreateSome();

        addPermission.setVisible(shouldAllowEdits);
        editPermission.setVisible(shouldAllowEdits);
        removePermission.setVisible(shouldAllowEdits);

        roleName.setEditable(canEdit && shouldAllowEdits);
        roleDescription.setEditable(canEdit && shouldAllowEdits);
        addPermission.setEnabled(canEdit && shouldAllowEdits);
        addAssignment.setEnabled(flags.canUpdateSome() || flags.canCreateSome());

        // these are enabled later if selections / permissions allow
        editPermission.setEnabled(false);
        removePermission.setEnabled(false);
        removeAssignment.setEnabled(false);        
    }

    private void enablePermissionEditDeleteButtons() {
        boolean validRowSelected = permissionsTable.getModel().getRowCount() != 0 &&
                getSelectedPermission() != null;

        boolean hasEditPermission = flags.canUpdateSome() || flags.canCreateSome();

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
        boolean validRowSelected = this.roleAssigneeTable.getSelectedRow() > -1;

        boolean hasEditPermission = flags.canUpdateSome() || flags.canCreateSome();

        //we should only enable the assignment remove button if a valid row is selected AND if the user has permission
        //in the first place.
        removeAssignment.setEnabled(validRowSelected && hasEditPermission);
    }


    private Functions.Unary<String, Permission> operationCellGetter = new Functions.Unary<String, Permission>() {
        @Override
        public String call(Permission perm) {
            if (perm.getOperation() == OperationType.OTHER) {
                return perm.getOtherOperationName();
            } else {
                return perm.getOperation().getName();
            }
        }
    };

    private Functions.Unary<String, Permission> appliesToCellGetter = new Functions.Unary<String, Permission>() {
        @Override
        public String call(Permission perm) {
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
        }
    };

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
                if (e.getClickCount() == 2 && shouldAllowEdits) {
                    final Permission perm = getSelectedPermission();
                    if (perm != null) {
                        Permission p = showEditPermissionDialog(perm);
                        if (p != null) {
                            perm.copyFrom(p);
                            permissionsTableModel.fireTableDataChanged();
                        }
                    }
                }
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

                if (result == null)
                    return;

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
            final Permission perm = new Permission(role, null, EntityType.ANY);
            Permission p = showEditPermissionDialog(perm);
            if (p != null) {
                perm.copyFrom(p);
                permissionsTableModel.addRow(perm);
            }
            return;
        }

        final Permission perm = getSelectedPermission();
        if (perm == null) return;

        if (srcButton == editPermission) {
            Permission p = showEditPermissionDialog(perm);
            if (p != null) {
                perm.copyFrom(p);
                permissionsTableModel.fireTableDataChanged();
            }
        } else if (srcButton == removePermission) {
            Utilities.doWithConfirmation(EditRoleDialog.this, "manageRoles.removePermissionTitle", "removePermissionMessage", new Runnable() {
                @Override
                public void run() {
                    permissionsTableModel.removeRow(perm);
                }
            });
        }
    }

    // Returns a possibly-edited anymous copy of the specified permission if the edit dialog is confirmed, or null if the dialog was canceled.
    // Caller is responsible for ensuring that any changes get written back to the appropriate place.
    // Note that the permission returned by this method, being an anonymous copy, will have a null role.
    private Permission showEditPermissionDialog(Permission perm) {
        EditPermissionsDialog dlg = new EditPermissionsDialog(perm.getAnonymousClone(), this);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        return dlg.getPermission();
    }

    private Permission getSelectedPermission() {
        int row = permissionsTable.getSelectedRow();
        if (row == -1)
            return null;
        row = permissionsTable.convertRowIndexToModel(row);
        return permissionsTableModel.getRowObject(row);
    }

    private void onOK() {
        if (flags.canUpdateSome() || flags.canCreateSome()) {
            if ( shouldAllowEdits ) {
                role.setName(roleName.getText());
                role.setDescription(roleDescription.getText());
            }
            Set<Permission> perms = role.getPermissions();
            perms.clear();
            for (Permission perm : permissionsTableModel.getRows()) {
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
            confirmed = true;
        } else {
            role = null;
        }
        dispose();
    }

    public Role getRole() {
        return confirmed ? role : null;
    }

    private void onCancel() {
        confirmed = false;
        role = null;
        dispose();
    }
}
