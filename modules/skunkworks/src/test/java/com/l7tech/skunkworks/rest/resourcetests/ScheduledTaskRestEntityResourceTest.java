package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.gateway.api.ScheduledTaskMO;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.common.task.JobStatus;
import com.l7tech.gateway.common.task.JobType;
import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.task.ScheduledTaskManager;
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
public class ScheduledTaskRestEntityResourceTest extends RestEntityTests<ScheduledTask, ScheduledTaskMO> {

    private ScheduledTaskManager scheduledTaskManager;
    private List<ScheduledTask> tasks = new ArrayList<>();
    private PolicyManager policyManager;
    private List<Policy> policies = new ArrayList<>();
    private Folder rootFolder;
    private PolicyVersionManager policyVersionManager;
    private FolderManager folderManager;
    private final String POLICY_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\"/>\n" +
            "</wsp:Policy>";
    @Before
    public void before() throws ObjectModelException {
        scheduledTaskManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("scheduledTaskManager", ScheduledTaskManager.class);
        policyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyManager", PolicyManager.class);
        policyVersionManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyVersionManager", PolicyVersionManager.class);
        folderManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("folderManager", FolderManager.class);
        rootFolder = folderManager.findRootFolder();

        //Create the policies
        Policy policy = new Policy(PolicyType.POLICY_BACKED_OPERATION, "Policy 1",
                POLICY_XML,
                false
        );
        policy.setFolder(rootFolder);
        policy.setInternalTag("com.l7tech.objectmodel.polback.BackgroundTask");
        policy.setInternalSubTag("run");
        policy.setGuid(UUID.randomUUID().toString());
        policy.setSoap(true);

        policyManager.save(policy);
        policyVersionManager.checkpointPolicy(policy,true,"comment",true);
        policies.add(policy);

        policy = new Policy(PolicyType.POLICY_BACKED_OPERATION, "Policy 2",
                POLICY_XML,
                false
        );
        policy.setFolder(rootFolder);
        policy.setInternalTag("com.l7tech.objectmodel.polback.BackgroundTask");
        policy.setInternalSubTag("run");
        policy.setGuid(UUID.randomUUID().toString());
        policy.setSoap(true);

        policyManager.save(policy);
        policyVersionManager.checkpointPolicy(policy,true,"comment",true);
        policies.add(policy);

        policy = new Policy(PolicyType.INTERNAL, "Policy 3",
                POLICY_XML,
                false
        );
        policy.setFolder(rootFolder);
        policy.setGuid(UUID.randomUUID().toString());
        policy.setSoap(true);

        policyManager.save(policy);
        policyVersionManager.checkpointPolicy(policy,true,"comment",true);
        policies.add(policy);

        //Create the scheduled tasks

        ScheduledTask scheduledTask = new ScheduledTask();
        scheduledTask.setName("Scheduled Task 1");
        scheduledTask.setPolicy(policies.get(0));
        scheduledTask.setJobType(JobType.ONE_TIME);
        scheduledTask.setJobStatus(JobStatus.COMPLETED);
        scheduledTask.setExecutionDate(System.currentTimeMillis());
        scheduledTask.setUseOneNode(true);

        scheduledTaskManager.save(scheduledTask);
        tasks.add(scheduledTask);

        scheduledTask = new ScheduledTask();
        scheduledTask.setName("Scheduled Task 2");
        scheduledTask.setPolicy(policies.get(1));
        scheduledTask.setJobType(JobType.RECURRING);
        scheduledTask.setJobStatus(JobStatus.SCHEDULED);
        scheduledTask.setCronExpression("* * */5 * ?");
        scheduledTask.setUseOneNode(false);
        scheduledTaskManager.save(scheduledTask);
        tasks.add(scheduledTask);

        scheduledTask = new ScheduledTask();
        scheduledTask.setName("Scheduled Task 3");
        scheduledTask.setPolicy(policies.get(1));
        scheduledTask.setJobType(JobType.RECURRING);
        scheduledTask.setJobStatus(JobStatus.DISABLED);
        scheduledTask.setCronExpression("* * */5 * ?");
        scheduledTask.setUseOneNode(false);

        scheduledTaskManager.save(scheduledTask);
        tasks.add(scheduledTask);

    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<ScheduledTask> all = scheduledTaskManager.findAll();
        for (ScheduledTask scheduledTask : all) {
            scheduledTaskManager.delete(scheduledTask.getGoid());
        }

        Collection<Policy> allPolicies = policyManager.findAll();
        for (Policy policy : allPolicies) {
            policyManager.delete(policy.getGoid());
        }
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(tasks, new Functions.Unary<String, ScheduledTask>() {
            @Override
            public String call(ScheduledTask task) {
                return task.getId();
            }
        });
    }

