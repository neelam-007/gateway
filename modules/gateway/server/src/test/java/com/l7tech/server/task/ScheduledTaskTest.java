package com.l7tech.server.task;

import com.l7tech.objectmodel.Goid;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;

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

    @Test
    public void runPolicy() throws Exception {

        Goid policyID = new Goid(4, 5);
        when(jobDataMap.getString(Matchers.eq(ScheduledTaskJobManager.JOB_DETAIL_POLICY_GOID))).thenReturn(policyID.toString());
        when(jobDataMap.getString(Matchers.eq(ScheduledTaskJobManager.JOB_DETAIL_NODE))).thenReturn(ScheduledTaskJobManager.JOB_DETAIL_NODE_ONE);
        when(executionContext.getTrigger()).thenReturn(trigger);
        when(trigger.getJobDataMap()).thenReturn(jobDataMap);
        ScheduledPolicyRunner.setInstance(policyRunner);
        stub(policyRunner.isClusterMaster()).toReturn(true);

        Job job = new ScheduledServiceQuartzJob();
        job.execute(executionContext);

        verify(policyRunner).runBackgroundTask(policyID);
        verify(policyRunner).isClusterMaster();
    }

}
