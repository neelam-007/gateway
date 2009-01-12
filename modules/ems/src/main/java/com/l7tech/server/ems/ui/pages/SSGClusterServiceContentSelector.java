package com.l7tech.server.ems.ui.pages;

import com.l7tech.objectmodel.EntityType;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 28, 2008
 */
public class SSGClusterServiceContentSelector extends SSGClusterContentSelector {
    public SSGClusterServiceContentSelector() {
        setEntityTypes(new EntityType[] {
            EntityType.FOLDER,
            EntityType.SERVICE,
            EntityType.SERVICE_ALIAS
        });
    }
}
