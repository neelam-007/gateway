package com.l7tech.console.panels;

import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.UserMappingConfig;
import com.l7tech.identity.ldap.PasswdStrategy;
import com.l7tech.console.util.SortedListModel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.Vector;
import java.util.Comparator;

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class LdapUserMappingPanel extends WizardStepPanel {

    /** Creates new form ServicePanel */
    public LdapUserMappingPanel(WizardStepPanel next) {
        super(next);

        initComponents();

    }

    /** @return the wizard step label    */
    public String getStepLabel() {
        return "User ObjectClass mappings";
    }

    public void updateListModel(UserMappingConfig userMapping) {

        if(userMapping != null) {
            userMapping.setObjClass(objectClass.getText());
            userMapping.setNameAttrName(nameAttribute.getText());
            userMapping.setEmailNameAttrName(emailAttribute.getText());
            userMapping.setFirstNameAttrName(firstNameAttribute.getText());
            userMapping.setLastNameAttrName(lastNameAttribute.getText());
            userMapping.setLoginAttrName(loginNameAttribute.getText());
            userMapping.setPasswdAttrName(passwordAttribute.getText());

            PasswdStrategy ps = new PasswdStrategy();
            ps.setVal(passwordStrategyAttribute.getSelectedIndex());
            userMapping.setPasswdType(ps);
        }
    }

    public void readSettings(Object settings) throws IllegalArgumentException {

        if (settings instanceof LdapIdentityProviderConfig) {

            iProviderConfig = (LdapIdentityProviderConfig) settings;

            if (iProviderConfig.getOid() != -1) {

                UserMappingConfig[] userMappings = iProviderConfig.getUserMappings();

                // clear the model
                getUserListModel().clear();

                for (int i = 0; i < userMappings.length; i++) {

                    // update the user list display
                    getUserListModel().add(userMappings[i]);
                }

                // select the first row for display of attributes
                if(getUserListModel().getSize() > 0) {
                    getUserList().setSelectedIndex(0);
                }
            }
        }
    }

    public void storeSettings(Object settings) {

        Object userMapping = null;

        // store the current record if selected
        if((userMapping = getUserList().getSelectedValue()) != null) {
             updateListModel((UserMappingConfig) userMapping);
        }

        if (settings instanceof LdapIdentityProviderConfig) {

            SortedListModel dataModel = getUserListModel();
            UserMappingConfig[] userMappings = new UserMappingConfig[dataModel.getSize()];

            for (int i = 0; i < dataModel.getSize(); i++) {
                userMappings[i] = (UserMappingConfig) dataModel.getElementAt(i);
            }
            ((LdapIdentityProviderConfig) settings).setUserMappings(userMappings);
        }
    }

    private void readSelectedUserSettings(Object settings) {

        if (settings instanceof UserMappingConfig) {

            UserMappingConfig userMapping = (UserMappingConfig) settings;
            emailAttribute.setText(userMapping.getEmailNameAttrName());
            firstNameAttribute.setText(userMapping.getFirstNameAttrName());
            lastNameAttribute.setText(userMapping.getLastNameAttrName());
            loginNameAttribute.setText(userMapping.getLoginAttrName());
            nameAttribute.setText(userMapping.getNameAttrName());
            objectClass.setText(userMapping.getObjClass());
            passwordAttribute.setText(userMapping.getPasswdAttrName());
            passwordStrategyAttribute.setSelectedIndex(userMapping.getPasswdType().getVal());

            originalObjectClass = userMapping.getObjClass();
        }
    }

    private JButton getAddButton() {
        if (addButton != null) return addButton;

        addButton = new JButton();
        addButton.setText("Add");

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                 if (getUserList().getSelectedValue() != null) {
                     updateListModel(lastSelectedUser);
                 }

                // create a new user mapping
                UserMappingConfig newEntry = new UserMappingConfig();
                newEntry.setObjClass("untitled" + ++nameIndex);
                newEntry.setPasswdType(PasswdStrategy.CLEAR);
                getUserListModel().add(newEntry);

                getUserList().setSelectedValue(newEntry, false);
            }
        });

        return addButton;
    }

    private JButton getRemoveButton() {
        if (removeButton != null) return removeButton;

        removeButton = new JButton();
        removeButton.setText("Remove");
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

            }
        });

        return removeButton;
    }

    private JTextField getObjectClassField() {
        if (objectClass != null) return objectClass;

        objectClass = new JTextField();
        objectClass.setPreferredSize(new java.awt.Dimension(150, 20));

        objectClass.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent ke) {
                // don't care
            }

            public void keyReleased(KeyEvent ke) {
                UserMappingConfig currentEntry = (UserMappingConfig) getUserList().getSelectedValue();


                if (objectClass.getText().length() == 0) {
                    currentEntry.setObjClass(originalObjectClass);
                    objectClass.setText(originalObjectClass);
                } else {
                    currentEntry.setObjClass(objectClass.getText());
                }
                getUserList().setSelectedValue(currentEntry, false);
            }

            public void keyTyped(KeyEvent ke) {
                // don't care
            }
        });

        return objectClass;

    }

    private JList getUserList() {
        if (userList != null) return userList;

        userList = new javax.swing.JList(getUserListModel());
        userList.setFont(new java.awt.Font("Dialog", 0, 12));
        userList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        userList.setMaximumSize(new java.awt.Dimension(300, 400));
        userList.setMinimumSize(new java.awt.Dimension(150, 250));
        userList.setCellRenderer(renderer);

        userList.getSelectionModel().
                addListSelectionListener(new ListSelectionListener() {
                    /**
                     * Called whenever the value of the selection changes.
                     * @param e the event that characterizes the change.
                     */
                    public void valueChanged(ListSelectionEvent e) {
                        Object selectedUser = userList.getSelectedValue();

                        if (selectedUser != null) {
                            // save the changes in the data model
                             updateListModel(lastSelectedUser);

                            readSelectedUserSettings(selectedUser);
                        }

                        lastSelectedUser = (UserMappingConfig) selectedUser;
                    }
                });

        return userList;
    }

    /**
     * Create if needed a default list model
     *
     * @return SortedListModel
     */
    private SortedListModel getUserListModel() {
        if (userListModel != null) return userListModel;

        userListModel =
                new SortedListModel(new Comparator() {
                    /**
                     * Compares user objectclass mapping by objectclass alphabetically.
                     * @param o1 the first object to be compared.
                     * @param o2 the second object to be compared.
                     * @return a negative integer, zero, or a positive integer as the
                     * 	       first argument is less than, equal to, or greater than the
                     *	       second.
                     * @throws ClassCastException if the arguments' types prevent them from
                     * 	       being compared by this Comparator.
                     */
                    public int compare(Object o1, Object o2) {
                        UserMappingConfig e1 = (UserMappingConfig) o1;
                        UserMappingConfig e2 = (UserMappingConfig) o2;

                        return e1.getObjClass().compareTo(e2.getObjClass());
                    }
                });

        /*       userListModel.addListDataListener(new ListDataListener() {

                   public void intervalAdded(ListDataEvent e) {
                       if (!isLoading) {
                           groupPanel.setModified(true);
                           updateGroupMembers();
                       }
                   }


                   public void intervalRemoved(ListDataEvent e) {
                       if (!isLoading) {
                           groupPanel.setModified(true);
                           updateGroupMembers();
                       }
                   }


                   public void contentsChanged(ListDataEvent e) {
                       if (!isLoading) {
                           groupPanel.setModified(true);
                           updateGroupMembers();
                       }
                   }

                   private void updateGroupMembers() {
                       Set memberHeaders = groupPanel.getGroupMembers();
                       memberHeaders.clear();
                       for (int i = 0; i < userListModel.getSize(); i++) {
                           EntityHeader g = (EntityHeader) userListModel.getElementAt(i);
                           memberHeaders.add(g);
                       }
                   }

               });
       */
        return userListModel;
    }

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        userPanel = new javax.swing.JPanel();
        userListPanel = new javax.swing.JPanel();
        userListScrollPane = new javax.swing.JScrollPane();
        userListTitleLabel = new javax.swing.JLabel();
        userActionPanel = new javax.swing.JPanel();
        userAttributePanel = new javax.swing.JPanel();
        attributeTitleLabel = new javax.swing.JLabel();
        nameAttributeLabel = new javax.swing.JLabel();
        loginNameAttributeLabel = new javax.swing.JLabel();
        nameAttribute = new javax.swing.JTextField();
        loginNameAttribute = new javax.swing.JTextField();
        passwordAttributeLabel = new javax.swing.JLabel();
        passwordAttribute = new javax.swing.JTextField();
        firstNameAttributeLabel = new javax.swing.JLabel();
        firstNameAttribute = new javax.swing.JTextField();
        lastNameAttributeLabel = new javax.swing.JLabel();
        lastNameAttribute = new javax.swing.JTextField();
        emailAttributeLabel = new javax.swing.JLabel();
        emailAttribute = new javax.swing.JTextField();
        passwordStrategyAttributeLabel = new javax.swing.JLabel();
        passwordStrategyAttribute = new javax.swing.JComboBox();
        valueTitleLabel = new javax.swing.JLabel();
        mappingTitleLabel = new javax.swing.JLabel();
        objectClassLabel = new javax.swing.JLabel();

        userPanel.setLayout(new java.awt.BorderLayout());

        userPanel.setMinimumSize(new java.awt.Dimension(450, 300));
        userPanel.setPreferredSize(new java.awt.Dimension(500, 350));
        userListPanel.setLayout(new java.awt.BorderLayout());

        userListPanel.setMinimumSize(new java.awt.Dimension(102, 30));
        userListPanel.setPreferredSize(new java.awt.Dimension(180, 300));

        userListScrollPane.setViewportView(getUserList());

        userListPanel.add(userListScrollPane, java.awt.BorderLayout.CENTER);

        userListTitleLabel.setText("User Object Class List");
        userListTitleLabel.setMaximumSize(new java.awt.Dimension(102, 40));
        userListTitleLabel.setMinimumSize(new java.awt.Dimension(102, 30));
        userListTitleLabel.setPreferredSize(new java.awt.Dimension(102, 40));
        userListPanel.add(userListTitleLabel, java.awt.BorderLayout.NORTH);

        userPanel.add(userListPanel, java.awt.BorderLayout.WEST);

        userActionPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        userActionPanel.setMinimumSize(new java.awt.Dimension(400, 36));
        userActionPanel.setPreferredSize(new java.awt.Dimension(400, 36));

        userActionPanel.add(getAddButton());
        userActionPanel.add(getRemoveButton());

        userPanel.add(userActionPanel, java.awt.BorderLayout.SOUTH);

        userAttributePanel.setLayout(new java.awt.GridBagLayout());

        userAttributePanel.setFont(new java.awt.Font("Dialog", 1, 12));
        userAttributePanel.setMaximumSize(new java.awt.Dimension(800, 800));
        userAttributePanel.setMinimumSize(new java.awt.Dimension(300, 300));
        userAttributePanel.setPreferredSize(new java.awt.Dimension(350, 300));
        attributeTitleLabel.setText("Attribute");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        userAttributePanel.add(attributeTitleLabel, gridBagConstraints);

        nameAttributeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        nameAttributeLabel.setText("Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        userAttributePanel.add(nameAttributeLabel, gridBagConstraints);

        loginNameAttributeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        loginNameAttributeLabel.setText("Login Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        userAttributePanel.add(loginNameAttributeLabel, gridBagConstraints);

        nameAttribute.setToolTipText("Name Attribute Name");
        nameAttribute.setMinimumSize(new java.awt.Dimension(120, 20));
        nameAttribute.setPreferredSize(new java.awt.Dimension(150, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        userAttributePanel.add(nameAttribute, gridBagConstraints);

        loginNameAttribute.setPreferredSize(new java.awt.Dimension(150, 20));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        userAttributePanel.add(loginNameAttribute, gridBagConstraints);

        passwordAttributeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        passwordAttributeLabel.setText("Password:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        userAttributePanel.add(passwordAttributeLabel, gridBagConstraints);

        passwordAttribute.setPreferredSize(new java.awt.Dimension(150, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        userAttributePanel.add(passwordAttribute, gridBagConstraints);

        firstNameAttributeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        firstNameAttributeLabel.setText("First Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        userAttributePanel.add(firstNameAttributeLabel, gridBagConstraints);

        firstNameAttribute.setPreferredSize(new java.awt.Dimension(150, 20));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        userAttributePanel.add(firstNameAttribute, gridBagConstraints);

        lastNameAttributeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        lastNameAttributeLabel.setText("Last Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        userAttributePanel.add(lastNameAttributeLabel, gridBagConstraints);

        lastNameAttribute.setPreferredSize(new java.awt.Dimension(150, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        userAttributePanel.add(lastNameAttribute, gridBagConstraints);

        emailAttributeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        emailAttributeLabel.setText("Email:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        userAttributePanel.add(emailAttributeLabel, gridBagConstraints);

        emailAttribute.setPreferredSize(new java.awt.Dimension(150, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        userAttributePanel.add(emailAttribute, gridBagConstraints);

        passwordStrategyAttributeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        passwordStrategyAttributeLabel.setText("Password Strategy:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        userAttributePanel.add(passwordStrategyAttributeLabel, gridBagConstraints);

        passwordStrategyAttribute.setFont(new java.awt.Font("Dialog", 0, 12));
        passwordStrategyAttribute.setModel(new javax.swing.DefaultComboBoxModel(new String[]{"CLEAR", "HASHED"}));
        passwordStrategyAttribute.setPreferredSize(new java.awt.Dimension(150, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        userAttributePanel.add(passwordStrategyAttribute, gridBagConstraints);

        valueTitleLabel.setText("Value");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
        userAttributePanel.add(valueTitleLabel, gridBagConstraints);

        mappingTitleLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        mappingTitleLabel.setText("Attribute Mapping:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(25, 0, 8, 0);
        userAttributePanel.add(mappingTitleLabel, gridBagConstraints);

        objectClassLabel.setText("Object Class");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        userAttributePanel.add(objectClassLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
        userAttributePanel.add(getObjectClassField(), gridBagConstraints);

        userPanel.add(userAttributePanel, java.awt.BorderLayout.CENTER);

        add(userPanel);

    }


    private final ListCellRenderer renderer = new DefaultListCellRenderer() {
        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            if (isSelected) {
                this.setBackground(list.getSelectionBackground());
                this.setForeground(list.getSelectionForeground());
            } else {
                this.setBackground(list.getBackground());
                this.setForeground(list.getForeground());
            }
            this.setFont(new Font("Dialog", Font.PLAIN, 12));

            // Based on value type, determine cell contents
            UserMappingConfig umc = (UserMappingConfig) value;
            setText(umc.getObjClass());

            return this;
        }
    };

    // Variables declaration - do not modify

    private javax.swing.JLabel attributeTitleLabel;
    private javax.swing.JLabel firstNameAttributeLabel;
    private javax.swing.JLabel emailAttributeLabel;
    private javax.swing.JLabel lastNameAttributeLabel;
    private javax.swing.JLabel loginNameAttributeLabel;
    private javax.swing.JLabel mappingTitleLabel;
    private javax.swing.JLabel nameAttributeLabel;
    private javax.swing.JLabel objectClassLabel;
    private javax.swing.JLabel passwordAttributeLabel;
    private javax.swing.JLabel passwordStrategyAttributeLabel;
    private javax.swing.JLabel valueTitleLabel;

    private javax.swing.JTextField emailAttribute;
    private javax.swing.JTextField firstNameAttribute;
    private javax.swing.JTextField lastNameAttribute;
    private javax.swing.JTextField loginNameAttribute;
    private javax.swing.JTextField nameAttribute;
    private javax.swing.JTextField objectClass;
    private javax.swing.JTextField passwordAttribute;

    private javax.swing.JComboBox passwordStrategyAttribute;
    private javax.swing.JButton addButton;
    private javax.swing.JButton removeButton;
    private javax.swing.JPanel userActionPanel;
    private javax.swing.JPanel userAttributePanel;
    private javax.swing.JList userList;
    private javax.swing.JPanel userListPanel;
    private javax.swing.JScrollPane userListScrollPane;
    private javax.swing.JLabel userListTitleLabel;
    private javax.swing.JPanel userPanel;

    // End of variables declaration


    private LdapIdentityProviderConfig iProviderConfig = null;
    private Vector users = new Vector();
    private SortedListModel userListModel = null;
    private static int nameIndex = 0;
    private String originalObjectClass = "";

    private UserMappingConfig lastSelectedUser = null;
}


