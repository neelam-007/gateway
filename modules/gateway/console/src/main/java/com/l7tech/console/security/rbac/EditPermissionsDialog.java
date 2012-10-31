package com.l7tech.console.security.rbac;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class EditPermissionsDialog extends JDialog {
    private JPanel contentPane;
    private JPanel informationPanel;
    private JPanel buttonPanel;
    private JButton buttonOK;
    private JButton buttonCancel;

    private JComboBox typeSelection;
    private JComboBox operationSelection;
    private JTextArea scopeField;
    private JButton browseForScope;

    private Permission permission;
    private boolean confirmed = false;
    private JLabel scopeLabel;

    private final RunOnChangeListener enableDisableListener = new RunOnChangeListener(new Runnable() {
        @Override
        public void run() {
            enableDisable();
        }
    });

    public EditPermissionsDialog(Permission permission, Dialog parent) {
        super(parent);
        this.permission = permission;
        initialize();
    }

    private void initialize() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        Set<EntityType> types = new TreeSet<EntityType>(EntityType.NAME_COMPARATOR);
        for (EntityType type : EntityType.values()) {
            if (type.isDisplayedInGui()) types.add(type);
        }

        typeSelection.setModel(new DefaultComboBoxModel(types.toArray(new EntityType[types.size()])));

        typeSelection.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED && !permission.getEntityType().equals(e.getItem())){
                    //default to ANY when the combobox changes
                    permission.setScope(new HashSet<ScopePredicate>());
                    permission.setEntityType(EntityType.ANY);
                    scopeField.setText(getScopeString(permission));
                }
            }
        });
        EntityType etype = permission.getEntityType();
        if (etype != null) typeSelection.setSelectedItem(etype);

        java.util.List<OperationType> values = new ArrayList<OperationType>();
        for (OperationType opType : OperationType.values()) {
            if (opType != OperationType.NONE && opType != OperationType.OTHER)
                values.add(opType);
        }
        operationSelection.setModel(new DefaultComboBoxModel(values.toArray(new OperationType[values.size()])));

        OperationType op = permission.getOperation();
        if (op != null) operationSelection.setSelectedItem(op);

        setupButtonListeners();
        setupActionListeners();

        if (permission.getOid() == Permission.DEFAULT_OID) {
            setTitle("Create new Permission");
            scopeField.setText(getScopeString(permission));
        } else {
            setTitle("Edit Permission");
            scopeField.setText(getScopeString(permission));
        }

        pack();
        enableDisable();
    }

    void enableDisable() {
        EntityType etype = (EntityType)typeSelection.getSelectedItem();
        buttonOK.setEnabled(etype != null && operationSelection.getSelectedItem() != null);

        boolean scopeEnabled = RbacUtilities.isEnableRoleEditing() || etype == EntityType.SERVICE;
        scopeLabel.setVisible(scopeEnabled);
        scopeField.setVisible(scopeEnabled);
        browseForScope.setVisible(scopeEnabled);
        browseForScope.setEnabled(scopeEnabled);
    }

    private void setupButtonListeners() {
        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        operationSelection.addActionListener(enableDisableListener);
        typeSelection.addActionListener(enableDisableListener);

        browseForScope.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ScopeDialog sd = new ScopeDialog(EditPermissionsDialog.this, permission.getAnonymousClone(), (EntityType)typeSelection.getSelectedItem());
                sd.pack();
                Utilities.centerOnScreen(sd);
                sd.setVisible(true);
                EntityHeader header = null;
                final Permission perm = sd.getPermission();
                if (perm != null) {
                    permission.copyFrom(perm);
                    Set<ScopePredicate> scopes = permission.getScope();
                    if (scopes.size() == 1) {
                        ScopePredicate pred = scopes.iterator().next();
                        if (pred instanceof ObjectIdentityPredicate) {
                            ObjectIdentityPredicate objectIdentityPredicate = (ObjectIdentityPredicate) pred;
                            header = objectIdentityPredicate.getHeader();
                        }
                    }

                    if (header != null) {
                        scopeField.setText(header.getName());
                    } else
                        scopeField.setText(getScopeString(sd.getPermission()));
                }
            }
        });
    }

    private String getScopeString(Permission perm) {
        StringBuilder theScope = new StringBuilder();
        if (perm != null) {
            int i = perm.getScope().size();
            if (i == 0) {
                theScope.append("<Any ").append(perm.getEntityType().getName()).append(">\n");
            } else {
                Set<ScopePredicate> scopes = perm.getScope();
                for (ScopePredicate scopePredicate : scopes) {
                    theScope.append(scopePredicate.toString()).append("\n");
                }
            }
        }
        return theScope.toString();
    }

    private void setupActionListeners() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });


        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        permission.setEntityType((EntityType) typeSelection.getSelectedItem());
        permission.setOperation((OperationType) operationSelection.getSelectedItem());
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public Permission getPermission() {
        return confirmed ? permission : null;
    }
}
