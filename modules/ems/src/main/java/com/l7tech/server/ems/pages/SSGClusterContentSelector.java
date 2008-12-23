package com.l7tech.server.ems.pages;

import com.l7tech.gateway.common.security.rbac.RequiredPermissionSet;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.ems.enterprise.*;
import com.l7tech.server.ems.gateway.*;
import com.l7tech.server.management.api.node.GatewayApi;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.*;
import java.util.logging.Logger;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 26, 2008
 */
@RequiredPermissionSet()
public class SSGClusterContentSelector extends EsmBaseWebPage {
    private static final Logger logger = Logger.getLogger(SSGClusterContentSelector.class.getName());

    @SpringBean
    GatewayClusterClientManager gatewayClusterClientManager;

    private EntityType[] entityTypes = new EntityType[] {
        EntityType.FOLDER,
        EntityType.SERVICE,
        EntityType.POLICY
    };

    protected boolean keepRootFolder = true;

    public SSGClusterContentSelector() {
        JsonInteraction interaction = new JsonInteraction("actiondiv", "clusterContentJsonUrl", new JsonDataProvider() {
            @SuppressWarnings({"ThrowableInstanceNeverThrown"})
            @Override
            public Object getData() {
                try {
                    return buildSsgClusterContent();
                } catch (GatewayNetworkException e) {
                    return new JSONMessage("Cluster not available.");                        
                } catch (GatewayNotMappedException e) {
                    return new JSONMessage("No access account.");
                } catch (GatewayNoTrustException e) {
                    return new JSONMessage("Trust not established.");    
                } catch (GatewayException e) {
                    if ( "Access Denied".equals(e.getMessage()) ||
                         "Authentication Required".equals(e.getMessage()) ||
                         "Not Licensed".equals(e.getMessage())) {
                        return new JSONMessage(e.getMessage()+".");
                    } else {
                        logger.warning(e.toString());
                        return new JSONException(e);
                    }
                }
            }

            @Override
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
        List<Object> jsonContentList = new ArrayList<Object>();

        // Get the content of entities using GatewayApi
        String ssgClusterId = RequestCycle.get().getRequest().getParameter("ssgClusterId");
        GatewayClusterClient cluster = gatewayClusterClientManager.getGatewayClusterClient(ssgClusterId, getUser());
        Collection<GatewayApi.EntityInfo> rawEntitiesInfo = cluster.getEntityInfo(Arrays.asList(entityTypes));
        // Sort the raw list by entity name with case insensitive.
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
        orderEntityInfosByPreorder(sortedEntitiesInfo, root, 0, rawEntitiesInfo);  // the raw list has been sorted by entity name first with case insensitive.
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

        // Finally add rbac_cud into each json content.
        for (SsgClusterContent content: contentList) {
            jsonContentList.add(new JSONSupport(content) {
                @Override
                protected void writeJson() {
                    super.writeJson();
                    add(JSONConstants.RBAC_CUD, true);
                }
            });
        }

        return jsonContentList;
    }

    /**
     * Sort a list of entity infos in preorder.  In every same level, the order of entity infos is Folder, Published Service, Policy Fragment.
     *
     * @param sort The list to store the ordered entity infos.
     * @param parent The parent entity info used to find all children entity infos.
     * @param insertIndex The index where the parent should be inserted into the "sort" list.
     * @param raw The original list of the unsorted entity infos.
     * @return an integer indicating how many entity infos have been inserted into the "sort" list.
     */
    private int orderEntityInfosByPreorder(List<GatewayApi.EntityInfo> sort, GatewayApi.EntityInfo parent, int insertIndex, Collection<GatewayApi.EntityInfo> raw) {
        int count = 0;
        int index = insertIndex;

        // Step 1: validate the parent and insert it into the sort list if it is not null.
        if (parent == null) {
            return 0;
        } else {
            sort.add(index++, parent); // add the parent into the sort list
            raw.remove(parent);        // remove the parent from the raw list to save memory and also save time for later recursives.
            count++;                   // increment the counter
        }

        // Step 2: find all published services and policy fragments and directly insert them into the "sort" list.
        // Also, find all children folders of the parent without insertion and store them into a temp list, folderList.
        // The temp list will be used in Step 3.
        List<GatewayApi.EntityInfo> folderList = new ArrayList<GatewayApi.EntityInfo>();
        for (EntityType type: entityTypes) {               // run insertions by each entity type, so the order of inserted entity infos is the same as the order of the entity types in the type list.
            for (Iterator<GatewayApi.EntityInfo> itr = raw.iterator(); itr.hasNext();) {
                GatewayApi.EntityInfo info = itr.next();
                if (info.getEntityType().equals(type) && parent.getId().equals(info.getParentId())) {
                    if (type.equals(EntityType.FOLDER)) {  // add a folder into the temp folder list if the type is Folder.
                        folderList.add(info);
                    } else {                               // insert a published service or policy fragment into the sort list if the type is not Folder
                        sort.add(index++, info);
                        count++;
                    }
                    itr.remove();
                }
            }
        }

        // Step 3: recursively run through the children folder list to add decedants into the "sort" list in preorder.
        index = insertIndex + 1;   // initiate the insertion index of the first recursive
        for (GatewayApi.EntityInfo info: folderList) {
            int n = orderEntityInfosByPreorder(sort, info, index, raw);
            index += n;            // update the insertion index for the next recursive.
            count += n;            // increment the counter.
        }

        return count;
    }
}
