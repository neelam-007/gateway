package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.gateway.common.cluster.ServiceUsage;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.cluster.ServiceUsageManager;

import java.util.ArrayList;
import java.util.Collection;

/**
 * User: rseminoff
 * Date: 25/05/12
 *
 * Copy of com.l7tech.external.assertions.snmpagent.server.MockServiceUsageManager, except it overrides
 *    public ServiceUsage[] findByNode(String nodeId) throws FindException
 * with a local version
 */
public class MockServiceUsageManager implements ServiceUsageManager {

    @Override
    public Collection<ServiceUsage> getAll() throws FindException {
        return new ArrayList<>();/* {{

            add(new ServiceUsage() {{
                setOid(TEST_SERVICE_OID);
                setServiceid(TEST_SERVICE_OID);
                setNodeid(TEST_SERVICE_NAME);
                setName(TEST_SERVICE_NAME);
                setRequests(TEST_REQUESTS_RECEIVED_FINE);
                setAuthorized(TEST_REQUESTS_AUTHORIZED_UNPROCESSED_FINE);
                setCompleted(TEST_REQUESTS_AUTHORIZED_PROCESSED_FINE);
            }});

        }};*/
    }

    @Override
    public ServiceUsage[] findByNode(String nodeId) throws FindException {
        return new ServiceUsage[] {
                new ServiceUsage() {{
                    setNodeid("1");
                }}
        };
    }

    @Override
    public ServiceUsage[] findByServiceGoid(Goid serviceGoid) throws FindException {
        System.out.println("*** CALL *** MockServiceUsageManager: findByServiceGoid()");
        return new ServiceUsage[0];
    }

    @Override
    public void record(ServiceUsage data) throws UpdateException {
        System.out.println("*** CALL *** MockServiceUsageManager: record()");
    }

    @Override
    public void clear(String nodeid) throws DeleteException {
        System.out.println("*** CALL *** MockServiceUsageManager: clear()");
    }
}
