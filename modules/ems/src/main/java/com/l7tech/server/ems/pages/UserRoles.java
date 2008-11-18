package com.l7tech.server.ems.pages;

import com.l7tech.server.ems.NavigationPage;
import com.l7tech.server.ems.EmsAccountManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleAssignment;
import com.l7tech.gateway.common.security.rbac.RbacUtilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.identity.internal.InternalUser;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.Serializable;
import java.text.MessageFormat;

/**
 * A page to manage a role for users such as assign a role to a user or unassign a user from a role.
 */
@NavigationPage(page="UserRoles",pageIndex=300,section="Settings",sectionIndex=200,pageUrl="UserRoles.html")
public class UserRoles extends EmsPage {
    private static final Logger logger = Logger.getLogger(UserRoles.class.getName());

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private RoleManager roleManager;

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private EmsAccountManager emsAccountManager;

    private RoleModel roleModel;
    private EnterpriseSearchUsersPanel.SearchUsersModel searchAssignedUsersModel;
    private EnterpriseSearchUsersPanel.SearchUsersModel searchUnassignedUsersModel;
    private WebMarkupContainer roleManagementContainer; // contains role description, assigned user table, and unassigned user table.
    private WebMarkupContainer warningDialogContainer;  // contains a warning dialog displaying a warning message during role update.

    /**
     * Constructor to create a select-a-role form and initialize two containers.
     */
    public UserRoles() {
        // Initialize all models
        initModels();

        // Create a form to select a role
        add(new SelectRoleForm("form.selectRole"));

        // Add headers to page early to fix issue with CSS ordering
        YuiDataTable.contributeHeaders( this );

        // Create a container to contain all components to manage roles for users.
        initRoleManagementContainer();
        add(roleManagementContainer);

        // Create a warning dialog, just in case where some errors occur during role updates.
        initWarningDialogContainer();
        add(warningDialogContainer);
    }

    /**
     * Initialize all models used in the page.
     */
    private void initModels() {
        roleModel = new RoleModel();
        searchAssignedUsersModel = new EnterpriseSearchUsersPanel.SearchUsersModel();
        searchUnassignedUsersModel = new EnterpriseSearchUsersPanel.SearchUsersModel();
    }

    /**
     * Initialize the role-management container.
     */
    private void initRoleManagementContainer() {
        roleManagementContainer = new WebMarkupContainer("container.roleManagement");
        final Form assignedUsersRoleForm = new Form("form.assignedUsers");
        final Form unassignedUsersRoleForm = new Form("form.unassignedUsers");

        // Step 1: create a textarea to display the description of the selected role.
        TextArea roleDescriptionTextArea = new TextArea("textarea.roleDescription", new PropertyModel(roleModel, "roleDescription"));
        roleDescriptionTextArea.setEscapeModelStrings(false);

        // Step 2: create components to unassign assigned users from a role.
        HiddenField hiddenFieldForAssignedUser = new HiddenField("assignedUserId", new Model(""));
        YuiAjaxButton buttonToUnassignRole = new YuiAjaxButton("button.unassignRole") {
            @Override
            protected void onSubmit(final AjaxRequestTarget ajaxRequestTarget, Form form) {
                final String userId = (String) form.get("assignedUserId").getModelObject();
                try {
                    Role role = roleManager.findByPrimaryKey(roleModel.selectedRole.getOid());
                    role.removeAssignedUser(emsAccountManager.findByLogin(userId));
                    roleManager.update(role);
                    roleModel.selectedRole = role;

                    // Render the role management container
                    this.setEnabled(false);
                    ajaxRequestTarget.addComponent(roleManagementContainer);
                } catch (FindException e) {
                    logger.warning("Cannot find the user, " + userId);
                } catch (UpdateException e) {
                    logger.warning("Cannot update the role, " + roleModel.selectedRole.getName());

                    Label confirmLabel = new Label(YuiDialog.getContentId(), e.getMessage());
                    YuiDialog dialog = new YuiDialog("dialog.warning.roleUnassignment", "Role Unassignment Warning",
                        YuiDialog.Style.CLOSE, confirmLabel, null);

                    warningDialogContainer.removeAll();
                    warningDialogContainer.add(dialog);
                    ajaxRequestTarget.addComponent(warningDialogContainer);
                }
            }
        };
        buttonToUnassignRole.setEnabled(false);
        Panel assignedUserTable = getUserTablePanel(true, hiddenFieldForAssignedUser,  buttonToUnassignRole);
        Panel searchAssignedUsersPanel = new EnterpriseSearchUsersPanel("panel.search.assignedUsers",
            searchAssignedUsersModel, Collections.singleton(assignedUserTable));

        assignedUsersRoleForm.add(buttonToUnassignRole);
        assignedUsersRoleForm.add(hiddenFieldForAssignedUser.setOutputMarkupId(true));
        assignedUsersRoleForm.add(assignedUserTable);
        assignedUsersRoleForm.add(searchAssignedUsersPanel);

        // Step 3: create components to assign a role to unassigned users
        HiddenField hiddenFieldForUnassignedUser = new HiddenField("unassignedUserId", new Model(""));
        YuiAjaxButton buttonToAssignRole = new YuiAjaxButton("button.assignRole") {
            @Override
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget, Form form) {
                final String userId = (String) form.get("unassignedUserId").getModelObject();
                try {
                    Role role = roleManager.findByPrimaryKey(roleModel.selectedRole.getOid());
                    role.addAssignedUser(emsAccountManager.findByLogin(userId));
                    roleManager.update(role);
                    roleModel.selectedRole = role;

                    // Render the role management container
                    this.setEnabled(false);
                    ajaxRequestTarget.addComponent(roleManagementContainer);
                } catch (FindException e) {
                    logger.warning("Cannot find the user, " + userId);
                } catch (UpdateException e) {
                    logger.warning("Cannot update the role, " + roleModel.selectedRole.getName());

                    Label confirmLabel = new Label(YuiDialog.getContentId(), e.getMessage());
                    YuiDialog dialog = new YuiDialog("dialog.warning.roleUnassignment", "Role Assignment Warning",
                        YuiDialog.Style.CLOSE, confirmLabel, null);

                    warningDialogContainer.removeAll();
                    warningDialogContainer.add(dialog);
                    ajaxRequestTarget.addComponent(warningDialogContainer);
                }
            }
        };
        buttonToAssignRole.setEnabled(false);
        Panel unassignedUserTable = getUserTablePanel(false, hiddenFieldForUnassignedUser, buttonToAssignRole);
        Panel searchUnassignedUsersPanel = new EnterpriseSearchUsersPanel("panel.search.unassignedUsers",
            searchUnassignedUsersModel, Collections.singleton(unassignedUserTable));

