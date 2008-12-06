package com.l7tech.server.ems.pages;

import com.l7tech.server.ems.NavigationPage;
import com.l7tech.server.ems.TypedPropertyColumn;
import com.l7tech.server.ems.migration.MigrationRecordManager;
import com.l7tech.server.ems.migration.MigrationMappingRecordManager;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.server.ems.gateway.GatewayContextFactory;
import com.l7tech.server.ems.gateway.GatewayContext;
import com.l7tech.server.ems.gateway.GatewayException;
import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.ExportedItem;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.EntityHeaderRef;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.migration.MigrationException;
import com.l7tech.objectmodel.migration.MigrationMapping;
import com.l7tech.util.Pair;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebRequest;
import org.mortbay.util.ajax.JSON;

import javax.xml.ws.soap.SOAPFaultException;
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
import java.util.Iterator;
import java.io.Serializable;

/**
 *
 */
@NavigationPage(page="PolicyMigration",section="ManagePolicies",pageUrl="PolicyMigration.html")
public class PolicyMigration extends EmsPage  {

    //- PUBLIC

    public PolicyMigration() {
        final DependencyKey[] lastSourceKey = new DependencyKey[1];
        final String[] lastTargetClusterId = new String[1];
        final EntityMappingModel mappingModel = new EntityMappingModel();

        final WebMarkupContainer dependenciesContainer = new WebMarkupContainer("dependencies");
        final Form dependenciesOptionsContainer = new Form("dependencyOptionsContainer");
        YuiDataTable.contributeHeaders(this);

        final WebMarkupContainer srcItemDependencies = new WebMarkupContainer("srcItemDependencies");
        add( srcItemDependencies.setOutputMarkupId(true) );
        showDependencies( srcItemDependencies, Collections.emptyList() );
        srcItemDependencies.add( buildDependencyDisplayBehaviour(srcItemDependencies, "srcItemSelectionCallbackUrl" ) );

        final WebMarkupContainer destItemDependencies = new WebMarkupContainer("destItemDependencies");
        add( destItemDependencies.setOutputMarkupId(true) );
        showDependencies( destItemDependencies, Collections.emptyList() );
        destItemDependencies.add( buildDependencyDisplayBehaviour(destItemDependencies, "destItemSelectionCallbackUrl" ) );

        Form selectionJsonForm = new Form("selectionForm");
        final HiddenField hiddenSelectionForm = new HiddenField("selectionJson", new Model(""));
        final HiddenField hiddenDestClusterId = new HiddenField("destinationClusterId", new Model(""));
        final HiddenField hiddenDestFolderId = new HiddenField("destinationFolderId", new Model(""));
        final HiddenField hiddenEnableNewServices = new HiddenField("enableNewServices", new Model(""));
        final HiddenField hiddenOverwriteDependencies = new HiddenField("overwriteDependencies", new Model(""));
        selectionJsonForm.add( hiddenSelectionForm );
        selectionJsonForm.add( hiddenDestClusterId );
        selectionJsonForm.add( hiddenDestFolderId );
        selectionJsonForm.add( hiddenEnableNewServices );
        selectionJsonForm.add( hiddenOverwriteDependencies );
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

                    final Collection<DependencyItem> deps = retrieveDependencies(dir);
                    loadMappings( mappingModel, dir.clusterId, lastTargetClusterId[0], deps );
                    YuiDataTable ydt = new YuiDataTable(
                            "dependenciesTable",
                            Arrays.asList(
                                    new PropertyColumn(new Model(""), "uid"),
                                    new TypedPropertyColumn(new Model(""), "optional", "optional", String.class, false),
                                    new PropertyColumn(new Model("Name"), "name", "name"),
                                    new TypedPropertyColumn(new Model("Ver."), "version", "version", Integer.class, true),
                                    new PropertyColumn(new Model("Type"), "type", "type")
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
                                logger.fine("Selected dependency for mapping id '"+typeIdPair[1]+"', type '"+typeIdPair[0]+"'.");

                                DependencyItem selectedItem = null;
                                for ( DependencyItem item : deps ) {
                                    if ( item.type.equals(typeIdPair[0]) && item.id.equals(typeIdPair[1]) ) {
                                        selectedItem = item;
                                        break;
                                    }
                                }

                                if ( selectedItem != null ) {
                                    DependencyKey sourceKey = new DependencyKey( dir.clusterId, selectedItem.asEntityHeader() );
                                    lastSourceKey[0] = sourceKey;

                                    List optionList = retrieveDependencyOptions( lastTargetClusterId[0], sourceKey, null );
                                    addDependencyOptions( dependenciesOptionsContainer, sourceKey, lastTargetClusterId[0], optionList, mappingModel );
                                } else {
                                    addDependencyOptions( dependenciesOptionsContainer, null, null, Collections.emptyList(), mappingModel );
                                }

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
                final String targetFolderId = hiddenDestFolderId.getModelObjectAsString();
                final String enableNewServices = hiddenEnableNewServices.getModelObjectAsString();
                final String overwriteDependencies = hiddenOverwriteDependencies.getModelObjectAsString();
                try {
                    String jsonData = value.replaceAll("&quot;", "\"");

                    Map jsonMap = (Map) JSON.parse(jsonData);
                    final DependencyItemsRequest dir = new DependencyItemsRequest();
                    dir.fromJSON(jsonMap);

                    boolean enableServices = Boolean.valueOf(enableNewServices);
                    boolean overwrite = Boolean.valueOf(overwriteDependencies);


                    performMigration( dir.clusterId, targetClusterId, targetFolderId, enableServices, overwrite, dir, mappingModel );
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
                logger.fine("Searching for dependencies with filter '"+searchModel.getSearchValue()+"'.");
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
    private MigrationRecordManager migrationRecordManager;

    @SpringBean
    private MigrationMappingRecordManager migrationMappingRecordManager;

    @SpringBean
    private GatewayContextFactory gatewayContextFactory;

    private AbstractDefaultAjaxBehavior buildDependencyDisplayBehaviour( final WebMarkupContainer itemDependenciesContainer, final String jsVar ) {
        return new AbstractDefaultAjaxBehavior(){
            @Override
            public void renderHead( final IHeaderResponse iHeaderResponse ) {
                super.renderHead( iHeaderResponse );
                iHeaderResponse.renderJavascript("var "+jsVar+" = '"+getCallbackUrl(true)+"';", null);
            }

            @Override
            protected void respond( final AjaxRequestTarget ajaxRequestTarget ) {
                WebRequest request = (WebRequest) RequestCycle.get().getRequest();
                String clusterId = request.getParameter("clusterId");
                String type = request.getParameter("type");
                String id = request.getParameter("id");
                logger.info( "Processing request for cluster " + clusterId + " type " + type + " id " + id );
                DependencyItemsRequest dir = new DependencyItemsRequest();
                dir.clusterId = clusterId;
                dir.entities = new DependencyItem[]{ new DependencyItem() };
                dir.entities[0].type = type;
                dir.entities[0].id = id;
                List<DependencyItem> options = Collections.emptyList();
                try {
                    options = new ArrayList<DependencyItem>(retrieveDependencies( dir ));
                    for ( Iterator<DependencyItem> itemIter = options.iterator(); itemIter.hasNext();  ) {
                        DependencyItem item = itemIter.next();
                        if ( EntityType.FOLDER.toString().equals(item.type) ) {
                            itemIter.remove();
                        }
                    }
                } catch (Exception e) {
                    logger.log( Level.WARNING, "Error processing selection.", e);
                }
                showDependencies( itemDependenciesContainer, options );
                ajaxRequestTarget.addComponent( itemDependenciesContainer );
            }
        };
    }

    private void showDependencies( final WebMarkupContainer container, final List options ) {
        ListView listView = new ListView("optionRepeater", options) {
            @Override
            protected void populateItem( final ListItem item ) {
                final DependencyItem dependencyItem = ((DependencyItem)item.getModelObject());
                item.add(new Label("optional", dependencyItem.optional).setEscapeModelStrings(false));
                item.add(new Label("name", dependencyItem.name));
                item.add(new Label("type", fromEntityType(EntityType.valueOf(dependencyItem.type))));
                item.add(new Label("version", dependencyItem.getVersionAsString()));
            }
        };

        if ( container.get( "optionRepeater" ) == null ) {
            container.add( listView.setOutputMarkupId(true) );
        } else {
            container.replace( listView.setOutputMarkupId(true) );
        }
    }

    private static String fromEntityType( final EntityType entityType ) {
        String type;

        switch ( entityType ) {
            case FOLDER:
                type = "folder";
                break;
            case SERVICE:
                type = "published service";
                break;
            case POLICY:
                type = "policy fragment";
                break;
            case ID_PROVIDER_CONFIG:
                type = "identity provider";
                break;
            case USER:
                type = "user";
                break;
            case GROUP:
                type = "group";
                break;
            default:
                type = entityType.toString();
                break;
        }

        return type;
    }

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
                        logger.fine("Selection callback for : " + dependencyItem);
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
                for (EntityHeader header : metadata.getMappableDependencies()) {
                    deps.add( new DependencyItem( header, toImgIcon(metadata.isMappingRequired(header)) ) );
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
                            logger.fine("No entities found when searching for candidates of ID '"+sourceKey.id+"', type '"+sourceKey.type+"'.");
                        }
                    } else {
                        logger.fine("No candidates found when searching for candidates of ID '"+sourceKey.id+"', type '"+sourceKey.type+"'.");
                    }
                }
            }
        } catch ( GatewayException ge ) {
            logger.log( Level.WARNING, "Error while gettings dependency options.", ge );
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error while gettings dependency options.", fe );
        } catch ( MigrationException me ) {
            logger.log( Level.WARNING, "Error while gettings dependency options.", me );
        }

