package com.l7tech.gateway.common.task;

import com.l7tech.objectmodel.Goid;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author alee, 8/11/2015
 */
public class ScheduledTaskTest {
    private static final Goid TASK_GOID = new Goid(0, 1);
    private static final Goid POLICY_GOID = new Goid(0, 2);
    private static final Goid PROVIDER_GOID = new Goid(0, -2);
    private static final String TASK_NAME = "Test Task";
    private static final String CRON_EXPRESSION = "* * * * * ?";
    private static final String USER = "admin";

    @Test
    public void copyFrom() {
        final ScheduledTask task = new ScheduledTask();
        task.setGoid(TASK_GOID);
        task.setVersion(1);
        task.setName(TASK_NAME);
        task.setPolicyGoid(POLICY_GOID);
        task.setUseOneNode(true);
        task.setExecuteImmediately(true);
        task.setJobType(JobType.RECURRING);
        task.setJobStatus(JobStatus.SCHEDULED);
        task.setCronExpression(CRON_EXPRESSION);
        task.setUserId(USER);
        task.setIdProviderGoid(PROVIDER_GOID);

        final ScheduledTask copy = new ScheduledTask();
        copy.copyFrom(task);

        assertEquals(TASK_GOID, copy.getGoid());
        assertEquals(1, copy.getVersion());
        assertEquals(TASK_NAME, copy.getName());
        assertEquals(POLICY_GOID, copy.getPolicyGoid());
        assertTrue(copy.isUseOneNode());
        assertTrue(copy.isExecuteImmediately());
        assertEquals(JobType.RECURRING, copy.getJobType());
        assertEquals(JobStatus.SCHEDULED, copy.getJobStatus());
        assertEquals(CRON_EXPRESSION, copy.getCronExpression());
        assertEquals(USER, copy.getUserId());
        assertEquals(PROVIDER_GOID, copy.getIdProviderGoid());
    }
}
