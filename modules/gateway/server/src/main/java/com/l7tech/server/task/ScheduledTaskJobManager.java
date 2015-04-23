package com.l7tech.server.task;

import com.l7tech.gateway.common.task.JobStatus;
import com.l7tech.gateway.common.task.JobType;
import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.cluster.ClusterMaster;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.event.system.Stopped;
import com.l7tech.server.polback.PolicyBackedServiceRegistry;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.ExceptionUtils;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.listeners.TriggerListenerSupport;
import org.quartz.simpl.SimpleThreadPool;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 */

public class ScheduledTaskJobManager implements PostStartupApplicationListener {

    private static final Logger logger = Logger.getLogger(ScheduledTaskJobManager.class.getName());
    public static final String JOB_DETAIL_NODE = "node";
    public static final String JOB_DETAIL_JOBTYPE = "jobType";
    public static final String JOB_DETAIL_ENITTY_GOID = "entityGoid";
    public static final String JOB_DETAIL_POLICY_GOID = "policyGoid";
    public static final String JOB_DETAIL_NODE_ONE = "One";
    public static final String JOB_DETAIL_NODE_ALL = "All";

    @Inject
    protected ScheduledTaskManager scheduledTaskManager;
    @Inject
    private ServerConfig config;
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
    private final int MAX_THREADS = 10;

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
                                    logger.log(Level.WARNING, MessageFormat.format("Unable to find created/updated scheduled task #{0}", goid), e);
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
                logger.log(Level.WARNING, "Failed to shutting down scheduler", e);
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
            logger.log(Level.WARNING, "Failed to create scheduled jobs", ExceptionUtils.getDebugException(e));
        }
    }

    protected Scheduler getScheduler() {
        if (scheduler == null) {
            try {
                int maxThreads = config.getIntProperty(ServerConfigParams.PARAM_SCHEDULED_TASK_MAX_THREADS, MAX_THREADS);
                SimpleThreadPool threadPool = new SimpleThreadPool(maxThreads, Thread.NORM_PRIORITY);
                threadPool.initialize();
                Properties quartzProperties = new Properties();
                // skip version update check
                quartzProperties.setProperty("org.quartz.scheduler.skipUpdateCheck","true");
                quartzProperties.setProperty("org.quartz.threadPool.threadCount", Integer.toString(maxThreads));
                SchedulerFactory schedulerFactory = new StdSchedulerFactory(quartzProperties);
                scheduler = schedulerFactory.getScheduler();

                if(scheduler == null){
                    logger.log(Level.WARNING, "Failed to create scheduler");
                    return null;
                }
                scheduler.getListenerManager().addTriggerListener(new TriggerListenerSupport() {
                    @Override
                    public String getName() {
                        return "overlap";
                    }

                    @Override
                    public void triggerComplete(Trigger trigger, JobExecutionContext context, Trigger.CompletedExecutionInstruction triggerInstructionCode) {
                        // update one time jobs as completed
                        String nodeId = context.getJobDetail().getJobDataMap().getString(JOB_DETAIL_NODE);
                        if(ScheduledTaskJobManager.JOB_DETAIL_NODE_ALL.equals(nodeId) || clusterMaster.isMaster()) {
                            String jobType = context.getJobDetail().getJobDataMap().getString(JOB_DETAIL_JOBTYPE);
                            String entityGoid = context.getJobDetail().getJobDataMap().getString(JOB_DETAIL_ENITTY_GOID);
                            try {
                                if (jobType.equals(JobType.ONE_TIME.name())) {
                                    ScheduledTask task = scheduledTaskManager.findByPrimaryKey(Goid.parseGoid(entityGoid),true);
                                    if(!task.getJobStatus().equals(JobStatus.COMPLETED)) {
                                        task.setJobStatus(JobStatus.COMPLETED);
                                        scheduledTaskManager.update(task);
                                    }
                                }
                            } catch (UpdateException | FindException e) {
                                logger.log(Level.WARNING, "Failed to update scheduled task #" + entityGoid + " as completed.", ExceptionUtils.getDebugException(e));
                            }
                        }
                    }
                });
                scheduler.start();

                logger.log(Level.INFO, "Scheduler initialized");

            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to create scheduler", ExceptionUtils.getDebugException(e));
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

        try {
            if (job.getJobStatus().equals(JobStatus.COMPLETED) || job.getExecutionDate() < System.currentTimeMillis()){
                   if(getScheduler().deleteJob(JobKey.jobKey(job.getId()))){
                    logger.log(Level.INFO, "Removed disabled recurring job for " + job.getName());
                }
                return;
            }
            JobDetail jobDetail = getJobDetail(job);
            Trigger trigger = newTrigger().withIdentity(job.getId()).startAt(new Date(job.getExecutionDate())).build();
            if(getScheduler().checkExists(jobDetail.getKey())){
                getScheduler().rescheduleJob(TriggerKey.triggerKey(job.getId()), trigger);
                logger.log(Level.INFO, "One time job rescheduled for " + job.getName());
            }else {
                getScheduler().scheduleJob(jobDetail, trigger);
                logger.log(Level.INFO, "One time job scheduled for " + job.getName());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Fail to create one time job for scheduled task " +job.getName() , ExceptionUtils.getDebugException(e));
        }
    }


    private JobDetail getJobDetail(ScheduledTask job) {
        return newJob(ScheduledServiceQuartzJob.class)
                .withIdentity(job.getId())
                .usingJobData(JOB_DETAIL_NODE, job.isUseOneNode() ? JOB_DETAIL_NODE_ONE : JOB_DETAIL_NODE_ALL)
                .usingJobData(JOB_DETAIL_JOBTYPE, job.getJobType().toString())
                .usingJobData(JOB_DETAIL_ENITTY_GOID, job.getId())
                .usingJobData(JOB_DETAIL_POLICY_GOID, job.getPolicyGoid().toString())
                .build();
    }


    public void scheduleRecurringJob(ScheduledTask job) {

        try {
            if(job.getJobStatus().equals(JobStatus.DISABLED)){
                if(getScheduler().deleteJob(JobKey.jobKey(job.getId()))) {
                    logger.log(Level.INFO, "Removed disabled recurring job for " + job.getName());
                }
                return;
            }
            CronTrigger cronTrigger = newTrigger().withIdentity(job.getId()).withSchedule(cronSchedule(job.getCronExpression())).build();
            JobDetail jobDetail = getJobDetail(job);
            if(getScheduler().checkExists(jobDetail.getKey())){
                getScheduler().rescheduleJob(TriggerKey.triggerKey(job.getId()), cronTrigger);
                logger.log(Level.INFO, "Recurring job rescheduled for " + job.getName());
            }else {

                getScheduler().scheduleJob(jobDetail, cronTrigger);
                logger.log(Level.INFO, "Recurring job scheduled for " + job.getName());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Fail to create recurring job for scheduled task " +job.getName() , ExceptionUtils.getDebugException(e));
        }

    }

    public void removeJob(Goid taskGoid) {
        try {
            boolean deleted = getScheduler().deleteJob(JobKey.jobKey(taskGoid.toString()));
            if (deleted) {
                logger.log(Level.INFO, "Job removed for scheduled task id:" + taskGoid.toString());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to remove job with id: " + taskGoid.toString() , ExceptionUtils.getDebugException(e));
        }
    }


}
