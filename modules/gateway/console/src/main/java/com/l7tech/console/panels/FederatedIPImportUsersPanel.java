package com.l7tech.console.panels;

import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.fed.FederatedGroup;
import com.l7tech.identity.fed.FederatedUser;

import javax.swing.*;
import java.util.*;
import java.awt.*;

/**
 * A panel that is shown when the user is creating a new Federated Identity Provider. This is only shown
 * if the user is creating a new Federated Identity Provider as part of a policy import. The user uses
 * this panel to choose whether they want to import the users and group from the foreign Federated Identity
 * Provider.
 */
public class FederatedIPImportUsersPanel extends WizardStepPanel {
    private static class GroupListElement {
        private FederatedGroup group;

        public GroupListElement(FederatedGroup group) {
            this.group = group;
        }

        public FederatedGroup getGroup() {
            return group;
        }

        public String toString() {
            return group.getName();
        }
    }

    private static class UserListElement {
        private FederatedUser user;

        public UserListElement(FederatedUser user) {
            this.user = user;
        }

        public FederatedUser getUser() {
            return user;
        }

        public String toString() {
            return user.getName();
        }
    }

    private static final String RES_STEP_TITLE = "importUsersGroups.step.label";
    private static final String RES_STEP_DESCRIPTION = "importUsersGroups.step.description";

    private JCheckBox importUsersCheckBox;
    private JCheckBox importGroupsCheckBox;
    private JList groupsList;
    private JList usersList;
    private JPanel mainPanel;

    private ResourceBundle resources;

    private HashMap<String, Set<String>> importedGroupMembership;

    /**
     * Constructor - create a new provider type panel.
     *
     * @param next  The panel for use in the next step.
     */
    public FederatedIPImportUsersPanel(WizardStepPanel next) {
        this(next, false);
    }

    /**
     * Constructor - create a new provider type panel.
     *
     * @param next  The panel for use in the next step.
     */
    public FederatedIPImportUsersPanel(WizardStepPanel next, boolean readOnly) {
        super(next);
        initResources();
        initComponents(readOnly);
    }

    /**
     * Loads locale-specific resources: strings  etc
     */
    private void initResources() {
        Locale locale = Locale.getDefault();
        resources = ResourceBundle.getBundle("com.l7tech.console.resources.IdentityProviderDialog", locale);
    }

    /**
     * Init UI, disable components if read-only
     */
    private void initComponents(final boolean readOnly) {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        importUsersCheckBox.setEnabled(!readOnly);
        importGroupsCheckBox.setEnabled(!readOnly);
    }

    /** @return the wizard step label    */
    public String getStepLabel() {
        return resources.getString(RES_STEP_TITLE);
    }

    /**
     * Provide the description for the step being taken on this panel.
     *
     * @return  String  The descritpion of the step.
     */
    public String getDescription() {
        return resources.getString(RES_STEP_DESCRIPTION);
    }

    /**
     * Populate the configuration data from the wizard input object to the visual components of the panel.
     *
     * @param settings  The current value of configuration items in the wizard input object.
     */
    @Override
    public void readSettings(Object settings) {
        if (settings != null) {
            if (settings instanceof FederatedIdentityProviderConfig) {

                FederatedIdentityProviderConfig fiProviderConfig = (FederatedIdentityProviderConfig) settings;

                DefaultListModel listModel = new DefaultListModel();
                for(FederatedGroup group : fiProviderConfig.getImportedGroups().values()) {
                    listModel.addElement(new GroupListElement(group));
                }
                groupsList.setModel(listModel);

                listModel = new DefaultListModel();
                for(FederatedUser user : fiProviderConfig.getImportedUsers().values()) {
                    listModel.addElement(new UserListElement(user));
                }
                usersList.setModel(listModel);

                importedGroupMembership = fiProviderConfig.getImportedGroupMembership();
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
    @Override
    public void storeSettings(Object settings) {
        if (!(settings instanceof FederatedIdentityProviderConfig))
            throw new IllegalArgumentException("The settings object must be FederatedIdentityProviderConfig");

        FederatedIdentityProviderConfig iProviderConfig = (FederatedIdentityProviderConfig) settings;

        HashMap<String, FederatedGroup> groupsToImport = new HashMap<String, FederatedGroup>();
        if(importGroupsCheckBox.isSelected()) {
            for(int i = 0;i < groupsList.getModel().getSize();i++) {
                GroupListElement groupListElement = (GroupListElement)groupsList.getModel().getElementAt(i);
                groupsToImport.put(groupListElement.getGroup().getId(), groupListElement.getGroup());
            }

            iProviderConfig.setImportedGroupMembership(importedGroupMembership);
        }
        iProviderConfig.setImportedGroups(groupsToImport);

        HashMap<String, FederatedUser> usersToImport = new HashMap<String, FederatedUser>();
        if(importUsersCheckBox.isSelected()) {
            for(int i = 0;i < usersList.getModel().getSize();i++) {
                UserListElement userListElement = (UserListElement)usersList.getModel().getElementAt(i);
                usersToImport.put(userListElement.getUser().getId(), userListElement.getUser());
            }
        }
        iProviderConfig.setImportedUsers(usersToImport);
    }
}
