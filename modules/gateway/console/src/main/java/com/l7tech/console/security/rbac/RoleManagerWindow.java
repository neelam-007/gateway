package com.l7tech.console.security.rbac;

import com.l7tech.console.action.Actions;
import com.l7tech.console.panels.FilterPanel;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.*;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.comparator.NamedEntityComparator;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.TextUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * CRUD dialog for Roles.
 */
public class RoleManagerWindow extends JDialog {
    private static final Logger logger = Logger.getLogger(RoleManagerWindow.class.getName());
    private static final ResourceBundle RESOURCES = ResourceBundle.getBundle(RoleManagerWindow.class.getName());
    private static final String DELETE_CONFIRMATION = "delete.confirmation";
    private static final String DELETE_CONFIRMATION_NAME_MAX_CHARS = "delete.confirmation.name.max.chars";
    private static final String CUSTOM = "Custom";
    private static final String SYSTEM = "System";
    private JPanel contentPanel;
    private JTable rolesTable;
    private JButton closeButton;
    private JButton helpButton;
    private FilterPanel filterPanel;
    private JLabel filterLabel;
    private JButton createButton;
    private JTabbedPane tabbedPanel;
    private JPanel assignmentsTab;
    private JPanel propertiesTab;
    private JButton editButton;
    private RoleAssignmentsPanel assignmentsPanel;
    private JButton removeButton;
    private RolePropertiesPanel propertiesPanel;
    private SimpleTableModel<Role> rolesTableModel;
    private EntityCrudController<Role> crudController;

    public RoleManagerWindow(@NotNull final Window owner) {
        super(owner, "Manage Roles", DEFAULT_MODALITY_TYPE);
        setContentPane(contentPanel);
        initTable();
        initFiltering();
        initCrudController();
        initButtons();
        handleTableChange();
    }

