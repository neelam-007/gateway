/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.security.rbac;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.text.MessageFormat;
import java.lang.reflect.Method;
import java.beans.*;

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
    private JRadioButton attributeRadioButton;
    private JTextField attrValue;
    private JLabel attrNameLabel;
    private JLabel attrValueLabel;
    private JComboBox attrNamesList;

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
        setupAttributeNames();
        setModal(true);
        radioListener = new RadioListener();

        setRadioText(allRadioButton);
        setRadioText(specificRadioButton);
        setRadioText(attributeRadioButton);

        ButtonGroup rbGroup = new ButtonGroup();
        rbGroup.add(allRadioButton);
        rbGroup.add(specificRadioButton);
        rbGroup.add(attributeRadioButton);

        allRadioButton.addActionListener(radioListener);
        specificRadioButton.addActionListener(radioListener);
        attributeRadioButton.addActionListener(radioListener);

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
                final FindEntityDialog fed = new FindEntityDialog(ScopeDialog.this, entityType);
                fed.pack();
                Utilities.centerOnScreen(fed);
                DialogDisplayer.display(fed, new Runnable() {
                    public void run() {
                        EntityHeader header = fed.getSelectedEntityHeader();
                        specificEntity = header;
                        if (header != null) {
                            scope.clear();
                            scope.add(new ObjectIdentityPredicate(permission, header.getOid()));
                            specificText.setText(header.getName());
                        }
                    }
                });
            }
        });

        enableDisable();

        add(mainPanel);
    }

    private void setupAttributeNames() {
        List<String> names = new ArrayList<String>();

        Class eClazz = entityType.getEntityClass();
        try {
            BeanInfo info = Introspector.getBeanInfo(eClazz);
            PropertyDescriptor[] props = info.getPropertyDescriptors();
            for (PropertyDescriptor propertyDescriptor : props) {
                Method getter = propertyDescriptor.getReadMethod();
                if (getter != null)
                    //there is a getter for this property, so use it in the list
                    names.add(propertyDescriptor.getName());
            }
            attrNamesList.setModel(new DefaultComboBoxModel(names.toArray(new String[0])));
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }
        Method[] getters = eClazz.getMethods();
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
        attrNameLabel.setEnabled(attributeRadioButton.isSelected());
        attrValueLabel.setEnabled(attributeRadioButton.isSelected());
        attrNamesList.setEnabled(attributeRadioButton.isSelected());
        attrValue.setEnabled(attributeRadioButton.isSelected());
    }

    void ok() {

        if (attributeRadioButton.isSelected())
            addAttributePredicate();

        permission.getScope().clear();
        permission.getScope().addAll(scope);
        dispose();
    }

    private void addAttributePredicate() {
        AttributePredicate aPred = new AttributePredicate(permission, (String) attrNamesList.getSelectedItem(), attrValue.getText());
        scope.add(aPred);
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