    @Override
    public List<ScheduledTaskMO> getCreatableManagedObjects() {
        List<ScheduledTaskMO> tasks = new ArrayList<>();

        ScheduledTaskMO scheduledTask = ManagedObjectFactory.createScheduledTaskMO();
        scheduledTask.setId(getGoid().toString());
        scheduledTask.setName("Test Scheduled Task created");
        scheduledTask.setPolicyReference(new ManagedObjectReference(PolicyMO.class,policies.get(0).getId()));
        scheduledTask.setJobType(ScheduledTaskMO.ScheduledTaskJobType.RECURRING);
        scheduledTask.setJobStatus(ScheduledTaskMO.ScheduledTaskJobStatus.DISABLED);
        scheduledTask.setCronExpression("* * */5 * ?");
        scheduledTask.setUseOneNode(false);
        tasks.add(scheduledTask);

        return tasks;
    }

    @Override
    public List<ScheduledTaskMO> getUpdateableManagedObjects() {
        List<ScheduledTaskMO> tasks = new ArrayList<>();

        ScheduledTask task = this.tasks.get(0);
        ScheduledTaskMO scheduledTask = ManagedObjectFactory.createScheduledTaskMO();
        scheduledTask.setName(task.getName() + " Updated");
        scheduledTask.setId(task.getId());
        scheduledTask.setVersion(task.getVersion());
        scheduledTask.setPolicyReference(new ManagedObjectReference(PolicyMO.class,policies.get(0).getId()));
        scheduledTask.setJobType(ScheduledTaskMO.ScheduledTaskJobType.RECURRING);
        scheduledTask.setJobStatus(ScheduledTaskMO.ScheduledTaskJobStatus.DISABLED);
        scheduledTask.setCronExpression("* * */5 * ?");
        tasks.add(scheduledTask);

        //update twice
        scheduledTask = ManagedObjectFactory.createScheduledTaskMO();
        scheduledTask.setId(task.getId());
        scheduledTask.setName(task.getName() + " Updated");
        scheduledTask.setVersion(task.getVersion());
        scheduledTask.setPolicyReference(new ManagedObjectReference(PolicyMO.class,policies.get(0).getId()));
        scheduledTask.setJobType(ScheduledTaskMO.ScheduledTaskJobType.RECURRING);
        scheduledTask.setJobStatus(ScheduledTaskMO.ScheduledTaskJobStatus.DISABLED);
        scheduledTask.setCronExpression("* * */6 * ?");
        tasks.add(scheduledTask);

        return tasks;
    }

    @Override
    public Map<ScheduledTaskMO, Functions.BinaryVoid<ScheduledTaskMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<ScheduledTaskMO, Functions.BinaryVoid<ScheduledTaskMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        ScheduledTaskMO scheduledTask = ManagedObjectFactory.createScheduledTaskMO();
        scheduledTask.setName(tasks.get(0).getName());
        scheduledTask.setPolicyReference(new ManagedObjectReference(PolicyMO.class,policies.get(2).getId()));
        scheduledTask.setJobType(ScheduledTaskMO.ScheduledTaskJobType.RECURRING);
        scheduledTask.setJobStatus(ScheduledTaskMO.ScheduledTaskJobStatus.DISABLED);
        scheduledTask.setCronExpression("* * */5 * ?");
        scheduledTask.setUseOneNode(false);

