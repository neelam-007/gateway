package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;

/**
 * This class manages the ssg_node table.
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 14, 2008
 */
public interface SsgNodeManager extends EntityManager<SsgNode, EntityHeader> {

    SsgNode findByGuid(String guid) throws FindException;
}
