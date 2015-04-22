package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.WorkQueueMO;

import com.l7tech.gateway.common.workqueue.WorkQueue;
import com.l7tech.objectmodel.*;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.workqueue.WorkQueueEntityManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;

import java.net.URLEncoder;
import java.util.*;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class WorkQueueRestEntityResourceTest extends RestEntityTests<WorkQueue, WorkQueueMO> {
    private WorkQueueEntityManager workQueueEntityManager;
    private SecurityZoneManager securityZoneManager;
    private List<WorkQueue> workQueues = new ArrayList<>();
    private SecurityZone securityZone;

    @Before
    public void before() throws Exception {
        workQueueEntityManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("workQueueEntityManager", WorkQueueEntityManager.class);
        securityZoneManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securityZoneManager", SecurityZoneManager.class);

        //Create new work queue
        WorkQueue wq1 = new WorkQueue();
        wq1.setGoid(getGoid());
        wq1.setName("Test work queue 1");
        wq1.setMaxQueueSize(5);
        wq1.setThreadPoolMax(2);
        wq1.setRejectPolicy(WorkQueue.REJECT_POLICY_WAIT_FOR_ROOM);
        workQueues.add(wq1);
        workQueueEntityManager.save(wq1);

        securityZone = new SecurityZone();
        securityZone.setName("Zone1");
        securityZone.setPermittedEntityTypes(CollectionUtils.set(EntityType.ANY));
        securityZone.setGoid(securityZoneManager.save(securityZone));

        WorkQueue wq2 = new WorkQueue();
        wq2.setGoid(getGoid());
        wq2.setName("Test work queue 2");
        wq2.setMaxQueueSize(10);
        wq2.setThreadPoolMax(5);
        wq2.setRejectPolicy(WorkQueue.REJECT_POLICY_FAIL_IMMEDIATELY);
        wq2.setSecurityZone(securityZone);
        workQueues.add(wq2);
        workQueueEntityManager.save(wq2);
    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<WorkQueue> all = workQueueEntityManager.findAll();
        for (WorkQueue workQueue : all) {
            workQueueEntityManager.delete(workQueue.getGoid());
        }

        securityZoneManager.delete(securityZone);
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(workQueues, new Functions.Unary<String, WorkQueue>() {
            @Override
            public String call(WorkQueue workQueue) {
                return workQueue.getId();
            }
        });
    }

    @Override
    public List<WorkQueueMO> getCreatableManagedObjects() {
        List<WorkQueueMO> workQueueMOs = new ArrayList<>();

        WorkQueueMO workQueueMO = ManagedObjectFactory.createWorkQueueMO();
        workQueueMO.setId(getGoid().toString());
        workQueueMO.setName("Test work queue created");
        workQueueMO.setMaxQueueSize(5);
        workQueueMO.setThreadPoolMax(2);
        workQueueMO.setRejectPolicy(WorkQueue.REJECT_POLICY_WAIT_FOR_ROOM);

        workQueueMOs.add(workQueueMO);

        workQueueMO = ManagedObjectFactory.createWorkQueueMO();
        workQueueMO.setId(getGoid().toString());
        workQueueMO.setName("Test work queue created minimal");
        workQueueMO.setMaxQueueSize(10);
        workQueueMO.setThreadPoolMax(5);
        workQueueMO.setRejectPolicy(WorkQueue.REJECT_POLICY_FAIL_IMMEDIATELY);

        workQueueMOs.add(workQueueMO);

        return workQueueMOs;
    }

    @Override
    public List<WorkQueueMO> getUpdateableManagedObjects() {
        List<WorkQueueMO> workQueueMOs = new ArrayList<>();

        WorkQueue workQueue = this.workQueues.get(0);
        WorkQueueMO workQueueMO = ManagedObjectFactory.createWorkQueueMO();
        workQueueMO.setId(workQueue.getId());
        workQueueMO.setVersion(workQueue.getVersion());
        workQueueMO.setName(workQueue.getName() + " Updated 1");
        workQueueMO.setMaxQueueSize(50);
        workQueueMO.setThreadPoolMax(20);
        workQueueMO.setRejectPolicy(WorkQueue.REJECT_POLICY_WAIT_FOR_ROOM);
        workQueueMOs.add(workQueueMO);

        //update twice
        workQueueMO = ManagedObjectFactory.createWorkQueueMO();
        workQueueMO.setId(workQueue.getId());
        workQueueMO.setVersion(workQueue.getVersion());
        workQueueMO.setName(workQueue.getName() + " Updated 2");
        workQueueMO.setMaxQueueSize(500);
        workQueueMO.setThreadPoolMax(200);
        workQueueMO.setRejectPolicy(WorkQueue.REJECT_POLICY_WAIT_FOR_ROOM);
        workQueueMOs.add(workQueueMO);

        return workQueueMOs;
    }

    @Override
    public Map<WorkQueueMO, Functions.BinaryVoid<WorkQueueMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<WorkQueueMO, Functions.BinaryVoid<WorkQueueMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        WorkQueueMO workQueueMO = ManagedObjectFactory.createWorkQueueMO();
        workQueueMO.setName("UnCreatable work queue wrong reject policy");
        workQueueMO.setMaxQueueSize(10);
        workQueueMO.setThreadPoolMax(5);
        workQueueMO.setRejectPolicy("Something");
        workQueueMO.setSecurityZoneId("12345");

        builder.put(workQueueMO, new Functions.BinaryVoid<WorkQueueMO, RestResponse>() {
            @Override
            public void call(WorkQueueMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<WorkQueueMO, Functions.BinaryVoid<WorkQueueMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<WorkQueueMO, Functions.BinaryVoid<WorkQueueMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        WorkQueueMO workQueueMO = ManagedObjectFactory.createWorkQueueMO();
        workQueueMO.setId(workQueues.get(0).getId());
        workQueueMO.setVersion(workQueues.get(0).getVersion());
        workQueueMO.setName(workQueues.get(0).getName() + " Updated");
        workQueueMO.setMaxQueueSize(10);
        workQueueMO.setThreadPoolMax(5);
        workQueueMO.setRejectPolicy("Something");
        workQueueMO.setSecurityZoneId("12345");

        builder.put(workQueueMO, new Functions.BinaryVoid<WorkQueueMO, RestResponse>() {
            @Override
            public void call(WorkQueueMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        CollectionUtils.MapBuilder<String, Functions.BinaryVoid<String, RestResponse>> builder = CollectionUtils.MapBuilder.builder();
        builder.put("asdf" + getGoid().toString(), new Functions.BinaryVoid<String, RestResponse>() {
            @Override
            public void call(String s, RestResponse restResponse) {
                Assert.assertEquals("Expected successful response", 400, restResponse.getStatus());
            }
        });
        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnDeleteableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries() {
        return Collections.emptyMap();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Functions.map(workQueues, new Functions.Unary<String, WorkQueue>() {
            @Override
            public String call(WorkQueue workQueue) {
                return workQueue.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "workQueues";
    }

    @Override
    public String getType() {
        return EntityType.WORK_QUEUE.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        WorkQueue entity = workQueueEntityManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        WorkQueue entity = workQueueEntityManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, WorkQueueMO managedObject) throws FindException {
        WorkQueue entity = workQueueEntityManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            Assert.assertEquals(entity.getMaxQueueSize(), managedObject.getMaxQueueSize());
            Assert.assertEquals(entity.getThreadPoolMax(), managedObject.getThreadPoolMax());
            Assert.assertEquals(entity.getRejectPolicy(), managedObject.getRejectPolicy());
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(workQueues, new Functions.Unary<String, WorkQueue>() {
                    @Override
                    public String call(WorkQueue message) {
                        return message.getId();
                    }
                }))
                .put("name=" + URLEncoder.encode(workQueues.get(0).getName()),
                        Arrays.asList(workQueues.get(0).getId()))
                .put("name=" + URLEncoder.encode(workQueues.get(0).getName()) + "&name=" + URLEncoder.encode(workQueues.get(1).getName()),
                        Functions.map(workQueues.subList(0, 2), new Functions.Unary<String, WorkQueue>() {
                            @Override
                            public String call(WorkQueue workQueue) {
                                return workQueue.getId();
                            }
                        }))
                .put("name=banName", Collections.<String>emptyList())
                .put("name=" + URLEncoder.encode(workQueues.get(0).getName()) + "&name=" + URLEncoder.encode(workQueues.get(1).getName()) + "&sort=name&order=desc",
                        Arrays.asList(workQueues.get(1).getId(), workQueues.get(0).getId()))
                .put("maxQueueSize=" + URLEncoder.encode(String.valueOf(workQueues.get(0).getMaxQueueSize())), Arrays.asList(workQueues.get(0).getId()))
                .put("maxQueueSize=" + URLEncoder.encode(String.valueOf(workQueues.get(0).getMaxQueueSize())) + "&maxQueueSize=" + URLEncoder.encode(String.valueOf(workQueues.get(1).getMaxQueueSize())),
                        Functions.map(workQueues.subList(0, 2), new Functions.Unary<String, WorkQueue>() {
                            @Override
                            public String call(WorkQueue workQueue) {
                                return workQueue.getId();
                            }
                        }))
                .put("threadPoolMax=" + URLEncoder.encode(String.valueOf(workQueues.get(0).getThreadPoolMax())), Arrays.asList(workQueues.get(0).getId()))
                .put("rejectPolicy=" + URLEncoder.encode(workQueues.get(0).getRejectPolicy()), Arrays.asList(workQueues.get(0).getId()))
                .put("securityZone.id=" + securityZone.getId(), Arrays.asList(workQueues.get(1).getId()))
                .map();
    }
}
