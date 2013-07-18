package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.FilterPanel;
import com.l7tech.console.panels.GroupPanel;
import com.l7tech.console.panels.UserPanel;
import com.l7tech.console.panels.identity.finder.FindIdentitiesDialog;
import com.l7tech.console.panels.identity.finder.Options;
import com.l7tech.console.panels.identity.finder.SearchType;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleAssignment;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.*;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

public class RoleAssignmentsPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(RoleAssignmentsPanel.class.getName());
    private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("com.l7tech.console.resources.RbacGui");
    private static final int NAME_COL_INDEX = 0;
    private static final int TYPE_COL_INDEX = 1;
    private Icon GROUP_ICON = ImageCache.getInstance().getIconAsIcon(GroupPanel.GROUP_ICON_RESOURCE);
    private Icon USER_ICON = ImageCache.getInstance().getIconAsIcon(UserPanel.USER_ICON_RESOURCE);
    private static final String UNKNOWN = "unknown";
    private static final String UNAVAILABLE = "unavailable";
    private JPanel assignmentsContentPanel;
    private JTextField assignmentsRoleTextField;
    private JTable assignmentsTable;
    private FilterPanel assignmentsFilterPanel;
    private JLabel assignmentsFilterLabel;
    private JButton addButton;
    private JButton removeButton;
    private Role role;
    private SimpleTableModel<RoleAssignment> assignmentsTableModel;
    private Map<Long, IdentityProviderConfig> knownProviders = new HashMap<>();

    public RoleAssignmentsPanel() {
        initTable();
        initFiltering();
        initButtons();
    }

    public void configure(@Nullable final Role role) {
        this.role = role;
        if (this.role != null) {
            String name = "name unavailable";
            try {
                name = Registry.getDefault().getEntityNameResolver().getNameForEntity(role, true);
            } catch (final FindException e) {
                logger.log(Level.WARNING, "Unable to retrieve name for role: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
            assignmentsRoleTextField.setText(name);
        }
        loadTable();
        loadCount();
    }

    private void initTable() {
        assignmentsTableModel = TableUtil.configureTable(assignmentsTable,
                column("Name", 80, 300, 99999, new Functions.Unary<String, RoleAssignment>() {
                    @Override
                    public String call(final RoleAssignment assignment) {
                        String name = UNKNOWN;
                        final IdentityAdmin identityAdmin = Registry.getDefault().getIdentityAdmin();
                        try {
                            if (EntityType.USER.getName().equalsIgnoreCase(assignment.getEntityType())) {
                                final User found = identityAdmin.findUserByID(assignment.getProviderId(), assignment.getIdentityId());
                                if (found != null) {
                                    name = found.getLogin();
                                }
                            } else if (EntityType.GROUP.getName().equalsIgnoreCase(assignment.getEntityType())) {
                                final Group found = identityAdmin.findGroupByID(assignment.getProviderId(), assignment.getIdentityId());
                                if (found != null) {
                                    name = found.getName();
                                }
                            } else {
                                logger.log(Level.WARNING, "Expected group or user but received: " + assignment.getEntityType());
                            }
                        } catch (final FindException e) {
                            logger.log(Level.WARNING, "Error retrieving identity: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                            name = UNAVAILABLE;
                        }
                        return name;
                    }
                }),
                column("Type", 80, 300, 99999, new Functions.Unary<String, RoleAssignment>() {
                    @Override
                    public String call(final RoleAssignment assignment) {
                        return assignment.getEntityType();
                    }
                }),
                column("Provider", 80, 300, 99999, new Functions.Unary<String, RoleAssignment>() {
                    @Override
                    public String call(final RoleAssignment assignment) {
                        String providerName = UNKNOWN;
                        final long providerId = assignment.getProviderId();
                        final IdentityProviderConfig knownProvider = knownProviders.get(providerId);
                        if (knownProvider != null) {
                            providerName = knownProvider.getName();
                        } else {
                            try {
                                final IdentityProviderConfig found = Registry.getDefault().getIdentityAdmin().findIdentityProviderConfigByID(providerId);
                                if (found != null) {
                                    providerName = found.getName();
                                    knownProviders.put(providerId, found);
                                }
                            } catch (final FindException e) {
                                logger.log(Level.WARNING, "Error retrieving provider: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                                providerName = UNAVAILABLE;
                            }
                        }
                        return providerName;
                    }
                }));
        assignmentsTable.setModel(assignmentsTableModel);
        assignmentsTable.getColumnModel().getColumn(NAME_COL_INDEX).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof String) {
                    final String name = (String) value;
                    final Object type = assignmentsTable.getValueAt(row, TYPE_COL_INDEX);
                    final JLabel label = new JLabel(name);
                    if (type instanceof String && type.equals(EntityType.USER.getName())) {
                        label.setIcon(USER_ICON);
                    } else if (type instanceof String && type.equals(EntityType.GROUP.getName())) {
                        label.setIcon(GROUP_ICON);
                    }
                    if (isSelected) {
                        label.setBackground(table.getSelectionBackground());
                        label.setForeground(table.getSelectionForeground());
                        label.setOpaque(true);
                    } else {
                        label.setBackground(table.getBackground());
                        label.setForeground(table.getForeground());
                        label.setOpaque(false);
                    }
                    return label;
                } else {
                    return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                }
            }
        });
        assignmentsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        assignmentsTable.getSelectionModel().addListSelectionListener(new RunOnChangeListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                removeButton.setEnabled(assignmentsTable.getSelectedRowCount() > 0);
            }
        });
        Utilities.setRowSorter(assignmentsTable, assignmentsTableModel);
    }

    private void initButtons() {
        addButton.addActionListener(new AddAssignmentActionListener());
        removeButton.addActionListener(new RemoveAssignmentActionListener());
        removeButton.setEnabled(false);
    }

    private void initFiltering() {
        assignmentsFilterPanel.registerFilterCallback(new Runnable() {
            @Override
            public void run() {
                loadCount();
            }
        });
        assignmentsFilterPanel.attachRowSorter((TableRowSorter) (assignmentsTable.getRowSorter()), new int[]{0});
        loadCount();
    }

    private void loadCount() {
        final int visible = assignmentsTable.getRowCount();
        final int total = assignmentsTableModel.getRowCount();
        assignmentsFilterLabel.setText("showing " + visible + " of " + total + " items");
    }

    private void loadTable() {
        if (role != null) {
            assignmentsTableModel.setRows(new ArrayList<>(this.role.getRoleAssignments()));
        } else {
            assignmentsTableModel.setRows(Collections.<RoleAssignment>emptyList());
        }
    }

    private Collection<RoleAssignment> getSelectedAssignments() {
        final Set<RoleAssignment> assignments = new HashSet<>();
        int[] rows = assignmentsTable.getSelectedRows();
        for (int i = 0; i < rows.length; i++) {
            final int rowIndex = rows[i];
            if (rowIndex >= 0) {
                final int modelRow = assignmentsTable.convertRowIndexToModel(rowIndex);
                if (modelRow >= 0) {
                    final RoleAssignment assignment = assignmentsTableModel.getRowObject(modelRow);
                    if (assignment != null) {
                        assignments.add(assignment);
                    }
                }
            }
        }
        return assignments;
    }

    private void saveAndReloadRole() {
        final RbacAdmin rbacAdmin = Registry.getDefault().getRbacAdmin();
        try {
            final long oid = rbacAdmin.saveRole(role);
            final Role reloadedRole = rbacAdmin.findRoleByPrimaryKey(oid);
            if (reloadedRole != null) {
                // version has changed
                configure(reloadedRole);
            } else {
                throw new FindException("Unable to retrieve saved role");
            }
        } catch (final SaveException ex) {
            logger.log(Level.WARNING, "Unable to save role: " + ExceptionUtils.getMessage(ex), ExceptionUtils.getDebugException(ex));
            DialogDisplayer.showMessageDialog(this, "Error updating assignments.", "Error", JOptionPane.ERROR_MESSAGE, null);
        } catch (final FindException ex) {
            logger.log(Level.WARNING, "Unable to refresh role: " + ExceptionUtils.getMessage(ex), ExceptionUtils.getDebugException(ex));
            DialogDisplayer.showMessageDialog(this, "Error refreshing role.", "Error", JOptionPane.ERROR_MESSAGE, null);
        }
    }

    private class RemoveAssignmentActionListener implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent e) {
            if (role != null) {
                final Collection<RoleAssignment> selectedAssignments = getSelectedAssignments();
                if (!selectedAssignments.isEmpty()) {
                    Utilities.doWithConfirmation(
                            RoleAssignmentsPanel.this,
                            RESOURCES.getString("manageRoles.removeAssignmentTitle"), RESOURCES.getString("manageRoles.removeAssignmentMessage"), new Runnable() {
                        @Override
                        public void run() {
                            for (final RoleAssignment assignment : selectedAssignments) {
                                if (EntityType.USER.getName().equalsIgnoreCase(assignment.getEntityType())) {
                                    final UserBean user = new UserBean();
                                    user.setProviderId(assignment.getProviderId());
                                    user.setUniqueIdentifier(assignment.getIdentityId());
                                    role.removeAssignedUser(user);
                                } else if (EntityType.GROUP.getName().equalsIgnoreCase(assignment.getEntityType())) {
                                    final GroupBean group = new GroupBean();
                                    group.setProviderId(assignment.getProviderId());
                                    group.setUniqueIdentifier(assignment.getIdentityId());
                                    role.removeAssignedGroup(group);
                                } else {
                                    logger.log(Level.WARNING, "Expected group or user but received: " + assignment.getEntityType());
                                }
                            }
                            if (!selectedAssignments.isEmpty()) {
                                saveAndReloadRole();
                            }
                        }
                    });
                }
            }
        }
    }

    private class AddAssignmentActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (role != null) {
                final FindIdentitiesDialog searchDialog = new FindIdentitiesDialog(TopComponents.getInstance().getTopParent(), true, createDefaultSearchOptions());
                searchDialog.pack();
                Utilities.centerOnScreen(searchDialog);
                final FindIdentitiesDialog.FindResult result = searchDialog.showDialog();
                if (result != null && result.entityHeaders != null) {
                    final IdentityAdmin identityAdmin = Registry.getDefault().getIdentityAdmin();
                    try {
                        long providerId = result.providerConfigOid;
                        for (final EntityHeader header : result.entityHeaders) {
                            switch (header.getType()) {
                                case USER:
                                    final User user = identityAdmin.findUserByID(providerId, header.getStrId());
                                    if (user != null) {
                                        role.addAssignedUser(user);
                                    }
                                    break;
                                case GROUP:
                                    final Group group = identityAdmin.findGroupByID(providerId, header.getStrId());
                                    if (group != null) {
                                        role.addAssignedGroup(group);
                                    }
                                    break;
                                default:
                                    logger.log(Level.WARNING, "Expected user or group but received: " + header.getType());
                                    continue;
                            }
                        }
                    } catch (final FindException ex) {
                        logger.log(Level.WARNING, "Unable to assign users/groups to role: " + ExceptionUtils.getMessage(ex), ExceptionUtils.getDebugException(ex));
                        DialogDisplayer.showMessageDialog(RoleAssignmentsPanel.this, "Error adding assignments.", "Error", JOptionPane.ERROR_MESSAGE, null);
                    }
                    if (result.entityHeaders.length > 0) {
                        saveAndReloadRole();
                    }
                }
            }
        }

        private Options createDefaultSearchOptions() {
            Options opts = new Options();
            opts.setInitialProvider(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
            opts.setSearchType(SearchType.ALL);
            opts.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            opts.setDisposeOnSelect(true);
            opts.setDisableOpenProperties(true);
            opts.setAdminOnly(true);
            return opts;
        }
    }
}
