package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.OkCancelPanel;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gui.SelectableTableModel;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * Dialog which allows selection of roles for which a user/group should be assigned.
 */
public class RoleSelectionDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(RoleSelectionDialog.class.getName());
    private static final String NAME = "Name";
    private static final String TYPE = "Type";
    private static final String DESCRIPTION = "Description";
    private static final int CHECK_COL_INDEX = 0;
    private static final int NAME_COL_INDEX = 1;
    private static final int MAX_WIDTH = 99999;
    private static final int CHECK_BOX_WIDTH = 30;
    private static final String ROLES = "roles";
    private static final String ADD = "Add";
    private static final String SELECT = "Select";
    private static final String UNAVAILABLE = "unavailable";
    private JPanel contentPanel;
    private OkCancelPanel okCancelPanel;
    private SelectableFilterableTablePanel tablePanel;
    private boolean confirmed;
    private SelectableTableModel<Role> rolesModel;
    private Collection<Role> rolesToFilter;
    private boolean allowMultiSelect;

    public RoleSelectionDialog(@NotNull final Window owner, @NotNull final String title, @NotNull Collection<Role> rolesToFilter) {
        this(owner, title, rolesToFilter, true);
    }

    public RoleSelectionDialog(@NotNull final Window owner, @NotNull final String title, @NotNull Collection<Role> rolesToFilter, final boolean allowMultiSelect) {
        super(owner, title, DEFAULT_MODALITY_TYPE);
        this.rolesToFilter = rolesToFilter;
        this.allowMultiSelect = allowMultiSelect;
        setContentPane(contentPanel);
        initButtons();
        initTable();
        enableDisableOk();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * @return a list of selected roles or a list with a single role if multi-select is not supported.
     *         These roles will not have permissions or attached entities.
     */
    public java.util.List<Role> getSelectedRoles() {
        if (isConfirmed()) {
            return rolesModel.getSelected();
        }
        throw new IllegalStateException("Dialog is not yet confirmed");
    }

    private void initButtons() {
        if (allowMultiSelect) {
            okCancelPanel.setOkButtonText(ADD);
        } else {
            okCancelPanel.setOkButtonText(SELECT);
        }
        okCancelPanel.getOkButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                confirmed = true;
                dispose();
            }
        });
        okCancelPanel.getCancelButton().addActionListener(Utilities.createDisposeAction(this));
        getRootPane().setDefaultButton(okCancelPanel.getOkButton());
        Utilities.setEscAction(this, okCancelPanel.getCancelButton());
    }

    private void initTable() {
        rolesModel = TableUtil.configureSelectableTable(tablePanel.getSelectableTable(), allowMultiSelect, CHECK_COL_INDEX,
                column(StringUtils.EMPTY, CHECK_BOX_WIDTH, CHECK_BOX_WIDTH, MAX_WIDTH, new Functions.Unary<Boolean, Role>() {
                    @Override
                    public Boolean call(final Role role) {
                        return rolesModel.isSelected(role);
                    }
                }),
                column(NAME, 30, 200, MAX_WIDTH, new Functions.Unary<String, Role>() {
                    @Override
                    public String call(final Role role) {
                        String name = UNAVAILABLE;
                        try {
                            name = Registry.getDefault().getEntityNameResolver().getNameForEntity(role, true);
                        } catch (final FindException e) {
                            logger.log(Level.WARNING, "Unable to resolve name for role: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        }
                        return name;
                    }
                }),
                column(TYPE, 30, 200, MAX_WIDTH, new Functions.Unary<String, Role>() {
                    @Override
                    public String call(final Role role) {
                        return role.isUserCreated() ? BasicRolePropertiesPanel.CUSTOM : BasicRolePropertiesPanel.SYSTEM;
                    }
                }),
                column(DESCRIPTION, 30, 400, MAX_WIDTH, new Functions.Unary<String, Role>() {
                    @Override
                    public String call(final Role role) {
                        return RbacUtilities.getDescriptionString(role, false);
                    }
                }));
        try {
            final ArrayList<Role> rows = new ArrayList<>();
            final Collection<EntityHeader> headers = Registry.getDefault().getRbacAdmin().findAllRoleHeaders();
            final SecurityProvider securityProvider = Registry.getDefault().getSecurityProvider();
            for (final EntityHeader header : headers) {
                if (header instanceof RoleEntityHeader) {
                    final Role role = RbacUtilities.fromEntityHeader((RoleEntityHeader) header);
                    if (!rolesToFilter.contains(role) && securityProvider.hasPermission(new AttemptedUpdate(EntityType.RBAC_ROLE, role))) {
                        rows.add(role);
                    }
                }
            }
            rolesModel.setRows(rows);
        } catch (final FindException e) {
            logger.log(Level.WARNING, "Unable to retrieve roles: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            rolesModel.setRows(Collections.<Role>emptyList());
        }
        tablePanel.configure(rolesModel, new int[]{NAME_COL_INDEX}, ROLES);
        tablePanel.getSelectableTable().getColumnModel().getColumn(NAME_COL_INDEX).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (component instanceof JLabel) {
                    final RoleLabel roleLabel = new RoleLabel(value.toString(), rolesModel.getRowObject(table.convertRowIndexToModel(row)));
                    roleLabel.setOpaque(true);
                    if (isSelected) {
                        roleLabel.setBackground(table.getSelectionBackground());
                        roleLabel.setForeground(table.getSelectionForeground());
                    } else {
                        roleLabel.setBackground(table.getBackground());
                        roleLabel.setForeground(table.getForeground());
                    }
                    component = roleLabel;
                }
                return component;
            }
        });
        if (!allowMultiSelect) {
            tablePanel.getSelectableTable().getColumnModel().getColumn(CHECK_COL_INDEX).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
                    final JRadioButton radio = new JRadioButton();
                    radio.setSelected(isSelected);
                    radio.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                    radio.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                    return radio;
                }
            });
            rolesModel.addTableModelListener(new RunOnChangeListener(new Runnable() {
                @Override
                public void run() {
                    enableDisableOk();
                }
            }));
        }
    }

    private void enableDisableOk() {
        if (!allowMultiSelect) {
            okCancelPanel.getOkButton().setEnabled(rolesModel.getSelected().iterator().hasNext());
        }
    }

    /**
     * Custom JLabel for a Role which fetches the tool tip as needed.
     */
    private class RoleLabel extends JLabel {
        private final Role role;

        public RoleLabel(@NotNull final String label, @NotNull final Role role) {
            super(label);
            this.role = role;
        }

        @Override
        public String getToolTipText() {
            String toolTip = getText();
            try {
                final Role found = Registry.getDefault().getRbacAdmin().findRoleByPrimaryKey(role.getGoid());
                if (found != null) {
                    toolTip = Registry.getDefault().getEntityNameResolver().getNameForEntity(found, true);
                }
            } catch (final FindException | PermissionDeniedException e) {
                logger.log(Level.WARNING, "Unable to retrieve role: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
            return toolTip;
        }
    }
}
