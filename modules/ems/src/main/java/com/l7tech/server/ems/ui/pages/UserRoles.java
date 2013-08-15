package com.l7tech.server.ems.ui.pages;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.ems.ui.NavigationPage;
import com.l7tech.server.ems.user.EsmAccountManager;
import com.l7tech.server.security.rbac.RoleManager;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A page to manage a role for users such as assign a role to a user or unassign a user from a role.
 */
@RequiredPermissionSet(
    requiredPermissions={
        @RequiredPermission(entityType=EntityType.RBAC_ROLE, operationType=OperationType.UPDATE),
        @RequiredPermission(entityType=EntityType.USER, operationType=OperationType.READ)
    }
)
@NavigationPage(page="UserRoles",pageIndex=300,section="Settings",sectionIndex=200,pageUrl="UserRoles.html")
public class UserRoles extends EsmStandardWebPage {
    private static final Logger logger = Logger.getLogger(UserRoles.class.getName());

    @Inject
    private RoleManager roleManager;

    @Inject
    private EsmAccountManager emsAccountManager;

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

        WebMarkupContainer secured = new SecureWebMarkupContainer( "secured", new AttemptedUpdateAny(EntityType.RBAC_ROLE) );

        // Create a form to select a role
        secured.add(new SelectRoleForm("form.selectRole"));

        // Add headers to page early to fix issue with CSS ordering
        YuiDataTable.contributeHeaders( this );

        // Create a container to contain all components to manage roles for users.
        initRoleManagementContainer();
        secured.add(roleManagementContainer);

        // Create a warning dialog, just in case where some errors occur during role updates.
        initWarningDialogContainer();
        secured.add(warningDialogContainer);

        add( secured );
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
        TextArea<String> roleDescriptionTextArea = new TextArea<String>("textarea.roleDescription", new PropertyModel<String>(roleModel, "roleDescription"));

