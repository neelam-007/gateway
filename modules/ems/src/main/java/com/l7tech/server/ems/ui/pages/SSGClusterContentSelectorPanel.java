package com.l7tech.server.ems.ui.pages;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.ems.enterprise.*;
import com.l7tech.server.ems.gateway.*;
import com.l7tech.server.ems.util.JsonUtil;
import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.util.Functions;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.inject.Inject;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

/**
 * Cluster content selection tree component.
 */
public class SSGClusterContentSelectorPanel extends Panel {

    //- PUBLIC

    /**
     * Create a new cluster content selector.
     *
     * @param id The markup ideitifer the component.
     * @param userModel The user model
     * @param clusterId The identifier (GUID) of the cluster to display (may be null)
     * @param highlightable The set of highlightable entity types
     * @param selectable The set of selectable entity types
     * @param multipleSelections True to permit multiple selection (checkboxes), false for single selection (radio buttons)
     */
    public SSGClusterContentSelectorPanel( final String id,
                                           final IModel<User> userModel,
                                           final String clusterId,
                                           final Set<EntityType> highlightable,
                                           final Set<EntityType> selectable,
                                           final boolean multipleSelections ) {
        super(id);
        this.userModel = userModel;
        this.clusterId = clusterId;
        this.highlightable = highlightable;
        this.selectable = selectable;
        this.multipleSelections = multipleSelections;
        setMarkupId(id);
        setOutputMarkupId( true );

        treeHolder = new WebMarkupContainer("entityTreeTableBody");
        treeHolder.setOutputMarkupId( true );
        add( treeHolder );

        scriptLabel = new Label("tableScript", buildScriptModel());
        add( scriptLabel.setEscapeModelStrings( false ) );

        add( new AbstractDefaultAjaxBehavior() {
            @Override
            public CharSequence getCallbackUrl( final boolean onlyTargetActivePage ) {
                return super.getCallbackUrl( onlyTargetActivePage ) + "&entityId='+entityId+'&entityType='+entityType+'&entityAction='+entityAction+'";
            }

            @Override
            public void renderHead( final IHeaderResponse response ) {
                super.renderHead( response );
                response.renderJavascript("function ssgClusterContentSelectorCallback"+id+"(entityId,entityType,entityAction){"+getCallbackScript(true)+"}", null);
            }

            @SuppressWarnings({ "unchecked" })
            @Override
            protected void respond( final AjaxRequestTarget ajaxRequestTarget ) {
                final String entityId = RequestCycle.get().getRequest().getParameter("entityId");
                final String entityTypeStr = RequestCycle.get().getRequest().getParameter("entityType");
                final String entityAction = RequestCycle.get().getRequest().getParameter("entityAction");

                if ( entityTypeStr != null && entityId != null && entityAction != null ) {
                    final EntityType entityType = JSONConstants.EntityType.ENTITY_TYPE_MAP.get( entityTypeStr );

                    if ( entityType != null ) {
                        if ( "highlighted".equals( entityAction ) ) {
                            onEntityHighlighted( ajaxRequestTarget, entityType, entityTypeStr, entityId );
                        } else if ( "selected".equals( entityAction ) ) {
                            if ( clusterData instanceof List ) {
                                updateSelections( (List<SsgClusterContent>) clusterData, entityType, entityId );
                            }
                            rebuildScriptModel();
                            onSelectionChanged( ajaxRequestTarget );
                        }
                    }
                }

            }
        } );
    }

    /**
     * Refresh cluster content.
     */
    public final void refresh() {
        clusterData = null;
        rebuildScriptModel();
    }

    /**
     * Get the currently selected cluster identifier.
     *
     * @return The cluster identifier (GUID)
     */
    public final String getClusterId() {
        return clusterId;
    }

    /**
     * Set the currently selected cluster identifier.
     *
     * @param clusterId The cluster identifier (GUID)
     */
    public final void setClusterId( final String clusterId ) {
        this.clusterId = clusterId;
        selectedIds.clear();
        refresh();
    }

    /**
     * Get the selected content.
     *
     * @return The collection of selected content (never null)
     */
    @SuppressWarnings({ "unchecked" })
    public final Collection<SsgClusterContent> getSelectedContent() {
        final List<SsgClusterContent> selectedContent = new ArrayList<SsgClusterContent>();

        if ( clusterData instanceof List ) {
            for ( final SsgClusterContent content : (List<SsgClusterContent>) clusterData ) {
                if ( selectedIds.contains( content.getExternalId() ) ) {
                    selectedContent.add( content );
                }
            }
        }

        return selectedContent;
    }

