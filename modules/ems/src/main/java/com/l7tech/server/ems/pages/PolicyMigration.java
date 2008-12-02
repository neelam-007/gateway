package com.l7tech.server.ems.pages;

import com.l7tech.server.ems.NavigationPage;
import com.l7tech.server.ems.TypedPropertyColumn;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.server.ems.gateway.GatewayContextFactory;
import com.l7tech.server.ems.gateway.GatewayContext;
import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.EntityHeaderRef;
import com.l7tech.objectmodel.migration.MigrationMapping;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.mortbay.util.ajax.JSON;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Arrays;
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

                    Collection deps = retrieveDependencies(dir);
                    YuiDataTable ydt = new YuiDataTable(
                            "dependenciesTable",
                            Arrays.asList(
                                    new TypedPropertyColumn(new Model(""), "optional", "optional", String.class, false),
                                    new PropertyColumn(new Model("Name"), "name", "name"),
                                    new PropertyColumn(new Model("Type"), "type", "type")
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

    @SpringBean
    private SsgClusterManager ssgClusterManager;

    @SpringBean
    private GatewayContextFactory gatewayContextFactory;

    private Collection<DependencyItem> retrieveDependencies( final DependencyItemsRequest request ) throws Exception {
        Collection<DependencyItem> deps = new ArrayList<DependencyItem>();

        SsgCluster cluster = ssgClusterManager.findByGuid(request.clusterId);
        if ( cluster != null ) {
            if ( cluster.getTrustStatus() ) {
                GatewayContext context = gatewayContextFactory.getGatewayContext( getUser(), cluster.getSslHostName(), cluster.getAdminPort() );
                MigrationApi api = context.getMigrationApi();
                MigrationMetadata metadata = api.findDependencies( request.asEntityHeaders() );
                if ( metadata != null && metadata.getMappableDependencies() != null ) {
                    for ( MigrationMapping mapping : metadata.getMappings() ) {
                        EntityHeaderRef sourceRef = mapping.getSource();
                        EntityHeader header = metadata.getHeader( sourceRef );
                        deps.add( new DependencyItem( header.getStrId(), header.getType().toString(), header.getName(), toImgIcon(metadata.isMappingRequired(sourceRef)) ) );
                    }
                }
            } 
        }

        return deps;
    }

    private String toImgIcon( final boolean required ) {
        String icon = "";

        if ( required ) {
            icon = "<img src=/images/unresolved.png />"; //TODO JSON quote escaping
        }

        return icon;
    }

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

        public Collection<EntityHeader> asEntityHeaders() {
            Collection<EntityHeader> headers = new ArrayList<EntityHeader>();

            for ( DependencyItem entity : entities ) {
                EntityType type = JSONConstants.Entity.ENTITY_TYPE_MAP.get( entity.type );
                if ( type != null ) {
                    headers.add( new EntityHeader( entity.id, type, entity.name, null) );
                } else {
                    logger.warning("Entity with unknown type '"+entity.type+"' requested.");
                }
            }

            return headers;
        }
    }

    private static class DependencyItem implements JSON.Convertible, Serializable {
        private String id;
        private String type;
        private String name;
        @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
        private String optional; // accessed via property model

        public DependencyItem(){
        }

        public DependencyItem( final String id, final String type, final String name, final String optional ) {
            this.id = id;
            this.type = type;
            this.name = name;
            this.optional = optional;
        }

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