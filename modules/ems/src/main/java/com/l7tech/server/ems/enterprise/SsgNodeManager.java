package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.GoidEntityManager;

/**
 * This class manages the ssg_node table.
 *
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 14, 2008
 */
public interface SsgNodeManager extends GoidEntityManager<SsgNode, EntityHeader> {

    SsgNode findByGuid(String guid) throws FindException;
}
