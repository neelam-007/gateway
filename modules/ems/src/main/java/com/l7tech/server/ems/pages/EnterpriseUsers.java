package com.l7tech.server.ems.pages;

import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.AjaxRequestTarget;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ems.EmsAccountManager;
import com.l7tech.server.ems.NavigationPage;
import com.l7tech.identity.internal.InternalUser;

/**
 * Enterprise Users page
 */
@NavigationPage(page="EnterpriseUsers",section="Enterprise",pageUrl="EnterpriseUsers.html")
public class EnterpriseUsers extends EmsPage {

    //- PUBLIC

    public EnterpriseUsers() {

        final Form pageForm = new Form("form");
        add ( pageForm );

        Button addButton = new AjaxButton("addUserButton") {
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget, Form form) {
                this.setResponsePage( EnterpriseUsersNew.class );
            }
        };

        Button editButton = new AjaxButton("editUserButton") {
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget, Form form) {
                this.setResponsePage( new EnterpriseUsersEdit( (String)form.get("userId").getModel().getObject() ) );
            }
        };

        HiddenField hidden = new HiddenField("userId", new Model(""));

        pageForm.add( addButton );
        pageForm.add( editButton );
        pageForm.add( hidden.setOutputMarkupId(true) );

        List<PropertyColumn> columns = new ArrayList<PropertyColumn>();
        PropertyColumn loginPropertyColumn = new PropertyColumn(new StringResourceModel("usertable.column.login", this, null), "login", "login");
        columns.add(loginPropertyColumn);
        columns.add(new PropertyColumn(new StringResourceModel("usertable.column.lastName", this, null), "lastName", "lastName"));
        columns.add(new PropertyColumn(new StringResourceModel("usertable.column.firstName", this, null), "firstName", "firstName"));

        YuiDataTable table = new YuiDataTable("usertable", columns, "login", true, new UserDataProvider("login", true), hidden, "login", new Button[]{ editButton });
        pageForm.add( table );        
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
