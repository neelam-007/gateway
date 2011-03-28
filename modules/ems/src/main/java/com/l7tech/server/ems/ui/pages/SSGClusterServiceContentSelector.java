package com.l7tech.server.ems.ui.pages;

import com.l7tech.gateway.common.security.rbac.RequiredPermissionSet;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.ems.gateway.GatewayClusterClientManager;
import org.apache.wicket.RequestCycle;

import javax.inject.Inject;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 28, 2008
 */
@RequiredPermissionSet()
public class SSGClusterServiceContentSelector extends EsmBaseWebPage {

    @Inject
    GatewayClusterClientManager gatewayClusterClientManager;

    private static final EntityType[] entityTypes = new EntityType[] {
        EntityType.FOLDER,
        EntityType.SERVICE,
        EntityType.SERVICE_ALIAS
    };

    public SSGClusterServiceContentSelector() {
        JsonInteraction interaction = new JsonInteraction("actiondiv", "clusterContentJsonUrl", new JsonDataProvider() {
            @SuppressWarnings({"ThrowableInstanceNeverThrown"})
            @Override
            public Object getData() {
                return SSGClusterContentSelectorPanel.buildTreeData(
                        gatewayClusterClientManager,
                        getUser(),
                        RequestCycle.get().getRequest().getParameter( "ssgClusterId" ),
                        entityTypes );
            }

            @Override
            public void setData(Object jsonData) {
                throw new UnsupportedOperationException("setData not required in JsonInteraction");
            }
        });

        add(interaction);
    }
}