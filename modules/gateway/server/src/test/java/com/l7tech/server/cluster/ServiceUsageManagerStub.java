package com.l7tech.server.cluster;

import com.l7tech.gateway.common.cluster.ServiceUsage;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;

public class ServiceUsageManagerStub implements ServiceUsageManager {

    @Transactional(readOnly = true)
    public Collection<ServiceUsage> getAll() throws FindException {
        return Collections.emptyList();
    }

    @Transactional(readOnly = true)
    public ServiceUsage[] findByNode(String nodeId) throws FindException {
        return new ServiceUsage[0];
    }

    @Transactional(readOnly = true)
    public ServiceUsage[] findByServiceGoid(Goid serviceGoid) throws FindException {
        return new ServiceUsage[0];
    }

    public void record(ServiceUsage data) throws UpdateException {
    }

    public void clear(String nodeid) throws DeleteException {
    }
}
