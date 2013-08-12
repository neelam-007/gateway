package com.l7tech.console.panels;

import com.l7tech.console.security.rbac.BasicRolePropertiesPanel;
import com.l7tech.console.security.rbac.IdentityRoleRemovalDialog;
import com.l7tech.console.security.rbac.RoleSelectionDialog;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleAssignment;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * Panel for displaying/modifying the Roles that an Identity (User/Group) is assigned to.
 */
public class IdentityRoleAssignmentsPanel extends JPanel {
    static Logger log = Logger.getLogger(IdentityRoleAssignmentsPanel.class.getName());
    private static final int NAME_COL_INDEX = 0;
    private static final int INHERITED_COL_INDEX = 2;
    private static final String NAME_UNAVAILABLE = "name unavailable";
    private JPanel mainPanel;
    private JTable rolesTable;
    private BasicRolePropertiesPanel rolePropertiesPanel;
    private JButton removeButton;
    private JButton addButton;
    private SimpleTableModel<Role> rolesModel;
    private EntityType entityType;
    private String identityName;
    private List<Role> assignedRoles;
    private Set<IdentityHeader> identityGroups;
    private boolean readOnly;

    /**
     * @param entityType     the identity type for which this panel is displaying roles (EntityType.USER or EntityType.GROUP).
     * @param identityName   the display name of the identity.
     * @param assignedRoles  the Roles assigned to the identity.
     * @param identityGroups the groups that the identity is contained in (can be null).
     * @param readOnly       true if the panel should only display the roles, and not allow any changes.
     */
    public IdentityRoleAssignmentsPanel(@NotNull final EntityType entityType, @NotNull final String identityName, @NotNull List<Role> assignedRoles, @Nullable Set<IdentityHeader> identityGroups, final boolean readOnly) {
        if (entityType != EntityType.USER && entityType != EntityType.GROUP) {
            throw new IllegalArgumentException("Identity must be a user or group.");
        }
        this.entityType = entityType;
        this.identityName = identityName;
        this.assignedRoles = assignedRoles;
        this.identityGroups = identityGroups;
        this.readOnly = readOnly;
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        initTable();
        initButtons();
        handleSelectionChange();
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
                    final RoleSelectionDialog selectDialog = new RoleSelectionDialog(TopComponents.getInstance().getTopParent(), identityName, rolesModel.getRows());
                    selectDialog.pack();
                    Utilities.centerOnParentWindow(selectDialog);
                    DialogDisplayer.display(selectDialog, new Runnable() {
                        @Override
                        public void run() {
                            if (selectDialog.isConfirmed()) {
                                final List<Role> selectedRoles = selectDialog.getSelectedRoles();
                                if (!selectedRoles.isEmpty()) {
                                    for (final Role selectedRole : selectedRoles) {
                                        rolesModel.addRow(selectedRole);
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
                        final Map<Role, String> assignedRolesToRemove = new HashMap<>(selectedRoles.size());
                        for (final Role selectedRole : selectedRoles) {
                            final boolean currentlyAssigned = assignedRoles.contains(selectedRole);
                            final boolean roleInherited = isRoleInherited(selectedRole);
                            if (!roleInherited && currentlyAssigned) {
                                final int row = rolesModel.getRowIndex(selectedRole);
                                final Object nameVal = rolesModel.getValueAt(row, NAME_COL_INDEX);
                                final String name;
                                if (nameVal instanceof String) {
                                    name = (String) nameVal;
                                } else {
                                    name = NAME_UNAVAILABLE;
                                }
                                assignedRolesToRemove.put(selectedRole, name);
                            } else if (!roleInherited && !currentlyAssigned) {
                                // role was added then removed - don't need confirmation
                                rolesModel.removeRow(selectedRole);
                            }
                        }
                        if (!assignedRolesToRemove.isEmpty()) {
                            final IdentityRoleRemovalDialog confirmation = new IdentityRoleRemovalDialog(TopComponents.getInstance().getTopParent(), entityType, identityName, assignedRolesToRemove);
                            confirmation.pack();
                            Utilities.centerOnParentWindow(confirmation);
                            DialogDisplayer.display(confirmation, new Runnable() {
                                @Override
                                public void run() {
                                    if (confirmation.isConfirmed()) {
                                        for (final Role role : assignedRolesToRemove.keySet()) {
                                            rolesModel.removeRow(role);
                                        }
                                    }
                                }
                            });
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
                column("Inherited", 40, 80, 99999, new Functions.Unary<Boolean, Role>() {
                    @Override
                    public Boolean call(final Role role) {
                        if (identityGroups != null) {
                            for (final RoleAssignment assignment : role.getRoleAssignments()) {
                                if (EntityType.GROUP.getName().equals(assignment.getEntityType())) {
                                    for (final IdentityHeader userGroup : identityGroups) {
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
        rolesTable.getColumnModel().getColumn(INHERITED_COL_INDEX).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(final JTable table, Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
                if (value.equals(Boolean.FALSE)) {
                    value = "No";
                } else if (value.equals(Boolean.TRUE)) {
                    value = "Yes";
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });
        Utilities.setRowSorter(rolesTable, rolesModel);
        // use a copy of the assigned roles so that the original collection is not mutated
        rolesModel.setRows(new ArrayList<>(assignedRoles));
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
