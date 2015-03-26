package com.l7tech.server.task;

import com.l7tech.objectmodel.Goid;
import org.quartz.*;

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

        Goid policyGoid = Goid.parseGoid(context.getMergedJobDataMap().getString("policyGoid"));
        String nodeId = context.getMergedJobDataMap().getString("nodeId");

        if (nodeId.equals("All") || ScheduledPolicyRunner.getInstance(null).isClusterMaster()) {
            try {
                ScheduledPolicyRunner.getInstance(null).runBackgroundTask(policyGoid);
            } catch (Exception e) {
                throw new JobExecutionException(e);
            }finally{
                // mark one time tasks as completed
                ScheduledPolicyRunner.getInstance(null).markAsCompleted(context.getMergedJobDataMap().getString("entityGoid"));
            }
        }

    }


}
