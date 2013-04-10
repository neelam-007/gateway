/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.console.security.rbac;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Set;

class ScopeDialog extends JDialog {
    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RbacGui");

    private final boolean allowChanges;
    private boolean confirmed = false;
    private Permission permission;
    private final EntityType entityType;
    private EntityHeader specificEntity;
    private JScrollPane scopesTableScrollPane;
    private JTable scopesTable;
    private JButton removeButton;
    private JButton addButton;
    private JRadioButton noScopePredicatesRadioButton;
    private JRadioButton scopePredicatesRadioButton;
    private JPanel predicatesPanel;
    private JRadioButton scopeSpecificRadioButton;
    private JPanel specificPanel;
    private JTextField specificText;
    private JButton specificFindButton;

    private SimpleTableModel<ScopePredicate> scopesTableModel;

    static final String OPTION_FOLDER = resources.getString("scopeDialog.scope.folder.option");
    static final String OPTION_FOLDER_ANCESTRY = resources.getString("scopeDialog.scope.folderAncestry.option");
    static final String OPTION_ATTRIBUTE = resources.getString("scopeDialog.scope.attribute.option");
    static final String OPTION_SECURITY_ZONE = resources.getString("scopeDialog.scope.securityZone.option");

    // Scope predicates to offer for folderable entities
    private static final String[] SCOPE_OPTION_TYPES_FOLDER = {
        OPTION_FOLDER,
        OPTION_FOLDER_ANCESTRY,
        OPTION_ATTRIBUTE,
        OPTION_SECURITY_ZONE
    };

    // Scope predicates to offer for EntityType.ANY
    private static final String[] SCOPE_OPTION_TYPES_ANY = {
        OPTION_FOLDER,   // Folder predicate will fail at runtime to grant permission for any non-folderable entity type
        OPTION_ATTRIBUTE,
        OPTION_SECURITY_ZONE // Zone predicate will fail at runtime to grant permission for any non-ZoneableEntity type
    };

    // Scope predicates to offer for any other non-folderable entity type
    private static final String[] SCOPE_OPTION_TYPES_NO_FOLDER = {
        OPTION_ATTRIBUTE,
        OPTION_SECURITY_ZONE  // Zone predicate will fail at runtime to grant permission for any non-ZeoneableEntity type
    };

    public ScopeDialog(Window owner, Permission perm, EntityType etype, boolean allowChanges) throws HeadlessException {
        super(owner);
        this.permission = perm;
        this.entityType = etype;
        this.allowChanges = allowChanges;
        initialize();
    }

