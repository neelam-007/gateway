package com.l7tech.console.panels;

import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.UserMappingConfig;
import com.l7tech.identity.ldap.PasswdStrategy;
import com.l7tech.console.util.SortedListModel;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.Comparator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * This class provides a panel for users to add/delete/modify the LDAP attribute mapping of the user objectclass.
 *
 * <p>Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 *
 * $Id$
 */

public class LdapUserMappingPanel extends IdentityProviderStepPanel {

    static final Logger log = Logger.getLogger(LdapUserMappingPanel.class.getName());

    /**
     * Constructor - create a new user attribute mapping panel.
     *
     * @param next  The panel for use in the next step.
     */
    public LdapUserMappingPanel(WizardStepPanel next) {
        super(next);
        initResources();
        initComponents();
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        thisPanel = this;
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
        return "Map the attributes for each user objectclass in the LDAP Identity Provider.";
    }

    /** @return the wizard step label    */
    public String getStepLabel() {
        return "User Object Classes";
    }

    /**
     * Update the visual components of the panel with the new values.
     *
     * @param userMapping  The object contains the new values.
     */
    public void updateListModel(UserMappingConfig userMapping) {

        if(userMapping != null) {
            userMapping.setObjClass(objectClass.getText());
            userMapping.setNameAttrName(nameAttribute.getText());
            userMapping.setEmailNameAttrName(emailAttribute.getText());
            userMapping.setFirstNameAttrName(firstNameAttribute.getText());
            userMapping.setLastNameAttrName(lastNameAttribute.getText());
            userMapping.setLoginAttrName(loginNameAttribute.getText());
            userMapping.setPasswdAttrName(passwordAttribute.getText());

/*   Commented out the password strategry for the time being as the server does not handle it right now (Bugzilla #615)
            PasswdStrategy ps = new PasswdStrategy();
            ps.setVal(passwordStrategyAttribute.getSelectedIndex());
            userMapping.setPasswdType(ps);  */
        }
    }

