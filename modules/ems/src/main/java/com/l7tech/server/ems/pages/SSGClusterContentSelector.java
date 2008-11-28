package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.RequestCycle;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.l7tech.server.ems.enterprise.*;
import com.l7tech.server.ems.EmsSecurityManager;
import com.l7tech.server.ems.gateway.GatewayContextFactory;
import com.l7tech.server.ems.gateway.GatewayContext;
import com.l7tech.server.ems.user.UserPropertyManager;
import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteSpecific;
import com.l7tech.gateway.common.security.rbac.RequiredPermissionSet;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.identity.User;

import javax.servlet.http.HttpServletRequest;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 26, 2008
 */
@RequiredPermissionSet()
public class SSGClusterContentSelector extends WebPage {
    private static final Logger logger = Logger.getLogger(SSGClusterContentSelector.class.getName());

    @SpringBean
    GatewayContextFactory gatewayContextFactory;

    @SpringBean
    private SsgClusterManager ssgClusterManager;

    @SpringBean
    private EmsSecurityManager securityManager;

    @SpringBean
    private UserPropertyManager userPropertyManager;

    private EntityType[] entityTypes = new EntityType[] {
        EntityType.FOLDER,
        EntityType.SERVICE,
        EntityType.POLICY
    };

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
                } catch (Exception e) {
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
     * @throws Exception
     */
    private List<Object> buildSsgClusterContent() throws Exception {
        List<Object> jasonContentList = new ArrayList<Object>();

        // Get the content of entities using GatewayApi
        String ssgClusterId = RequestCycle.get().getRequest().getParameter("ssgClusterId");
        final SsgCluster ssgCluster = ssgClusterManager.findByGuid(ssgClusterId);
        GatewayContext context = gatewayContextFactory.getGatewayContext(getUser(), ssgCluster.getSslHostName(), ssgCluster.getAdminPort());
        GatewayApi api = context.getApi();
        Collection<GatewayApi.EntityInfo> rawEntitiesInfo = api.getEntityInfo(Arrays.asList(entityTypes));
        Collections.sort((List<GatewayApi.EntityInfo>)rawEntitiesInfo);

        // Find the root folder and sort the raw entities data to have an ordered tree.
        Collection<GatewayApi.EntityInfo> sortedEntitiesInfo = new ArrayList<GatewayApi.EntityInfo>();
        GatewayApi.EntityInfo root = null;
        for (GatewayApi.EntityInfo info: rawEntitiesInfo) {
            if (info.getEntityType().equals(EntityType.FOLDER) && info.getParentId() == null) {
                root = info;
                break;
            }
        }
        preorderTraversal(sortedEntitiesInfo, root, rawEntitiesInfo);

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

    private EmsSecurityManager.LoginInfo getLoginInfo() {
        EmsSecurityManager.LoginInfo info;

        ServletWebRequest servletWebRequest = (ServletWebRequest) getRequest();
        HttpServletRequest request = servletWebRequest.getHttpServletRequest();
        info = securityManager.getLoginInfo( request.getSession(true) );

        return info;
    }


    private User getUser() {
        User user = null;

        EmsSecurityManager.LoginInfo info = getLoginInfo();
        if ( info != null ) {
            user = info.getUser();
        }

        return user;
    }
}
