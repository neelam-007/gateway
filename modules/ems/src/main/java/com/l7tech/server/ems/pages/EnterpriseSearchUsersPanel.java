package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;

import java.util.Collection;
import java.util.Arrays;
import java.io.Serializable;

/**
 * The class creates a search panel for searching users in a given manner (contains or starts with)
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Oct 28, 2008
 */
public class EnterpriseSearchUsersPanel extends Panel {

    public EnterpriseSearchUsersPanel(String id, SearchUsersModel searchUsersModel, final Collection<? extends Component> refreshComponents) {
        super(id);

        Form form = new Form("form.searchUsers");

        String[] searchManners = new String[] {
            new StringResourceModel("search.manner.contains", this, null).getString(),
            new StringResourceModel("search.manner.startswith", this, null).getString()};
        
        form.add(new DropDownChoice("list.searchManners", new PropertyModel(searchUsersModel, "searchManner"), Arrays.asList(searchManners)) {
            protected CharSequence getDefaultChoice(Object o) {
                return new StringResourceModel("search.manner.contains", this, null).getString();
            }
        });
        form.add(new TextField("textfield.searchValues", new PropertyModel(searchUsersModel, "searchValue")));
        form.add(new YuiAjaxButton("button.searchUsers") {
            protected void onSubmit(final AjaxRequestTarget target, final Form form) {
                for (Component component : refreshComponents) {
                    target.addComponent(component);
                }
            }
        });
        
        add(form);
    }

    /**
     * A modle to store a search manner and a value to search.
     */
    public final static class SearchUsersModel implements Serializable {
        private String searchManner;
        private String searchValue;

        public String getSearchManner() {
            return searchManner;
        }

        public void setSearchManner(String searchManner) {
            this.searchManner = searchManner;
        }

        public String getSearchValue() {
            return searchValue;
        }

        public void setSearchValue(String searchValue) {
            this.searchValue = searchValue;
        }
    }
}
