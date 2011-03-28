package com.l7tech.server.ems.ui.pages;

import com.l7tech.gateway.common.security.rbac.AttemptedDeleteSpecific;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ems.enterprise.EnterpriseFolder;
import com.l7tech.server.ems.enterprise.EnterpriseFolderManager;
import com.l7tech.server.ems.enterprise.JSONConstants;
import com.l7tech.server.ems.enterprise.JSONException;
import com.l7tech.server.ems.enterprise.JSONMessage;
import com.l7tech.server.ems.enterprise.JSONSupport;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.gateway.FailoverException;
import com.l7tech.server.ems.gateway.GatewayContext;
import com.l7tech.server.ems.ui.EsmSecurityManager;
import com.l7tech.server.ems.user.UserPropertyManager;
import com.l7tech.server.ems.util.JsonUtil;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import javax.inject.Inject;
import javax.xml.ws.WebServiceException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cluster selection tree component.
 */
public class SSGClusterSelectorPanel extends Panel {

    //- PUBLIC

    /**
     * Create a new Cluster selection component.
     *
     * @param id The markup identifier for the component
     * @param userModel The user model
     * @param clusterModel The cluster model
     */
    public SSGClusterSelectorPanel( final String id,
                                    final IModel<User> userModel,
                                    final IModel<String> clusterModel ) {
        super( id );
        this.userModel = userModel;
        this.cluster = clusterModel;
        setMarkupId( id );
        setOutputMarkupId( true );

        treeHolder = new WebMarkupContainer("entityTreeTableBody");
        treeHolder.setOutputMarkupId( true );
        add( treeHolder );

        scriptLabel = new Label("tableScript", buildScriptModel());
        add( scriptLabel.setEscapeModelStrings( false ) );

        add( new AbstractDefaultAjaxBehavior() {
            @Override
            public CharSequence getCallbackUrl( final boolean onlyTargetActivePage ) {
                return super.getCallbackUrl( onlyTargetActivePage ) + "&clusterGuid='+clusterGuid+'&clusterAction='+clusterAction+'";
            }

            @Override
            public void renderHead( final IHeaderResponse response ) {
                super.renderHead( response );
                response.renderJavascript("function ssgClusterSelectorCallback"+id+"(clusterGuid,clusterAction){ "+getCallbackScript(true)+"; } ", null);
            }

            @Override
            protected void respond( final AjaxRequestTarget ajaxRequestTarget ) {
                final String clusterGuid = RequestCycle.get().getRequest().getParameter("clusterGuid");
                final String clusterAction = RequestCycle.get().getRequest().getParameter("clusterAction");

                if ( clusterGuid != null && clusterAction != null ) {
                    if ( "selected".equals( clusterAction ) ) {
                        cluster.setObject( clusterGuid );
                        onClusterSelected( ajaxRequestTarget, clusterGuid );
                    } else if ( "trust".equals( clusterAction ) ) {
                        onClusterTrust( clusterGuid, ajaxRequestTarget );
                    } else if ( "access".equals( clusterAction ) ) {
                        onClusterAccess( clusterGuid, ajaxRequestTarget );
                    }
                }
            }
        } );
    }

    /**
     * Refesh the cluster information.
     */
    public void refresh() {
        scriptLabel.setDefaultModelObject( buildScriptModel() );
    }

    /**
     * Get the identifier for the currently selected cluster.
     *
     * @return The cluster identifier (GUID)
     */
    public String getClusterId() {
        return cluster.getObject();
    }

    /**
     * Get the currently displayed message.
     *
     * @return The message or null
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the currently displayed message.
     *
     * <p>The message will be displayed instead of the cluster tree.</p>
     *
     * @param message The message to display.
     */
    public void setMessage( final String message ) {
        this.message = message;
        refresh();
    }

    /**
     * Clear the current message.
     */
    public void clearMessage() {
        setMessage( null );
        refresh();
    }

    /**
     * Get the set of offline clusters that are being displayed.
     *
     * @return The cluster identifiers (GUIDs, never null)
     */
    public final Set<String> getIncludedOfflineClusters() {
        return Collections.unmodifiableSet( new HashSet<String>( offlineClusterIds ) );
    }

    /**
     * Set the included offline clusters.
     *
     * <p>These offline clusters will be available for selection.</p>
     *
     * @param clusterIds The cluster identifiers (GUIDs, required)
     */
    public final void setIncludedOfflineClusters( final Set<String> clusterIds ) {
        this.offlineClusterIds.clear();
        this.offlineClusterIds.addAll( clusterIds );
        refresh();
    }

    @Override
    public final void detachModels() {
        super.detachModels();

        userModel.detach();
    }

    //- PROTECTED

