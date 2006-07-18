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

    private JRadioButton attributeRadioButton;
    private JButton addAttributeButton;
    private JButton editAttributeButton;
    private JButton removeAttributeButton;
    private JList attributeList;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RbacGui");

    private final List<ScopePredicate> scope = new ArrayList<ScopePredicate>();
    private Permission permission;
    private AttributeListModel attributeListModel;
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

    private class AttributeListModel extends AbstractListModel {
        public int getSize() {
            return scope.size();
        }

        public Object getElementAt(int index) {
            return scope.get(index);
        }

        void add(AttributePredicate pred) {
            for (ScopePredicate predicate : scope) {
                if (!(predicate instanceof AttributePredicate)) throw new RuntimeException("Can't add an AttributePredicate to a scope that already contains something else");
            }
            scope.add(pred);
            fireContentsChanged(attributeList, 0, scope.size());
        }

        void remove(AttributePredicate pred) {
            scope.remove(pred);
            fireContentsChanged(attributeList, 0, scope.size());
        }
    }

    private class RadioListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (attributeRadioButton.isSelected()) {
                attributeList.setModel(attributeListModel);
            } else {
                attributeList.setModel(emptyListModel);
            }
            enableDisable();
        }
    }

    private void initialize() {
        attributeListModel = new AttributeListModel();
        radioListener = new RadioListener();

        setRadioText(allRadioButton);
        setRadioText(specificRadioButton);
        setRadioText(attributeRadioButton);

        allRadioButton.addActionListener(radioListener);
        specificRadioButton.addActionListener(radioListener);
        attributeRadioButton.addActionListener(radioListener);

        if (scope.size() == 0) {
            allRadioButton.setSelected(true);
            attributeList.setModel(emptyListModel);
        } else {
            if (scope.get(0) instanceof ObjectIdentityPredicate) {
                if (scope.size() > 1) throw new RuntimeException("Found an ObjectIdentityPredicate in a scope with size > 1");
                specificRadioButton.setSelected(true);
                specificText.setText(getSpecificLabel());
                attributeList.setModel(emptyListModel);
            } else {
                attributeList.setModel(attributeListModel);
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
        // TODO lookup name
        return entityType.getName() + " #" + oidp.getTargetEntityOid();
    }

    private void enableDisable() {
        specificFindButton.setEnabled(specificRadioButton.isSelected());
        specificText.setText(specificRadioButton.isSelected() ? getSpecificLabel() : "");
        specificText.setEnabled(specificRadioButton.isSelected());

        attributeList.setEnabled(attributeRadioButton.isSelected());
        addAttributeButton.setEnabled(attributeRadioButton.isSelected());
        removeAttributeButton.setEnabled(attributeRadioButton.isSelected());
        editAttributeButton.setEnabled(attributeRadioButton.isSelected());
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
}
