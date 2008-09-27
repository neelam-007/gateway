package com.l7tech.server.ems.pages;

import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.server.ems.EmsAccountManager;
import com.l7tech.server.ems.NavigationPage;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
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
@NavigationPage(page="EnterpriseUsers",pageIndex=200,section="Settings",sectionIndex=200,pageUrl="EnterpriseUsers.html")
public class EnterpriseUsers extends EmsPage {

    //- PUBLIC

    public EnterpriseUsers() {
        final WebMarkupContainer container = new WebMarkupContainer("user.container");
        container.setOutputMarkupId(true);
        container.add(new EmptyPanel("user.content"));
        add ( container );
        final WebMarkupContainer tableContainer = new WebMarkupContainer("usertable.container");

        final Form pageForm = new Form("form");
        add ( pageForm );
        pageForm.add ( tableContainer.setOutputMarkupId(true) );

        final WebMarkupContainer userContainer1 = new WebMarkupContainer("user.container.1");
        userContainer1.setOutputMarkupId(true);
        userContainer1.add(new EmptyPanel("user.content"));
        add ( userContainer1 );

        final WebMarkupContainer userContainer2 = new WebMarkupContainer("user.container.2");
        userContainer2.setOutputMarkupId(true);
        userContainer2.add(new EmptyPanel("user.content"));
        add ( userContainer2 );

        Button addButton = new AjaxButton("addUserButton") {
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget, Form form) {
                userContainer1.removeAll();
                userContainer2.removeAll();

                userContainer1.add( new EnterpriseUsersNewPanel( "user.content", Collections.singleton(tableContainer) ) );
                userContainer2.add( new EmptyPanel("user.content") );

                ajaxRequestTarget.addComponent( userContainer1 );
                ajaxRequestTarget.addComponent( userContainer2 );
            }
        };

        Button deleteButton = new AjaxButton("deleteUserButton") {
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget, Form form) {
                final String id = (String) form.get("userId").getModel().getObject();
                container.removeAll();
                Label confirmLabel = new Label( YuiDialog.getContentId(), "Are you sure you want to delete user '"+id+"'?" );
                YuiDialog dialog = new YuiDialog( "user.content", "Delete User", YuiDialog.Style.OK_CANCEL, confirmLabel, new YuiDialog.OkCancelCallback(){
                    public void onAction(YuiDialog dialog, AjaxRequestTarget target, YuiDialog.Button button) {
                        if ( button == YuiDialog.Button.OK ) {
                            try {
                                emsAccountManager.delete( id );
                                target.addComponent(tableContainer);
                            } catch (DeleteException de) {
                                logger.log(Level.WARNING, "Error deleting user.", de);
                            }
                        }
                    }
                } );
                container.add( dialog );
                ajaxRequestTarget.addComponent( container );
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

        public int size() {
            try {
                return emsAccountManager.getUserCount();
            } catch (FindException fe) {
                logger.log( Level.WARNING, "Error finding users", fe );
                return 0;
            }
        }

        public IModel model(final Object userObject) {
             return new AbstractReadOnlyModel() {
                public Object getObject() {
                    return userObject;
                }
            };
        }
    }}
