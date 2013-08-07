package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.FilterPanel;
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
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

public class RoleAssignmentsPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(RoleAssignmentsPanel.class.getName());
    private static final int NAME_COL_INDEX = 0;
    private static final int TYPE_COL_INDEX = 1;
    private static final int PROVIDER_COL_INDEX = 2;
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
    private Map<RoleAssignment, String> knownAssignmentNames = new HashMap<>();

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
        } else {
            assignmentsRoleTextField.setText(StringUtils.EMPTY);
        }
        reloadAssignments();
    }

    private void reloadAssignments() {
        knownAssignmentNames.clear();
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
                            if (!name.isEmpty()) {
                                knownAssignmentNames.put(assignment, name);
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
        assignmentsTable.getColumnModel().getColumn(NAME_COL_INDEX).setCellRenderer(new RoleAssignmentNameCellRenderer(assignmentsTable, TYPE_COL_INDEX));
        assignmentsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        final EnableDisableListener enableDisableListener = new EnableDisableListener();
        assignmentsTable.getSelectionModel().addListSelectionListener(enableDisableListener);
        assignmentsTableModel.addTableModelListener(enableDisableListener);
        Utilities.setRowSorter(assignmentsTable, assignmentsTableModel);
    }

    private void initButtons() {
        addButton.addActionListener(new AddAssignmentActionListener());
        addButton.setEnabled(false);
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
                role.setVersion(reloadedRole.getVersion());
                reloadAssignments();
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

    private class EnableDisableListener extends RunOnChangeListener {
        private EnableDisableListener() {
            super(new Runnable() {
                @Override
                public void run() {
                    addButton.setEnabled(role != null);
                    removeButton.setEnabled(role != null && assignmentsTable.getSelectedRowCount() > 0);
                    assignmentsRoleTextField.setEnabled(role != null);
                    assignmentsTable.setEnabled(role != null);
                    assignmentsFilterPanel.allowFiltering(role != null && !role.getRoleAssignments().isEmpty());
                }
            });
        }
    }

    private class RemoveAssignmentActionListener implements ActionListener {
        @Override
        public void actionPerformed(final ActionEvent e) {
            if (role != null) {
                final Collection<RoleAssignment> selectedAssignments = getSelectedAssignments();
                if (!selectedAssignments.isEmpty()) {
                    final Map<RoleAssignment, String[]> toRemove = new HashMap<>();
                    for (final RoleAssignment selectedAssignment : selectedAssignments) {
                        final int index = assignmentsTableModel.getRowIndex(selectedAssignment);
                        final Object nameVal = assignmentsTableModel.getValueAt(index, NAME_COL_INDEX);
                        final String name = nameVal instanceof String ? (String) nameVal : null;
                        final Object providerVal = assignmentsTableModel.getValueAt(index, PROVIDER_COL_INDEX);
                        final String provider = providerVal instanceof String ? (String) providerVal : null;
                        if (name != null && provider != null) {
                            toRemove.put(selectedAssignment, new String[]{name, provider});
                        }
                    }
                    final RoleAssignmentRemovalDialog removalDialog = new RoleAssignmentRemovalDialog(TopComponents.getInstance().getTopParent(),
                            assignmentsRoleTextField.getText(), toRemove);
                    removalDialog.pack();
                    Utilities.centerOnParentWindow(removalDialog);
                    DialogDisplayer.display(removalDialog, new Runnable() {
                        @Override
                        public void run() {
                            if (removalDialog.isConfirmed()) {
                                for (final RoleAssignment assignment : selectedAssignments) {
                                    role.getRoleAssignments().remove(assignment);
                                }
                                saveAndReloadRole();
                            } else {
                                logger.log(Level.FINEST, "Assignment removal cancelled.");
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