    /**
     * Get the identifiers of the selected content.
     *
     * <p>Note that is is possible that identifiers do not match the available content
     * for the cluster.</p>
     *
     * @return The content identifiers.
     */
    public final Set<String> getSelectedContentIds() {
        return Collections.unmodifiableSet( new HashSet<String>( selectedIds ) );
    }

    /**
     * Set the identifiers of the selected content.
     *
     * @param identifiers The identifiers to use (required)
     */
    public final void setSelectedContentIds( final Set<String> identifiers ) {
        selectedIds.clear();
        selectedIds.addAll( identifiers );
    }

    /**
     * Set the data provider for a cluster.
     *
     * @param clusterId The id of the cluster
     * @param provider The (Serializable) provider
     */
    public final void setProviderForCluster( final String clusterId,
                                             final SsgClusterContentProvider provider ) {
        providersByCluster.put( clusterId, provider );
    }

    /**
     * Get the current message for the cluster selector.
     *
     * @return The message or null
     */
    public final String getMessage() {
        return message;
    }

    /**
     * Set the message for the cluster selector.
     *
     * <p>The message will be displayed in place of the cluster content.</p>
     *
     * @param message The message to display.
     */
    public final void setMessage( final String message ) {
        this.message = message;
        rebuildScriptModel();
    }

    /**
     * Clear the current message.
     */
    public final void clearMessage() {
        setMessage( null );
        rebuildScriptModel();
    }

    @Override
    public final void detachModels() {
        super.detachModels();

        userModel.detach();

        for ( final SsgClusterContentProvider provider : providersByCluster.values() ) {
            if ( provider instanceof IDetachable ) {
                ((IDetachable)provider).detach();
            }
        }
    }

    /**
     * Interface for implementations of cluster content providers.
     *
     * @see #setProviderForCluster(String,SsgClusterContentProvider)
     */
    public static interface SsgClusterContentProvider extends Serializable {
        Collection<SsgClusterContent> getContent( EntityType[] entityTypes ) throws GatewayException;
    }

    //- PROTECTED

    protected void onEntityHighlighted( final AjaxRequestTarget ajaxRequestTarget, final EntityType type, final String jsonEntityType, final String entityId ) {
    }

    protected void onSelectionChanged( final AjaxRequestTarget ajaxRequestTarget ) {
    }

    protected final void setEntityTypes(EntityType[] entityTypes) {
        this.entityTypes = entityTypes;
    }

    //- PACKAGE

    static Object buildTreeData( final GatewayClusterClientManager gatewayClusterClientManager,
                                 final User user,
                                 final String clusterId,
                                 final EntityType[] entityTypes ) {

        return buildTreeData(
                new DefaultSsgClusterContentProvider( gatewayClusterClientManager, user, clusterId ),
                entityTypes );
    }

