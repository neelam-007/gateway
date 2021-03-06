package com.l7tech.console.panels;

import com.l7tech.console.security.rbac.BasicRolePropertiesPanel;
import com.l7tech.console.security.rbac.RoleSelectionDialog;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleAssignment;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.Group;
import com.l7tech.identity.Identity;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * Panel for displaying/modifying the Roles that an Identity (User/Group) is assigned to.
 */
public class IdentityRoleAssignmentsPanel extends JPanel {
    static Logger log = Logger.getLogger(IdentityRoleAssignmentsPanel.class.getName());
    private static final int INHERITED_COL_INDEX = 2;
    private static final String NAME_UNAVAILABLE = "name unavailable";
    private static final String YES = "Yes";
    private static final String NO = "No";
    private JPanel mainPanel;
    private JTable rolesTable;
    private BasicRolePropertiesPanel rolePropertiesPanel;
    private JButton removeButton;
    private JButton addButton;
    private JLabel defaultRoleLabel;
    private SimpleTableModel<Role> rolesModel;
    private Identity identity;
    private EntityType entityType;
    private Set<Role> assignedRoles;
    private Role defaultRole;
    private boolean readOnly;

    /**
     * @param identity      the Identity that this panel is displaying role assignments for.
     * @param assignedRoles the Roles assigned to the identity.
     * @param readOnly      true if the panel should only display the roles, and not allow any changes.
     */
    public IdentityRoleAssignmentsPanel(@NotNull final Identity identity, @NotNull Set<Role> assignedRoles, @Nullable Role defaultRole, final boolean readOnly) {
        if (!(identity instanceof User) && !(identity instanceof Group)) {
            throw new IllegalArgumentException("Identity must be a user or group");
        }
        this.identity = identity;
        this.entityType = identity instanceof User ? EntityType.USER : EntityType.GROUP;
        this.assignedRoles = assignedRoles;
        this.defaultRole = defaultRole;
        this.readOnly = readOnly;
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        initTable();
        initButtons();
        handleSelectionChange();
        handleTableDataChange();
        checkDefaultRole();
    }

    private void checkDefaultRole() {
        if (defaultRole == null || rolesModel.getRowCount() > 0) {
            defaultRoleLabel.setText("Not using default role");
            defaultRoleLabel.setVisible(false);
            return;
        }

        final String defaultRoleName = getNameForRole(defaultRole);
        defaultRoleLabel.setText("Using default role: " + defaultRoleName);
        defaultRoleLabel.setVisible(true);
        rolePropertiesPanel.configure(defaultRole, defaultRoleName);
    }

    /**
     * @return a list of Roles that the user has selected to be added to the role assignments for the identity.
     */
    public List<Role> getAddedRoles() {
        final List<Role> addedRoles = new ArrayList<>();
        for (final Role modelRow : rolesModel.getRows()) {
            if (!assignedRoles.contains(modelRow)) {
                addedRoles.add(modelRow);
            }
        }
        return addedRoles;
    }

    /**
     * @return a list of Roles that the user has selected to be removed from the role assignments for the identity.
     */
    public List<Role> getRemovedRoles() {
        final List<Role> removedRoles = new ArrayList<>();
        final List<Role> modelRows = rolesModel.getRows();
        for (final Role assignedRole : assignedRoles) {
            if (!modelRows.contains(assignedRole)) {
                removedRoles.add(assignedRole);
            }
        }
        return removedRoles;
    }

    private void initButtons() {
        addButton.setVisible(!readOnly);
        removeButton.setVisible(!readOnly);
        if (!readOnly) {
            addButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    final RoleSelectionDialog selectDialog = new RoleSelectionDialog(TopComponents.getInstance().getTopParent(), "Add Roles to " + identity.getName(), rolesModel.getRows());
                    selectDialog.pack();
                    Utilities.centerOnParentWindow(selectDialog);
                    DialogDisplayer.display(selectDialog, new Runnable() {
                        @Override
                        public void run() {
                            if (selectDialog.isConfirmed()) {
                                final List<Role> selectedRoles = selectDialog.getSelectedRoles();
                                if (!selectedRoles.isEmpty()) {
                                    for (final Role selectedRole : selectedRoles) {
                                        try {
                                            final Role roleWithPermissions = Registry.getDefault().getRbacAdmin().findRoleByPrimaryKey(selectedRole.getGoid());
                                            if (roleWithPermissions != null) {
                                                if (entityType == EntityType.GROUP) {
                                                    roleWithPermissions.addAssignedGroup((Group) identity);
                                                } else if (entityType == EntityType.USER) {
                                                    roleWithPermissions.addAssignedUser((User) identity);
                                                }
                                                rolesModel.addRow(roleWithPermissions);
                                            } else {
                                                log.log(Level.WARNING, "Selected role with goid " + selectedRole.getGoid() + " does not exist.");
                                            }
                                        } catch (final FindException | PermissionDeniedException e) {
                                            log.log(Level.WARNING, "Unable to retrieve selected role: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                                        }
                                    }
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
                        for (final Role selectedRole : selectedRoles) {
                            rolesModel.removeRow(selectedRole);
                        }
                    }
                }
            }
            );
        }
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
                column("Inherited", 40, 80, 99999, new Functions.Unary<String, Role>() {
                    @Override
                    public String call(final Role role) {
                        final String inherited;
                        final Set<RoleAssignment> roleAssignments = role.getRoleAssignments();
                        if (!roleAssignments.isEmpty()) {
                            boolean directlyAssigned = false;
                            // it is possible for the same role to be assigned more than once to an identity via its groups
                            // if at least one of the assignments is a direct assignment, we allow them to remove the role
                            for (final RoleAssignment assignment : roleAssignments) {
                                if (assignment.getEntityType() != null && entityType.getName().equals(assignment.getEntityType()) &&
                                        assignment.getIdentityId().equals(identity.getId()) && !assignment.isInherited()) {
                                    directlyAssigned = true;
                                    break;
                                }
                            }
                            inherited = directlyAssigned ? NO : YES;
                        } else {
                            // role was added by the user
                            inherited = NO;
                        }
                        return inherited;
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
        // use a copy of the assigned roles so that the original collection is not mutated
        rolesModel.setRows(new ArrayList<>(assignedRoles));
    }

    private void handleSelectionChange() {
        final Role selected = roleFromRowIndex(rolesTable.getSelectedRow());
        removeButton.setEnabled(selected != null && canRemove(selected));
        rolePropertiesPanel.configure(selected, selected == null ? null : getNameForRole(selected));
        if (rolesModel.getRowCount() < 1 && defaultRole != null)
            checkDefaultRole();
    }

    private void handleTableDataChange() {
        rolesTable.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                checkDefaultRole();
            }
        });
    }

    private boolean canRemove(final Role role) {
        boolean canRemove = true;
        if (role != null) {
            final int row = rolesModel.getRowIndex(role);
            final Object inheritedVal = rolesModel.getValueAt(row, INHERITED_COL_INDEX);
            if (inheritedVal.equals(YES)) {
                canRemove = false;
            }
        }
        return canRemove;
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
