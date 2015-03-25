package com.l7tech.server.cluster;

import com.l7tech.gateway.common.cluster.ServiceUsage;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.EntityManagerTest;
import com.l7tech.util.Functions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

public class ServiceUsageManagerTest extends EntityManagerTest {
    private ServiceUsageManager serviceUsageManager;

    @Before
    public void setUp() throws Exception {
        serviceUsageManager = applicationContext.getBean("serviceUsageManager", ServiceUsageManager.class);
    }

    @After
    public void tearDown() throws Exception {
        HashSet<String> nodeIds = Functions.reduce(serviceUsageManager.getAll(), new HashSet<String>(), new Functions.Binary<HashSet<String>, HashSet<String>, ServiceUsage>() {
            @Override
            public HashSet<String> call(HashSet<String> nodeIds, ServiceUsage serviceUsage) {
                nodeIds.add(serviceUsage.getNodeid());
                return nodeIds;
            }
        });
        for (String nodeId : nodeIds) {
            serviceUsageManager.clear(nodeId);
        }
    }

    private ServiceUsage createServiceUsage(String nodeid, Goid serviceid, long authorized, long completed, long requests) throws UpdateException {
        ServiceUsage serviceUsage = new ServiceUsage();
        serviceUsage.setNodeid(nodeid);
        serviceUsage.setServiceid(serviceid);
        serviceUsage.setAuthorized(authorized);
        serviceUsage.setCompleted(completed);
        serviceUsage.setRequests(requests);

        serviceUsageManager.record(serviceUsage);
        session.flush();

        return serviceUsage;
    }

    @Test
    public void testCreate() throws UpdateException, FindException {
        String nodeid = UUID.randomUUID().toString();
        createServiceUsage(nodeid, new Goid(123, 456), 111L, 222L, 333L);

        Collection<ServiceUsage> serviceUsages = serviceUsageManager.getAll();

        Assert.assertEquals(1, serviceUsages.size());
        ServiceUsage serviceUsage = serviceUsages.iterator().next();
        Assert.assertEquals(nodeid, serviceUsage.getNodeid());
        Assert.assertEquals(new Goid(123, 456), serviceUsage.getServiceid());
        Assert.assertEquals(111L, serviceUsage.getAuthorized());
        Assert.assertEquals(222L, serviceUsage.getCompleted());
        Assert.assertEquals(333L, serviceUsage.getRequests());
    }

    @Test
    public void testFindByNode() throws UpdateException, FindException {
        String nodeid1 = UUID.randomUUID().toString();
        String nodeid2 = UUID.randomUUID().toString();
        createServiceUsage(nodeid1, new Goid(123, 1), 111L, 222L, 333L);
        createServiceUsage(nodeid1, new Goid(123, 2), 111L, 222L, 333L);
        createServiceUsage(nodeid1, new Goid(123, 3), 111L, 222L, 333L);
        createServiceUsage(nodeid2, new Goid(123, 1), 111L, 222L, 333L);
        createServiceUsage(nodeid2, new Goid(123, 2), 111L, 222L, 333L);
        createServiceUsage(nodeid2, new Goid(123, 4), 111L, 222L, 333L);

        ServiceUsage[] serviceUsages = serviceUsageManager.findByNode(nodeid1);

        Assert.assertEquals(3, serviceUsages.length);
        Assert.assertTrue("Missing service: " + new Goid(123, 1), Functions.exists(Arrays.asList(serviceUsages), matchService(nodeid1, new Goid(123, 1))));
        Assert.assertTrue("Missing service: " + new Goid(123, 2), Functions.exists(Arrays.asList(serviceUsages), matchService(nodeid1, new Goid(123, 2))));
        Assert.assertTrue("Missing service: " + new Goid(123, 3), Functions.exists(Arrays.asList(serviceUsages), matchService(nodeid1, new Goid(123, 3))));

        serviceUsages = serviceUsageManager.findByNode(nodeid2);

        Assert.assertEquals(3, serviceUsages.length);
        Assert.assertTrue("Missing service: " + new Goid(123, 1), Functions.exists(Arrays.asList(serviceUsages), matchService(nodeid2, new Goid(123, 1))));
        Assert.assertTrue("Missing service: " + new Goid(123, 2), Functions.exists(Arrays.asList(serviceUsages), matchService(nodeid2, new Goid(123, 2))));
        Assert.assertTrue("Missing service: " + new Goid(123, 4), Functions.exists(Arrays.asList(serviceUsages), matchService(nodeid2, new Goid(123, 4))));
    }

