package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.FilterPanel;
import com.l7tech.console.panels.GroupPanel;
import com.l7tech.console.panels.UserPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleAssignment;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

public class RoleAssignmentsPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(RoleAssignmentsPanel.class.getName());
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
        Utilities.setRowSorter(assignmentsTable, assignmentsTableModel);
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
            assignmentsTableModel.setRows(new ArrayList<RoleAssignment>(this.role.getRoleAssignments()));
        } else {
            assignmentsTableModel.setRows(Collections.<RoleAssignment>emptyList());
        }
    }
}
