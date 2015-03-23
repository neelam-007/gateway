package com.l7tech.server.task;

import com.l7tech.objectmodel.Goid;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.logging.Logger;

/**
* Created by luiwy01 on 3/23/2015.
*/
public class ScheduledServiceQuartzJob implements Job {

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

        Goid policyGoid = Goid.parseGoid(context.getJobDetail().getJobDataMap().getString("policyGoid"));
        String nodeId = context.getJobDetail().getJobDataMap().getString("nodeId");

        if (nodeId.equals("All") || ScheduledPolicyRunner.getInstance(null).isClusterMaster()) {
            try {
                ScheduledPolicyRunner.getInstance(null).runBackgroundTask(policyGoid);
            } catch (Exception e) {
                throw new JobExecutionException(e);
            }
        }

    }


}
