package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.gateway.api.ScheduledTaskMO;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.common.task.JobStatus;
import com.l7tech.gateway.common.task.JobType;
import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author alee, 8/11/2015
 */
@RunWith(MockitoJUnitRunner.class)
public class ScheduledTaskTransformerTest {
    private static final Goid TASK_GOID = new Goid(0, 1);
    private static final Goid POLICY_GOID = new Goid(0, 2);
    private static final Goid PROVIDER_GOID = new Goid(0, -2);
    private static final String TASK_NAME = "Test Task";
    private static final String CRON_EXPRESSION = "* * * * * ?";
    private static final String USER = "admin";
    private ScheduledTaskTransformer transformer;
    @Mock
    private PolicyManager policyManager;
    @Mock
    private SecurityZoneManager securityZoneManager;
    @Mock
    private SecretsEncryptor encryptor;
    @Mock
    ServiceManager serviceManager;
    private ScheduledTask task;

    @Before
    public void setup() {
        transformer = new ScheduledTaskTransformer();
        ApplicationContexts.inject(transformer, CollectionUtils.<String, Object>mapBuilder()
                .put("policyManager", policyManager)
                .put("securityZoneManager", securityZoneManager)
                .put("serviceManager", serviceManager)
                .map(), false);
        task = new ScheduledTask();
    }

    @Test
    public void covertToMO() {
        task.setGoid(TASK_GOID);
        task.setVersion(1);
        task.setName(TASK_NAME);
        task.setPolicyGoid(POLICY_GOID);
        task.setUseOneNode(true);
        task.setExecuteOnCreate(true);
        task.setJobType(JobType.RECURRING);
        task.setJobStatus(JobStatus.SCHEDULED);
        task.setCronExpression(CRON_EXPRESSION);
        task.setUserId(USER);
        task.setIdProviderGoid(PROVIDER_GOID);
        final ScheduledTaskMO mo = transformer.convertToMO(task, encryptor);

        assertEquals(TASK_GOID.toString(), mo.getId());
        assertEquals(new Integer(1), mo.getVersion());
        assertEquals(TASK_NAME, mo.getName());
        assertEquals(POLICY_GOID.toString(), mo.getPolicyReference().getId());
        assertTrue(mo.isUseOneNode());
        assertTrue(mo.isExecuteOnCreate());
        assertEquals(ScheduledTaskMO.ScheduledTaskJobType.RECURRING, mo.getJobType());
        assertEquals(ScheduledTaskMO.ScheduledTaskJobStatus.SCHEDULED, mo.getJobStatus());
        assertNull(mo.getExecutionDate());
        assertEquals(CRON_EXPRESSION, mo.getCronExpression());
        assertEquals(USER, mo.getProperties().get("userId"));
        assertEquals(PROVIDER_GOID.toString(), mo.getProperties().get("idProvider"));
    }

    @Test
    public void convertFromMO() throws Exception {
        final ScheduledTaskMO mo = createDefaultMO();
        final ScheduledTask task = transformer.convertFromMO(mo, false, encryptor).getEntity();

        assertEquals(TASK_GOID, task.getGoid());
        assertEquals(1, task.getVersion());
        assertEquals(TASK_NAME, task.getName());
        assertEquals(POLICY_GOID, task.getPolicyGoid());
        assertTrue(task.isUseOneNode());
        assertTrue(task.isExecuteOnCreate());
        assertEquals(JobType.RECURRING, task.getJobType());
        assertEquals(JobStatus.SCHEDULED, task.getJobStatus());
        assertEquals(CRON_EXPRESSION, task.getCronExpression());
        assertEquals(USER, task.getUserId());
        assertEquals(PROVIDER_GOID, task.getIdProviderGoid());
    }