    @Test
    public void testFindByServiceId() throws UpdateException, FindException {
        String nodeid1 = UUID.randomUUID().toString();
        String nodeid2 = UUID.randomUUID().toString();
        createServiceUsage(nodeid1, new Goid(123, 1), 111L, 222L, 333L);
        createServiceUsage(nodeid1, new Goid(123, 2), 111L, 222L, 333L);
        createServiceUsage(nodeid1, new Goid(123, 3), 111L, 222L, 333L);
        createServiceUsage(nodeid2, new Goid(123, 1), 111L, 222L, 333L);
        createServiceUsage(nodeid2, new Goid(123, 2), 111L, 222L, 333L);
        createServiceUsage(nodeid2, new Goid(123, 4), 111L, 222L, 333L);

        ServiceUsage[] serviceUsages = serviceUsageManager.findByServiceGoid(new Goid(123, 1));

        Assert.assertEquals(2, serviceUsages.length);
        Assert.assertTrue("Missing node: " + nodeid1, Functions.exists(Arrays.asList(serviceUsages), matchService(nodeid1, new Goid(123, 1))));
        Assert.assertTrue("Missing node: " + nodeid2, Functions.exists(Arrays.asList(serviceUsages), matchService(nodeid2, new Goid(123, 1))));

        serviceUsages = serviceUsageManager.findByServiceGoid(new Goid(123, 2));

        Assert.assertEquals(2, serviceUsages.length);
        Assert.assertTrue("Missing node: " + nodeid1, Functions.exists(Arrays.asList(serviceUsages), matchService(nodeid1, new Goid(123, 2))));
        Assert.assertTrue("Missing node: " + nodeid2, Functions.exists(Arrays.asList(serviceUsages), matchService(nodeid2, new Goid(123, 2))));

        serviceUsages = serviceUsageManager.findByServiceGoid(new Goid(123, 3));

        Assert.assertEquals(1, serviceUsages.length);
        Assert.assertTrue("Missing node: " + nodeid1, Functions.exists(Arrays.asList(serviceUsages), matchService(nodeid1, new Goid(123, 3))));

        serviceUsages = serviceUsageManager.findByServiceGoid(new Goid(123, 4));

        Assert.assertEquals(1, serviceUsages.length);
        Assert.assertTrue("Missing node: " + nodeid2, Functions.exists(Arrays.asList(serviceUsages), matchService(nodeid2, new Goid(123, 4))));

    }

    @Test
    public void testClear() throws UpdateException, FindException, DeleteException {
        String nodeid1 = UUID.randomUUID().toString();
        String nodeid2 = UUID.randomUUID().toString();
        createServiceUsage(nodeid1, new Goid(123, 1), 111L, 222L, 333L);
        createServiceUsage(nodeid1, new Goid(123, 2), 111L, 222L, 333L);
        createServiceUsage(nodeid1, new Goid(123, 3), 111L, 222L, 333L);
        createServiceUsage(nodeid2, new Goid(123, 1), 111L, 222L, 333L);
        createServiceUsage(nodeid2, new Goid(123, 2), 111L, 222L, 333L);
        createServiceUsage(nodeid2, new Goid(123, 4), 111L, 222L, 333L);

        serviceUsageManager.clear(nodeid1);
        ServiceUsage[] serviceUsages = serviceUsageManager.findByNode(nodeid1);

        Assert.assertEquals(0, serviceUsages.length);

        serviceUsages = serviceUsageManager.findByNode(nodeid2);

        Assert.assertEquals(3, serviceUsages.length);

        serviceUsageManager.clear(nodeid2);

        serviceUsages = serviceUsageManager.findByNode(nodeid2);

        Assert.assertEquals(0, serviceUsages.length);
    }

    private Functions.Unary<Boolean, ServiceUsage> matchService(final String nodeid1, final Goid serviceId) {
        return new Functions.Unary<Boolean, ServiceUsage>() {
            @Override
            public Boolean call(ServiceUsage serviceUsage) {
                return nodeid1.equals(serviceUsage.getNodeid()) && serviceId.equals(serviceUsage.getServiceid());
            }
        };
    }

}
