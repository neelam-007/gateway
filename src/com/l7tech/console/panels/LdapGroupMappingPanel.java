package com.l7tech.console.panels;

import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.GroupMappingConfig;
import com.l7tech.identity.ldap.MemberStrategy;
import com.l7tech.console.util.SortedListModel;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.*;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class LdapGroupMappingPanel extends WizardStepPanel {

    /** Creates new form ServicePanel */
    public LdapGroupMappingPanel(WizardStepPanel next) {
        super(next);
        initResources();
        initComponents();
    }


    /**
     * Loads locale-specific resources: strings  etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        //todo: change the property file from IdentityProviderDialog to LdapIdentityProviderConfigPanel
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.IdentityProviderDialog", locale);
    }

    /** @return the wizard step label    */
    public String getStepLabel() {
        return "Group ObjectClass mappings";
    }

    public void updateListModel(GroupMappingConfig groupMapping) {

        if(groupMapping != null) {
            groupMapping.setObjClass(objectClass.getText());
            groupMapping.setNameAttrName(nameAttribute.getText());
            groupMapping.setMemberAttrName(memberAttribute.getText());

            MemberStrategy ms = new MemberStrategy();
            ms.setVal(memberStrategy.getSelectedIndex());
            groupMapping.setMemberStrategy(ms);
        }
    }

    public void readSettings(Object settings) throws IllegalArgumentException {

        if (settings instanceof LdapIdentityProviderConfig) {

            iProviderConfig = (LdapIdentityProviderConfig) settings;

            if (iProviderConfig.getOid() != -1) {

                GroupMappingConfig[] groupMappings = iProviderConfig.getGroupMappings();

                // clear the model
                getGroupListModel().clear();

                for (int i = 0; i < groupMappings.length; i++) {

                    // update the user list display
                    getGroupListModel().add(groupMappings[i]);
                }

                // select the first row for display of attributes
                if(getGroupListModel().getSize() > 0) {
                    getGroupList().setSelectedIndex(0);
                }
            }
        }
    }

    public void storeSettings(Object settings) {

        Object groupMapping = null;

        // store the current record if selected
        if((groupMapping = getGroupList().getSelectedValue()) != null) {
             updateListModel((GroupMappingConfig) groupMapping);
        }

        if (settings instanceof LdapIdentityProviderConfig) {

            SortedListModel dataModel = getGroupListModel();

            GroupMappingConfig[] groupMappings = new GroupMappingConfig[dataModel.getSize()];

            for (int i = 0; i < dataModel.getSize(); i++) {
                groupMappings[i] = (GroupMappingConfig) dataModel.getElementAt(i);
            }
            ((LdapIdentityProviderConfig) settings).setGroupMappings(groupMappings);
        }
    }

    private void readSelectedGroupSettings(Object settings) {

        if (settings instanceof GroupMappingConfig) {

            GroupMappingConfig groupMapping = (GroupMappingConfig) settings;
            objectClass.setText(groupMapping.getObjClass());
            nameAttribute.setText(groupMapping.getNameAttrName());
            memberAttribute.setText(groupMapping.getMemberAttrName());
            memberStrategy.setSelectedIndex(groupMapping.getMemberStrategy().getVal());
            originalObjectClass = groupMapping.getObjClass();
        }
    }

    private JButton getAddButton() {
        if (addButton != null) return addButton;

        addButton = new JButton();
        addButton.setText("Add");

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                if (getGroupList().getSelectedValue() != null) {
                    updateListModel(lastSelectedGroup);
                }

                // create a new group mapping
                GroupMappingConfig newEntry = new GroupMappingConfig();
                newEntry.setObjClass("untitled" + ++nameIndex);
                newEntry.setMemberStrategy(MemberStrategy.MEMBERS_ARE_DN);
                getGroupListModel().add(newEntry);

                getGroupList().setSelectedValue(newEntry, false);

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
        objectClass.setPreferredSize(new java.awt.Dimension(170, 20));

        final JPanel thisPanel = this;
        objectClass.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent ke) {
                // don't care
            }

            public void keyReleased(KeyEvent ke) {
                GroupMappingConfig currentEntry = (GroupMappingConfig) getGroupList().getSelectedValue();

                if (currentEntry == null) {
                    // tell user to press the addButton first to add an entry
                    JOptionPane.showMessageDialog(thisPanel, resources.getString("add.entry.required"),
                            resources.getString("add.error.title"),
                            JOptionPane.ERROR_MESSAGE);
                } else {

                    if (objectClass.getText().length() == 0) {
                        currentEntry.setObjClass(originalObjectClass);
                        objectClass.setText(originalObjectClass);
                    } else {
                        currentEntry.setObjClass(objectClass.getText());
                    }
                    getGroupList().setSelectedValue(currentEntry, false);
                }
            }

            public void keyTyped(KeyEvent ke) {
                // don't care
            }
        });

        return objectClass;

    }

    private JList getGroupList() {
        if (groupList != null) return groupList;

        groupList = new javax.swing.JList(getGroupListModel());

        groupList.setFont(new java.awt.Font("Dialog", 0, 12));
        groupList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        groupList.setMaximumSize(new java.awt.Dimension(300, 400));
        groupList.setMinimumSize(new java.awt.Dimension(150, 250));
        groupList.setCellRenderer(renderer);

        groupList.getSelectionModel().
                addListSelectionListener(new ListSelectionListener() {
                    /**
                     * Called whenever the value of the selection changes.
                     * @param e the event that characterizes the change.
                     */
                    public void valueChanged(ListSelectionEvent e) {
                        Object selectedGroup = groupList.getSelectedValue();

                        if (selectedGroup != null)
                        {
                             // save the changes in the data model
                             updateListModel(lastSelectedGroup);

                             readSelectedGroupSettings(selectedGroup);
                        }

                        lastSelectedGroup = (GroupMappingConfig) selectedGroup;
                    }
                });

        return groupList;
    }

    /**
     * Create if needed a default list model
     *
     * @return SortedListModel
     */
    private SortedListModel getGroupListModel() {
        if (groupListModel != null) return groupListModel;

        groupListModel =
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
                        GroupMappingConfig e1 = (GroupMappingConfig) o1;
                        GroupMappingConfig e2 = (GroupMappingConfig) o2;

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
        return groupListModel;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        groupPanel = new javax.swing.JPanel();
        groupListPanel = new javax.swing.JPanel();
        groupListScrollPane = new javax.swing.JScrollPane();
        groupListTitle = new javax.swing.JLabel();
        groupActionPanel = new javax.swing.JPanel();
        groupAttributesPanel = new javax.swing.JPanel();
        attributeTitleLabel = new javax.swing.JLabel();
        nameAttributeLabel = new javax.swing.JLabel();
        memberAttributeLabel = new javax.swing.JLabel();
        nameAttribute = new javax.swing.JTextField();
        memberAttribute = new javax.swing.JTextField();
        memberStrategyAttributeLabel = new javax.swing.JLabel();
        memberStrategy = new javax.swing.JComboBox();
        valueTitleLabel = new javax.swing.JLabel();
        mappingTitle = new javax.swing.JLabel();
        objectClassLabel = new javax.swing.JLabel();

        groupPanel.setLayout(new java.awt.BorderLayout());

        groupPanel.setMinimumSize(new java.awt.Dimension(450, 300));
        groupPanel.setPreferredSize(new java.awt.Dimension(500, 300));
        groupListPanel.setLayout(new java.awt.BorderLayout());

        groupListPanel.setMinimumSize(new java.awt.Dimension(102, 30));
        groupListPanel.setPreferredSize(new java.awt.Dimension(180, 300));

        groupListScrollPane.setViewportView(getGroupList());

        groupListPanel.add(groupListScrollPane, java.awt.BorderLayout.CENTER);

        groupListTitle.setText("Group Object Class List");
        groupListTitle.setMaximumSize(new java.awt.Dimension(102, 40));
        groupListTitle.setMinimumSize(new java.awt.Dimension(102, 30));
        groupListTitle.setPreferredSize(new java.awt.Dimension(102, 40));
        groupListPanel.add(groupListTitle, java.awt.BorderLayout.NORTH);

        groupPanel.add(groupListPanel, java.awt.BorderLayout.WEST);

        groupActionPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        groupActionPanel.setMinimumSize(new java.awt.Dimension(400, 36));
        groupActionPanel.setPreferredSize(new java.awt.Dimension(400, 36));

        groupActionPanel.add(getAddButton());
        groupActionPanel.add(getRemoveButton());

        groupPanel.add(groupActionPanel, java.awt.BorderLayout.SOUTH);

        groupAttributesPanel.setLayout(new java.awt.GridBagLayout());

        groupAttributesPanel.setFont(new java.awt.Font("Dialog", 1, 12));
        groupAttributesPanel.setMaximumSize(new java.awt.Dimension(800, 800));
        groupAttributesPanel.setMinimumSize(new java.awt.Dimension(300, 300));
        groupAttributesPanel.setPreferredSize(new java.awt.Dimension(350, 300));
        attributeTitleLabel.setText("Attribute");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(attributeTitleLabel, gridBagConstraints);

        nameAttributeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        nameAttributeLabel.setText("Name:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(nameAttributeLabel, gridBagConstraints);

        memberAttributeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        memberAttributeLabel.setText("Member:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(memberAttributeLabel, gridBagConstraints);

        nameAttribute.setToolTipText("Name Attribute Name");
        nameAttribute.setMinimumSize(new java.awt.Dimension(120, 20));
        nameAttribute.setPreferredSize(new java.awt.Dimension(170, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(nameAttribute, gridBagConstraints);

        memberAttribute.setPreferredSize(new java.awt.Dimension(170, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(memberAttribute, gridBagConstraints);

        memberStrategyAttributeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        memberStrategyAttributeLabel.setText("Member Strategy:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(memberStrategyAttributeLabel, gridBagConstraints);

        memberStrategy.setFont(new java.awt.Font("Dialog", 0, 12));
        memberStrategy.setModel(new javax.swing.DefaultComboBoxModel(new String[]{"MEMBERS_ARE_DN", "MEMBERS_ARE_LOGIN", "MEMBERS_ARE_NVPAIR", "MEMBERS_BY_OU"}));
        memberStrategy.setPreferredSize(new java.awt.Dimension(170, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(memberStrategy, gridBagConstraints);

        valueTitleLabel.setText("Value");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(valueTitleLabel, gridBagConstraints);

        mappingTitle.setFont(new java.awt.Font("Dialog", 0, 12));
        mappingTitle.setText("Attribute Mapping:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(25, 0, 8, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(mappingTitle, gridBagConstraints);

        objectClassLabel.setText("Object Class");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(objectClassLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(getObjectClassField(), gridBagConstraints);

        groupPanel.add(groupAttributesPanel, java.awt.BorderLayout.CENTER);

        add(groupPanel);
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
            GroupMappingConfig gmc = (GroupMappingConfig) value;
            setText(gmc.getObjClass());

            return this;
        }
    };

    // Variables declaration - do not modify
    private javax.swing.JButton addButton;
    private javax.swing.JButton removeButton;
    private javax.swing.JLabel objectClassLabel;
    private javax.swing.JLabel valueTitleLabel;
    private javax.swing.JLabel attributeTitleLabel;
    private javax.swing.JPanel groupActionPanel;
    private javax.swing.JPanel groupAttributesPanel;
    private javax.swing.JList groupList;
    private javax.swing.JPanel groupListPanel;
    private javax.swing.JScrollPane groupListScrollPane;
    private javax.swing.JLabel groupListTitle;
    private javax.swing.JPanel groupPanel;
    private javax.swing.JLabel mappingTitle;
    private javax.swing.JLabel memberAttributeLabel;
    private javax.swing.JLabel memberStrategyAttributeLabel;
    private javax.swing.JLabel nameAttributeLabel;

    private javax.swing.JTextField memberAttribute;
    private javax.swing.JTextField nameAttribute;
    private javax.swing.JTextField objectClass;
    private javax.swing.JComboBox memberStrategy;

    private LdapIdentityProviderConfig iProviderConfig = null;
    private SortedListModel groupListModel = null;
    private static int nameIndex = 0;
    private String originalObjectClass = "";
    private GroupMappingConfig lastSelectedGroup = null;
    private ResourceBundle resources = null;
}