    @Test
    public void convertFromMOStrict() throws Exception {
        final ScheduledTaskMO mo = createDefaultMO();
        final Policy policy = buildTaskPolicy();
        when(policyManager.findByPrimaryKey(POLICY_GOID)).thenReturn(policy);
        final ScheduledTask task = transformer.convertFromMO(mo, true, encryptor).getEntity();

        assertEquals(TASK_GOID, task.getGoid());
        assertEquals(1, task.getVersion());
        assertEquals(TASK_NAME, task.getName());
        assertEquals(POLICY_GOID, task.getPolicyGoid());
        assertTrue(task.isUseOneNode());
        assertTrue(task.isExecuteOnCreate());
        assertEquals(JobType.RECURRING, task.getJobType());
        assertEquals(JobStatus.SCHEDULED, task.getJobStatus());
        assertEquals(CRON_EXPRESSION, task.getCronExpression());
        assertEquals(USER, task.getUserId());
        assertEquals(PROVIDER_GOID, task.getIdProviderGoid());
    }

    @Test
    public void convertFromMONullExecuteOnCreate() throws Exception {
        final ScheduledTaskMO mo = createDefaultMO();
        mo.setExecuteOnCreate(null);
        final ScheduledTask task = transformer.convertFromMO(mo, false, encryptor).getEntity();
        assertFalse(task.isExecuteOnCreate());
    }

    @Test(expected = ResourceFactory.InvalidResourceException.class)
    public void convertFromMOStrictPolicyNotFound() throws Exception {
        final ScheduledTaskMO mo = createDefaultMO();
        when(policyManager.findByPrimaryKey(POLICY_GOID)).thenReturn(null);
        try {
            transformer.convertFromMO(mo, true, encryptor).getEntity();
            fail("Expected InvalidResourceException");
        } catch (final ResourceFactory.InvalidResourceException e) {
            assertEquals(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, e.getType());
            assertEquals("Resource validation failed due to 'INVALID_VALUES' Invalid or unknown policy reference '" + POLICY_GOID.toString() + "'.", e.getMessage());
            throw e;
        }
    }

    private Policy buildTaskPolicy() {
        final Policy policy = new Policy(PolicyType.POLICY_BACKED_OPERATION, "Task Policy", "<xml/>", false);
        policy.setGoid(POLICY_GOID);
        policy.setInternalTag("com.l7tech.objectmodel.polback.BackgroundTask");
        policy.setInternalSubTag("run");
        return policy;
    }

    @Test(expected = ResourceFactory.InvalidResourceException.class)
    public void convertFromMOStrictPolicyFindException() throws Exception {
        final ScheduledTaskMO mo = createDefaultMO();
        when(policyManager.findByPrimaryKey(POLICY_GOID)).thenThrow(new FindException("mocking exception"));
        try {
            transformer.convertFromMO(mo, true, encryptor).getEntity();
            fail("Expected InvalidResourceException");
        } catch (final ResourceFactory.InvalidResourceException e) {
            assertEquals(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, e.getType());
            assertEquals("Resource validation failed due to 'INVALID_VALUES' Invalid or unknown policy reference '" + POLICY_GOID.toString() + "'.", e.getMessage());
            throw e;
        }
    }

    private ScheduledTaskMO createDefaultMO() {
        final ScheduledTaskMO mo = ManagedObjectFactory.createScheduledTaskMO();
        mo.setId(TASK_GOID.toString());
        mo.setVersion(1);
        mo.setName(TASK_NAME);
        mo.setPolicyReference(new ManagedObjectReference(PolicyMO.class, POLICY_GOID.toString()));
        mo.setUseOneNode(true);
        mo.setExecuteOnCreate(true);
        mo.setJobType(ScheduledTaskMO.ScheduledTaskJobType.RECURRING);
        mo.setJobStatus(ScheduledTaskMO.ScheduledTaskJobStatus.SCHEDULED);
        mo.setCronExpression(CRON_EXPRESSION);
        mo.setProperties(new HashMap<String, String>());
        mo.getProperties().put("userId", USER);
        mo.getProperties().put("idProvider", PROVIDER_GOID.toString());
        return mo;
    }
}
