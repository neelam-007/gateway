package com.l7tech.server.ems.pages;

import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.ems.EmsAccountManager;
import com.l7tech.server.ems.NavigationPage;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdateAny;
import com.l7tech.gateway.common.security.rbac.RequiredPermissionSet;
import com.l7tech.gateway.common.security.rbac.RequiredPermission;
import com.l7tech.gateway.common.security.rbac.OperationType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enterprise Users page
 */
@RequiredPermissionSet(
    requiredPermissions={
        @RequiredPermission(entityType=EntityType.RBAC_ROLE, operationType= OperationType.READ),
        @RequiredPermission(entityType=EntityType.USER, operationType=OperationType.DELETE)
    }
)@NavigationPage(page="EnterpriseUsers",pageIndex=200,section="Settings",sectionIndex=200,pageUrl="EnterpriseUsers.html")
public class EnterpriseUsers extends EmsPage {

    //- PUBLIC

    public EnterpriseUsers() {
        final WebMarkupContainer secured = new SecureWebMarkupContainer( "secured", new AttemptedUpdateAny(EntityType.RBAC_ROLE) );

        final WebMarkupContainer container = new WebMarkupContainer("user.container");
        container.setOutputMarkupId(true);
        container.add(new EmptyPanel("user.content"));
        secured.add ( container );
        final WebMarkupContainer tableContainer = new WebMarkupContainer("usertable.container");

        final Form pageForm = new Form("form");
        secured.add ( pageForm );
        pageForm.add ( tableContainer.setOutputMarkupId(true) );

        final WebMarkupContainer userContainer1 = new WebMarkupContainer("user.container.1");
        userContainer1.setOutputMarkupId(true);
        userContainer1.add(new EmptyPanel("user.content"));
        secured.add ( userContainer1 );

        final WebMarkupContainer userContainer2 = new WebMarkupContainer("user.container.2");
        userContainer2.setOutputMarkupId(true);
        userContainer2.add(new EmptyPanel("user.content"));
        secured.add ( userContainer2 );

        Button addButton = new YuiAjaxButton("addUserButton") {
            @Override
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget, Form form) {
                userContainer1.removeAll();
                userContainer2.removeAll();

                userContainer1.add( new EnterpriseUsersNewPanel( "user.content", Collections.singleton(tableContainer) ) );
                userContainer2.add( new EmptyPanel("user.content") );

                ajaxRequestTarget.addComponent( tableContainer ); // refresh to clear selection
                ajaxRequestTarget.addComponent( userContainer1 );
                ajaxRequestTarget.addComponent( userContainer2 );
            }
        };

        Button deleteButton = new YuiAjaxButton("deleteUserButton") {
            @Override
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget, Form form) {
                final String id = (String) form.get("userId").getModel().getObject();
                if ( id != null && !id.isEmpty() ) {
                    container.removeAll();
                    User u = getUser();
                    String userLogin = u.getLogin();
                    //todo need to also check the identity provider of the current user
                    if(userLogin.equals(id)){
                        Label confirmLabel = new Label(YuiDialog.getContentId(), new StringResourceModel("deleteuser.error.message", this, null, new Object[]{"'"+userLogin+"'"}));
                        YuiDialog dialog = new YuiDialog( "user.content", new StringResourceModel("deleteuser.error.heading", this, null).getString(), YuiDialog.Style.CLOSE, confirmLabel, null);
                        container.add( dialog );
                    }else{
                        Label confirmLabel = new Label( YuiDialog.getContentId(), new StringResourceModel("deleteuser.confirm", this, null, new Object[]{"'"+id+"'"}));
                        YuiDialog dialog = new YuiDialog( "user.content", new StringResourceModel("deleteuser.heading", this, null).getString(), YuiDialog.Style.OK_CANCEL, confirmLabel, new YuiDialog.OkCancelCallback(){
                            @Override
                            public void onAction(YuiDialog dialog, AjaxRequestTarget target, YuiDialog.Button button) {
                                if ( button == YuiDialog.Button.OK ) {
                                    try {
                                        emsAccountManager.delete( id );

                                        userContainer1.replace( new EmptyPanel("user.content") );
                                        userContainer2.replace( new EmptyPanel("user.content") );

                                        target.addComponent(tableContainer);
                                        target.addComponent( userContainer1 );
                                        target.addComponent( userContainer2 );
                                    } catch (DeleteException de) {
                                        logger.log(Level.WARNING, "Error deleting user.", de);
                                    }
                                }
                            }
                        } );
                        container.add( dialog );
                    }

                    ajaxRequestTarget.addComponent( container );
                }
            }
        };

        HiddenField hidden = new HiddenField("userId", new Model(""));

        pageForm.add( addButton );
        pageForm.add( deleteButton );
        pageForm.add( hidden.setOutputMarkupId(true) );

        List<PropertyColumn> columns = new ArrayList<PropertyColumn>();
        columns.add(new PropertyColumn(new StringResourceModel("usertable.column.login", this, null), "login", "login"));
        columns.add(new PropertyColumn(new StringResourceModel("usertable.column.lastName", this, null), "lastName", "lastName"));
        columns.add(new PropertyColumn(new StringResourceModel("usertable.column.firstName", this, null), "firstName", "firstName"));

        YuiDataTable table = new YuiDataTable("usertable", columns, "login", true, new UserDataProvider("login", true), hidden, "login", false, new Button[]{ deleteButton }){
            @Override
            @SuppressWarnings({"UnusedDeclaration"})
            protected void onSelect( final AjaxRequestTarget ajaxRequestTarget, final String value ) {
                if ( value != null && value.length() > 0 ) {
                    userContainer1.removeAll();
                    userContainer1.add(new EnterpriseUsersEditPanel( "user.content", value, Collections.singleton(tableContainer) ));
                    ajaxRequestTarget.addComponent( userContainer1 );

                    userContainer2.removeAll();
                    userContainer2.add( new EnterpriseUsersResetPasswordPanel( "user.content", value ) );
                    ajaxRequestTarget.addComponent( userContainer2 );
                }
            }
        };
        tableContainer.add( table );

        add( secured );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( EnterpriseUsers.class.getName() );

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private EmsAccountManager emsAccountManager;

    private class UserDataProvider extends SortableDataProvider {
        public UserDataProvider(String sort, boolean asc) {
            setSort(sort, asc);
        }

        @Override
        @SuppressWarnings({"unchecked"})
        public Iterator iterator(int first, int count) {
            try {
                Collection<InternalUser> users = emsAccountManager.getUserPage(first, count, getSort().getProperty(), getSort().isAscending());

                return users.iterator();
            } catch (FindException fe) {
                logger.log( Level.WARNING, "Error finding users", fe );
                return Collections.emptyList().iterator();
            }
        }

        @Override
        public int size() {
            try {
                return emsAccountManager.getUserCount();
            } catch (FindException fe) {
                logger.log( Level.WARNING, "Error finding users", fe );
                return 0;
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
    }
}
