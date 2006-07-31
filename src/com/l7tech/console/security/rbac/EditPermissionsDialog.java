package com.l7tech.console.security.rbac;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.security.rbac.Permission;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

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

        typeSelection.setModel(new DefaultComboBoxModel(EntityType.values()));
        EntityType etype = permission.getEntityType();
        if (etype != null) typeSelection.setSelectedItem(etype);

        operationSelection.setModel(new DefaultComboBoxModel(OperationType.values()));
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

        boolean scopeEnabled = etype == EntityType.SERVICE;
        scopeLabel.setVisible(scopeEnabled);
        scopeField.setVisible(scopeEnabled);
        browseForScope.setVisible(scopeEnabled);

        if (scopeEnabled)
            browseForScope.setEnabled(etype != EntityType.ANY);
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

        typeSelection.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
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