    static Object buildTreeData( final SsgClusterContentProvider provider,
                                 final EntityType[] entityTypes ) {
        try {
            return buildSsgClusterContent( provider, entityTypes );
        } catch (FailoverException e) {
            return new JSONMessage("Cluster not available.");
        } catch (GatewayNotMappedException e) {
            return new JSONMessage("No access account.");
        } catch (GatewayNoTrustException e) {
            return new JSONMessage("Trust not established.");
        } catch (GatewayException e) {
            if ( GatewayContext.isConfigurationException(e) ) {
                return new JSONMessage(e.getMessage()+".");
            } else {
                logger.warning(e.toString());
                return new JSONException(e);
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SSGClusterContentSelectorPanel.class.getName());


    private static final Collection<EnumSet<EntityType>> contentOrder = Arrays.asList(
            EnumSet.of( EntityType.FOLDER ),
            EnumSet.of( EntityType.SERVICE, EntityType.SERVICE_ALIAS ),
            EnumSet.of( EntityType.POLICY, EntityType.POLICY_ALIAS ) );

    @Inject
    private GatewayClusterClientManager gatewayClusterClientManager;

    private final IModel<User> userModel;

    private EntityType[] entityTypes = new EntityType[] {
        EntityType.FOLDER,
        EntityType.SERVICE,
        EntityType.SERVICE_ALIAS,
        EntityType.POLICY,
        EntityType.POLICY_ALIAS
    };

    private String clusterId;
    private Object clusterData;
    private String message;
    private final Set<String> selectedIds = new HashSet<String>(); //TODO this is incorrect since IDs are not guaranteed to be unique across types (the tree component needs fixing)
    private final Set<EntityType> highlightable;
    private final Set<EntityType> selectable;
    private final boolean multipleSelections;
    private final Map<String,SsgClusterContentProvider> providersByCluster = new HashMap<String,SsgClusterContentProvider>();

    private final WebMarkupContainer treeHolder;
    private final Label scriptLabel;

    /**
     * If an entity is not a folder then it can be directly selected.
     *
     * For folders we need to select/deselect all items under the folder.
     */
    private boolean updateSelections( final List<SsgClusterContent> contents,
                                      final EntityType entityType,
                                      final String entityId ) {
        boolean selected;

        // clear any invalid selections
        selectedIds.retainAll( Functions.map( contents, new Functions.Unary<String,SsgClusterContent>(){
            @Override
            public String call( final SsgClusterContent ssgClusterContent ) {
                return ssgClusterContent.getExternalId();
            }
        } ) );

        if ( !multipleSelections ) { // radio button style selection
            selected = true;
            selectedIds.clear();
            selectedIds.add( entityId );
        } else if ( entityType == EntityType.FOLDER ) {
            final List<String> descendantFolderIds = new ArrayList<String>();
            descendantFolderIds.add( entityId );
            final List<String> selectionIds = new ArrayList<String>();
            for ( final SsgClusterContent content : contents ) {
                if ( content.isOperation() ) continue;

                if ( descendantFolderIds.contains( content.getParentId() ) ) {
                    if ( content.getEntityType() == EntityType.FOLDER ) {
                        descendantFolderIds.add( content.getExternalId() );
                    } else {
                        selectionIds.add( content.getExternalId() );
                    }
                }
            }

            // If any are currently selected then clear all the
            // selections. Else select them all.
            selected = !selectedIds.removeAll( selectionIds );
            if ( selected ) {
                selectedIds.addAll( selectionIds );
            }
        } else {
            selected = !selectedIds.contains( entityId );
            if ( selected ) {
                selectedIds.add( entityId );
            } else {
                selectedIds.remove( entityId );
            }
        }

        return selected;
    }

    private void rebuildScriptModel() {
        scriptLabel.setDefaultModelObject( buildScriptModel() );
    }

    private IModel buildScriptModel() {
        return new Model(){
            @Override
            public Serializable getObject() {
                final StringBuffer builder = new StringBuffer( 2048 );
                builder.append("YAHOO.util.Event.onDOMReady( function(){\n");
                builder.append( "var highlightableEntities = " ).append( toJson(highlightable) ).append( ";\n" );
                builder.append( "var entitiesWithTristateCheckbox = " ).append( toJson( multipleSelections ? selectable : null ) ).append( ";\n" );
                builder.append( "var entitiesWithRadioButton = " ).append( toJson( !multipleSelections ? selectable : null ) ).append( ";\n" );
                builder.append( "var treeData = " );
                JsonUtil.writeJson( getTreeData(), builder );
                builder.append( ";\n" );
                builder.append( "initContentEntityTree('" ).append( treeHolder.getMarkupId() ).append( "', treeData,").append( toJson(selectedIds,null) ).append(", ssgClusterContentSelectorCallback" ).append( getMarkupId() ).append( ", highlightableEntities, entitiesWithTristateCheckbox, entitiesWithRadioButton );\n" );
                builder.append("} );");
                return builder.toString();
            }
        };
    }

    private <T> String toJson( final Set<T> values,
                               final Functions.Unary<String,T> stringer ) {
        final StringBuilder builder = new StringBuilder();

        if ( values == null ) {
            builder.append( "null" );
        } else {
            boolean first = true;
            builder.append( "[" );
            for ( final T value : values ) {
                final String stringValue = stringer==null ? value.toString() : stringer.call( value );
                if ( stringValue != null ) {
                    if ( !first ) builder.append( "," );
                    builder.append( " '" );
                    builder.append( stringValue );
                    builder.append( "'" );
                    first = false;
                }
            }
            builder.append( " ]" );
        }

        return builder.toString();
    }

    private String toJson( final Set<EntityType> types ) {
        return toJson( types, new Functions.Unary<String, EntityType>() {
            @Override
            public String call( final EntityType entityType ) {
                return SsgClusterContent.getJsonType( entityType );
            }
        } );
    }

    private Object getTreeData() {
        if ( message != null ) {
            return new JSONMessage(message);
        }

        Object treeData = this.clusterData;

        if ( treeData == null ) {
            treeData = this.clusterData = buildTreeData();
        }

        return treeData;
    }

    private Object buildTreeData() {
        SsgClusterContentProvider provider = providersByCluster.get( clusterId );
        if ( provider == null ) {
            provider = new DefaultSsgClusterContentProvider( gatewayClusterClientManager, userModel.getObject(), clusterId );
        }
        return buildTreeData( provider, entityTypes );
    }

    private static final class DefaultSsgClusterContentProvider implements SsgClusterContentProvider {
        private final GatewayClusterClientManager gatewayClusterClientManager;
        private final User user;
        private final String clusterId;

        private DefaultSsgClusterContentProvider( final GatewayClusterClientManager gatewayClusterClientManager,
                                                  final User user,
                                                  final String clusterId ) {
            this.gatewayClusterClientManager = gatewayClusterClientManager;
            this.user = user;
            this.clusterId = clusterId;
        }

        @Override
        public Collection<SsgClusterContent> getContent( final EntityType[] entityTypes ) throws GatewayException {
            final List<SsgClusterContent> contents = new ArrayList<SsgClusterContent>();

            if ( clusterId != null ) {
                final GatewayClusterClient cluster = gatewayClusterClientManager.getGatewayClusterClient( clusterId,user );
                final Collection<GatewayApi.EntityInfo> entityInfo = cluster.getEntityInfo(Arrays.asList(entityTypes));

                // Convert EntityInfo to SsgClusterContent while adding operation content if a published service has operations.
                for ( final GatewayApi.EntityInfo info: entityInfo ) {
                    // Add this entity content
                    contents.add( new SsgClusterContent( info.getExternalId(), info.getRelatedId(), info.getParentId(), info.getEntityType(), info.getName(), info.getVersion() ) );

                    // Add operation contents if the entity has operations
                    if (info.getOperations() != null && info.getOperations().length > 0) {
                        for (String operation : info.getOperations()) {
                            contents.add( new SsgClusterContent( UUID.randomUUID().toString(), info.getId(), operation ) );
                        }
                    }
                }
            }

            return contents;
        }
    }

    /**
     * Build a list of SSG Cluster content for entities (folder, published service, and policy fragment)
     * @return a list of entities content
     * @throws GatewayException if there is a problem accessing the Gateway cluster
     */
    private static List<SsgClusterContent> buildSsgClusterContent( final SsgClusterContentProvider provider,
                                                                   final EntityType[] entityTypes ) throws GatewayException {
        final List<SsgClusterContent> contentList = new ArrayList<SsgClusterContent>( provider.getContent( entityTypes ) );

        // Sort the raw list by name with case insensitive.
        Collections.sort(contentList);

        // Find the root folder and sort the raw entities data to have an ordered tree.
        final List<SsgClusterContent> sortedContentList = new ArrayList<SsgClusterContent>(contentList.size());
        SsgClusterContent root = null;
        for ( final SsgClusterContent content : contentList ) {
            if ( content.getEntityType() == EntityType.FOLDER && content.getParentId() == null ) {
                root = content;
                break;
            }
        }

        orderContentByPreorder( sortedContentList, root, contentList );

        return sortedContentList;
    }

    /**
     * Sort a list of SsgClusterContent in preorder.
     *
     * @param sort The list to store the ordered SsgClusterContent.
     * @param parent The parent entity info used to find all children SsgClusterContents.
     * @param raw The original list of the unsorted SsgClusterContents.
     * @return an integer indicating how many SsgClusterContents have been inserted into the "sort" list.
     */
    private static void orderContentByPreorder( final List<SsgClusterContent> sort,
                                                final SsgClusterContent parent,
                                                final Collection<SsgClusterContent> raw ) {
        if ( parent != null ) {
            sort.add( parent );

            boolean firstPass = true;
            for ( final EnumSet<EntityType> types : contentOrder ) {
                for ( final SsgClusterContent info : raw ) {
                    if ( parent.getExternalId().equals( info.getParentId() ) && ((info.getEntityType() == null && firstPass) || types.contains( info.getEntityType() )) ) {
                        orderContentByPreorder( sort, info, raw );
                    }
                }
                firstPass = false;
            }
        }
    }
}