    private void initCrudController() {
        crudController = new EntityCrudController<>();
        crudController.setEntityTable(rolesTable);
        crudController.setEntityTableModel(rolesTableModel);
        crudController.setEntityCreator(new EntityCreator<Role>() {
            @Override
            public Role createNewEntity() {
                final Role role = new Role();
                role.setUserCreated(true);
                return role;
            }
        });
        crudController.setEntityDeleter(new EntityDeleter<Role>() {
            @Override
            public void deleteEntity(@NotNull final Role entity) throws DeleteException {
                Registry.getDefault().getRbacAdmin().deleteRole(entity);
            }

            @Override
            public void displayDeleteDialog(@NotNull final Role role, @NotNull final Functions.UnaryVoid<Role> afterDeleteListener) {
                final Integer nameMaxChars = Integer.valueOf(RESOURCES.getString(DELETE_CONFIRMATION_NAME_MAX_CHARS));
                final String displayName = TextUtils.truncateStringAtEnd(role.getName(), nameMaxChars);
                final String confirmation = MessageFormat.format(RESOURCES.getString(DELETE_CONFIRMATION), displayName, role.getRoleAssignments().size());
                DialogDisplayer.showOptionDialog(
                        RoleManagerWindow.this,
                        confirmation,
                        "Remove Role",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        new Object[]{"Remove Role", "Cancel"},
                        null,
                        new DialogDisplayer.OptionListener() {
                            @Override
                            public void reportResult(int option) {
                                if (option == 0) {
                                    afterDeleteListener.call(role);
                                } else {
                                    afterDeleteListener.call(null);
                                }
                            }
                        });
            }
        });
        crudController.setEntitySaver(new EntitySaver<Role>() {
            @Override
            public Role saveEntity(@NotNull final Role entity) throws SaveException {
                long oid = Registry.getDefault().getRbacAdmin().saveRole(entity);
                try {
                    return Registry.getDefault().getRbacAdmin().findRoleByPrimaryKey(oid);
                } catch (final FindException e) {
                    logger.log(Level.WARNING, "Unable to retrieve saved role: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    entity.setOid(oid);
                    return entity;
                } catch (final PermissionDeniedException e) {
                    throw new SaveException("Cannot retrieve saved entity: " + ExceptionUtils.getMessage(e), e);
                }
            }
        });
        crudController.setEntityEditor(new EntityEditor<Role>() {
            @Override
            public void displayEditDialog(@NotNull final Role role, @NotNull final Functions.UnaryVoidThrows<Role, SaveException> afterEditListener) {
                boolean create = Role.DEFAULT_OID == role.getOid();
                AttemptedOperation operation = create
                        ? new AttemptedCreateSpecific(EntityType.RBAC_ROLE, role)
                        : new AttemptedUpdate(EntityType.RBAC_ROLE, role);
                boolean readOnly = !Registry.getDefault().getSecurityProvider().hasPermission(operation);
                final Set<String> roleNames = new HashSet<>();
                if (!readOnly) {
                    for (int i = 0; i < rolesTableModel.getRowCount() - 1; i++) {
                        final Object value = rolesTableModel.getValueAt(i, 0);
                        if (value instanceof String && !value.equals(role.getName())) {
                            roleNames.add((String) value);
                        }
                    }
                }
                final RolePropertiesDialog dlg = new RolePropertiesDialog(RoleManagerWindow.this, role, readOnly, roleNames, afterEditListener);
                dlg.pack();
                Utilities.centerOnParentWindow(dlg);
                DialogDisplayer.display(dlg);
            }
        });
    }

    private void initFiltering() {
        filterPanel.registerFilterCallback(new Runnable() {
            @Override
            public void run() {
                loadCount();
            }
        });
        filterPanel.attachRowSorter((TableRowSorter) (rolesTable.getRowSorter()), new int[]{0});
        loadCount();
    }

    private void initTable() {
        rolesTableModel = TableUtil.configureTable(rolesTable,
                column("Name", 80, 400, 99999, new Functions.Unary<String, Role>() {
                    @Override
                    public String call(final Role role) {
                        return getNameForRole(role);
                    }
                }),
                column("Type", 40, 80, 99999, new Functions.Unary<String, Role>() {
                    @Override
                    public String call(final Role role) {
                        return role.isUserCreated() ? CUSTOM : SYSTEM;
                    }
                }));
        final RunOnChangeListener tableListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                handleTableChange();
            }
        });
        rolesTable.getSelectionModel().addListSelectionListener(tableListener);
        rolesTableModel.addTableModelListener(tableListener);
        Utilities.setRowSorter(rolesTable, rolesTableModel);
        loadTable();
    }

    private void handleTableChange() {
        final Role selectedRole = getSelectedRole();
        propertiesPanel.configure(selectedRole, selectedRole == null ? null : getNameForRole(selectedRole));
        assignmentsPanel.configure(selectedRole);
        final SecurityProvider securityProvider = Registry.getDefault().getSecurityProvider();
        editButton.setEnabled(selectedRole != null && selectedRole.isUserCreated() &&
                securityProvider.hasPermission(new AttemptedUpdate(EntityType.RBAC_ROLE, selectedRole)));
        removeButton.setEnabled(selectedRole != null && selectedRole.isUserCreated() &&
                securityProvider.hasPermission(new AttemptedDeleteSpecific(EntityType.RBAC_ROLE, selectedRole)));
        filterPanel.allowFiltering(rolesTableModel.getRowCount() > 0);
        loadCount();
    }

    private String getNameForRole(Role role) {
        String name = "name unavailable";
        try {
            name = Registry.getDefault().getEntityNameResolver().getNameForEntity(role, true);
            return name;
        } catch (final FindException e) {
            logger.log(Level.WARNING, "Unable to retrieve name for role: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
        return name;
    }

    private void initButtons() {
        createButton.addActionListener(crudController.createCreateAction());
        createButton.setEnabled(Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedCreate(EntityType.RBAC_ROLE)));
        editButton.addActionListener(crudController.createEditAction());
        Utilities.setDoubleClickAction(rolesTable, editButton);
        removeButton.addActionListener(crudController.createDeleteAction());
        closeButton.addActionListener(Utilities.createDisposeAction(this));
        Utilities.setEscAction(this, closeButton);
        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                Actions.invokeHelp(RoleManagerWindow.this);
            }
        });
    }

    private void loadCount() {
        final int visible = rolesTable.getRowCount();
        final int total = rolesTableModel.getRowCount();
        filterLabel.setText("showing " + visible + " of " + total + " items");
    }

    private void loadTable() {
        try {
            final ArrayList<Role> roles = new ArrayList<>(Registry.getDefault().getRbacAdmin().findAllRoles());
            Collections.sort(roles, new NamedEntityComparator());
            rolesTableModel.setRows(roles);
        } catch (final FindException e) {
            logger.log(Level.WARNING, "Unable to retrieve rows: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    @Nullable
    private Role getSelectedRole() {
        Role selected = null;
        final int rowIndex = rolesTable.getSelectedRow();
        if (rowIndex >= 0) {
            final int modelIndex = rolesTable.convertRowIndexToModel(rowIndex);
            selected = rolesTableModel.getRowObject(modelIndex);
        }
        return selected;
    }
}