    private final RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
        @Override
        public void run() {
            enableDisable();
        }
    });

    private void initialize() {
        setTitle(resources.getString("scopeDialog.title"));
        setModal(true);

        Utilities.equalizeButtonSizes(okButton, cancelButton);

        noScopePredicatesRadioButton.addActionListener(changeListener);
        scopePredicatesRadioButton.addActionListener(changeListener);
        scopeSpecificRadioButton.addActionListener(changeListener);

        updateButtonLabel(noScopePredicatesRadioButton, true);
        updateButtonLabel(scopeSpecificRadioButton, false);
        updateButtonLabel(scopePredicatesRadioButton, true);

        //noinspection unchecked
        scopesTableModel = TableUtil.configureTable(
            scopesTable,
            TableUtil.column("Type", 100, 100, 500, scopeTypeGetter),
            TableUtil.column("Value", 200, 300, 9999, scopeValueGetter)
        );

        final boolean allowSpecificScope = entityType.isAllowSpecificScope();
        scopeSpecificRadioButton.setEnabled(allowSpecificScope);
        scopeSpecificRadioButton.setVisible(allowSpecificScope);
        specificPanel.setVisible(allowSpecificScope);

        final boolean allowCustomScopes = canHaveCustomScopes(entityType);
        scopePredicatesRadioButton.setEnabled(allowCustomScopes);
        scopePredicatesRadioButton.setVisible(allowCustomScopes);
        predicatesPanel.setVisible(allowCustomScopes);

        Set<ScopePredicate> scopes = permission.getScope();
        boolean haveAnyScope = scopes != null && scopes.size() > 0;
        ObjectIdentityPredicate scopeSpecific = findSpecificScope(scopes);
        specificEntity = scopeSpecific == null ? null : scopeSpecific.getHeader();
        if (specificEntity != null)
            specificText.setText(specificEntity.getName());
        boolean haveSpecificScope = scopeSpecific != null;
        boolean haveCustomScope = haveAnyScope && !haveSpecificScope;
        scopesTableModel.setRows(haveCustomScope ? new ArrayList<ScopePredicate>(scopes) : new ArrayList<ScopePredicate>());
        scopesTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scopesTable.getSelectionModel().addListSelectionListener(changeListener);

        noScopePredicatesRadioButton.setSelected(!haveAnyScope);
        scopeSpecificRadioButton.setSelected(haveSpecificScope);
        scopePredicatesRadioButton.setSelected(haveAnyScope && !haveSpecificScope);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        specificFindButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(!entityType.isAllowSpecificScope()) return;

                final FindEntityDialog fed = new FindEntityDialog(ScopeDialog.this, entityType, null);
                fed.pack();
                Utilities.centerOnScreen(fed);
                DialogDisplayer.display(fed, new Runnable() {
                    public void run() {
                        EntityHeader header = fed.getSelectedEntityHeader();
                        specificEntity = header;
                        if (header != null) {
                            specificText.setText(header.getName());
                        }
                    }
                });
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = scopesTable.getSelectedRow();
                if (row >= 0)
                    scopesTableModel.removeRowAt(row);
            }
        });

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!canHaveCustomScopes(entityType))
                    return;

                final String[] optionTypes = EntityType.ANY.equals(entityType) ? SCOPE_OPTION_TYPES_ANY :
                    canHaveFolder(entityType) ? SCOPE_OPTION_TYPES_FOLDER :
                        SCOPE_OPTION_TYPES_NO_FOLDER;

                final DialogDisplayer.OptionListener optionListener = new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option >= 0) {
                            ScopePredicate scope;

                            String optionStr = optionTypes[option];
                            if (OPTION_ATTRIBUTE.equals(optionStr)) {
                                scope = new AttributePredicate(permission, null, "");
                            } else if (OPTION_FOLDER.equals(optionStr)) {
                                scope = new FolderPredicate(permission, null, true);
                            } else if (OPTION_FOLDER_ANCESTRY.equals(optionStr)) {
                                scope = new EntityFolderAncestryPredicate(permission, entityType, -1L);
                            } else if (OPTION_SECURITY_ZONE.equals(optionStr)) {
                                scope = new SecurityZonePredicate(permission, null);
                            } else {
                                throw new IllegalStateException("Unknown option: " + optionStr + " index " + option);
                            }

                            doEditScope(scope, -1);
                        }
                    }
                };

                DialogDisplayer.showOptionDialog(addButton,
                    resources.getString("scopeDialog.add.scope.text"),
                    resources.getString("scopeDialog.add.scope.title"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    optionTypes,
                    null,
                    optionListener);
            }
        });

        Utilities.enableGrayOnDisabled(okButton, noScopePredicatesRadioButton, scopeSpecificRadioButton, scopePredicatesRadioButton,
            specificFindButton, addButton, removeButton);
        enableDisable();

        add(mainPanel);
    }

    /**
     * Check whether the specified entity type supports folder ancestry.
     * This currently assumes that any entity type that can be placed in a folder will be assignable to HasFolder.
     *
     * @param entityType the entity type to check.  Required.
     * @return true if it makes sense to have folder ancestor predicates for entities of this type.
     */
    public static boolean canHaveFolder(@NotNull EntityType entityType) {
        return canHaveCustomScopes(entityType) && HasFolder.class.isAssignableFrom(entityType.getEntityClass());
    }

    /**
     * Check whether the specified entity type supports custom scope predicates (such as attribute predicates).
     * This currently assumes that any entity type whose entity class is available within the SSM code can have attribute
     * predicates created.
     *
     * @param entityType the entity type to check.  Required.
     * @return true if it makes sense to have attribute predicates for entities of this type.
     */
    public static boolean canHaveCustomScopes(@NotNull EntityType entityType) {
        return entityType.getEntityClass() != null;
    }

    private ObjectIdentityPredicate findSpecificScope(Set<ScopePredicate> scopes) {
        ObjectIdentityPredicate oip = null;

        if (scopes != null) {
            for (ScopePredicate scope : scopes) {
                if (scope instanceof ObjectIdentityPredicate) {
                    oip = (ObjectIdentityPredicate) scope;
                    break;
                }
            }
        }

        return oip;
    }

    private void doEditScope(ScopePredicate scope, final int existingRow) {
        final OkCancelDialog<? extends ScopePredicate> dlg;

        if (scope instanceof AttributePredicate) {
            AttributePredicate attributePredicate = (AttributePredicate) scope;
            dlg = new OkCancelDialog<AttributePredicate>(this, "Restrict Permission by Attribute", true, new ScopeAttributePanel(attributePredicate, entityType));
        } else if (scope instanceof FolderPredicate) {
            FolderPredicate folderPredicate = (FolderPredicate) scope;
            dlg = new OkCancelDialog<FolderPredicate>(this, "Restrict Permission by Folder", true, new ScopeFolderPanel(folderPredicate, entityType));
        } else if (scope instanceof EntityFolderAncestryPredicate) {
            EntityFolderAncestryPredicate entityFolderAncestryPredicate = (EntityFolderAncestryPredicate) scope;
            dlg = new OkCancelDialog<EntityFolderAncestryPredicate>(this, "Restrict Permission to Folder Ancestors", true, new ScopeEntityFolderAncestryPanel(entityFolderAncestryPredicate, entityType));
        } else if (scope instanceof SecurityZonePredicate) {
            SecurityZonePredicate securityZonePredicate = (SecurityZonePredicate) scope;
            dlg = new OkCancelDialog<SecurityZonePredicate>(this, "Restrict Permission to Security Zone", true, new ScopeSecurityZonePanel(securityZonePredicate, entityType));
        } else if (scope instanceof ObjectIdentityPredicate) {
            throw new UnsupportedOperationException("Unable to add ObjectIdentityPredicate as custom scope");
        } else {
            throw new UnsupportedOperationException("Unsupported scope predicate type: " + scope.getClass().getName());
        }

        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (!dlg.wasOKed())
                    return;

                ScopePredicate newScope = dlg.getValue();
                if (existingRow >= 0) {
                    scopesTableModel.setRowObject(existingRow, newScope);
                } else {
                    scopesTableModel.addRow(newScope);
                }
            }
        });
    }

    private void enableDisable() {
        final boolean hasSpecificScope = scopeSpecificRadioButton.isSelected();
        specificPanel.setEnabled(hasSpecificScope);
        specificFindButton.setEnabled(allowChanges && hasSpecificScope);

        boolean hasCustomScopes = scopePredicatesRadioButton.isSelected();
        scopesTableScrollPane.setEnabled(hasCustomScopes);
        scopesTable.setEnabled(hasCustomScopes);
        addButton.setEnabled(allowChanges && hasCustomScopes);
        removeButton.setEnabled(allowChanges && hasCustomScopes && scopesTable.getSelectedRow() >= 0);

        noScopePredicatesRadioButton.setEnabled(allowChanges);
        scopeSpecificRadioButton.setEnabled(allowChanges);
        scopePredicatesRadioButton.setEnabled(allowChanges);
    }

    void ok() {
        permission.getScope().clear();

        if (scopeSpecificRadioButton.isSelected() && specificEntity != null) {
            final ObjectIdentityPredicate pred = new ObjectIdentityPredicate(permission, specificEntity.getStrId());
            pred.setHeader(specificEntity);
            permission.getScope().add(pred);
        } else if (scopePredicatesRadioButton.isSelected()) {
            permission.getScope().addAll(scopesTableModel.getRows());
        }

        confirmed = true;
        dispose();
    }

    void cancel() {
        dispose();
    }

    public Permission getPermission() {
        return confirmed ? permission : null;
    }

    private Functions.Unary<String, ScopePredicate> scopeTypeGetter = new Functions.Unary<String, ScopePredicate>() {
        @Override
        public String call(ScopePredicate sp) {
            return sp.getClass().getSimpleName();
        }
    };

    private Functions.Unary<String, ScopePredicate> scopeValueGetter = new Functions.Unary<String, ScopePredicate>() {
        @Override
        public String call(ScopePredicate sp) {
            return sp.toString();
        }
    };

    private void updateButtonLabel(AbstractButton button, boolean plural) {
        button.setText(MessageFormat.format(button.getText(), plural ? entityType.getPluralName() : entityType.getName()));
    }
}