        builder.put(scheduledTask, new Functions.BinaryVoid<ScheduledTaskMO, RestResponse>() {
            @Override
            public void call(ScheduledTaskMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<ScheduledTaskMO, Functions.BinaryVoid<ScheduledTaskMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<ScheduledTaskMO, Functions.BinaryVoid<ScheduledTaskMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        ScheduledTaskMO scheduledTask = ManagedObjectFactory.createScheduledTaskMO();
        scheduledTask.setId(tasks.get(0).getId());
        scheduledTask.setName(tasks.get(1).getName());
        scheduledTask.setVersion(tasks.get(1).getVersion());
        scheduledTask.setPolicyReference(new ManagedObjectReference(PolicyMO.class,policies.get(0).getId()));
        scheduledTask.setJobType(ScheduledTaskMO.ScheduledTaskJobType.RECURRING);
        scheduledTask.setJobStatus(ScheduledTaskMO.ScheduledTaskJobStatus.DISABLED);
        scheduledTask.setCronExpression("* * */6 * ?");

        builder.put(scheduledTask, new Functions.BinaryVoid<ScheduledTaskMO, RestResponse>() {
            @Override
            public void call(ScheduledTaskMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        CollectionUtils.MapBuilder<String, Functions.BinaryVoid<String, RestResponse>> builder = CollectionUtils.MapBuilder.builder();
        builder.put("asdf"+getGoid().toString(), new Functions.BinaryVoid<String, RestResponse>() {
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
        return Functions.map(tasks, new Functions.Unary<String, ScheduledTask>() {
            @Override
            public String call(ScheduledTask task) {
                return task.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "scheduledTasks";
    }

    @Override
    public String getType() {
        return EntityType.SCHEDULED_TASK.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        ScheduledTask entity = scheduledTaskManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        ScheduledTask entity = scheduledTaskManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, ScheduledTaskMO managedObject) throws FindException {
        ScheduledTask entity = scheduledTaskManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            Assert.assertEquals(entity.getPolicy().getId(), managedObject.getPolicyReference().getId());
            Assert.assertEquals(entity.isUseOneNode(), managedObject.isUseOneNode().booleanValue());
            Assert.assertEquals(entity.getJobStatus().toString(), managedObject.getJobStatus().toString());
            Assert.assertEquals(entity.getJobType().toString(), managedObject.getJobType().toString());
            Assert.assertEquals(entity.getExecutionDate(), managedObject.getExecutionDate() == null?0: managedObject.getExecutionDate().getTime() );
            Assert.assertEquals(entity.getCronExpression(), managedObject.getCronExpression());
            for (String key : entity.getProperties().keySet()) {
                Assert.assertEquals(entity.getProperties().get(key), managedObject.getProperties().get(key));
            }
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(tasks, new Functions.Unary<String, ScheduledTask>() {
                    @Override
                    public String call(ScheduledTask task) {
                        return task.getId();
                    }
                }))
                .put("name=" + URLEncoder.encode(tasks.get(0).getName()), Arrays.asList(tasks.get(0).getId()))
                .put("name=" + URLEncoder.encode(tasks.get(0).getName()) + "&name=" + URLEncoder.encode(tasks.get(1).getName()), Functions.map(tasks.subList(0, 2), new Functions.Unary<String, ScheduledTask>() {
                    @Override
                    public String call(ScheduledTask task) {
                        return task.getId();
                    }
                }))
                .put("name=banName", Collections.<String>emptyList())
                .put("node=all", Arrays.asList(tasks.get(1).getId(), tasks.get(2).getId()))
                .put("node=one", Arrays.asList(tasks.get(0).getId()))
                .put("type=Recurring", Arrays.asList(tasks.get(1).getId(), tasks.get(2).getId()))
                .put("status=Completed", Arrays.asList(tasks.get(0).getId()))
                .put("name=" + URLEncoder.encode(tasks.get(0).getName()) + "&name=" + URLEncoder.encode(tasks.get(1).getName()) + "&sort=name&order=desc", Arrays.asList(tasks.get(1).getId(), tasks.get(0).getId()))
                .map();
    }
}
