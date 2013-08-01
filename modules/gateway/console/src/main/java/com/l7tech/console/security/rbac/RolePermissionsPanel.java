package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.FilterPanel;
import com.l7tech.console.util.EntityNameResolver;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.ScopePredicate;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * Panel which displays Permissions as PermissionGroups in a filterable, sortable table.
 */
public class RolePermissionsPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(RolePermissionsPanel.class.getName());
    private static final ImageIcon CHECK = ImageCache.getInstance().getIconAsIcon("com/l7tech/console/resources/check16.gif");
    private static final ImageIcon CROSS = ImageCache.getInstance().getIconAsIcon("com/l7tech/console/resources/RedCrossSign16.gif");
    private static final String ALL = "<ALL>";
    private static final String NEW = "New";
    private static final String TYPE = "Type";
    private static final String SCOPE = "Scope";
    private static final String CREATE = "C";
    private static final String READ = "R";
    private static final String UPDATE = "U";
    private static final String DELETE = "D";
    private static final String OTHER = "O";
    private JPanel contentPanel;
    private JTable permissionsTable;
    private FilterPanel filterPanel;
    private JLabel countLabel;
    private JButton removeButton;
    private JButton addButton;
    private Role role;
    private Set<Permission> permissions;
    private SimpleTableModel<PermissionGroup> permissionsModel;
    private boolean readOnly;
    private Map<String, Integer> columnIndices = new HashMap<>();

    public RolePermissionsPanel() {
        this(true);
    }

    /**
     * @param readOnly true if the user should not be allowed to edit the permissions.
     */
    public RolePermissionsPanel(final boolean readOnly) {
        this.readOnly = readOnly;
        initTable();
        initButtons();
        initFiltering();
    }

    /**
     * Configure the panel using the permissions set on the given Role.
     *
     * @param role the Role which contains the permissions to display.
     */
    public void configure(@Nullable final Role role) {
        this.role = role;
        this.permissions = role == null ? null : role.getPermissions();
        loadTable();
    }

    /**
     * Configure the panel using the given permissions.
     *
     * @param permissions the permissions to display.
     */
    public void configure(@Nullable final Set<Permission> permissions) {
        this.role = null;
        this.permissions = permissions;
        loadTable();
    }

    private void initButtons() {
        removeButton.setVisible(!readOnly);
        // remove not yet supported
        removeButton.setEnabled(false);
        addButton.setVisible(!readOnly);
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (role != null) {
                    final AddPermissionsWizard wizard = new AddPermissionsWizard(TopComponents.getInstance().getTopParent(), role);
                    wizard.pack();
                    DialogDisplayer.display(wizard, new Runnable() {
                        @Override
                        public void run() {
                            configure(role);
                        }
                    });
                }
            }
        });
    }

    private void initTable() {
        final List<TableUtil.Col<PermissionGroup>> columns = new ArrayList<>();
        if (!readOnly) {
            columns.add(column(NEW, 7, 7, 99999, new Functions.Unary<Boolean, PermissionGroup>() {
                @Override
                public Boolean call(final PermissionGroup permissionGroup) {
                    return hasNewPermission(permissionGroup);
                }
            }));
        }
        columns.add(column(TYPE, 30, 60, 99999, new Functions.Unary<String, PermissionGroup>() {
            @Override
            public String call(final PermissionGroup permissionGroup) {
                return permissionGroup.getEntityType() == EntityType.ANY ? ALL : permissionGroup.getEntityType().getPluralName();
            }
        }));
        columns.add(column(SCOPE, 30, 175, 99999, new Functions.Unary<String, PermissionGroup>() {
            @Override
            public String call(final PermissionGroup permissionGroup) {
                final StringBuilder sb = new StringBuilder();
                final Set<ScopePredicate> scope = permissionGroup.getScope();
                if (scope.isEmpty()) {
                    sb.append(ALL);
                } else {
                    final Iterator<ScopePredicate> iterator = scope.iterator();
                    final EntityNameResolver resolver = Registry.getDefault().getEntityNameResolver();
                    for (int i = 0; i < scope.size(); i++) {
                        try {
                            sb.append(resolver.getNameForEntity(iterator.next(), true));
                        } catch (final FindException e) {
                            logger.log(Level.WARNING, "Unable to get name for scope predicate: " +
                                    ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                            sb.append("name unavailable");
                        }
                        if (i < scope.size() - 1) {
                            sb.append(", ");
                        }
                    }
                }
                return sb.toString();
            }
        }));
        columns.add(column(CREATE, 7, 7, 99999, new Functions.Unary<Boolean, PermissionGroup>() {
            @Override
            public Boolean call(final PermissionGroup permissionGroup) {
                return permissionGroup.getOperations().contains(OperationType.CREATE);
            }
        }));
        columns.add(column(READ, 7, 7, 99999, new Functions.Unary<Boolean, PermissionGroup>() {
            @Override
            public Boolean call(final PermissionGroup permissionGroup) {
                return permissionGroup.getOperations().contains(OperationType.READ);
            }
        }));
        columns.add(column(UPDATE, 7, 7, 99999, new Functions.Unary<Boolean, PermissionGroup>() {
            @Override
            public Boolean call(final PermissionGroup permissionGroup) {
                return permissionGroup.getOperations().contains(OperationType.UPDATE);
            }
        }));
        columns.add(column(DELETE, 7, 7, 99999, new Functions.Unary<Boolean, PermissionGroup>() {
            @Override
            public Boolean call(final PermissionGroup permissionGroup) {
                return permissionGroup.getOperations().contains(OperationType.DELETE);
            }
        }));
        columns.add(column(OTHER, 7, 7, 99999, new Functions.Unary<Boolean, PermissionGroup>() {
            @Override
            public Boolean call(final PermissionGroup permissionGroup) {
                final Set<OperationType> operations = permissionGroup.getOperations();
                return operations.contains(OperationType.OTHER) || operations.contains(OperationType.NONE);
            }
        }));

        int index = 0;
        if (!readOnly) {
            columnIndices.put(NEW, index++);
        }
        columnIndices.put(TYPE, index++);
        columnIndices.put(SCOPE, index++);
        columnIndices.put(CREATE, index++);
        columnIndices.put(READ, index++);
        columnIndices.put(UPDATE, index++);
        columnIndices.put(DELETE, index++);
        columnIndices.put(OTHER, index++);

        permissionsModel = TableUtil.configureTable(permissionsTable, columns.toArray(new TableUtil.Col[columns.size()]));

        permissionsTable.getTableHeader().setDefaultRenderer(new HeaderCellRenderer(permissionsTable.getTableHeader().getDefaultRenderer()));
        permissionsTable.getColumnModel().getColumn(columnIndices.get(TYPE)).setCellRenderer(new HighlightedCellRenderer());
        permissionsTable.getColumnModel().getColumn(columnIndices.get(SCOPE)).setCellRenderer(new ScopeCellRenderer());
        if (!readOnly) {
            permissionsTable.getColumnModel().getColumn(columnIndices.get(NEW)).setCellRenderer(new CheckCellRenderer());
        }
        final CheckOrXCellRenderer checkXRenderer = new CheckOrXCellRenderer();
        permissionsTable.getColumnModel().getColumn(columnIndices.get(CREATE)).setCellRenderer(checkXRenderer);
        permissionsTable.getColumnModel().getColumn(columnIndices.get(READ)).setCellRenderer(checkXRenderer);
        permissionsTable.getColumnModel().getColumn(columnIndices.get(UPDATE)).setCellRenderer(checkXRenderer);
        permissionsTable.getColumnModel().getColumn(columnIndices.get(DELETE)).setCellRenderer(checkXRenderer);
        permissionsTable.getColumnModel().getColumn(columnIndices.get(OTHER)).setCellRenderer(checkXRenderer);
        Utilities.setRowSorter(permissionsTable, permissionsModel);
    }

    private void loadTable() {
        if (permissions != null) {
            permissionsModel.setRows(new ArrayList<>(PermissionGroup.groupPermissions(permissions)));
        } else {
            permissionsModel.setRows(Collections.<PermissionGroup>emptyList());
        }
    }

    private void initFiltering() {
        filterPanel.setFilterLabel("Filter on type");
        filterPanel.registerFilterCallback(new Runnable() {
            @Override
            public void run() {
                loadCount();
            }
        });
        filterPanel.attachRowSorter((TableRowSorter) (permissionsTable.getRowSorter()), new int[]{columnIndices.get(TYPE)});
        loadCount();
    }

    private void loadCount() {
        final int visible = permissionsTable.getRowCount();
        final int total = permissionsModel.getRowCount();
        countLabel.setText("showing " + visible + " of " + total + " items");
    }

    private boolean hasNewPermission(@NotNull final PermissionGroup group) {
        boolean hasNewPermission = false;
        for (final Permission permission : group.getPermissions()) {
            if (permission.getOid() == Permission.DEFAULT_OID) {
                hasNewPermission = true;
                break;
            }
        }
        return hasNewPermission;
    }

    /**
     * Displays a check or X icon instead of the boolean value.
     */
    private class CheckOrXCellRenderer extends HighlightedCellRenderer {
        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof Boolean && component instanceof JLabel) {
                final Boolean booleanValue = (Boolean) value;
                final JLabel label = (JLabel) component;
                label.setIcon(booleanValue ? CHECK : CROSS);
                label.setText(StringUtils.EMPTY);
                if (booleanValue && column == columnIndices.get(OTHER)) {
                    int modelIndex = permissionsTable.convertRowIndexToModel(row);
                    if (modelIndex > 0) {
                        final PermissionGroup permissionGroup = permissionsModel.getRowObject(modelIndex);
                        final Set<String> otherOps = new HashSet<>();
                        for (final Permission permission : permissionGroup.getPermissions()) {
                            if (permission.getOperation() == OperationType.OTHER && StringUtils.isNotBlank(permission.getOtherOperationName())) {
                                otherOps.add(permission.getOtherOperationName());
                            } else if (permission.getOperation() == OperationType.NONE) {
                                otherOps.add(OperationType.NONE.getName());
                            }
                        }
                        label.setToolTipText(StringUtils.join(otherOps, ","));
                    }
                }
            }
            return component;
        }
    }

    /**
     * Displays a check or no icon instead of the boolean value.
     */
    private class CheckCellRenderer extends HighlightedCellRenderer {
        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof Boolean && component instanceof JLabel) {
                final Boolean booleanValue = (Boolean) value;
                final JLabel label = (JLabel) component;
                label.setText(StringUtils.EMPTY);
                label.setIcon(booleanValue ? CHECK : null);
            }
            return component;
        }
    }

    private class ScopeCellRenderer extends HighlightedCellRenderer {
        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
            final Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (component instanceof JLabel && value instanceof String) {
                final JLabel label = (JLabel) component;
                final String scopeStr = (String) value;
                final String[] split = StringUtils.split(scopeStr, ",");
                final String join = StringUtils.join(split, ",<br/>");
                label.setToolTipText("<html>" + join + "</html>");
            }
            return component;
        }
    }

    /**
     * If the panel is not read-only, highlights rows which contain at least one new permission.
     */
    private class HighlightedCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
            final Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                component.setForeground(table.getForeground());
                if (!readOnly) {
                    if (row >= 0) {
                        final int modelIndex = permissionsTable.convertRowIndexToModel(row);
                        if (modelIndex >= 0) {
                            final PermissionGroup group = permissionsModel.getRowObject(modelIndex);
                            boolean hasNewPermission = hasNewPermission(group);
                            if (hasNewPermission) {
                                component.setBackground(Color.YELLOW);
                            } else {
                                component.setBackground(table.getBackground());
                            }
                        }
                    }
                } else {
                    // table is just for viewing, don't highlight
                    component.setBackground(table.getBackground());
                }
            } else {
                component.setBackground(table.getSelectionBackground());
                component.setForeground(table.getSelectionForeground());
            }
            return component;
        }
    }

    /**
     * Header renderer could be different based on operating system.
     */
    private class HeaderCellRenderer extends DefaultTableCellRenderer {
        private TableCellRenderer delegate;

        private HeaderCellRenderer(@Nullable final TableCellRenderer delegate) {
            this.delegate = delegate != null ? delegate : new DefaultTableCellRenderer();
        }

        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
            final Component component = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (component instanceof JLabel && value instanceof String) {
                final JLabel label = (JLabel) component;
                final String strVal = (String) value;
                String tooltip = null;
                if (CREATE.equals(strVal)) {
                    tooltip = OperationType.CREATE.getName();
                } else if (READ.equals(strVal)) {
                    tooltip = OperationType.READ.getName();
                } else if (UPDATE.equals(strVal)) {
                    tooltip = OperationType.UPDATE.getName();
                } else if (DELETE.equals(strVal)) {
                    tooltip = OperationType.DELETE.getName();
                } else if (OTHER.equals(strVal)) {
                    tooltip = OperationType.OTHER.getName();
                } else {
                    // no tooltip
                }
                if (tooltip != null) {
                    label.setToolTipText(tooltip);
                }
            }
            return component;
        }
    }
}
