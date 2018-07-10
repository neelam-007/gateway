package com.l7tech.server.task;

import com.l7tech.objectmodel.Goid;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

import java.util.logging.Logger;

/**
 * Created by luiwy01 on 3/23/2015.
 */
@DisallowConcurrentExecution
public class ScheduledServiceQuartzJob implements StatefulJob {

    private static Logger logger = Logger.getLogger(ScheduledServiceQuartzJob.class.getName());


    /**
     * Quartz requires a public empty constructor so that the
     * scheduler can instantiate the class whenever it needs.
     */
    public ScheduledServiceQuartzJob() {
    }

    /**
     * <p>
     * Called by the <code>{@link org.quartz.Scheduler}</code> when a
     * <code>{@link org.quartz.Trigger}</code> fires that is associated with
     * the <code>Job</code>.
     * </p>
     *
     * @throws org.quartz.JobExecutionException if there is an exception while executing the job.
     */
    public void execute(JobExecutionContext context)
            throws JobExecutionException {

        Goid policyGoid = Goid.parseGoid(context.getTrigger().getJobDataMap().getString(ScheduledTaskJobManager.JOB_DETAIL_POLICY_GOID));
        String nodeId = context.getTrigger().getJobDataMap().getString(ScheduledTaskJobManager.JOB_DETAIL_NODE);
        String providerId = context.getTrigger().getJobDataMap().getString(ScheduledTaskJobManager.JOB_DETAIL_ID_PROVIDER_GOID);
        String userId = context.getTrigger().getJobDataMap().getString(ScheduledTaskJobManager.JOB_DETAIL_USER_ID);
        String name = context.getTrigger().getJobDataMap().getString(ScheduledTaskJobManager.JOB_DETAIL_NAME);

        ScheduledPolicyRunner scheduledPolicyRunner = ScheduledPolicyRunner.getInstance();
        if (scheduledPolicyRunner != null && (ScheduledTaskJobManager.JOB_DETAIL_NODE_ALL.equals(nodeId) || scheduledPolicyRunner.isClusterMaster())) {
            try {
            	scheduledPolicyRunner.runBackgroundTask(name, policyGoid, providerId == null ? null : Goid.parseGoid(providerId), userId);
            } catch (Exception e) {
                throw new JobExecutionException(e);
            }
        }
    }
}
