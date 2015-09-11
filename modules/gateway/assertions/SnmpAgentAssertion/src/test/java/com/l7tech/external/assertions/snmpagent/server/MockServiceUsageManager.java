package com.l7tech.external.assertions.snmpagent.server;

import com.l7tech.gateway.common.cluster.ServiceUsage;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.cluster.ServiceUsageManager;

import java.util.*;

import static com.l7tech.external.assertions.snmpagent.server.MockSnmpValues.*;

/**
 * User: rseminoff
 * Date: 14/05/12
 */
public class MockServiceUsageManager implements ServiceUsageManager {

    @Override
    public Collection<ServiceUsage> getAll() throws FindException {
        return new ArrayList<ServiceUsage>() {{
            add(new ServiceUsage() {{
                setServiceid(new Goid(TEST_SERVICE_GOID_STR));
                setNodeid(TEST_SERVICE_NAME);
                setName(TEST_SERVICE_NAME);
                setRequests(TEST_REQUESTS_RECEIVED_FINE);
                setAuthorized(TEST_REQUESTS_AUTHORIZED_UNPROCESSED_FINE);
                setCompleted(TEST_REQUESTS_AUTHORIZED_PROCESSED_FINE);
            }});

        }};
    }

    @Override
    public ServiceUsage[] findByNode(String nodeId) throws FindException {
        System.out.println("*** CALL *** MockServiceUsageManager: findByNode()");
        return new ServiceUsage[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ServiceUsage[] findByServiceGoid(Goid serviceGoid) throws FindException {
        System.out.println("*** CALL *** MockServiceUsageManager: findByServiceOid()");
        return new ServiceUsage[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void record(ServiceUsage data) throws UpdateException {
        System.out.println("*** CALL *** MockServiceUsageManager: record()");
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void clear(String nodeid) throws DeleteException {
        System.out.println("*** CALL *** MockServiceUsageManager: clear()");
    }
}
