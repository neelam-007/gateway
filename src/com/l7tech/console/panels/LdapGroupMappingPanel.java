package com.l7tech.console.panels;

import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.GroupMappingConfig;
import com.l7tech.identity.ldap.MemberStrategy;
import com.l7tech.console.util.SortedListModel;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.*;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * This class provides a panel for users to add/delete/modify the LDAP attribute mapping of the group bjectclass.
 *
 * <p>Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 *
 * $Id$
 */

public class LdapGroupMappingPanel extends IdentityProviderStepPanel {

    static final Logger log = Logger.getLogger(LdapGroupMappingPanel.class.getName());

    /**
     * Constructor - create a new group attribute mapping panel.
     *
     * @param next  The panel for use in the next step.
     */
    public LdapGroupMappingPanel(WizardStepPanel next) {
        super(next);
        initResources();
        initComponents();
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * Loads locale-specific resources: strings  etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();

        resources = ResourceBundle.getBundle("com.l7tech.console.resources.IdentityProviderDialog", locale);
    }

    /**
     * Provide the description for the step being taken on this panel.
     *
     * @return  String  The descritpion of the step.
     */
    public String getDescription() {
        return "Map the attributes for each group object class in the LDAP Identity Provider.";
    }

    /** @return the wizard step label    */
    public String getStepLabel() {
        return "Group Object Classes";
    }

    /**
     * Update the visual components of the panel with the new values.
     *
     * @param groupMapping  The object contains the new values.
     */
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

    /**
     * Validate the input data
     *
     * @param name  The object class name
     * @return  boolean  true if the input data is valid, false otherwise.
     */
    private boolean validateInput(String name) {

        boolean rc = true;

        Iterator itr = getGroupListModel().iterator();
        while (itr.hasNext()) {
            Object o = itr.next();
            if (o instanceof GroupMappingConfig) {
                if (((GroupMappingConfig) o).getObjClass().compareToIgnoreCase(name) == 0) {
                    rc = false;
                    break;
                }
            }
        }
        return rc;
    }

    /**
     * Populate the configuration data from the wizard input object to the visual components of the panel.
     *
     * @param settings  The current value of configuration items in the wizard input object.
     *
     * @throws IllegalArgumentException   if the data provided by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {

        if (settings instanceof LdapIdentityProviderConfig) {

            iProviderConfig = (LdapIdentityProviderConfig) settings;

            GroupMappingConfig[] groupMappings = iProviderConfig.getGroupMappings();

            // clear the model
            getGroupListModel().clear();

            for (int i = 0; i < groupMappings.length; i++) {

                // update the user list display
                getGroupListModel().add(groupMappings[i]);
            }

            // select the first row for display of attributes
            if (getGroupListModel().getSize() > 0) {

                if (lastSelectedGroup != null) {

                    Iterator itr = getGroupListModel().iterator();
                    boolean found = false;
                    Object obj = null;
                    while (itr.hasNext()) {
                        obj = itr.next();
                        if (obj instanceof GroupMappingConfig) {
                            if (((GroupMappingConfig) obj).getObjClass().equals(lastSelectedGroup.getObjClass())) {
                                // the selected group found
                                found = true;
                                break;
                            }
                        }
                    }
                    if(found) {
                        getGroupList().setSelectedValue(obj, true);
                        lastSelectedGroup = (GroupMappingConfig) obj;
                    } else {
                        getGroupList().setSelectedIndex(0);
                        lastSelectedGroup = null;
                    }
                } else {
                    getGroupList().setSelectedIndex(0);
                }
            }

        }
    }

    /**
     * Store the values of all fields on the panel to the wizard object which is a used for
     * keeping all the modified values. The wizard object will be used for providing the
     * updated values when updating the server.
     *
     * @param settings the object representing wizard panel state
     */
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

    /**
     * Display the data of the selected group.
     *
     * @param settings   The data of the selected group.
     */
    private void readSelectedGroupSettings(Object settings) {

        if (settings instanceof GroupMappingConfig) {

            GroupMappingConfig groupMapping = (GroupMappingConfig) settings;
            objectClass.setText(groupMapping.getObjClass());
            nameAttribute.setText(groupMapping.getNameAttrName());
            memberAttribute.setText(groupMapping.getMemberAttrName());
            memberStrategy.setSelectedIndex(groupMapping.getMemberStrategy().getVal());
        }
    }

    /**
     * Clear the display of the group mapping
     *
     */
    private void clearDisplay() {
            objectClass.setText("");
            nameAttribute.setText("");
            memberAttribute.setText("");
            memberStrategy.setSelectedIndex(0);
    }