    /**
     * Validate the input data
     *
     * @return  boolean  true if the input data is valid, false otherwise.
     */
    private boolean validateInput(String name) {

         boolean rc = true;

         Iterator itr = getUserListModel().iterator();
         while (itr.hasNext()) {
             Object o = itr.next();
             if (o instanceof UserMappingConfig) {
                 if (((UserMappingConfig) o).getObjClass().compareToIgnoreCase(name) == 0) {
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

            UserMappingConfig[] userMappings = iProviderConfig.getUserMappings();

            // clear the model
            getUserListModel().clear();

            for (int i = 0; i < userMappings.length; i++) {

                // update the user list display
                getUserListModel().add(userMappings[i]);
            }

            // select the first row for display of attributes
            if (getUserListModel().getSize() > 0) {
                if (lastSelectedUser != null) {

                    boolean found = false;
                    Object obj = null;
                    Iterator itr = getUserListModel().iterator();
                    while (itr.hasNext()) {
                        obj = itr.next();
                        if (obj instanceof UserMappingConfig) {
                            if (((UserMappingConfig) obj).getObjClass().equals(lastSelectedUser.getObjClass())) {
                                // the selected group found
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found) {
                        getUserList().setSelectedValue(obj, true);
                        lastSelectedUser = (UserMappingConfig) obj;
                    } else {
                        getUserList().setSelectedIndex(0);
                        lastSelectedUser = null;
                    }

                } else {
                    getUserList().setSelectedIndex(0);
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

    /**
     * Display the data of the selected user.
     *
     * @param settings   The data of the selected user.
     */
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

 /*   Commented out the password strategry for the time being as the server does not handle it right now (Bugzilla #615)
            passwordStrategyAttribute.setSelectedIndex(userMapping.getPasswdType().getVal());  */

        }
    }

    /**
     * Clear the display of the user mapping
     *
     */
    private void clearDisplay() {
        emailAttribute.setText("");
        firstNameAttribute.setText("");
        lastNameAttribute.setText("");
        loginNameAttribute.setText("");
        nameAttribute.setText("");
        objectClass.setText("");
        passwordAttribute.setText("");

        /*   Commented out the password strategry for the time being as the server does not handle it right now (Bugzilla #615)
        passwordStrategyAttribute.setSelectedIndex(0);  */
    }

    /**
     * Test whether the step panel allows testing the settings.
     *
     * @return true if the panel is valid, false otherwis
     */

    public boolean canTest() {
        return true;
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
        addButton.setToolTipText("Add a new user object class");

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
                getUserList().setSelectedValue(newEntry, true);
                enableUserMappingTextFields(true);
            }
        });

        return addButton;
    }

    private void enableUserMappingTextFields(boolean enable) {
        emailAttribute.setEnabled(enable);
        firstNameAttribute.setEnabled(enable);
        lastNameAttribute.setEnabled(enable);
        loginNameAttribute.setEnabled(enable);
        nameAttribute.setEnabled(enable);
        objectClass.setEnabled(enable);
        passwordAttribute.setEnabled(enable);
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
        removeButton.setToolTipText("Remove the selected user object class");

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object o = getUserList().getSelectedValue();
                 if(o != null) {
                     // remove the item from the data model
                     getUserListModel().removeElement(o);

                     // clear the setting as it doesn't exist any more
                     lastSelectedUser = null;

                     getUserList().getSelectionModel().clearSelection();

                     // select the first item for display
                    if (getUserListModel().getSize() > 0) {
                        getUserList().setSelectedIndex(0);
                    } else {
                        clearDisplay();
                        // gray out the fields
                        enableUserMappingTextFields(false);
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
        objectClass.setPreferredSize(new java.awt.Dimension(150, 20));
        objectClass.setToolTipText(resources.getString("objectClassNameTextField.tooltip"));

        objectClass.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent ke) {
                // don't care
            }

            public void keyReleased(KeyEvent ke) {
                UserMappingConfig currentEntry = (UserMappingConfig) getUserList().getSelectedValue();

                if (currentEntry == null) {
                    // tell user to press the addButton first to add an entry
                    JOptionPane.showMessageDialog(thisPanel, resources.getString("add.entry.required"),
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
                                JOptionPane.showMessageDialog(thisPanel, resources.getString("add.entry.duplicated"),
                                        resources.getString("add.error.title"),
                                        JOptionPane.ERROR_MESSAGE);

                                objectClass.setText(currentEntry.getObjClass());
                            } else {

                                // remove the old entry
                                getUserListModel().removeElement(currentEntry);

                                // modify the object class name
                                currentEntry.setObjClass(objectClass.getText());

                                // add the new entry
                                getUserListModel().add(currentEntry);
                            }

                            getUserList().setSelectedValue(currentEntry, true);
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
     * A JList for the user objects.
     *
     * @return  JList   The list of user objects.
     */
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
     * The data model of the userobjects.
     *
     * @return SortedListModel  The data model for the user objects.
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

        return userListModel;
    }

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        mainPanel = new javax.swing.JPanel();
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


        /*   Commented out the password strategry for the time being as the server does not handle it right now (Bugzilla #615)
        passwordStrategyAttributeLabel = new javax.swing.JLabel();
        passwordStrategyAttribute = new javax.swing.JComboBox();  */

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

        userListTitleLabel.setText("User Object Classes");
        userListTitleLabel.setMaximumSize(new java.awt.Dimension(102, 40));
        userListTitleLabel.setMinimumSize(new java.awt.Dimension(102, 30));
        userListTitleLabel.setPreferredSize(new java.awt.Dimension(102, 40));
        userListPanel.add(userListTitleLabel, java.awt.BorderLayout.NORTH);

        userPanel.add(userListPanel, java.awt.BorderLayout.WEST);

        userActionPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        userActionPanel.setBorder(new EtchedBorder());

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
        nameAttributeLabel.setText(resources.getString("userNameAttributeTextField.label"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        userAttributePanel.add(nameAttributeLabel, gridBagConstraints);

        loginNameAttributeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        loginNameAttributeLabel.setText(resources.getString("loginNameAttributeTextField.label"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        userAttributePanel.add(loginNameAttributeLabel, gridBagConstraints);

        nameAttribute.setToolTipText(resources.getString("userNameAttributeTextField.tooltip"));
        nameAttribute.setMinimumSize(new java.awt.Dimension(120, 20));
        nameAttribute.setPreferredSize(new java.awt.Dimension(150, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        userAttributePanel.add(nameAttribute, gridBagConstraints);

        loginNameAttribute.setPreferredSize(new java.awt.Dimension(150, 20));
        loginNameAttribute.setToolTipText(resources.getString("loginNameAttributeTextField.tooltip"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        userAttributePanel.add(loginNameAttribute, gridBagConstraints);

        passwordAttributeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        passwordAttributeLabel.setText(resources.getString("passwordAttributeTextField.label"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        userAttributePanel.add(passwordAttributeLabel, gridBagConstraints);

        passwordAttribute.setPreferredSize(new java.awt.Dimension(150, 20));
        passwordAttribute.setToolTipText(resources.getString("passwordAttributeTextField.tooltip"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        userAttributePanel.add(passwordAttribute, gridBagConstraints);

        firstNameAttributeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        firstNameAttributeLabel.setText(resources.getString("firstNameAttributeTextField.label"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        userAttributePanel.add(firstNameAttributeLabel, gridBagConstraints);

        firstNameAttribute.setPreferredSize(new java.awt.Dimension(150, 20));
        firstNameAttribute.setToolTipText(resources.getString("firstNameAttributeTextField.tooltip"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        userAttributePanel.add(firstNameAttribute, gridBagConstraints);

        lastNameAttributeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        lastNameAttributeLabel.setText(resources.getString("lastNameAttributeTextField.label"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        userAttributePanel.add(lastNameAttributeLabel, gridBagConstraints);

        lastNameAttribute.setPreferredSize(new java.awt.Dimension(150, 20));
        lastNameAttribute.setToolTipText(resources.getString("lastNameAttributeTextField.tooltip"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        userAttributePanel.add(lastNameAttribute, gridBagConstraints);

        emailAttributeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        emailAttributeLabel.setText(resources.getString("emailAttributeTextField.label"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        userAttributePanel.add(emailAttributeLabel, gridBagConstraints);

        emailAttribute.setPreferredSize(new java.awt.Dimension(150, 20));
        emailAttribute.setToolTipText(resources.getString("emailAttributeTextField.tooltip"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        userAttributePanel.add(emailAttribute, gridBagConstraints);

        /*   Commented out the password strategry for the time being as the server does not handle it right now (Bugzilla #615)
        passwordStrategyAttributeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        passwordStrategyAttributeLabel.setText(resources.getString("passwordStrategyAttributeTextField.label"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        userAttributePanel.add(passwordStrategyAttributeLabel, gridBagConstraints);

        passwordStrategyAttribute.setFont(new java.awt.Font("Dialog", 0, 12));
        passwordStrategyAttribute.setModel(new javax.swing.DefaultComboBoxModel(new String[]{"CLEAR", "HASHED"}));
        passwordStrategyAttribute.setPreferredSize(new java.awt.Dimension(150, 20));
        passwordStrategyAttribute.setToolTipText(resources.getString("passwordStrategyAttributeTextField.tooltip"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
        userAttributePanel.add(passwordStrategyAttribute, gridBagConstraints);  */

        valueTitleLabel.setText("Mapped to");
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

        objectClassLabel.setText(resources.getString("objectClassNameTextField.Label"));
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
        userPanel.setBorder(new EtchedBorder());

        JPanel spacePanel = new JPanel();
        spacePanel.setPreferredSize(new java.awt.Dimension(500, 10));

        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(spacePanel, BorderLayout.SOUTH);
        mainPanel.add(userPanel, BorderLayout.CENTER);
    }

   /**
     *  A cell renderer for displaying the name of the user objectclass in JList component.
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
            UserMappingConfig umc = (UserMappingConfig) value;
            setText(umc.getObjClass());

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

                            UserMappingConfig currentEntry = (UserMappingConfig) getUserList().getSelectedValue();

                            if (currentEntry == null) {
                                log.severe("Internal error: No LDAP object class entry is selected");
                            } else {
                                if (ev.getActionCommand().compareToIgnoreCase(currentEntry.getObjClass()) != 0) {
                                    if (!validateInput(ev.getActionCommand())) {
                                        JOptionPane.showMessageDialog(thisPanel, resources.getString("add.entry.duplicated"),
                                                resources.getString("add.error.title"),
                                                JOptionPane.ERROR_MESSAGE);
                                        objectClass.setText(currentEntry.getObjClass());
                                    } else {
                                        // populate the name to the object class field
                                        objectClass.setText(ev.getActionCommand());

                                        // remove the old entry
                                        getUserListModel().removeElement(currentEntry);

                                        // modify the object class name
                                        currentEntry.setObjClass(ev.getActionCommand());

                                        // add the new entry
                                        getUserListModel().add(currentEntry);
                                    }
                                }

                                // select the new entry
                                getUserList().setSelectedValue(currentEntry, true);
                            }
                        }
                    });
                }
            };

    private javax.swing.JLabel attributeTitleLabel;
    private javax.swing.JLabel firstNameAttributeLabel;
    private javax.swing.JLabel emailAttributeLabel;
    private javax.swing.JLabel lastNameAttributeLabel;
    private javax.swing.JLabel loginNameAttributeLabel;
    private javax.swing.JLabel mappingTitleLabel;
    private javax.swing.JLabel nameAttributeLabel;
    private javax.swing.JLabel objectClassLabel;
    private javax.swing.JLabel passwordAttributeLabel;
    private javax.swing.JLabel valueTitleLabel;

    private javax.swing.JTextField emailAttribute;
    private javax.swing.JTextField firstNameAttribute;
    private javax.swing.JTextField lastNameAttribute;
    private javax.swing.JTextField loginNameAttribute;
    private javax.swing.JTextField nameAttribute;
    private javax.swing.JTextField objectClass;
    private javax.swing.JTextField passwordAttribute;

    /*   Commented out the password strategry for the time being as the server does not handle it right now (Bugzilla #615)
    private javax.swing.JLabel passwordStrategyAttributeLabel;
    private javax.swing.JComboBox passwordStrategyAttribute;   */

    private javax.swing.JButton addButton;
    private javax.swing.JButton removeButton;
    private javax.swing.JPanel userActionPanel;
    private javax.swing.JPanel userAttributePanel;
    private javax.swing.JList userList;
    private javax.swing.JPanel userListPanel;
    private javax.swing.JScrollPane userListScrollPane;
    private javax.swing.JLabel userListTitleLabel;
    private javax.swing.JPanel userPanel;
    private javax.swing.JPanel mainPanel;

    private LdapIdentityProviderConfig iProviderConfig = null;
    private SortedListModel userListModel = null;
    private static int nameIndex = 0;
//    private String originalObjectClass = "";

    private ResourceBundle resources = null;
    private UserMappingConfig lastSelectedUser = null;

    private final JPanel thisPanel;
}