        // Step 2: create components to unassign assigned users from a role.
        HiddenField<String> hiddenFieldForAssignedUser = new HiddenField<String>("assignedUserId", new Model<String>(""));
        hiddenFieldForAssignedUser.setOutputMarkupId(true);
        YuiAjaxButton buttonToUnassignRole = new YuiAjaxButton("button.unassignRole") {
            @Override
            protected void onSubmit(final AjaxRequestTarget ajaxRequestTarget, Form form) {
                final String userId =  YuiDataTable.unescapeIdentitifer((String) form.get("assignedUserId").getDefaultModelObject());
                try {
                    Role role = roleManager.findByPrimaryKey(roleModel.selectedRole.getGoid());
                    role.removeAssignedUser(emsAccountManager.findByLogin(userId));
                    roleManager.update(role);
                    roleModel.selectedRole = role;
                    rebuildSelectedUsers();

                    // Render the role management container
                    this.setEnabled(false);
                    ajaxRequestTarget.addComponent(this);
                    ajaxRequestTarget.addComponent(assignedUsersRoleForm.get("panel.assignedUsers"));
                    ajaxRequestTarget.addComponent(unassignedUsersRoleForm.get("panel.unassignedUsers"));
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
        Panel assignedUserTable = getPlaceholderUserTablePanel(true);
        Panel searchAssignedUsersPanel = new EnterpriseSearchUsersPanel("panel.search.assignedUsers",
            searchAssignedUsersModel, Collections.singleton(assignedUsersRoleForm)){
            @Override
            protected void onSearch() {
                rebuildSelectedUsers();
            }
        };

        assignedUsersRoleForm.add(buttonToUnassignRole);
        assignedUsersRoleForm.add(hiddenFieldForAssignedUser);
        assignedUsersRoleForm.add(assignedUserTable);
        assignedUsersRoleForm.add(searchAssignedUsersPanel);

        // Step 3: create components to assign a role to unassigned users
        HiddenField<String> hiddenFieldForUnassignedUser = new HiddenField<String>("unassignedUserId", new Model<String>(""));
        hiddenFieldForUnassignedUser.setOutputMarkupId(true);
        YuiAjaxButton buttonToAssignRole = new YuiAjaxButton("button.assignRole") {
            @Override
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget, Form form) {
                final String userId = YuiDataTable.unescapeIdentitifer((String) form.get("unassignedUserId").getDefaultModelObject());
                try {
                    Role role = roleManager.findByPrimaryKey(roleModel.selectedRole.getGoid());
                    role.addAssignedUser(emsAccountManager.findByLogin(userId));
                    roleManager.update(role);
                    roleModel.selectedRole = role;
                    rebuildSelectedUsers();

                    // Render the role management container
                    this.setEnabled(false);
                    ajaxRequestTarget.addComponent(this);
                    ajaxRequestTarget.addComponent(assignedUsersRoleForm.get("panel.assignedUsers"));
                    ajaxRequestTarget.addComponent(unassignedUsersRoleForm.get("panel.unassignedUsers"));
                } catch (FindException e) {
                    logger.log( Level.WARNING, "Error finding user '" + userId + "'.", e);
                } catch (UpdateException e) {
                    logger.log( Level.WARNING, "Error updating role '" + roleModel.selectedRole.getName() + "'.", e);

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
        Panel unassignedUserTable = getPlaceholderUserTablePanel(false);
        Panel searchUnassignedUsersPanel = new EnterpriseSearchUsersPanel("panel.search.unassignedUsers",
            searchUnassignedUsersModel, Collections.singleton(unassignedUsersRoleForm)){
            @Override
            protected void onSearch() {
                rebuildSelectedUsers();
            }
        };

        unassignedUsersRoleForm.add(buttonToAssignRole);
        unassignedUsersRoleForm.add(hiddenFieldForUnassignedUser);
        unassignedUsersRoleForm.add(unassignedUserTable);
        unassignedUsersRoleForm.add(searchUnassignedUsersPanel);

        // Step 4: create a container including all above components to manage roles.
        roleManagementContainer.add(roleDescriptionTextArea);
        roleManagementContainer.add(assignedUsersRoleForm.setOutputMarkupId(true));
        roleManagementContainer.add(unassignedUsersRoleForm).setOutputMarkupId(true);

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
                final List<Role> roles = new ArrayList<Role>(roleManager.findAll());
                ListChoice<Role> listChoice = new ListChoice<Role>("listChoice.roleList", new PropertyModel<Role>(roleModel, "selectedRole"),
                    new LoadableDetachableModel<List<Role>>() {
                        @Override
                        protected List<Role> load() {
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
                            rebuildSelectedUsers();
                            roleManagementContainer.setVisible(true);
                        }

                        // Render the role management container
                        target.addComponent(roleManagementContainer);
                    }
                });
                add(listChoice);
            } catch (FindException e) {
                logger.log( Level.WARNING, "Error accessing roles.", e );
            }
        }
    }

    private void rebuildSelectedUsers() {
        Form assignedUserForm = (Form)roleManagementContainer.get("form.assignedUsers");
        Form unassignedUserForm = (Form)roleManagementContainer.get("form.unassignedUsers");
        assignedUserForm.replace(getUserTablePanel(true, (HiddenField)assignedUserForm.get("assignedUserId"), (Button)assignedUserForm.get("button.unassignRole")));
        unassignedUserForm.replace(getUserTablePanel(false, (HiddenField)unassignedUserForm.get("unassignedUserId"), (Button)unassignedUserForm.get("button.assignRole")));        
    }

    private Collection newArrayList( final Iterator iterator ) {
        Collection<Object> collection = new ArrayList<Object>(100);

        while ( iterator.hasNext() ) {
            collection.add( iterator.next() );            
        }

        return collection;
    }

    private Panel getPlaceholderUserTablePanel(boolean usersAssigned) {
        String panelId = "panel." + (usersAssigned? "" : "un") + "assignedUsers";
        return new EmptyPanel(panelId);
    }

    /**
     * Create a table in a panel for assigned users or unassigned users.
     * @param usersAssigned: a user assigned a role or not.
     * @param hidden
     * @param button
     * @return a panel with a user table.
     */
    private Panel getUserTablePanel(boolean usersAssigned, HiddenField hidden, final Button button) {
        List<PropertyColumn<?>> columns = new ArrayList<PropertyColumn<?>>();
        columns.add(new PropertyColumn<String>(new StringResourceModel("usertable.column.login", this, null), "login", "login"));
        columns.add(new PropertyColumn<String>(new StringResourceModel("usertable.column.lastName", this, null), "lastName", "lastName"));
        columns.add(new PropertyColumn<String>(new StringResourceModel("usertable.column.firstName", this, null), "firstName", "firstName"));

        String panelId = "panel." + (usersAssigned? "" : "un") + "assignedUsers";
        return (YuiDataTable) new YuiDataTable(panelId, columns, "login", true, newArrayList(new UserDataProvider("login", true, usersAssigned).iterator(0,1000)), hidden,
            false, "login", false, new Button[]{button}) {
            @Override
            @SuppressWarnings({"UnusedDeclaration"})
            protected void onSelect(AjaxRequestTarget ajaxRequestTarget, String value) {
                boolean enabled = value != null && value.length() > 0;
                button.setEnabled(enabled);
                ajaxRequestTarget.addComponent(button);
            }
        }.setOutputMarkupId(true);
    }

    private final class UserDataProvider extends SortableDataProvider<InternalUser> {
        private boolean assigned;  // an indicator for the table storing assigned users or unassigned users.

        public UserDataProvider(String sort, boolean asc, boolean assigned) {
            setSort(sort, asc);
            this.assigned = assigned;
        }

        @Override
        public Iterator<InternalUser> iterator(int first, int count) {
            try {
                Collection<InternalUser> users =
                    emsAccountManager.getUserPage(first, count, getSort().getProperty(), getSort().isAscending());
                Role role = roleManager.findByPrimaryKey(roleModel.selectedRole.getGoid());

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
                return Collections.<InternalUser>emptyList().iterator();
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
        public IModel<InternalUser> model(final InternalUser userObject) {
            return new AbstractReadOnlyModel<InternalUser>() {
                @Override
                public InternalUser getObject() {
                    return userObject;
                }
            };
        }

        private Collection<InternalUser> filterUsers(boolean usersAssigned, Collection<InternalUser> users) {
            String searchManner = usersAssigned? searchAssignedUsersModel.getSearchManner() : searchUnassignedUsersModel.getSearchManner();
            String searchValue = usersAssigned? searchAssignedUsersModel.getSearchValue() : searchUnassignedUsersModel.getSearchValue();
            if (searchManner == null || searchValue == null || searchValue.isEmpty()) return users;
            searchValue = searchValue.toLowerCase();

            List<InternalUser> filteredUsers = new ArrayList<InternalUser>();
            String searchMannerContains = new StringResourceModel("search.manner.contains", UserRoles.this, null).getString();
            String searchMannerStartswith = new StringResourceModel("search.manner.startswith", UserRoles.this, null).getString();

            for (InternalUser user: users) {
                if (searchManner.equals(searchMannerContains)) {
                    if (user.getLogin() != null && user.getLogin().toLowerCase().contains(searchValue)) {
                        filteredUsers.add(user);
                    }
                } else if (searchManner.equals(searchMannerStartswith)) {
                    if (user.getLogin() != null && user.getLogin().toLowerCase().startsWith(searchValue)) {
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