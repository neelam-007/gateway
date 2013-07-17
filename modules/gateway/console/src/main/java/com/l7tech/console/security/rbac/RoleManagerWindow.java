package com.l7tech.console.security.rbac;

import com.l7tech.console.action.Actions;
import com.l7tech.console.action.DeleteEntityNodeAction;
import com.l7tech.console.panels.FilterPanel;
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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * CRUD dialog for Roles.
 */
public class RoleManagerWindow extends JDialog {
    private static final Logger logger = Logger.getLogger(RoleManagerWindow.class.getName());
    private static final String CUSTOM = "Custom";
    private static final String SYSTEM = "System";
    private static final String DELETE_CONFIRMATION_FORMAT = "Are you sure you want to remove the role {0}? This action cannot be undone.";
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
    private JTextField roleTextField;
    private JTextField typeTextField;
    private JTextPane descriptionTextPane;
    private JButton editButton;
    private RoleAssignmentsPanel assignmentsPanel;
    private SimpleTableModel<Role> rolesTableModel;
    private EntityCrudController<Role> crudController;

    public RoleManagerWindow(@NotNull final Window owner) {
        super(owner, "Manage Roles", DEFAULT_MODALITY_TYPE);
        setContentPane(contentPanel);
        initTable();
        initFiltering();
        initTabs();
        initCrudController();
        initButtons();
        handleTableChange();
    }

    private void initTabs() {
        descriptionTextPane.setContentType("text/html");
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
                final String msg = MessageFormat.format(DELETE_CONFIRMATION_FORMAT, role.getName());
                DialogDisplayer.showOptionDialog(
                        RoleManagerWindow.this,
                        WordUtils.wrap(msg, DeleteEntityNodeAction.LINE_CHAR_LIMIT, null, true),
                        "Remove " + role.getName(),
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        new Object[]{"Remove " + role.getName(), "Cancel"},
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
        if (selectedRole != null) {
            roleTextField.setText(getNameForRole(selectedRole));
            typeTextField.setText(selectedRole.isUserCreated() ? CUSTOM : SYSTEM);
            descriptionTextPane.setText(RbacUtilities.getDescriptionString(selectedRole, true));
        } else {
            roleTextField.setText(StringUtils.EMPTY);
            typeTextField.setText(StringUtils.EMPTY);
            descriptionTextPane.setText(StringUtils.EMPTY);
        }
        assignmentsPanel.configure(selectedRole);
        editButton.setEnabled(selectedRole != null && selectedRole.isUserCreated() &&
                Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.RBAC_ROLE, selectedRole)));
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
