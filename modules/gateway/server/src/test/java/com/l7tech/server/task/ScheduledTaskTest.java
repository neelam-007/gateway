package com.l7tech.server.task;

import com.l7tech.gateway.common.task.JobStatus;
import com.l7tech.gateway.common.task.JobType;
import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.objectmodel.Goid;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class ScheduledTaskTest {
    @Mock
    JobExecutionContext executionContext;
    @Mock
    JobDataMap jobDataMap;
    @Mock
    Trigger trigger;
    @Mock
    ScheduledPolicyRunner policyRunner;
    @Mock
    ScheduledTaskJobManager jobManger;
    @Mock
    Scheduler scheduler;

    @Test
    public void runPolicy() throws Exception {

        Goid policyID = new Goid(4, 5);
        when(jobDataMap.getString(Matchers.eq(ScheduledTaskJobManager.JOB_DETAIL_POLICY_GOID))).thenReturn(policyID.toString());
        when(jobDataMap.getString(Matchers.eq(ScheduledTaskJobManager.JOB_DETAIL_NODE))).thenReturn(ScheduledTaskJobManager.JOB_DETAIL_NODE_ONE);
        when(jobDataMap.getString(Matchers.eq(ScheduledTaskJobManager.JOB_DETAIL_NAME))).thenReturn("name");
        when(executionContext.getTrigger()).thenReturn(trigger);
        when(trigger.getJobDataMap()).thenReturn(jobDataMap);
        ScheduledPolicyRunner.setInstance(policyRunner);
        stub(policyRunner.isClusterMaster()).toReturn(true);

        Job job = new ScheduledServiceQuartzJob();
        job.execute(executionContext);

        verify(policyRunner).runBackgroundTask("name", policyID, null, null);
        verify(policyRunner).isClusterMaster();
    }

    @Test
    public void scheduleDisabledTask() throws Exception {

        Goid policyID = new Goid(4, 5);
        ScheduledTask task = new ScheduledTask();
        task.setName("Test task");
        task.setPolicyGoid(policyID);
        task.setJobType(JobType.ONE_TIME);
        task.setCronExpression("* * * * * ?");
        task.setJobStatus(JobStatus.DISABLED);
        doCallRealMethod().when(jobManger).scheduleJob(any(ScheduledTask.class));
        jobManger.scheduleJob(task);

        verify(jobManger, never()).scheduleRecurringJob(any(ScheduledTask.class));
        verify(jobManger, never()).scheduleOneTimeJob(any(ScheduledTask.class));
    }


    @Test
    public void scheduleCronTask() throws Exception {

        Goid policyID = new Goid(4, 5);
        Goid taskId = new Goid(9, 9);
        ScheduledTask task = new ScheduledTask();
        task.setName("Test task");
        task.setGoid(taskId);
        task.setPolicyGoid(policyID);
        task.setCronExpression("* * * * * ?");
        task.setJobStatus(JobStatus.SCHEDULED);
        task.setJobType(JobType.RECURRING);
        doCallRealMethod().when(jobManger).scheduleJob(any(ScheduledTask.class));
        doCallRealMethod().when(jobManger).scheduleRecurringJob(any(ScheduledTask.class));
        when(jobManger.getScheduler()).thenReturn(scheduler);
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(false);
        ArgumentCaptor<Trigger> triggerArg = ArgumentCaptor.forClass(Trigger.class);
        jobManger.scheduleJob(task);
        verify(scheduler).scheduleJob(any(JobDetail.class), triggerArg.capture());

        assertEquals(taskId.toString(), triggerArg.getValue().getJobKey().getName());
        assertEquals(policyID.toString(), triggerArg.getValue().getJobDataMap().get(ScheduledTaskJobManager.JOB_DETAIL_POLICY_GOID));
        assertEquals(ScheduledTaskJobManager.JOB_DETAIL_NODE_ALL, triggerArg.getValue().getJobDataMap().get(ScheduledTaskJobManager.JOB_DETAIL_NODE));
        assertEquals(null, triggerArg.getValue().getJobDataMap().get(ScheduledTaskJobManager.JOB_DETAIL_ID_PROVIDER_GOID));
        assertEquals(null, triggerArg.getValue().getJobDataMap().get(ScheduledTaskJobManager.JOB_DETAIL_USER_ID));
        assertTrue(triggerArg.getValue() instanceof CronTrigger);
        assertEquals(task.getCronExpression(), ((CronTrigger) triggerArg.getValue()).getCronExpression());
    }
}
