package com.l7tech.server.task;

import com.l7tech.gateway.common.task.JobStatus;
import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.cluster.ClusterMaster;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.event.system.Stopped;
import com.l7tech.server.polback.PolicyBackedServiceRegistry;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.util.PostStartupApplicationListener;
import org.quartz.*;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobStore;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 */

public class ScheduledTaskJobManager implements PostStartupApplicationListener {

    private static final Logger logger = Logger.getLogger(ScheduledTaskJobManager.class.getName());
    @Inject
    private ScheduledTaskManager scheduledTaskManager;
    @Inject
    private ClusterPropertyManager clusterPropertyManager;
    @Inject
    private PolicyCache policyCache;
    @Inject
    protected PolicyBackedServiceRegistry pbsReg;

    @Inject
    protected AuditContextFactory auditContextFactory;

    @Inject
    protected ApplicationContext applicationContext;

    @Inject
    @Named("clusterNodeId")
    protected String nodeId;

    @Inject
    protected ClusterMaster clusterMaster;

    @Inject
    protected ClusterInfoManager clusterInfoManager;

    private Scheduler scheduler = null;
    private final String SCHEDULER_MAX_THREADS = "scheduler.maxthreads"; //todo add system property
    private final String schedulerId = "L7ServiceScheduler";

    public ScheduledTaskJobManager() {
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ReadyForMessages) {
            doStart();
        } else if (applicationEvent instanceof Stopped) {
            doStop();
        } else if (applicationEvent instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent event = (EntityInvalidationEvent) applicationEvent;
            if (ScheduledTask.class.equals(event.getEntityClass())) {
                for (int i = 0; i < event.getEntityOperations().length; i++) {
                    final char op = event.getEntityOperations()[i];
                    final Goid goid = event.getEntityIds()[i];
                    switch (op) {
                        case EntityInvalidationEvent.UPDATE:
                            removeJob(goid);
                        case EntityInvalidationEvent.CREATE: // Intentional fallthrough
                            try {
                                ScheduledTask task = scheduledTaskManager.findByPrimaryKey(goid);
                                scheduleJob(task);
                            } catch (FindException e) {
                                if (logger.isLoggable(Level.WARNING)) {
                                    logger.log(Level.WARNING, MessageFormat.format("Unable to find created/updated jdbc connection #{0}", goid), e);
                                }
                            }
                            break;
                        case EntityInvalidationEvent.DELETE:
                            removeJob(goid);
                            break;
                    }
                }
            }
        }

    }

    private void doStop() {
        if (scheduler != null) {
            try {
                scheduler.shutdown();
            } catch (SchedulerException e) {
                logger.log(Level.WARNING, "Error shutting down scheduler", e);
            }
        }
    }

    private void doStart() {
        try {
            ScheduledPolicyRunner.getInstance(this);
            for (ScheduledTask task : scheduledTaskManager.findAll()) {
                scheduleJob(task);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error trying to create scheduled jobs", e);
        }
    }

    protected Scheduler getScheduler() {
        if (scheduler == null) {
            try {
                int maxThreads;
                try {
                    maxThreads = Integer.parseInt(clusterPropertyManager.getProperty(SCHEDULER_MAX_THREADS));
                } catch (NumberFormatException e) {
                    maxThreads = 10;
                }
                SimpleThreadPool threadPool = new SimpleThreadPool(maxThreads, Thread.NORM_PRIORITY);
                threadPool.initialize();
                JobStore jobStore = new RAMJobStore();
                DirectSchedulerFactory.getInstance().createScheduler(schedulerId, schedulerId, threadPool, jobStore);
                scheduler = DirectSchedulerFactory.getInstance().getScheduler(schedulerId);
                scheduler.start();

                logger.log(Level.INFO, "Scheduler initalized");

            } catch (Exception e) {
                logger.log(Level.WARNING, "Error trying to create scheduler", e);
            }
        }
        return scheduler;
    }

    public void scheduleJob(ScheduledTask job) {
        if (job.getJobStatus().equals(JobStatus.DISABLED))
            return;

        switch (job.getJobType()) {
            case ONE_TIME:
                scheduleOneTimeJob(job);
                break;
            case RECURRING:
                scheduleRecurringJob(job);
                break;
            default:
                logger.info("Unknown job type: " + job.getJobType());
        }

    }

    public void scheduleOneTimeJob(ScheduledTask job) {
        // todo only for future jobs?
        try {
            JobDetail jobDetail = getJobDetail(job);
            Trigger trigger = newTrigger().withIdentity(job.getId()).startAt(new Date(job.getExecutionDate())).build();
            getScheduler().scheduleJob(jobDetail, trigger);
            logger.info("Policy Scheduler has scheduled one time job for policy: " + job.getPolicyGoid() + " on " + job.getExecutionDate());  // todo real date
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error trying to schedule one time job for policy: " + job.getPolicyGoid() + " on " + job.getExecutionDate(), e);
        }

    }

    private String getPolicyName(Goid policyGoid) {
        return policyGoid.toString();  // todo
    }

    private JobDetail getJobDetail(ScheduledTask job) {
        return newJob(ScheduledServiceQuartzJob.class)
                .withIdentity(job.getId())
                .usingJobData("nodeId", job.isUseOneNode() ? "One" : "All")
                .usingJobData("jobType", job.getJobType().toString())
                .usingJobData("entityGoid", job.getId())
                .usingJobData("policyGoid", job.getPolicyGoid().toString())
//                .usingJobData("serviceName", job.getServiceName())
                .build();
    }


    public void scheduleRecurringJob(ScheduledTask job) {

        JobDetail jobDetail = getJobDetail(job);
        CronTrigger cronTrigger = newTrigger().withIdentity(job.getId()).withSchedule(cronSchedule(job.getCronExpression())).build();
        logger.log(Level.WARNING, "Policy Scheduler has scheduled recurring job for policy: " + job.getPolicyGoid() + " with schedule " + job.getCronExpression());
        try {
            getScheduler().scheduleJob(jobDetail, cronTrigger);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error trying to schedule recurring job for policy: " + job.getPolicyGoid() + " with schedule " + job.getCronExpression());
        }

    }

    public void removeJob(Goid taskGoid) {
        try {
            boolean deleted = getScheduler().deleteJob(JobKey.jobKey(taskGoid.toString()));
            if (deleted) {
                logger.log(Level.INFO, "Policy Scheduler has removed job " + taskGoid.toString());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error trying to remove job from Policy Scheduler with id: " + taskGoid.toString());
        }
    }


}
