package com.l7tech.server.ems.pages;

import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.FindException;
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
        EmptyPanel content = new EmptyPanel("user.content");
        container.add(content);
        add ( container );
        final WebMarkupContainer tableContainer = new WebMarkupContainer("usertable.container");

        final Form pageForm = new Form("form");
        add ( pageForm );
        pageForm.add ( tableContainer.setOutputMarkupId(true) );

        Button addButton = new AjaxButton("addUserButton") {
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget, Form form) {
                container.removeAll();
                EnterpriseUsersNewPanel panel = new EnterpriseUsersNewPanel( YuiDialog.getContentId() );
                YuiDialog dialog = new YuiDialog( "user.content", "Create User", YuiDialog.Style.OK_CANCEL, panel );
                container.add( dialog );
                ajaxRequestTarget.addComponent( container );
            }
        };

        Button editButton = new AjaxButton("editUserButton") {
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form) {
                String id = (String) form.get("userId").getModel().getObject();
                if ( id != null && id.length() > 0 ) {
                    container.removeAll();
                    EnterpriseUsersEditPanel panel = new EnterpriseUsersEditPanel( YuiDialog.getContentId(), id );
                    YuiDialog dialog = new YuiDialog( "user.content", "Edit User", YuiDialog.Style.OK_CANCEL, panel, new YuiDialog.OkCancelCallback(){
                        public void onAction( final YuiDialog dialog, final AjaxRequestTarget target, final YuiDialog.Button button) {
                            if ( YuiDialog.Button.OK == button ) {
                                target.addComponent( tableContainer );
                            }
                        }
                    } );
                    container.add( dialog );
                    ajaxRequestTarget.addComponent( container );
                }
            }
        };

        Button resetButton = new AjaxButton("resetUserPasswordButton") {
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form) {
                String id = (String) form.get("userId").getModel().getObject();
                if ( id != null && id.length() > 0 ) {
                    container.removeAll();
                    EnterpriseUsersResetPasswordPanel panel = new EnterpriseUsersResetPasswordPanel( YuiDialog.getContentId(), id );
                    YuiDialog dialog = new YuiDialog( "user.content", "Reset User Password", YuiDialog.Style.OK_CANCEL, panel );
                    container.add( dialog );
                    ajaxRequestTarget.addComponent( container );
                }
            }
        };
        
        HiddenField hidden = new HiddenField("userId", new Model(""));

        pageForm.add( addButton );
        pageForm.add( editButton );
        pageForm.add( resetButton );
        pageForm.add( hidden.setOutputMarkupId(true) );

        List<PropertyColumn> columns = new ArrayList<PropertyColumn>();
        columns.add(new PropertyColumn(new StringResourceModel("usertable.column.login", this, null), "login", "login"));
        columns.add(new PropertyColumn(new StringResourceModel("usertable.column.lastName", this, null), "lastName", "lastName"));
        columns.add(new PropertyColumn(new StringResourceModel("usertable.column.firstName", this, null), "firstName", "firstName"));

        YuiDataTable table = new YuiDataTable("usertable", columns, "login", true, new UserDataProvider("login", true), hidden, "login", false, new Button[]{ editButton });
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
