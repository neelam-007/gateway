package com.l7tech.server.ems.pages;

import com.l7tech.gateway.common.security.rbac.AttemptedDeleteSpecific;
import com.l7tech.gateway.common.security.rbac.RequiredPermissionSet;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ems.EmsSecurityManager;
import com.l7tech.server.ems.enterprise.*;
import com.l7tech.server.ems.gateway.*;
import com.l7tech.server.ems.user.UserPropertyManager;
import com.l7tech.server.management.api.node.GatewayApi;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 26, 2008
 */
@RequiredPermissionSet()
public class SSGClusterContentSelector extends EmsBaseWebPage {
    private static final Logger logger = Logger.getLogger(SSGClusterContentSelector.class.getName());

    @SpringBean
    GatewayClusterClientManager gatewayClusterClientManager;

    @SpringBean
    private EmsSecurityManager securityManager;

    @SpringBean
    private UserPropertyManager userPropertyManager;

    private EntityType[] entityTypes = new EntityType[] {
        EntityType.FOLDER,
        EntityType.SERVICE,
        EntityType.POLICY
    };

    protected boolean keepRootFolder = true;

    public SSGClusterContentSelector() {
        Map<String,String> up = Collections.emptyMap();
        try {
            up = userPropertyManager.getUserProperties( getUser() );
        } catch ( FindException fe ){
            logger.log( Level.WARNING, "Error loading user properties", fe );
        }
        final Map<String,String> userProperties = up;

        JsonInteraction interaction = new JsonInteraction("actiondiv", "clusterContentJasonUrl", new JsonDataProvider() {
            @Override
            public Object getData() {
                try {
                    return buildSsgClusterContent();
                } catch (GatewayNetworkException e) {
                    return new JSONException( new Exception("Gateway not available.") );                        
                } catch (GatewayException e) {
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }

            public void setData(Object jsonData) {
                throw new UnsupportedOperationException("setData not required in JsonInteraction");
            }
        });

        add(interaction);
    }

    protected void setEntityTypes(EntityType[] entityTypes) {
        this.entityTypes = entityTypes;
    }

    /**
     * Build a list of SSG Cluster content for entities (folder, published service, and policy fragment)
     * @return a list of entities content
     * @throws GatewayException if there is a problem accessing the Gateway cluster
     */
    private List<Object> buildSsgClusterContent() throws GatewayException {
        List<Object> jasonContentList = new ArrayList<Object>();

        // Get the content of entities using GatewayApi
        String ssgClusterId = RequestCycle.get().getRequest().getParameter("ssgClusterId");
        GatewayClusterClient cluster = gatewayClusterClientManager.getGatewayClusterClient(ssgClusterId, getUser());
        Collection<GatewayApi.EntityInfo> rawEntitiesInfo = cluster.getEntityInfo(Arrays.asList(entityTypes));
        Collections.sort((List<GatewayApi.EntityInfo>)rawEntitiesInfo);

        // Find the root folder and sort the raw entities data to have an ordered tree.
        List<GatewayApi.EntityInfo> sortedEntitiesInfo = new ArrayList<GatewayApi.EntityInfo>();
        GatewayApi.EntityInfo root = null;
        for (GatewayApi.EntityInfo info: rawEntitiesInfo) {
            if (info.getEntityType().equals(EntityType.FOLDER) && info.getParentId() == null) {
                root = info;
                break;
            }
        }
        preorderTraversal(sortedEntitiesInfo, root, rawEntitiesInfo);
        if (! keepRootFolder) sortedEntitiesInfo.remove(0);

        // Convert EntityInfo to SsgClusterContent while adding opertions content if a publiished service has opertions.
        List<SsgClusterContent> contentList = new ArrayList<SsgClusterContent>();
        for (GatewayApi.EntityInfo info: sortedEntitiesInfo) {
            // Add this entity content
            contentList.add(new SsgClusterContent(info.getId(), info.getParentId(), info.getEntityType(), info.getName(), info.getVersion()));

            // Add operation contents if the entity has operations
            if (info.getOperations() != null && info.getOperations().length > 0) {
                for (String operation: info.getOperations()) {
                    contentList.add(new SsgClusterContent(UUID.randomUUID().toString(), info.getId(), null, operation, null));
                }
            }
        }

        // Finally add rbac_cud into each jason content.
        final SsgCluster ssgCluster = cluster.getCluster();
        for (SsgClusterContent content: contentList) {
            jasonContentList.add(new JSONSupport(content) {
                @Override
                protected void writeJson() {
                    super.writeJson();
                    add(JSONConstants.RBAC_CUD, securityManager.hasPermission(new AttemptedDeleteSpecific(EntityType.ESM_SSG_CLUSTER, ssgCluster)));
                }
            });
        }

        return jasonContentList;
    }

    /**
     * Generate a new list of all entities content according to the preorder of folders, published services, and policy fragments.
     * @param sort
     * @param parent
     * @param raw
     */
    private void preorderTraversal(Collection<GatewayApi.EntityInfo> sort, GatewayApi.EntityInfo parent, Collection<GatewayApi.EntityInfo> raw) {
        if (parent == null) return;

        sort.add(parent);

        // If the entity is service or fragment, do not run the next step any more.
        if (parent.getEntityType().equals(EntityType.SERVICE) || parent.getEntityType().equals(EntityType.POLICY)) return;

        for (GatewayApi.EntityInfo child: raw) {
            if (parent.getId().equals(child.getParentId())) {
                preorderTraversal(sort, child, raw);
            }
        }
    }
}