        return deps;
    }

    private void loadMappings( final EntityMappingModel mappingModel,
                               final String sourceClusterId,
                               final String targetClusterId,
                               final Collection<DependencyItem> dependencyItems ) {
        if ( !dependencyItems.isEmpty() ) {
            try {
                for ( DependencyItem item : dependencyItems ) {
                    DependencyKey sourceKey = new DependencyKey( sourceClusterId, item.asEntityHeader() );
                    Pair<DependencyKey,String> mapKey = new Pair<DependencyKey,String>( sourceKey, targetClusterId );
                    if ( !mappingModel.dependencyMap.containsKey(mapKey) ) {
                        EntityHeader targetHeader = migrationMappingRecordManager.findEntityHeaderForMapping( sourceClusterId, item.asEntityHeader(), targetClusterId );
                        if ( targetHeader != null ) {
                            mappingModel.dependencyMap.put( mapKey, new DependencyItem( targetHeader, "" ) );
                        }
                    }
                }
            } catch ( FindException fe ) {
                logger.log( Level.WARNING, "Error loading dependency mappings.", fe );
            }
        }
    }

    private void performMigration( final String sourceClusterId,
                                   final String targetClusterId,
                                   final String targetFolderId,
                                   final boolean enableNewServices,
                                   final boolean overwriteDependencies,
                                   final DependencyItemsRequest requestedItems,
                                   final EntityMappingModel mappingModel ) {
        try {
            SsgCluster sourceCluster = ssgClusterManager.findByGuid(sourceClusterId);
            SsgCluster targetCluster = ssgClusterManager.findByGuid(targetClusterId);
            if ( sourceCluster != null && targetCluster != null) {
                if ( sourceCluster.getTrustStatus() && targetCluster.getTrustStatus() ) {
                    GatewayContext sourceContext = gatewayContextFactory.getGatewayContext( getUser(), sourceCluster.getSslHostName(), sourceCluster.getAdminPort() );
                    MigrationApi sourceMigrationApi = sourceContext.getMigrationApi();

                    GatewayContext targetContext = gatewayContextFactory.getGatewayContext( getUser(), targetCluster.getSslHostName(), targetCluster.getAdminPort() );
                    MigrationApi targetMigrationApi = targetContext.getMigrationApi();
                    GatewayApi targetGatewayApi = targetContext.getApi();

                    MigrationBundle export = sourceMigrationApi.exportBundle( requestedItems.asEntityHeaders() );
                    MigrationMetadata metadata = export.getMetadata();
                    for ( Map.Entry<Pair<DependencyKey,String>,DependencyItem> mapping : mappingModel.dependencyMap.entrySet() ) {
                        if ( mapping.getKey().left.clusterId.equals(sourceClusterId) && mapping.getKey().right.equals(targetClusterId) ) {
                            EntityHeaderRef sourceRef = new EntityHeaderRef( mapping.getKey().left.type, mapping.getKey().left.id );
                            metadata.mapName( sourceRef, mapping.getValue().asEntityHeader() );

                            migrationMappingRecordManager.persistMapping(
                                    sourceCluster.getGuid(),
                                    mapping.getKey().left.asEntityHeader(),
                                    targetCluster.getGuid(),
                                    mapping.getValue().asEntityHeader() );
                        }
                    }

                    Collection<GatewayApi.EntityInfo> folders = targetGatewayApi.getEntityInfo( Collections.singleton(EntityType.FOLDER) );
                    EntityHeader targetFolderHeader = null;
                    for ( GatewayApi.EntityInfo info : folders ) {
                        if ( targetFolderId.equals( info.getId() ) ) {
                            targetFolderHeader = new EntityHeader(info.getId(), EntityType.FOLDER, info.getName(), null, info.getVersion());
                        }
                    }

                    targetMigrationApi.importBundle( export, targetFolderHeader, false, overwriteDependencies, true, false );

                    migrationRecordManager.create( null, getUser(), sourceCluster, targetCluster, summarize(export, summarize(folders, targetFolderId), enableNewServices), new byte[]{} ); // TODO save migrated bundle
                }
            }
        } catch ( GatewayException ge ) {
            logger.log( Level.WARNING, "Error while performing migration.", ge );
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error while performing migration.", fe );
        } catch ( SaveException se ) {
            logger.log( Level.WARNING, "Error while performing migration.", se );
        } catch ( MigrationException me ) {
            logger.log( Level.WARNING, "Error while performing migration.", me );
        } catch (GatewayApi.GatewayException ge) {
            logger.log( Level.WARNING, "Error while performing migration.", ge );
        } catch (SOAPFaultException sfe) {
            logger.log( Level.WARNING, "Error while performing migration.", sfe );
        }
    }

    private String summarize( final Collection<GatewayApi.EntityInfo> folders, final String targetFolderId ) throws FindException {
        StringBuilder builder = new StringBuilder();

        List<GatewayApi.EntityInfo> folderPath = new ArrayList<GatewayApi.EntityInfo>();
        String targetId = targetFolderId;
        while ( targetId != null ) {
            GatewayApi.EntityInfo folder = null;
            for ( GatewayApi.EntityInfo info : folders ) {
                if ( targetId.equals( info.getId() ) ) {
                    folder = info;
                    break;
                }
            }

            if ( folder == null ) {
                throw new FindException("Could not find target folder.");               
            }

            targetId = folder.getParentId();
            if ( targetId != null || !folder.getName().equals("Root Node") ) {
                folderPath.add(0, folder);
            }
        }

        if ( folderPath.isEmpty() ) {
            builder.append("/");
        } else {
            for ( GatewayApi.EntityInfo folder : folderPath ) {
                builder.append( "/ " );
                builder.append( folder.getName() );
                builder.append( " " );
            }
        }

        return builder.toString().trim();
    }

    private String summarize( final MigrationBundle export, String targetFolderPath, final boolean enabled ) {
        StringBuilder builder = new StringBuilder();

        // overview
        builder.append( "Migration Options\n" );
        builder.append( "Imported to folder: " );
        builder.append( targetFolderPath );
        builder.append( "\n" );
        builder.append( "Services enabled on import: " );
        builder.append( enabled );
        builder.append( "\n" );
        builder.append( "Services migrated: " );
        builder.append( count(export.getExportedItems(),EntityType.SERVICE) );
        builder.append( "\n" );
        builder.append( "Policies migrated: " );
        builder.append( count(export.getExportedItems(),EntityType.POLICY) );
        builder.append( "\n\n" );

        // entity details
        MigrationMetadata metadata = export.getMetadata();
        builder.append( "Migrated Data\n" );
        for ( ExportedItem item : export.getExportedItems() ) {
            if ( !item.isMappedValue() && item.getHeaderRef().getType() != EntityType.FOLDER ) {
                EntityHeaderRef ref = item.getHeaderRef();
                EntityHeader header = metadata.getHeader( ref );
                builder.append( header.getType() );
                builder.append( ", " );
                builder.append( header.getName() );
                builder.append( "(#" );
                builder.append( ref.getStrId() );
                builder.append( ")" );
                builder.append( "\n" );
            }
        }
        builder.append( "\n" );

        // entity mappings
        StringBuilder mappingBuilder = new StringBuilder();
        for ( MigrationMapping mapping : metadata.getMappings() ) {
            if ( mapping.isMappedTarget() && !mapping.isUploadedByParent() ) {
                EntityHeaderRef sourceRef = mapping.getSource();
                EntityHeaderRef targetRef = mapping.getTarget();
                EntityHeader sourceHeader = metadata.getHeader( sourceRef );
                EntityHeader targetHeader = metadata.getHeader( targetRef );
                mappingBuilder.append( sourceRef.getType() );
                mappingBuilder.append( ", " );
                mappingBuilder.append( sourceHeader.getName() );
                mappingBuilder.append( " (#" );
                mappingBuilder.append( sourceHeader.getStrId() );
                mappingBuilder.append( ") mapped to " );
                mappingBuilder.append( targetHeader.getName() );
                mappingBuilder.append( " (#" );
                mappingBuilder.append( targetHeader.getStrId() );
                mappingBuilder.append( ")" );
                mappingBuilder.append( "\n" );
            }
        }
        String mappingText = mappingBuilder.toString();
        if ( !mappingText.isEmpty() ) {
            builder.append( "Mappings\n" );
            builder.append( mappingText );
        }

        return builder.toString();
    }

    private int count( final Collection<ExportedItem> items, final EntityType type ) {
        int count = 0;

        for ( ExportedItem item : items ) {
            if ( !item.isMappedValue() && item.getHeaderRef().getType() == type ) {
                count++;
            }
        }

        return count;
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
        private final EntityHeader header;

        DependencyKey( final String clusterId,
                       final EntityHeader header ) {
            this.clusterId = clusterId;
            this.type = header.getType();
            this.id = header.getStrId();
            this.header = header;
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

        public EntityHeader asEntityHeader() {
            return header;
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

        public String getType() {
            return fromEntityType(EntityType.valueOf(type));
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
