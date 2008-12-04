package com.l7tech.server.ems.pages;

import com.l7tech.server.ems.NavigationPage;
import com.l7tech.server.ems.TypedPropertyColumn;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.server.ems.gateway.GatewayContextFactory;
import com.l7tech.server.ems.gateway.GatewayContext;
import com.l7tech.server.ems.gateway.GatewayException;
import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.EntityHeaderRef;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.migration.MigrationMapping;
import com.l7tech.objectmodel.migration.MigrationException;
import com.l7tech.util.Pair;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.mortbay.util.ajax.JSON;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Arrays;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.io.Serializable;

/**
 *
 */
@NavigationPage(page="PolicyMigration",section="ManagePolicies",pageUrl="PolicyMigration.html")
public class PolicyMigration extends EmsPage  {

    //- PUBLIC

    public PolicyMigration() {
        final EntityMappingModel mappingModel = new EntityMappingModel();

        final WebMarkupContainer dependenciesContainer = new WebMarkupContainer("dependencies");
        final Form dependenciesOptionsContainer = new Form("dependencyOptionsContainer");
        YuiDataTable.contributeHeaders(this);

        Form selectionJsonForm = new Form("selectionForm");
        final DependencyKey[] lastSourceKey = new DependencyKey[1];
        final String[] lastTargetClusterId = new String[1];
        final HiddenField hiddenSelectionForm = new HiddenField("selectionJson", new Model(""));
        final HiddenField hiddenDestClusterId = new HiddenField("destinationClusterId", new Model(""));        
        final HiddenField hiddenDestFolderId = new HiddenField("destinationFolderId", new Model(""));        
        selectionJsonForm.add( hiddenSelectionForm );
        selectionJsonForm.add( hiddenDestClusterId );
        selectionJsonForm.add( hiddenDestFolderId );
        AjaxFormSubmitBehavior submitBehaviour = new AjaxFormSubmitBehavior(selectionJsonForm, "onclick"){
            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                final String value = hiddenSelectionForm.getModelObjectAsString();
                lastTargetClusterId[0] = hiddenDestClusterId.getModelObjectAsString();
                try {
                    String jsonData = value.replaceAll("&quot;", "\"");

                    Map jsonMap = (Map) JSON.parse(jsonData);
                    final DependencyItemsRequest dir = new DependencyItemsRequest();
                    dir.fromJSON(jsonMap);

                    Collection deps = retrieveDependencies(dir);
                    YuiDataTable ydt = new YuiDataTable(
                            "dependenciesTable",
                            Arrays.asList(
                                    new PropertyColumn(new Model(""), "uid"),
                                    new TypedPropertyColumn(new Model(""), "optional", "optional", String.class, false),
                                    new PropertyColumn(new Model("Name"), "name", "name"),
                                    new PropertyColumn(new Model("Type"), "type", "type"),
                                    new TypedPropertyColumn(new Model("Version"), "version", "version", Integer.class, true)
                            ),
                            "name",
                            true,
                            deps,
                            null,
                            "uid",
                            true,
                            null
                    ){
                        @Override
                        @SuppressWarnings({"UnusedDeclaration"})
                        protected void onSelect( final AjaxRequestTarget ajaxRequestTarget, final String value ) {
                            if ( lastTargetClusterId[0] != null && !lastTargetClusterId[0].isEmpty() && value != null && !value.isEmpty() ) {
                                String[] typeIdPair = value.split(":", 2);
                                logger.info("Selected dependency for mapping id '"+typeIdPair[1]+"', type '"+typeIdPair[0]+"'.");

                                DependencyKey sourceKey = new DependencyKey( dir.clusterId, EntityType.valueOf(typeIdPair[0]), typeIdPair[1] );
                                lastSourceKey[0] = sourceKey;

                                List optionList = retrieveDependencyOptions( lastTargetClusterId[0], sourceKey, null );
                                addDependencyOptions( dependenciesOptionsContainer, sourceKey, lastTargetClusterId[0], optionList, mappingModel );

                                ajaxRequestTarget.addComponent(dependenciesOptionsContainer);
                            }
                        }
                    };

                    addDependencyOptions( dependenciesOptionsContainer, null, null, Collections.emptyList(), mappingModel );
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
        WebComponent image = new WebComponent("identifyDependenciesImage");
        image.setMarkupId("identifyDependenciesImage");
        image.add(submitBehaviour);
        add(image);

        AjaxFormSubmitBehavior migrateBehaviour = new AjaxFormSubmitBehavior(selectionJsonForm, "onclick"){
            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                final String value = hiddenSelectionForm.getModelObjectAsString();
                final String targetClusterId = hiddenDestClusterId.getModelObjectAsString();
                try {
                    String jsonData = value.replaceAll("&quot;", "\"");

                    Map jsonMap = (Map) JSON.parse(jsonData);
                    final DependencyItemsRequest dir = new DependencyItemsRequest();
                    dir.fromJSON(jsonMap);

                    performMigration( dir.clusterId, targetClusterId, dir, mappingModel );
                } catch ( Exception e ) {
                    logger.log( Level.WARNING, "Error processing selection.", e);
                }
            }
            @Override
            protected void onError(final AjaxRequestTarget target) {
            }
        };
        WebComponent migrateImage = new WebComponent("migrateImage");
        migrateImage.setMarkupId("migrateImage");
        migrateImage.add(migrateBehaviour);
        add(migrateImage);

        add( selectionJsonForm );

        dependenciesContainer.add( new Label("dependenciesTable", "") );

        dependenciesContainer.add( dependenciesOptionsContainer.setOutputMarkupId(true) );

        final SearchModel searchModel = new SearchModel();
        String[] searchManners = new String[] {
            "contains",
            "starts with"
        };

        dependenciesOptionsContainer.add(new DropDownChoice("dependencySearchManner", new PropertyModel(searchModel, "searchManner"), Arrays.asList(searchManners)) {
            @Override
            protected CharSequence getDefaultChoice(Object o) {
                return "contains";
            }
        });
        dependenciesOptionsContainer.add(new TextField("dependencySearchText", new PropertyModel(searchModel, "searchValue")));
        dependenciesOptionsContainer.add(new YuiAjaxButton("dependencySearchButton") {
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                logger.info("Searching for dependencies with filter '"+searchModel.getSearchValue()+"'.");
                List optionList = retrieveDependencyOptions( lastTargetClusterId[0], lastSourceKey[0], searchModel.getSearchValue() );
                addDependencyOptions( dependenciesOptionsContainer, lastSourceKey[0], lastTargetClusterId[0], optionList, mappingModel );
                ajaxRequestTarget.addComponent(dependenciesOptionsContainer);
            }
        });

        addDependencyOptions( dependenciesOptionsContainer, null, null, Collections.emptyList(), mappingModel );

        add( dependenciesContainer.setOutputMarkupId(true) );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( PolicyMigration.class.getName() );

    @SpringBean
    private SsgClusterManager ssgClusterManager;

    @SpringBean
    private GatewayContextFactory gatewayContextFactory;

    private void addDependencyOptions( final WebMarkupContainer dependenciesOptionsContainer, final DependencyKey sourceKey, final String targetClusterId, final List options, final EntityMappingModel mappingModel ) {
        WebMarkupContainer markupContainer = new WebMarkupContainer("dependencyOptions");
        if ( dependenciesOptionsContainer.get( markupContainer.getId() ) == null ) {
            dependenciesOptionsContainer.add( markupContainer.setOutputMarkupId(true) );
        } else {
            dependenciesOptionsContainer.replace( markupContainer.setOutputMarkupId(true) );
        }

        final Pair<DependencyKey,String> mappingKey = new Pair<DependencyKey,String>(sourceKey, targetClusterId);
        final DependencyItem currentItem = mappingModel.dependencyMap.get( mappingKey );

        markupContainer.add(new ListView("optionRepeater", options) {
            @Override
            protected void populateItem( final ListItem item ) {
                final DependencyItem dependencyItem = ((DependencyItem)item.getModelObject());

                Component radioComponent = new WebComponent("uid").add( new AjaxEventBehavior("onchange"){
                    @Override
                    protected void onEvent(final AjaxRequestTarget target) {
                        logger.info("Selection callback for : " + dependencyItem);
                        mappingModel.dependencyMap.put( mappingKey, dependencyItem );
                    }
                } );

                if ( currentItem!=null && currentItem.equals( dependencyItem ) ) {
                    radioComponent.add( new AttributeModifier("checked", true, new Model("checked")) );
                }

                item.add(radioComponent);
                item.add(new Label("name", dependencyItem.name));
                item.add(new Label("version", dependencyItem.getVersionAsString()));
            }
        });
        markupContainer.setEnabled( targetClusterId != null );
    }

    private Collection<DependencyItem> retrieveDependencies( final DependencyItemsRequest request ) throws Exception {
        Collection<DependencyItem> deps = new LinkedHashSet<DependencyItem>();

        SsgCluster cluster = ssgClusterManager.findByGuid(request.clusterId);
        if ( cluster != null ) {
            if ( cluster.getTrustStatus() ) {
                GatewayContext context = gatewayContextFactory.getGatewayContext( getUser(), cluster.getSslHostName(), cluster.getAdminPort() );
                MigrationApi api = context.getMigrationApi();
                MigrationMetadata metadata = api.findDependencies( request.asEntityHeaders() );
                if ( metadata != null && metadata.getMappings() != null ) {
                    for ( MigrationMapping mapping : metadata.getMappings() ) {
                        EntityHeaderRef targetRef = mapping.getTarget();
                        EntityHeader header = metadata.getHeader( targetRef );
                        deps.add( new DependencyItem( header, toImgIcon(metadata.isMappingRequired(targetRef)) ) );
                    }
                }
            } 
        }

        return deps;
    }

    @SuppressWarnings({"unchecked"})
    private List<DependencyItem> retrieveDependencyOptions( final String targetClusterId, final DependencyKey sourceKey, final String filter ) {
        List<DependencyItem> deps = new ArrayList<DependencyItem>();

        try {
            SsgCluster cluster = ssgClusterManager.findByGuid(targetClusterId);
            if ( cluster != null ) {
                if ( cluster.getTrustStatus() ) {
                    GatewayContext context = gatewayContextFactory.getGatewayContext( getUser(), cluster.getSslHostName(), cluster.getAdminPort() );
                    MigrationApi api = context.getMigrationApi();
                    EntityHeader entityHeader = new EntityHeader( sourceKey.id, sourceKey.type, "", null );
                    Map candidates = MigrationApi.MappingCandidate.fromCandidates(api.retrieveMappingCandidates( Collections.singletonList( entityHeader ), filter ));
                    if ( candidates != null && candidates.containsKey(entityHeader) ) {
                        EntityHeaderSet<EntityHeader> entitySet = (EntityHeaderSet<EntityHeader>) candidates.get(entityHeader);
                        if ( entitySet != null ) {
                            for ( EntityHeader header : entitySet ) {
                                deps.add( new DependencyItem( header, "") );
                            }
                        } else {
                            logger.info("No entities found.");
                        }
                    } else {
                        logger.info("No candidates found.");
                    }
                }
            }
        } catch ( GatewayException ge ) {
            logger.log( Level.WARNING, "Error while gettings dependency options.", ge );
        } catch ( FindException fe ) {
            logger.log( Level.INFO, "Error while gettings dependency options.", fe );
        } catch ( MigrationException me ) {
            logger.log( Level.INFO, "Error while gettings dependency options.", me );
        }

        return deps;
    }

    private void performMigration( final String sourceClusterId, final String targetClusterId, final DependencyItemsRequest requestedItems, final EntityMappingModel mappingModel ) {
        try {
            SsgCluster sourceCluster = ssgClusterManager.findByGuid(sourceClusterId);
            SsgCluster targetCluster = ssgClusterManager.findByGuid(targetClusterId);
            if ( sourceCluster != null && targetCluster != null) {
                if ( sourceCluster.getTrustStatus() && targetCluster.getTrustStatus() ) {
                    GatewayContext sourceContext = gatewayContextFactory.getGatewayContext( getUser(), sourceCluster.getSslHostName(), sourceCluster.getAdminPort() );
                    MigrationApi sourceMigrationApi = sourceContext.getMigrationApi();

                    GatewayContext targetContext = gatewayContextFactory.getGatewayContext( getUser(), targetCluster.getSslHostName(), targetCluster.getAdminPort() );
                    MigrationApi targetMigrationApi = targetContext.getMigrationApi();

                    MigrationBundle export = sourceMigrationApi.exportBundle( requestedItems.asEntityHeaders() );
                    MigrationMetadata metadata = export.getMetadata();
                    for ( Map.Entry<Pair<DependencyKey,String>,DependencyItem> mapping : mappingModel.dependencyMap.entrySet() ) {
                        if ( mapping.getKey().left.clusterId.equals(sourceClusterId) && mapping.getKey().right.equals(targetClusterId) ) {
                            EntityHeaderRef sourceRef = new EntityHeaderRef( mapping.getKey().left.type, mapping.getKey().left.id );
                            metadata.mapName( sourceRef, mapping.getValue().asEntityHeader() );
                        }
                    }
                    targetMigrationApi.importBundle( export, true );
                }
            }
        } catch ( GatewayException ge ) {
            logger.log( Level.WARNING, "Error while performing migration.", ge );
        } catch ( FindException fe ) {
            logger.log( Level.INFO, "Error while performing migration.", fe );
        } catch ( MigrationException me ) {
            logger.log( Level.INFO, "Error while performing migration.", me );
        }
    }

    private String toImgIcon( final boolean required ) {
        String icon = "";

        if ( required ) {
            icon = "<img src=/images/unresolved.png />"; //TODO JSON quote escaping
        }

        return icon;
    }

    private static class DependencyItemsRequest implements JSON.Convertible, Serializable {
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

    private static final class DependencyKey implements Serializable {
        private final String clusterId;
        private final EntityType type;
        private final String id;

        DependencyKey( final String clusterId,
                       final EntityType type,
                       final String id ) {
            this.clusterId = clusterId;
            this.type = type;
            this.id = id;
        }

        @Override
        public String toString() {
            return "DependencyKey[clusterId='"+clusterId+"'; id='"+id+"'; type='"+type+"']";
        }

        @Override
        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DependencyKey that = (DependencyKey) o;

            if (!clusterId.equals(that.clusterId)) return false;
            if (!id.equals(that.id)) return false;
            if (type != that.type) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = clusterId.hashCode();
            result = 31 * result + type.hashCode();
            result = 31 * result + id.hashCode();
            return result;
        }
    }

    private static final class DependencyItem implements JSON.Convertible, Serializable {
        private EntityHeader entityHeader;
        private String uid; // id and type
        private String id;
        private String type;
        private String name;
        private String optional;
        private Integer version;

        public DependencyItem() {        
        }

        public DependencyItem( final EntityHeader entityHeader, final String optional ) {
            this.entityHeader = entityHeader;
            this.uid = entityHeader.getType().toString() +":" + entityHeader.getStrId();
            this.id = entityHeader.getStrId();
            this.type = entityHeader.getType().toString();
            this.name = entityHeader.getName();
            this.optional = optional;
            this.version = entityHeader.getVersion();
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

        @SuppressWarnings({"RedundantIfStatement"})
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DependencyItem that = (DependencyItem) o;

            if (id != null ? !id.equals(that.id) : that.id != null) return false;
            if (name != null ? !name.equals(that.name) : that.name != null) return false;
            if (optional != null ? !optional.equals(that.optional) : that.optional != null) return false;
            if (type != null ? !type.equals(that.type) : that.type != null) return false;
            if (uid != null ? !uid.equals(that.uid) : that.uid != null) return false;
            if (version != null ? !version.equals(that.version) : that.version != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            result = (uid != null ? uid.hashCode() : 0);
            result = 31 * result + (id != null ? id.hashCode() : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (version != null ? version.hashCode() : 0);
            result = 31 * result + (optional != null ? optional.hashCode() : 0);
            return result;
        }

        public String getVersionAsString() {
            return version==null ? "" : Integer.toString(version);
        }

        public EntityHeader asEntityHeader() {
            return entityHeader;
        }
    }

    /**
     * A model to store the selected entity mappings (anything the user has
     * selected, not just mappings for currently selected source entities)
     */
    public final static class EntityMappingModel implements Serializable {
        private final Map<Pair<DependencyKey,String>,DependencyItem> dependencyMap =
            new HashMap<Pair<DependencyKey,String>,DependencyItem>();
    }

    /**
     * A model to store a search manner and a value to search.
     */
    public final static class SearchModel implements Serializable {
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