        unassignedUsersRoleForm.add(buttonToAssignRole);
        unassignedUsersRoleForm.add(hiddenFieldForUnassignedUser.setOutputMarkupId(true));
        unassignedUsersRoleForm.add(unassignedUserTable);
        unassignedUsersRoleForm.add(searchUnassignedUsersPanel);

        // Step 4: create a container including all above components to manage roles.
        roleManagementContainer.add(roleDescriptionTextArea);
        roleManagementContainer.add(assignedUsersRoleForm);
        roleManagementContainer.add(unassignedUsersRoleForm);

        // Initially, set the container to be invisible, since the components in the container display until the user
        // selects a role from the choice list.
        roleManagementContainer.setOutputMarkupPlaceholderTag(true);
        roleManagementContainer.setVisible(false);
    }

    /**
     * Initialize the dialog to display a warning message.
     */
    private void initWarningDialogContainer() {
        YuiDialog dialog = new YuiDialog("dialog.warning.roleUnassignment", null, YuiDialog.Style.CLOSE, null, null);
        warningDialogContainer = new WebMarkupContainer("container.warning.dialog");
        warningDialogContainer.setOutputMarkupId(true);
        warningDialogContainer.add(dialog.setVisibilityAllowed(false));
    }

    /**
     * A role model to store the selected role and the description of the selected role.
     */
    private final class RoleModel implements Serializable {
        private Role selectedRole;
        private String roleDescription;

        public Role getSelectedRole() {
            return selectedRole;
        }

        public void setSelectedRole(Role selectedRole) {
            this.selectedRole = selectedRole;
            if (selectedRole != null) {
                setRoleDescription( RbacUtilities.getDescriptionString(selectedRole, false));
            }
        }

        public String getRoleDescription() {
            return roleDescription;
        }

        public void setRoleDescription(String roleDescription) {
            this.roleDescription = roleDescription;
        }
    }

    /**
     * The select-a-role form lists all roles.  After the user selects one of them, the page will display
     * the role description and a container, which providers tools to manage roles such as assign a user with
     * a role or unassign a user from a role.
     */
    private final class SelectRoleForm extends Form {

        public SelectRoleForm(final String componentName) {
            super(componentName);
            try {
                @SuppressWarnings({"ToArrayCallWithZeroLengthArrayArgument"})
                final List<Role> roles = Arrays.asList(roleManager.findAll().toArray(new Role[0]));
                ListChoice listChoice = new ListChoice("listChoice.roleList", new PropertyModel(roleModel, "selectedRole"),
                    new LoadableDetachableModel() {
                        @Override
                        protected Object load() {
                            return roles;
                        }
                    }
                );

                listChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        // Update the visibility of the role management container
                        if (roleModel.getSelectedRole() == null) {
                            roleManagementContainer.setVisible(false);
                        } else {
                            roleManagementContainer.setVisible(true);
                        }

                        // Render the role management container
                        target.addComponent(roleManagementContainer);
                    }
                });
                add(listChoice);
            } catch (FindException e) {
                logger.warning("Cannot find roles.");
            }
        }
    }

    /**
     * Create a table in a panel for assigned users or unassigned users.
     * @param usersAssigned: a user assigned a role or not.
     * @param hidden
     * @param button
     * @return a panel with a user table.
     */
    private Panel getUserTablePanel(boolean usersAssigned, HiddenField hidden, final Button button) {
        List<PropertyColumn> columns = new ArrayList<PropertyColumn>();
        columns.add(new PropertyColumn(new StringResourceModel("usertable.column.login", this, null), "login", "login"));
        columns.add(new PropertyColumn(new StringResourceModel("usertable.column.lastName", this, null), "lastName", "lastName"));
        columns.add(new PropertyColumn(new StringResourceModel("usertable.column.firstName", this, null), "firstName", "firstName"));

        String panelId = "panel." + (usersAssigned? "" : "un") + "assignedUsers";
        return new YuiDataTable(panelId, columns, "login", true, new UserDataProvider("login", true, usersAssigned), hidden,
            "login", false, new Button[]{button}) {
            @Override
            @SuppressWarnings({"UnusedDeclaration"})
            protected void onSelect(AjaxRequestTarget ajaxRequestTarget, String value) {
                boolean enabled = value != null && value.length() > 0;
                button.setEnabled(enabled);
                ajaxRequestTarget.addComponent(button);
            }
        };
    }

    private final class UserDataProvider extends SortableDataProvider {
        private boolean assigned;  // an indicator for the table storing assigned users or unassigned users.

        public UserDataProvider(String sort, boolean asc, boolean assigned) {
            setSort(sort, asc);
            this.assigned = assigned;
        }

        @Override
        public Iterator iterator(int first, int count) {
            try {
                Collection<InternalUser> users =
                    emsAccountManager.getUserPage(first, count, getSort().getProperty(), getSort().isAscending());
                Role role = roleManager.findByPrimaryKey(roleModel.selectedRole.getOid());

                if (assigned) {
                    List<InternalUser> assignedUsers = new ArrayList<InternalUser>();
                    for (RoleAssignment ra: role.getRoleAssignments()) {
                        assignedUsers.add(emsAccountManager.findByPrimaryKey(ra.getIdentityId()));
                    }
                    users.retainAll(assignedUsers);
                } else {
                    for (RoleAssignment ra: role.getRoleAssignments()) {
                        users.remove(emsAccountManager.findByPrimaryKey(ra.getIdentityId()));
                    }
                }

                return filterUsers(assigned, users).iterator();
            } catch (FindException fe) {
                logger.log( Level.WARNING, "Error finding users", fe );
                return Collections.emptyList().iterator();
            }
        }

        @Override
        public int size() {
            if (roleModel.selectedRole == null) {
                return 0;
            } else if (assigned) {
                return roleModel.selectedRole.getRoleAssignments().size();
            } else {
                try {
                    return emsAccountManager.getUserCount() - roleModel.selectedRole.getRoleAssignments().size();
                } catch (FindException e) {
                    return 0;
                }
            }
        }

        @Override
        public IModel model(final Object userObject) {
            return new AbstractReadOnlyModel() {
                @Override
                public Object getObject() {
                    return userObject;
                }
            };
        }

        private Collection<InternalUser> filterUsers(boolean usersAssigned, Collection<InternalUser> users) {
            String searchManner = usersAssigned? searchAssignedUsersModel.getSearchManner() : searchUnassignedUsersModel.getSearchManner();
            String searchValue = usersAssigned? searchAssignedUsersModel.getSearchValue() : searchUnassignedUsersModel.getSearchValue();
            if (searchManner == null || searchValue == null || searchValue.isEmpty()) return users;

            List<InternalUser> filteredUsers = new ArrayList<InternalUser>();
            String searchMannerContains = new StringResourceModel("search.manner.contains", UserRoles.this, null).getString();
            String searchMannerStartswith = new StringResourceModel("search.manner.startswith", UserRoles.this, null).getString();

            for (InternalUser user: users) {
                if (searchManner.equals(searchMannerContains)) {
                    if (user.getLogin() != null && user.getLogin().contains(searchValue)) {
                        filteredUsers.add(user);
                    }
                } else if (searchManner.equals(searchMannerStartswith)) {
                    if (user.getLogin() != null && user.getLogin().startsWith(searchValue)) {
                        filteredUsers.add(user);
                    }
                } else {
                    return users;
                }
            }
            return filteredUsers;
        }
    }
}