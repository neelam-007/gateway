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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.util.Functions.propertyTransform;

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
                    throw new SaveException(e);
                }
            }
        });
        crudController.setEntityEditor(new EntityEditor<Role>() {
            @Override
            public void displayEditDialog(@NotNull final Role role, @NotNull final Functions.UnaryVoid<Role> afterEditListener) {
                boolean create = Role.DEFAULT_OID == role.getOid();
                AttemptedOperation operation = create
                        ? new AttemptedCreateSpecific(EntityType.RBAC_ROLE, role)
                        : new AttemptedUpdate(EntityType.RBAC_ROLE, role);
                boolean readOnly = !Registry.getDefault().getSecurityProvider().hasPermission(operation);
                final Set<String> roleNames = new HashSet<>();
                if (!readOnly) {
                    for (int i = 0; i < rolesTableModel.getRowCount() - 1; i++) {
                        final Object value = rolesTableModel.getValueAt(i, 0);
                        if (value instanceof String) {
                            roleNames.add((String) value);
                        }
                    }
                }
                final RolePropertiesDialog dlg = new RolePropertiesDialog(RoleManagerWindow.this, role, readOnly, roleNames);
                dlg.pack();
                Utilities.centerOnParentWindow(dlg);
                DialogDisplayer.display(dlg, new Runnable() {
                    @Override
                    public void run() {
                        if (dlg.isConfirmed()) {
                            dlg.setDataOnRole(role);
                            afterEditListener.call(role);
                        } else {
                            afterEditListener.call(null);
                        }
                    }
                });
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
                column("Name", 80, 400, 99999, propertyTransform(Role.class, "contextualDescriptiveName")),
                column("Type", 40, 80, 99999, new Functions.Unary<String, Role>() {
                    @Override
                    public String call(final Role role) {
                        return role.isUserCreated() ? CUSTOM : SYSTEM;
                    }
                }));
        final RunOnChangeListener enableOrDisableListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                final Role selectedRole = getSelectedRole();
                if (selectedRole != null) {
                    roleTextField.setText(selectedRole.getName());
                    typeTextField.setText(selectedRole.isUserCreated() ? CUSTOM : SYSTEM);
                    descriptionTextPane.setText(getDescription(selectedRole));
                } else {
                    roleTextField.setText(StringUtils.EMPTY);
                    typeTextField.setText(StringUtils.EMPTY);
                    descriptionTextPane.setText(StringUtils.EMPTY);
                }
            }
        });
        rolesTable.getSelectionModel().addListSelectionListener(enableOrDisableListener);
        Utilities.setRowSorter(rolesTable, rolesTableModel);
        loadTable();
    }

    private String getDescription(@NotNull final Role role) {
        String roleDescription = role.getDescription();

        StringBuilder sb = new StringBuilder();
        sb.append("<html>");

        if (StringUtils.isNotEmpty(roleDescription)) {
            sb.append(RbacUtilities.getDescriptionString(role, true));
            sb.append("<br>");
        } else {
            sb.append("Users assigned to the ");
            sb.append(role.getName());
            sb.append(" role have the ability to ");
            Set<String> sorted = new TreeSet<String>();
            for (Permission p : role.getPermissions()) {
                StringBuilder sb1 = new StringBuilder();
                sb1.append(p.getOperation().toString());

                EntityType etype = p.getEntityType();
                switch (p.getScope().size()) {
                    case 0:
                        sb1.append("[Any");
                        if (etype == EntityType.ANY)
                            sb1.append(" Object");
                        else {
                            sb1.append(" ").append(etype.getName());
                        }
                        sb1.append("]");
                        break;
                    case 1:
                        break;
                    default:
                        sb1.append("[Complex Scope]");
                }
                sorted.add(sb1.toString());
            }
            String[] p = sorted.toArray(new String[sorted.size()]);
            for (int i = 0; i < p.length; i++) {
                if (i == p.length - 1) {
                    sb.append(" and ");
                } else if (i != 0) {
                    sb.append(", ");
                }
                sb.append(p[i]);
            }
            sb.append(" the ").append(role.getName());
        }

        sb.append("</html>");
        return sb.toString();
    }

    private void initButtons() {
        createButton.addActionListener(crudController.createCreateAction());
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
