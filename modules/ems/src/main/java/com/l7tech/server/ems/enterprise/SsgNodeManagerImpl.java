package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.server.HibernateEntityManager;

/**
 * The implementation for SsgNodeManager that manages the ssg_node table.
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 14, 2008
 */
public class SsgNodeManagerImpl extends HibernateEntityManager<SsgNode, EntityHeader> implements SsgNodeManager {

    public Class<? extends Entity> getImpClass() {
        return SsgNode.class;
    }

    public Class<? extends Entity> getInterfaceClass() {
        return SsgNode.class;
    }

    public String getTableName() {
        return "ssg_node";
    }
}