    protected void onClusterSelected( final AjaxRequestTarget ajaxRequestTarget, final String clusterId ) {
    }

    protected void onClusterTrust( final String clusterId,
                                   final AjaxRequestTarget ajaxRequestTarget ) {
    }

    protected void onClusterAccess( final String clusterId,
                                    final AjaxRequestTarget ajaxRequestTarget ) {
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SSGClusterSelectorPanel.class.getName());

    @Inject
    private EnterpriseFolderManager enterpriseFolderManager;

    @Inject
    private SsgClusterManager ssgClusterManager;

    @Inject
    private UserPropertyManager userPropertyManager;

    @Inject
    private EsmSecurityManager securityManager;

    private final IModel<User> userModel;

    private final IModel<String> cluster;

    private final WebMarkupContainer treeHolder;

    private final Label scriptLabel;

    private String message;
    private final Set<String> offlineClusterIds = new HashSet<String>();

    private String buildScriptModel() {
        final StringBuffer builder = new StringBuffer( 2048 );
        builder.append("YAHOO.util.Event.onDOMReady( function(){\n");
        builder.append( "var treeData = " );
        JsonUtil.writeJson( message!=null ? new JSONMessage(message) : buildClusterData(), builder );
        builder.append( ";\n" );
        builder.append( "initEntityTreeTable('" ).append( treeHolder.getMarkupId() ).append( "', treeData, ").append( quote( cluster.getObject() ) ).append( ", ssgClusterSelectorCallback" ).append( getMarkupId() ).append( ");\n" );
        builder.append("} );");
        return builder.toString();
    }

    /**
     * Simple string value quoter, assumes no quotes in text
     */
    private String quote( final String text ) {
        String value = text;

        if ( value != null ) {
            value = "'" + value + "'";
        }

        return value;
    }

    private Object buildClusterData() {
        final Map<String,String> userProperties;
        try {
            userProperties = userPropertyManager.getUserProperties( userModel.getObject() );
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error loading user properties", fe );
            return new JSONException(new Exception("Error loading user information.", fe));
        }

        try {
            final List<Object> entities = new ArrayList<Object>();
            final EnterpriseFolder rootFolder = enterpriseFolderManager.findRootFolder();
            entities.add(new JSONSupport( rootFolder ){
                @Override
                protected void writeJson() {
                    super.writeJson();
                    add( JSONConstants.RBAC_CUD, securityManager.hasPermission( new AttemptedDeleteSpecific( EntityType.ESM_ENTERPRISE_FOLDER, rootFolder)) );
                }
            } );
            addChildren(userProperties, entities, rootFolder);
            return entities;
        } catch (FailoverException fo) {
            return new JSONException(fo);
        } catch (WebServiceException e) {
            if ( GatewayContext.isNetworkException( e ) ) {
                return new JSONException("Gateway not available.");
            } else if ( GatewayContext.isConfigurationException(e) ) {
                return new JSONException(e.getMessage());
            } else {
                logger.warning(e.toString());
                return new JSONException(e);
            }
        } catch (FindException fe) {
            logger.log( Level.WARNING, "Error loading enterprise folders.", fe );
            return new JSONException(new Exception("Error loading folders.", fe));
        }

    }

    private void addChildren( final Map<String,String> userProperties,
                              final List<Object> nodes,
                              final EnterpriseFolder folder) throws FindException {
        // Display order is alphabetical on name, with folders before clusters.
        for ( final EnterpriseFolder childFolder : enterpriseFolderManager.findChildFolders(folder)) {
            nodes.add( new JSONSupport( childFolder ){
                @Override
                protected void writeJson() {
                    super.writeJson();
                    add(JSONConstants.RBAC_CUD, securityManager.hasPermission( new AttemptedDeleteSpecific(EntityType.ESM_ENTERPRISE_FOLDER, childFolder)) );
                }
            }  );
            addChildren(userProperties, nodes, childFolder);
        }

        for ( final SsgCluster childCluster : ssgClusterManager.findChildSsgClusters(folder, true) ) {
            if ( childCluster.isOffline() && !offlineClusterIds.contains( childCluster.getGuid() ) ) {
                continue;
            }

            nodes.add( new JSONSupport( childCluster ){
                @Override
                protected void writeJson() {
                    super.writeJson();
                    add(JSONConstants.RBAC_CUD, securityManager.hasPermission( new AttemptedDeleteSpecific(EntityType.ESM_SSG_CLUSTER, childCluster)) );
                    if ( !childCluster.isOffline() ) {
                        add(JSONConstants.ACCESS_STATUS, userProperties.containsKey("cluster." +  childCluster.getGuid() + ".trusteduser"));
                    }
                }
            });
        }
    }
}