        /**
     * Test whether the step panel allows testing the settings.
     *
     * @return true if the panel is valid, false otherwis
     */

    public boolean canTest() {
        return false;
    }

    /**
     * The button for adding the attribute mapping of a new group objectclass.
     *
     * @return JButton  The button for the add operation.
     */
    private JButton getAddButton() {
        if (addButton != null) return addButton;

        addButton = new JButton();
        addButton.setText("Add");
        addButton.setToolTipText("Add a new group object class");

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
                getGroupList().setSelectedValue(newEntry, true);
                enableGroupMappingTextFields(true);
            }
        });

        return addButton;
    }

    private void enableGroupMappingTextFields(boolean enable) {
        memberAttribute.setEnabled(enable);
        nameAttribute.setEnabled(enable);
        objectClass.setEnabled(enable);
        memberStrategy.setEnabled(enable);
    }

    /**
     * The button for deleting the attribute mapping of a new group objectclass.
     *
     * @return JButton  The button for the remove operation.
     */
    private JButton getRemoveButton() {
        if (removeButton != null) return removeButton;

        removeButton = new JButton();
        removeButton.setText("Remove");
        removeButton.setToolTipText("Remove the selected group object class");

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                Object o = getGroupList().getSelectedValue();
                if (o != null) {
                    // remove the item from the data model
                    getGroupListModel().removeElement(o);

                    // clear the setting as it doesn't exist any more
                    lastSelectedGroup = null;

                    getGroupList().getSelectionModel().clearSelection();

                    // select the first item for display
                    if (getGroupListModel().getSize() > 0) {
                        getGroupList().setSelectedIndex(0);
                    } else {
                        // clear the fields
                        clearDisplay();
                        // gray out the fields
                        enableGroupMappingTextFields(false);
                    }
                }
            }
        });

        return removeButton;
    }

    /**
     * The objectclass field component.
     *
     * @return JTextField  The text field for the objectclass.
     */
    private JTextField getObjectClassField() {
        if (objectClass != null) return objectClass;

        objectClass = new JTextField();
        objectClass.setPreferredSize(new java.awt.Dimension(170, 20));
        objectClass.setToolTipText(resources.getString("objectClassNameTextField.tooltip"));

        objectClass.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent ke) {
                // don't care
            }

            public void keyReleased(KeyEvent ke) {
                GroupMappingConfig currentEntry = (GroupMappingConfig) getGroupList().getSelectedValue();

                if (currentEntry == null) {
                    // tell user to press the addButton first to add an entry
                    JOptionPane.showMessageDialog(LdapGroupMappingPanel.this, resources.getString("add.entry.required"),
                            resources.getString("add.error.title"),
                            JOptionPane.ERROR_MESSAGE);
                } else {

                    if (objectClass.getText().length() == 0) {
                        // restore the value into objectClass field. this prevents the empty content in case of cancellation by user
                        objectClass.setText(currentEntry.getObjClass());

                        // create a dialog
                        EditLdapObjectClassNameDialog d = new EditLdapObjectClassNameDialog(TopComponents.getInstance().getMainWindow(), objectClassNameChangeListener, objectClass.getText());

                        // show the dialog
                        d.show();

                    } else {
                        if (objectClass.getText().compareToIgnoreCase(currentEntry.getObjClass()) != 0) {
                            if (!validateInput(objectClass.getText())) {
                                JOptionPane.showMessageDialog(LdapGroupMappingPanel.this, resources.getString("add.entry.duplicated"),
                                        resources.getString("add.error.title"),
                                        JOptionPane.ERROR_MESSAGE);

                                objectClass.setText(currentEntry.getObjClass());
                            } else {

                                // remove the old entry
                                getGroupListModel().removeElement(currentEntry);

                                // modify the object class name
                                currentEntry.setObjClass(objectClass.getText());

                                // add the new entry
                                getGroupListModel().add(currentEntry);
                            }

                            getGroupList().setSelectedValue(currentEntry, true);
                        }
                    }

                }
            }

            public void keyTyped(KeyEvent ke) {
                // don't care
            }
        });

        return objectClass;

    }

    /**
     * A JList for the group objects.
     *
     * @return  JList   The list of group objects.
     */
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

                        if (selectedGroup != null) {
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
     * The data model of the group objects.
     *
     * @return SortedListModel  The data model for the group objects.
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

        return groupListModel;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        mainPanel =  new javax.swing.JPanel();
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

        groupListTitle.setText("Group Object Classes");
        groupListTitle.setMaximumSize(new java.awt.Dimension(102, 40));
        groupListTitle.setMinimumSize(new java.awt.Dimension(102, 30));
        groupListTitle.setPreferredSize(new java.awt.Dimension(102, 40));
        groupListPanel.add(groupListTitle, java.awt.BorderLayout.NORTH);

        groupPanel.add(groupListPanel, java.awt.BorderLayout.WEST);

        groupActionPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        groupActionPanel.setBorder(new EtchedBorder());

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
        attributeTitleLabel.setText(resources.getString("attributeMappedNameTitle"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(attributeTitleLabel, gridBagConstraints);

        nameAttributeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        nameAttributeLabel.setText(resources.getString("groupNameAttributeTextField.label"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(nameAttributeLabel, gridBagConstraints);

        memberAttributeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        memberAttributeLabel.setText(resources.getString("groupMemberAttributeTextField.label"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(memberAttributeLabel, gridBagConstraints);

        nameAttribute.setToolTipText(resources.getString("groupNameAttributeTextField.tooltip"));
        nameAttribute.setMinimumSize(new java.awt.Dimension(120, 20));
        nameAttribute.setPreferredSize(new java.awt.Dimension(170, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(nameAttribute, gridBagConstraints);

        memberAttribute.setToolTipText(resources.getString("groupMemberAttributeTextField.tooltip"));
        memberAttribute.setPreferredSize(new java.awt.Dimension(170, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(memberAttribute, gridBagConstraints);

        memberStrategyAttributeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        memberStrategyAttributeLabel.setText(resources.getString("groupMemberStrategy.label"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(memberStrategyAttributeLabel, gridBagConstraints);

        memberStrategy.setToolTipText(resources.getString("groupMemberStrategy.tooltip"));
        memberStrategy.setFont(new java.awt.Font("Dialog", 0, 12));
        memberStrategy.setModel(new javax.swing.DefaultComboBoxModel(new String[]{"Member is User DN",
                                                                                  "Member is User Login",
                                                                                  "Member is NV Pair",
                                                                                  "OU Group"}));
        memberStrategy.setPreferredSize(new java.awt.Dimension(170, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(memberStrategy, gridBagConstraints);

        valueTitleLabel.setText(resources.getString("attributeMappedValueTitle"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(valueTitleLabel, gridBagConstraints);

        mappingTitle.setFont(new java.awt.Font("Dialog", 0, 12));
        mappingTitle.setText(resources.getString("attributeMappingTitle"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(25, 0, 8, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        groupAttributesPanel.add(mappingTitle, gridBagConstraints);

        objectClassLabel.setText(resources.getString("objectClassNameTextField.Label"));
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
        groupPanel.setBorder(new EtchedBorder());

        JPanel spacePanel = new JPanel();
        spacePanel.setPreferredSize(new java.awt.Dimension(500, 10));

        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(spacePanel, BorderLayout.SOUTH);
        mainPanel.add(groupPanel, BorderLayout.CENTER);
    }

    /**
     *  A cell renderer for displaying the name of the group objectclass in JList component.
     *
     **/
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

    private ActionListener
            objectClassNameChangeListener = new ActionListener() {
                /**
                 * Fired when an set of children is updated.
                 */
                public void actionPerformed(final ActionEvent ev) {
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {

                            GroupMappingConfig currentEntry = (GroupMappingConfig) getGroupList().getSelectedValue();

                            if (currentEntry == null) {
                                log.severe("Internal error: No LDAP object class entry is selected");
                            } else {
                                if (ev.getActionCommand().compareToIgnoreCase(currentEntry.getObjClass()) != 0) {
                                    if (!validateInput(ev.getActionCommand())) {
                                        JOptionPane.showMessageDialog(LdapGroupMappingPanel.this, resources.getString("add.entry.duplicated"),
                                                resources.getString("add.error.title"),
                                                JOptionPane.ERROR_MESSAGE);
                                        objectClass.setText(currentEntry.getObjClass());
                                    } else {
                                        // populate the name to the object class field
                                        objectClass.setText(ev.getActionCommand());

                                        // remove the old entry
                                        getGroupListModel().removeElement(currentEntry);

                                        // modify the object class name
                                        currentEntry.setObjClass(ev.getActionCommand());

                                        // add the new entry
                                        getGroupListModel().add(currentEntry);
                                    }
                                }

                                // select the new entry
                                getGroupList().setSelectedValue(currentEntry, true);
                            }
                        }
                    });
                }
            };

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
    private javax.swing.JPanel mainPanel;
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
    private GroupMappingConfig lastSelectedGroup = null;
    private ResourceBundle resources = null;

}
