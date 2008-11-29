/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.console.security.rbac;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.Folder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

class ScopeDialog extends JDialog {
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
    private JTextField folderText;
    private JButton folderFindButton;
    private JRadioButton folderRadioButton;
    private JCheckBox transitiveCheckBox;

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

        Utilities.equalizeButtonSizes(okButton, cancelButton);

        setRadioText(allRadioButton, true);
        setRadioText(specificRadioButton, false);
        setRadioText(attributeRadioButton, true);
        setRadioText(folderRadioButton, true);

        allRadioButton.addActionListener(radioListener);
        specificRadioButton.addActionListener(radioListener);
        attributeRadioButton.addActionListener(radioListener);
        folderRadioButton.addActionListener(radioListener);
        transitiveCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (scope.isEmpty()) return;
                ScopePredicate pred = scope.get(0);
                if (pred instanceof FolderPredicate) {
                    FolderPredicate fp = (FolderPredicate)pred;
                    fp.setTransitive(transitiveCheckBox.isSelected());
                }
            }
        });

        if (scope.size() == 0) {
            allRadioButton.setSelected(true);
        } else {
            if (scope.get(0) instanceof ObjectIdentityPredicate) {
                if (scope.size() > 1) throw new RuntimeException("Found an ObjectIdentityPredicate in a scope with size > 1");
                specificRadioButton.setSelected(true);
                specificText.setText(getSpecificLabel());
            } else if (scope.get(0) instanceof FolderPredicate) {
                folderRadioButton.setSelected(true);
                folderText.setText(getFolderLabel());
                transitiveCheckBox.setSelected(((FolderPredicate)scope.get(0)).isTransitive());
            } else if (scope.get(0) instanceof AttributePredicate) {
                AttributePredicate ap = (AttributePredicate)scope.get(0);
                attributeRadioButton.setSelected(true);
                attrNamesList.setSelectedItem(ap.getAttribute());
                attrValue.setText(ap.getValue());
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

        folderFindButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final FindEntityDialog fed = new FindEntityDialog(ScopeDialog.this, EntityType.FOLDER);
                fed.pack();
                Utilities.centerOnScreen(fed);
                DialogDisplayer.display(fed, new Runnable() {
                    public void run() {
                        EntityHeader header = fed.getSelectedEntityHeader();
                        if (header != null) {
                            Folder folder;
                            try {
                                folder = Registry.getDefault().getFolderAdmin().findByPrimaryKey(header.getOid());
                            } catch (FindException e1) {
                                throw new RuntimeException("Couldn't lookup Folder", e1);
                            }
                            scope.clear();
                            scope.add(new FolderPredicate(permission, folder, transitiveCheckBox.isSelected()));
                            folderText.setText(header.getName());
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
    }

    private void setRadioText(JRadioButton rb, boolean plural) {
        rb.setText(MessageFormat.format(rb.getText(), plural ? entityType.getPluralName() : entityType.getName()));
    }

    private String getSpecificLabel() {
        if (scope.size() != 1  || !(scope.get(0) instanceof ObjectIdentityPredicate)) return "";
        ObjectIdentityPredicate oidp = (ObjectIdentityPredicate) scope.get(0);
        return oidp.toString();
    }

    private String getFolderLabel() {
        if (scope.size() != 1  || !(scope.get(0) instanceof FolderPredicate)) return "";
        FolderPredicate fp = (FolderPredicate) scope.get(0);
        return fp.getFolder().getName();
    }

    private void enableDisable() {
        specificFindButton.setEnabled(specificRadioButton.isSelected());
        specificText.setText(specificRadioButton.isSelected() ? getSpecificLabel() : "");
        specificText.setEnabled(specificRadioButton.isSelected());

        attrNameLabel.setEnabled(attributeRadioButton.isSelected());
        attrValueLabel.setEnabled(attributeRadioButton.isSelected());
        attrNamesList.setEnabled(attributeRadioButton.isSelected());
        attrValue.setEnabled(attributeRadioButton.isSelected());

        folderFindButton.setEnabled(folderRadioButton.isSelected());
        folderText.setText(folderRadioButton.isSelected() ? getFolderLabel() : "");
        folderText.setEnabled(folderRadioButton.isSelected());
        transitiveCheckBox.setEnabled(folderRadioButton.isSelected());
    }

    void ok() {
        if (attributeRadioButton.isSelected()) {
            addAttributePredicate();
        }
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
