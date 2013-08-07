package com.l7tech.console.panels;

import com.l7tech.console.security.rbac.BasicRolePropertiesPanel;
import com.l7tech.console.security.rbac.RoleSelectionDialog;
import com.l7tech.console.security.rbac.UserRoleRemovalDialog;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleAssignment;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * User: megery
 * Date: Oct 16, 2006
 * Time: 4:10:41 PM
 */
public class UserRoleAssignmentsPanel extends JPanel {
    static Logger log = Logger.getLogger(UserRoleAssignmentsPanel.class.getName());
    private static final int NAME_COL_INDEX = 0;
    private static final int INHERITED_COL_INDEX = 2;
    private static final String NAME_UNAVAILABLE = "name unavailable";
    private JPanel mainPanel;
    private JTable rolesTable;
    private BasicRolePropertiesPanel rolePropertiesPanel;
    private JButton removeButton;
    private JButton addButton;
    private boolean isAdminEnabled;
    private SimpleTableModel<Role> rolesModel;
    private User user;
    private Set<IdentityHeader> userGroups;

    public UserRoleAssignmentsPanel(@NotNull final User whichUser, @Nullable Set<IdentityHeader> userGroups, boolean isAdminEnabled) throws FindException {
        this.user = whichUser;
        this.userGroups = userGroups;
        this.isAdminEnabled = isAdminEnabled;
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        initTable();
        initButtons();
        handleSelectionChange();
    }

    private void initButtons() {
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final RoleSelectionDialog selectDialog = new RoleSelectionDialog(TopComponents.getInstance().getTopParent(), user.getName());
                selectDialog.pack();
                Utilities.centerOnParentWindow(selectDialog);
                DialogDisplayer.display(selectDialog, new Runnable() {
                    @Override
                    public void run() {
                        if (selectDialog.isConfirmed()) {
                            final RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();
                            final List<Role> selectedRoles = selectDialog.getSelectedRoles();
                            if (!selectedRoles.isEmpty()) {
                                try {
                                    for (final Role selectedRole : selectedRoles) {
                                        selectedRole.addAssignedUser(user);
                                        rbacAdmin.saveRole(selectedRole);
                                    }
                                } catch (final SaveException e) {
                                    log.log(Level.WARNING, "Error adding role assignment: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                                    DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), "Could not add role assignment", "Error", JOptionPane.ERROR_MESSAGE, null);
                                }
                                loadTable();
                            }
                        }
                    }
                });
            }
        });
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final List<Role> selectedRoles = getSelected();
                if (!selectedRoles.isEmpty()) {
                    final Map<Role, String> toRemove = new HashMap<>(selectedRoles.size());
                    for (final Role selectedRole : selectedRoles) {
                        if (!isRoleInherited(selectedRole)) {
                            final int row = rolesModel.getRowIndex(selectedRole);
                            final Object nameVal = rolesModel.getValueAt(row, NAME_COL_INDEX);
                            final String name;
                            if (nameVal instanceof String) {
                                name = (String) nameVal;
                            } else {
                                name = NAME_UNAVAILABLE;
                            }
                            toRemove.put(selectedRole, name);
                        }
                    }
                    final UserRoleRemovalDialog confirmation = new UserRoleRemovalDialog(TopComponents.getInstance().getTopParent(), user.getName(), toRemove);
                    confirmation.pack();
                    Utilities.centerOnParentWindow(confirmation);
                    DialogDisplayer.display(confirmation, new Runnable() {
                        @Override
                        public void run() {
                            if (confirmation.isConfirmed()) {
                                final RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();
                                try {
                                    for (final Role role : toRemove.keySet()) {
                                        role.removeAssignedUser(user);
                                        rbacAdmin.saveRole(role);
                                        rolesModel.removeRow(role);
                                    }
                                } catch (final SaveException ex) {
                                    log.log(Level.WARNING, "Error removing role assignment: " + ExceptionUtils.getMessage(ex), ExceptionUtils.getDebugException(ex));
                                    DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), "Could not remove role assignment", "Error", JOptionPane.ERROR_MESSAGE, null);
                                }
                            }
                        }
                    });
                }
            }
        }

        );
    }

    private void initTable() {
        rolesModel = TableUtil.configureTable(rolesTable,
                column("Name", 80, 400, 99999, new Functions.Unary<String, Role>() {
                    @Override
                    public String call(final Role role) {
                        return getNameForRole(role);
                    }
                }),
                column("Type", 40, 80, 99999, new Functions.Unary<String, Role>() {
                    @Override
                    public String call(final Role role) {
                        return role.isUserCreated() ? BasicRolePropertiesPanel.CUSTOM : BasicRolePropertiesPanel.SYSTEM;
                    }
                }),
                column("Inherited", 40, 80, 99999, new Functions.Unary<Boolean, Role>() {
                    @Override
                    public Boolean call(final Role role) {
                        if (userGroups != null) {
                            for (final RoleAssignment assignment : role.getRoleAssignments()) {
                                if (EntityType.GROUP.getName().equals(assignment.getEntityType())) {
                                    for (final IdentityHeader userGroup : userGroups) {
                                        if (userGroup.getProviderOid() == assignment.getProviderId() &&
                                                userGroup.getStrId().equals(assignment.getIdentityId())) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                        return false;
                    }
                }));
        rolesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                handleSelectionChange();
            }
        });
        rolesTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        Utilities.setRowSorter(rolesTable, rolesModel);
        loadTable();
    }

    private void handleSelectionChange() {
        final Role selected = roleFromRowIndex(rolesTable.getSelectedRow());
        removeButton.setEnabled(selected != null && !isRoleInherited(selected));
        rolePropertiesPanel.configure(selected, selected == null ? null : getNameForRole(selected));
    }

    private boolean isRoleInherited(final Role role) {
        boolean inherited = false;
        if (role != null) {
            final int row = rolesModel.getRowIndex(role);
            final Object inheritedVal = rolesModel.getValueAt(row, INHERITED_COL_INDEX);
            if (inheritedVal instanceof Boolean) {
                inherited = (Boolean) inheritedVal;
            }
        }
        return inherited;
    }

    private java.util.List<Role> getSelected() {
        final List<Role> selected = new ArrayList<>();
        final int[] selectedRows = rolesTable.getSelectedRows();
        for (int i = 0; i < selectedRows.length; i++) {
            final int row = selectedRows[i];
            final Role role = roleFromRowIndex(row);
            if (role != null) {
                selected.add(role);
            }
        }
        return selected;
    }

    private Role roleFromRowIndex(final int row) {
        Role role = null;
        if (row >= 0) {
            final int modelIndex = rolesTable.convertRowIndexToModel(row);
            if (modelIndex >= 0) {
                role = rolesModel.getRowObject(modelIndex);
            }
        }
        return role;
    }

    private void loadTable() {
        if (isAdminEnabled) {
            try {
                final java.util.List<Role> roles = new ArrayList<>(Registry.getDefault().getRbacAdmin().findRolesForUser(user));
                rolesModel.setRows(roles);
            } catch (final FindException e) {
                log.log(Level.WARNING, "Unable to retrieve user roles: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
    }

    private String getNameForRole(final Role role) {
        String name = NAME_UNAVAILABLE;
        try {
            name = Registry.getDefault().getEntityNameResolver().getNameForEntity(role, true);
            return name;
        } catch (final FindException e) {
            log.log(Level.WARNING, "Unable to retrieve name for role: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
        return name;
    }
}
