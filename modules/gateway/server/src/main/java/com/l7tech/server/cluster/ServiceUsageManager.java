/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.cluster;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.gateway.common.cluster.ServiceUsage;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * @author alex
 */
public interface ServiceUsageManager {
    String TABLE_NAME = "service_usage";
    String NODE_ID_COLUMN_NAME = "nodeid";
    String SERVICE_ID_COLUMN_NAME = "serviceid";

    @Transactional(readOnly=true)
    Collection<ServiceUsage> getAll() throws FindException;

    @Transactional(readOnly=true)
    ServiceUsage[] findByNode(String nodeId) throws FindException;

    @Transactional(readOnly=true)
    ServiceUsage[] findByServiceGoid(Goid serviceGoid) throws FindException;

    void record(ServiceUsage data) throws UpdateException;

    void clear(String nodeid) throws DeleteException;
}
