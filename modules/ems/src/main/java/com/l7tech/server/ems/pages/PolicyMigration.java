package com.l7tech.server.ems.pages;

import com.l7tech.server.ems.NavigationPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.mortbay.util.ajax.JSON;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;
import java.io.Serializable;

/**
 *
 */
@NavigationPage(page="PolicyMigration",section="ManagePolicies",pageUrl="PolicyMigration.html")
public class PolicyMigration extends EmsPage  {

    //- PUBLIC

    public PolicyMigration() {
        final WebMarkupContainer dependenciesContainer = new WebMarkupContainer("dependencies");
        YuiDataTable.contributeHeaders(this);

        Form selectionJsonForm = new Form("selectionForm");
        final HiddenField hiddenSelectionForm = new HiddenField("selectionJson", new Model(""));
        selectionJsonForm.add( hiddenSelectionForm );
        AjaxFormSubmitBehavior submitBehaviour = new AjaxFormSubmitBehavior(selectionJsonForm, "onclick"){
            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                String value = hiddenSelectionForm.getModelObjectAsString();
                try {
                    String jsonData = value.replaceAll("&quot;", "\"");

                    Map jsonMap = (Map) JSON.parse(jsonData);
                    DependencyItemsRequest dir = new DependencyItemsRequest();
                    dir.fromJSON(jsonMap);

                    Collection deps = Arrays.asList(dir.entities);
                    YuiDataTable ydt = new YuiDataTable(
                            "dependenciesTable",
                            Arrays.asList(
                                    new PropertyColumn(new Model("Name"), "name"),
                                    new PropertyColumn(new Model("Type"), "type")
                            ),
                            "name",
                            true,
                            deps
                    );

                    dependenciesContainer.replace(ydt);
                    target.addComponent( dependenciesContainer );
                } catch ( Exception e ) {
                    logger.log( Level.WARNING, "Error processing selection.", e);
                }
            }
            @Override
            protected void onError(final AjaxRequestTarget target) {

            }
        };
        WebComponent image = new WebComponent("identifyDependenciesImageButton");
        image.setMarkupId("identifyDependenciesImageButton");
        image.add(submitBehaviour);
        add(image);

        add( selectionJsonForm );

        dependenciesContainer.add( new Label("dependenciesTable", "") );

        add( dependenciesContainer.setOutputMarkupId(true) );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( PolicyMigration.class.getName() );

    /*
{
    "clusterId" : "c32bb1a9-1538-4792-baa1-566bfd418020",       // the source SSG Cluster
    "entities"  : [
        {
            "id"   : "229376",
            "type" : "publishedService",
            "name" : "Warehouse"
        },
        {
            "id"   : "b4fce666-d83f-4533-af06-6ea086271fcf"
            "type" : "policyFragment",
            "name" : "Policy Fragment 1"
        },
        ...
    ]
}

     */
    private static class DependencyItemsRequest implements JSON.Convertible {
        private String clusterId;
        private DependencyItem[] entities;

        @Override
        public String toString() {
            return "DependencyItemsRequest[clusterId='"+clusterId+"'; entities="+Arrays.asList(entities)+"]";
        }

        @Override
        public void toJSON(final JSON.Output out) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void fromJSON(final Map data) {
            clusterId = (String)data.get("clusterId");
            Object[] entitiesMap = (Object[])data.get("entities");
            if ( entitiesMap==null ) {
                entities = new DependencyItem[0];
            } else {
                entities = new DependencyItem[entitiesMap.length];
                int i=0;
                for ( Object entityMap : entitiesMap ) {
                    entities[i] = new DependencyItem();
                    entities[i++].fromJSON((Map)entityMap);
                }
            }
        }
    }

    private static class DependencyItem implements JSON.Convertible, Serializable {
        private String id;
        private String type;
        private String name;

        @Override
        public String toString() {
            return "DependencyItem[id='"+id+"'; type='"+type+"'; name='"+name+"']";
        }

        @Override
        public void toJSON(JSON.Output out) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void fromJSON(final Map data) {
            id = (String)data.get("id");
            type = (String)data.get("type");
            name = (String)data.get("name");
        }
    }
}