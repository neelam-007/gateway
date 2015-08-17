package com.l7tech.server.task;

import com.l7tech.gateway.common.log.TestHandler;
import com.l7tech.gateway.common.task.JobStatus;
import com.l7tech.gateway.common.task.JobType;
import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.cluster.ClusterMaster;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.polback.PolicyBackedServiceRegistry;
import com.l7tech.util.CollectionUtils;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.*;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.springframework.context.ApplicationContext;

import java.util.Date;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author alee, 8/7/2015
 */
@RunWith(MockitoJUnitRunner.class)
public class ScheduledTaskJobManagerTest {
    private static final Goid TASK_GOID = new Goid(0, 1);
    private static final Goid POLICY_GOID = new Goid(0, 2);
    private static final String TASK_NAME = "Test Task";
    private static final String CRON_EXPRESSION = "* * * * * ?";
    private ScheduledTaskJobManager jobManager;
    private Logger jobManagerLogger;
    @Mock
    private Scheduler scheduler;
    @Mock
    private ScheduledTaskManager taskManager;
    @Mock
    private ServerConfig config;
    @Mock
    private PolicyBackedServiceRegistry pbsReg;
    @Mock
    private IdentityProviderFactory identityProviderFactory;
    @Mock
    private AuditContextFactory auditContextFactory;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private ClusterMaster clusterMaster;
    @Mock
    private ClusterInfoManager clusterInfoManager;
    private ScheduledTask task;
    private TestHandler testLogHandler;

    @Before
    public void setup() throws Exception {
        jobManager = new MockedJobManager();
        task = new ScheduledTask();
        task.setGoid(TASK_GOID);
        task.setName(TASK_NAME);
        task.setPolicyGoid(POLICY_GOID);
        testLogHandler = new TestHandler();
        jobManagerLogger = Logger.getLogger(ScheduledTaskJobManager.class.getName());
        jobManagerLogger.addHandler(testLogHandler);
        ApplicationContexts.inject(jobManager, CollectionUtils.<String, Object>mapBuilder()
                .put("scheduledTaskManager", taskManager)
                .put("config", config)
                .put("pbsReg", pbsReg)
                .put("identityProviderFactory", identityProviderFactory)
                .put("auditContextFactory", auditContextFactory)
                .put("applicationContext", applicationContext)
                .put("clusterNodeId", "nodeId-abc123")
                .put("clusterMaster", clusterMaster)
                .put("clusterInfoManager", clusterInfoManager)
                .map(), false);

        when(taskManager.findByPrimaryKey(TASK_GOID)).thenReturn(task);
    }

    @After
    public void teardown() {
        testLogHandler.flush();
    }

    @Test
    public void onApplicationEventCreateRecurringJobExecuteOnCreate() throws Exception {
        task.setJobType(JobType.RECURRING);
        task.setJobStatus(JobStatus.SCHEDULED);
        task.setExecuteOnCreate(true);
        task.setCronExpression(CRON_EXPRESSION);
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);

        final EntityInvalidationEvent create = new EntityInvalidationEvent(this, ScheduledTask.class, new Goid[]{TASK_GOID}, new char[]{EntityInvalidationEvent.CREATE});
        jobManager.onApplicationEvent(create);

