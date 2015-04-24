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

        if (ScheduledTaskJobManager.JOB_DETAIL_NODE_ALL.equals(nodeId) || ScheduledPolicyRunner.getInstance(null).isClusterMaster()) {
            try {
                ScheduledPolicyRunner.getInstance(null).runBackgroundTask(policyGoid);
            } catch (Exception e) {
                throw new JobExecutionException(e);
            }
        }
    }
}
