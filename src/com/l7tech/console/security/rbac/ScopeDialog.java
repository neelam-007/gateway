/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.security.rbac;

import com.l7tech.common.security.rbac.*;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.text.MessageFormat;

/**
 * @author alex
 */
public class ScopeDialog extends JDialog {
    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;

    private JRadioButton allRadioButton;

    private JRadioButton specificRadioButton;
    private JButton specificFindButton;
    private JPanel specificPanel;
    private JTextField specificText;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RbacGui");

    private final List<ScopePredicate> scope = new ArrayList<ScopePredicate>();
    private Permission permission;
    private ListModel emptyListModel = new DefaultListModel();
    private RadioListener radioListener;
    private EntityHeader specificEntity;
    private final EntityType entityType;

    public ScopeDialog(Frame owner, Permission perm, EntityType etype) throws HeadlessException {
        super(owner);
        this.permission = perm;
        this.scope.addAll(perm.getScope());
        this.entityType = etype;
        initialize();
    }

    public ScopeDialog(Dialog owner, Permission perm, EntityType etype) throws HeadlessException {
        super(owner);
        this.permission = perm;
        this.scope.addAll(perm.getScope());
        this.entityType = etype;
        initialize();
    }

    private class RadioListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            enableDisable();
        }
    }

    private void initialize() {
        setModal(true);
        radioListener = new RadioListener();

        setRadioText(allRadioButton);
        setRadioText(specificRadioButton);

        ButtonGroup rbGroup = new ButtonGroup();
        rbGroup.add(allRadioButton);
        rbGroup.add(specificRadioButton);

        allRadioButton.addActionListener(radioListener);
        specificRadioButton.addActionListener(radioListener);

        if (scope.size() == 0) {
            allRadioButton.setSelected(true);
        } else {
            if (scope.get(0) instanceof ObjectIdentityPredicate) {
                if (scope.size() > 1) throw new RuntimeException("Found an ObjectIdentityPredicate in a scope with size > 1");
                specificRadioButton.setSelected(true);
                specificText.setText(getSpecificLabel());
            }
        }

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
                FindEntityDialog fed = new FindEntityDialog(ScopeDialog.this, entityType);
                fed.pack();
                Utilities.centerOnScreen(fed);
                fed.setVisible(true);
                EntityHeader header = fed.getSelectedEntityHeader();
                specificEntity = header;
                if (header != null) {
                    scope.clear();
                    scope.add(new ObjectIdentityPredicate(permission, header.getOid()));
                    specificText.setText(header.getName());
                }
            }
        });

        enableDisable();

        add(mainPanel);
    }

    private void setRadioText(JRadioButton rb) {
        rb.setText(MessageFormat.format(rb.getText(), entityType.getName()));
    }

    private String getSpecificLabel() {
        if (scope.size() != 1  || !(scope.get(0) instanceof ObjectIdentityPredicate)) return "";
        ObjectIdentityPredicate oidp = (ObjectIdentityPredicate) scope.get(0);
        return oidp.toString();
    }

    private void enableDisable() {
        specificFindButton.setEnabled(specificRadioButton.isSelected());
        specificText.setText(specificRadioButton.isSelected() ? getSpecificLabel() : "");
        specificText.setEnabled(specificRadioButton.isSelected());
    }

    void ok() {
        permission.getScope().clear();
        permission.getScope().addAll(scope);
        dispose();
    }

    void cancel() {
        permission = null;
        dispose();
    }

    public Permission getPermission() {
        return permission;
    }

    public EntityHeader getSpecificEntity() {
        return specificEntity;
    }
}