        assertTrue(TestHandler.isAuditPresentContaining("One time job scheduled for " + TASK_NAME));
        verify(scheduler).scheduleJob(argThat(new JobDetailMatcher(TASK_GOID.toString())), argThat(new CronTriggerMatcher(CRON_EXPRESSION)));
        verify(scheduler).scheduleJob(argThat(new SimpleTriggerMatcher(TASK_GOID.toString() + ScheduledTaskJobManager.ON_CREATE_SUFFIX)));
    }

    @Test
    public void onApplicationEventCreateRecurringJobExecuteOnCreateRescheduled() throws Exception {
        task.setJobType(JobType.RECURRING);
        task.setJobStatus(JobStatus.SCHEDULED);
        task.setExecuteOnCreate(true);
        task.setCronExpression(CRON_EXPRESSION);
        final TriggerKey triggerKey = TriggerKey.triggerKey(TASK_GOID.toString() + ScheduledTaskJobManager.ON_CREATE_SUFFIX);
        when(scheduler.checkExists(triggerKey)).thenReturn(true);

        final EntityInvalidationEvent create = new EntityInvalidationEvent(this, ScheduledTask.class, new Goid[]{TASK_GOID}, new char[]{EntityInvalidationEvent.CREATE});
        jobManager.onApplicationEvent(create);

        assertTrue(TestHandler.isAuditPresentContaining("One time job rescheduled for " + TASK_NAME));
        verify(scheduler).rescheduleJob(eq(triggerKey), argThat(new SimpleTriggerMatcher(TASK_GOID.toString() + ScheduledTaskJobManager.ON_CREATE_SUFFIX)));
        verify(scheduler, never()).scheduleJob(any(Trigger.class));
    }

    @Test
    public void onApplicationEventUpdateRecurringJobExecuteOnCreateIgnored() throws Exception {
        task.setJobType(JobType.RECURRING);
        task.setJobStatus(JobStatus.SCHEDULED);
        task.setExecuteOnCreate(true); // should be ignored
        task.setCronExpression(CRON_EXPRESSION);
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(true);

        final EntityInvalidationEvent update = new EntityInvalidationEvent(this, ScheduledTask.class, new Goid[]{TASK_GOID}, new char[]{EntityInvalidationEvent.UPDATE});
        jobManager.onApplicationEvent(update);

        assertFalse(TestHandler.isAuditPresentContaining("Immediate job scheduled for " + TASK_NAME));
        verify(scheduler).rescheduleJob(eq(TriggerKey.triggerKey(TASK_GOID.toString())), argThat(new CronTriggerMatcher(CRON_EXPRESSION)));
        verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
        verify(scheduler, never()).scheduleJob(any(Trigger.class));
    }

    @Test
    public void onApplicationEventCreateOneTimeJobExecuteOnCreateIgnored() throws Exception {
        task.setJobType(JobType.ONE_TIME);
        task.setJobStatus(JobStatus.SCHEDULED);
        task.setExecuteOnCreate(true); // should be ignored
        task.setExecutionDate(new Date().getTime());

        final EntityInvalidationEvent create = new EntityInvalidationEvent(this, ScheduledTask.class, new Goid[]{TASK_GOID}, new char[]{EntityInvalidationEvent.CREATE});
        jobManager.onApplicationEvent(create);

        assertFalse(TestHandler.isAuditPresentContaining("Immediate job scheduled for " + TASK_NAME));
        assertTrue(TestHandler.isAuditPresentContaining("One time job scheduled for " + TASK_NAME));
        verify(scheduler).scheduleJob(argThat(new JobDetailMatcher(TASK_GOID.toString())), argThat(new SimpleTriggerMatcher(TASK_GOID.toString())));
        verify(scheduler, never()).scheduleJob(any(Trigger.class));
    }

    @Test
    public void onApplicationEventCreateRecurringDisabledJobExecuteOnCreateIgnored() throws Exception {
        task.setJobType(JobType.RECURRING);
        task.setJobStatus(JobStatus.DISABLED);
        task.setExecuteOnCreate(true); // should be ignored
        task.setCronExpression(CRON_EXPRESSION);

        final EntityInvalidationEvent create = new EntityInvalidationEvent(this, ScheduledTask.class, new Goid[]{TASK_GOID}, new char[]{EntityInvalidationEvent.CREATE});
        jobManager.onApplicationEvent(create);

        assertFalse(TestHandler.isAuditPresentContaining("Immediate job scheduled for " + TASK_NAME));
        verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
        verify(scheduler, never()).scheduleJob(any(Trigger.class));
    }

    @Test
    public void onApplicationEventCreateRecurringJobExecuteOnCreateSchedulerException() throws Exception {
        task.setJobType(JobType.RECURRING);
        task.setJobStatus(JobStatus.SCHEDULED);
        task.setExecuteOnCreate(true);
        task.setCronExpression(CRON_EXPRESSION);
        when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class))).thenThrow(new SchedulerException("mocking exception"));

        final EntityInvalidationEvent create = new EntityInvalidationEvent(this, ScheduledTask.class, new Goid[]{TASK_GOID}, new char[]{EntityInvalidationEvent.CREATE});
        jobManager.onApplicationEvent(create);

        assertFalse(TestHandler.isAuditPresentContaining("One time job scheduled for " + TASK_NAME));
        assertTrue(TestHandler.isAuditPresentContaining("WARNING: Fail to create one time job for scheduled task Test Task"));
    }

    private class JobDetailMatcher extends ArgumentMatcher<JobDetail> {
        private String jobId;

        private JobDetailMatcher(final String jobId) {
            this.jobId = jobId;
        }

        @Override
        public boolean matches(final Object o) {
            final JobDetail detail = (JobDetail) o;
            return detail.getKey().getName().equals(jobId);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("JobDetail with key=" + jobId);
        }
    }

    private class CronTriggerMatcher extends ArgumentMatcher<Trigger> {
        private String cronExpression;

        private CronTriggerMatcher(final String cronExpression) {
            this.cronExpression = cronExpression;
        }

        @Override
        public boolean matches(Object o) {
            if (!(o instanceof CronTriggerImpl))  {
                return false;
            } else {
                final CronTriggerImpl cronTrigger = (CronTriggerImpl) o;
                return cronTrigger.getCronExpression().equals(cronExpression);
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("CronTriggerImpl with expression=" + cronExpression);
        }
    }

    private class SimpleTriggerMatcher extends ArgumentMatcher<Trigger> {
        private String triggerId;

        private SimpleTriggerMatcher(final String triggerId) {
            this.triggerId = triggerId;
        }

        @Override
        public boolean matches(Object o) {
            if (!(o instanceof SimpleTriggerImpl)) {
                return false;
            } else {
                final SimpleTriggerImpl trigger = (SimpleTriggerImpl) o;
                return trigger.getKey().getName().equals(triggerId);
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("SimpleTriggerImpl with key=" + triggerId);
        }
    }

    private class MockedJobManager extends ScheduledTaskJobManager {
        @Override
        protected Scheduler getScheduler() {
            return scheduler;
        }
    }
}
