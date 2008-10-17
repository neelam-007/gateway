package com.l7tech.console.security.rbac;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;

public class EditPermissionsDialog extends JDialog {
    private JPanel contentPane;
    private JPanel informationPanel;
    private JPanel buttonPanel;
    private JButton buttonOK;
    private JButton buttonCancel;

    private JComboBox typeSelection;
    private JComboBox operationSelection;
    private JTextField scopeField;
    private JButton browseForScope;

    private Permission permission;
    private JLabel scopeLabel;

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

        typeSelection.setModel(new DefaultComboBoxModel(types.toArray(new EntityType[0])));
        EntityType etype = permission.getEntityType();
        if (etype != null) typeSelection.setSelectedItem(etype);

        java.util.List<OperationType> values = new ArrayList<OperationType>();
        for (OperationType opType : OperationType.values()) {
            if (opType != OperationType.NONE && opType != OperationType.OTHER)
                values.add(opType);
        }
        operationSelection.setModel(new DefaultComboBoxModel(values.toArray(new OperationType[0])));

        OperationType op = permission.getOperation();
        if (op != null) operationSelection.setSelectedItem(op);

        setupButtonListeners();
        setupActionListeners();

        if (permission.getOid() == Permission.DEFAULT_OID)
            setTitle("Create new Permission");
        else
            setTitle("Edit Permission");

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

        operationSelection.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                OperationType opType = (OperationType) operationSelection.getSelectedItem();
                permission.setOperation(opType);
            }
        } );

        typeSelection.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                EntityType eType = (EntityType) typeSelection.getSelectedItem();
                permission.setEntityType(eType);
                enableDisable();
            }
        });

        browseForScope.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ScopeDialog sd = new ScopeDialog(EditPermissionsDialog.this, permission, (EntityType)typeSelection.getSelectedItem());
                sd.pack();
                Utilities.centerOnScreen(sd);
                sd.setVisible(true);
                EntityHeader header = sd.getSpecificEntity();
                final Permission perm = sd.getPermission();
                if (perm != null) {
                    Set<ScopePredicate> scopes = perm.getScope();
                    if (scopes.size() == 1) {
                        ScopePredicate pred = scopes.iterator().next();
                        if (pred instanceof ObjectIdentityPredicate) {
                            ObjectIdentityPredicate objectIdentityPredicate = (ObjectIdentityPredicate) pred;
                            objectIdentityPredicate.setHeader(header);
                        }
                    }
                }

                if (header != null) {
                    scopeField.setText(sd.getSpecificEntity().toString());
                } else
                    scopeField.setText(getScopeString(sd.getPermission()));
            }
        });
    }

    private String getScopeString(Permission perm) {
        String theScope = "";
        if (perm != null) {
            switch(perm.getScope().size()) {
                case 0:
                    if (perm.getEntityType() == EntityType.ANY)
                        theScope = "<Any Object>";
                        theScope += ">";
                        break;
                case 1:
                    theScope += perm.getScope().iterator().next().toString();
                    break;
                default:
                    theScope += "<Complex Scope>";
            }
        }
        return theScope;
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
        dispose();
    }

    private void onCancel() {
        permission = null;
        dispose();
    }

    public Permission getPermission() {
        return permission;
    }
}
