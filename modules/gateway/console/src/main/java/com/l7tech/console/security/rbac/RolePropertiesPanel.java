package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.FilterPanel;
import com.l7tech.console.util.EntityNameResolver;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

public class RolePropertiesPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(RolePropertiesPanel.class.getName());
    private static final String CUSTOM = "Custom";
    private static final String SYSTEM = "System";
    private static final ImageIcon CHECK = ImageCache.getInstance().getIconAsIcon("com/l7tech/console/resources/check16.gif");
    private static final ImageIcon CROSS = ImageCache.getInstance().getIconAsIcon("com/l7tech/console/resources/RedCrossSign16.gif");
    private static final int TYPE_COL_INDEX = 0;
    private static final int SCOPE_COL_INDEX = 1;
    private static final int CREATE_COL_INDEX = 2;
    private static final int READ_COL_INDEX = 3;
    private static final int UPDATE_COL_INDEX = 4;
    private static final int DELETE_COL_INDEX = 5;
    private static final int OTHER_COL_INDEX = 6;
    private static final String ALL = "<ALL>";
    private JPanel contentPanel;
    private JTextField roleTextField;
    private JTextPane descriptionTextPane;
    private JTextField typeTextField;
    private JTable permissionsTable;
    private FilterPanel filterPanel;
    private JLabel countLabel;
    private Role role;
    private String roleName;
    private SimpleTableModel<PermissionGroup> permissionsModel;

    public RolePropertiesPanel() {
        descriptionTextPane.setContentType("text/html");
        initTable();
        initFiltering();
        loadCount();
    }

    public void configure(@Nullable final Role role, @Nullable final String roleName) {
        this.role = role;
        this.roleName = roleName;
        loadTextFields();
        loadTable();
    }

    private void loadTextFields() {
        if (role != null) {
            roleTextField.setText(roleName);
            typeTextField.setText(role.isUserCreated() ? CUSTOM : SYSTEM);
            descriptionTextPane.setText(RbacUtilities.getDescriptionString(role, true));
        } else {
            roleTextField.setText(StringUtils.EMPTY);
            typeTextField.setText(StringUtils.EMPTY);
            descriptionTextPane.setText(StringUtils.EMPTY);
        }
    }

    private void initTable() {
        permissionsModel = TableUtil.configureTable(permissionsTable,
                column("Type", 30, 60, 99999, new Functions.Unary<String, PermissionGroup>() {
                    @Override
                    public String call(final PermissionGroup permissionGroup) {
                        return permissionGroup.getEntityType() == EntityType.ANY ? ALL : permissionGroup.getEntityType().getPluralName();
                    }
                }),
                column("Scope", 30, 175, 99999, new Functions.Unary<String, PermissionGroup>() {
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
                }),
                column("C", 7, 7, 99999, new Functions.Unary<Boolean, PermissionGroup>() {
                    @Override
                    public Boolean call(final PermissionGroup permissionGroup) {
                        return permissionGroup.getOperations().contains(OperationType.CREATE);
                    }
                }),
                column("R", 7, 7, 99999, new Functions.Unary<Boolean, PermissionGroup>() {
                    @Override
                    public Boolean call(final PermissionGroup permissionGroup) {
                        return permissionGroup.getOperations().contains(OperationType.READ);
                    }
                }),
                column("U", 7, 7, 99999, new Functions.Unary<Boolean, PermissionGroup>() {
                    @Override
                    public Boolean call(final PermissionGroup permissionGroup) {
                        return permissionGroup.getOperations().contains(OperationType.UPDATE);
                    }
                }),
                column("D", 7, 7, 99999, new Functions.Unary<Boolean, PermissionGroup>() {
                    @Override
                    public Boolean call(final PermissionGroup permissionGroup) {
                        return permissionGroup.getOperations().contains(OperationType.DELETE);
                    }
                }),
                column("O", 7, 7, 99999, new Functions.Unary<Boolean, PermissionGroup>() {
                    @Override
                    public Boolean call(final PermissionGroup permissionGroup) {
                        final Set<OperationType> operations = permissionGroup.getOperations();
                        return operations.contains(OperationType.OTHER) || operations.contains(OperationType.NONE);
                    }
                }));

        final CheckCellRenderer checkCellRenderer = new CheckCellRenderer();
        permissionsTable.getTableHeader().setDefaultRenderer(new HeaderCellRenderer(permissionsTable.getTableHeader().getDefaultRenderer()));
        permissionsTable.getColumnModel().getColumn(CREATE_COL_INDEX).setCellRenderer(checkCellRenderer);
        permissionsTable.getColumnModel().getColumn(READ_COL_INDEX).setCellRenderer(checkCellRenderer);
        permissionsTable.getColumnModel().getColumn(UPDATE_COL_INDEX).setCellRenderer(checkCellRenderer);
        permissionsTable.getColumnModel().getColumn(DELETE_COL_INDEX).setCellRenderer(checkCellRenderer);
        permissionsTable.getColumnModel().getColumn(OTHER_COL_INDEX).setCellRenderer(checkCellRenderer);
        Utilities.setRowSorter(permissionsTable, permissionsModel);
    }

    private void loadTable() {
        if (role != null) {
            permissionsModel.setRows(new ArrayList<>(PermissionGroup.groupPermissions(role.getPermissions())));
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
        filterPanel.attachRowSorter((TableRowSorter) (permissionsTable.getRowSorter()), new int[]{TYPE_COL_INDEX});
        loadCount();
    }

    private void loadCount() {
        final int visible = permissionsTable.getRowCount();
        final int total = permissionsModel.getRowCount();
        countLabel.setText("showing " + visible + " of " + total + " items");
    }

    private class CheckCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof Boolean) {
                final Boolean booleanValue = (Boolean) value;
                final JLabel label = new JLabel();
                label.setIcon(booleanValue ? CHECK : CROSS);
                label.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                label.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                label.setOpaque(isSelected);
                if (booleanValue && column == OTHER_COL_INDEX) {
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
                component = label;
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
            if (component instanceof JLabel) {
                final JLabel label = (JLabel) component;
                String tooltip = null;
                switch (column) {
                    case CREATE_COL_INDEX:
                        tooltip = OperationType.CREATE.getName();
                        break;
                    case READ_COL_INDEX:
                        tooltip = OperationType.READ.getName();
                        break;
                    case UPDATE_COL_INDEX:
                        tooltip = OperationType.UPDATE.getName();
                        break;
                    case DELETE_COL_INDEX:
                        tooltip = OperationType.DELETE.getName();
                        break;
                    case OTHER_COL_INDEX:
                        tooltip = "Other";
                        break;
                    default:
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
