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
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.polback.PolicyBackedServiceRegistry;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import org.jetbrains.annotations.Nullable;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.listeners.TriggerListenerSupport;
import org.quartz.simpl.SimpleThreadPool;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 */

public class ScheduledTaskJobManager implements PostStartupApplicationListener {

    private static final Logger logger = Logger.getLogger(ScheduledTaskJobManager.class.getName());
    public static final String JOB_DETAIL_NODE = "node";
    public static final String JOB_DETAIL_NAME = "name";
    public static final String JOB_DETAIL_JOBTYPE = "jobType";
    public static final String JOB_DETAIL_ENITTY_GOID = "entityGoid";
    public static final String JOB_DETAIL_POLICY_GOID = "policyGoid";
    public static final String JOB_DETAIL_USER_ID = "userId";
    public static final String JOB_DETAIL_ID_PROVIDER_GOID = "idProviderGoid";
    public static final String JOB_DETAIL_NODE_ONE = "One";
    public static final String JOB_DETAIL_NODE_ALL = "All";
    private static final int DEFAULT_DELAY_MILLIS = 16000;
    private static final String DELAY_SYS_PROP = "com.l7tech.server.task.ScheduledTaskJobManager.create.delay.millis";
    static final String ON_CREATE_SUFFIX = "-onCreate";

    @Inject
    protected ScheduledTaskManager scheduledTaskManager;
    @Inject
    private ServerConfig config;
    @Inject
    protected PolicyBackedServiceRegistry pbsReg;
    @Inject
    protected IdentityProviderFactory identityProviderFactory;
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
                        case EntityInvalidationEvent.CREATE: // Intentional fallthrough
                            try {
                                ScheduledTask task = scheduledTaskManager.findByPrimaryKey(goid);
                                scheduleJob(task);
                                if (op == EntityInvalidationEvent.CREATE && task.getJobType().equals(JobType.RECURRING)
                                        && task.getJobStatus().equals(JobStatus.SCHEDULED) && task.isExecuteOnCreate()) {
                                    scheduleOneTimeJob(task, getOnCreateDate(), ON_CREATE_SUFFIX);
                                }
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
                if (task.getJobStatus().equals(JobStatus.SCHEDULED)) {
                    scheduleJob(task);
                }
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
                quartzProperties.setProperty("org.quartz.scheduler.skipUpdateCheck", "true");
                quartzProperties.setProperty("org.quartz.threadPool.threadCount", Integer.toString(maxThreads));
                SchedulerFactory schedulerFactory = new StdSchedulerFactory(quartzProperties);
                scheduler = schedulerFactory.getScheduler();

                if (scheduler == null) {
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
                        String nodeId = trigger.getJobDataMap().getString(JOB_DETAIL_NODE);
                        if (clusterMaster.isMaster()) {
                            String jobType = trigger.getJobDataMap().getString(JOB_DETAIL_JOBTYPE);
                            String entityGoid = trigger.getJobDataMap().getString(JOB_DETAIL_ENITTY_GOID);
                            try {
                                if (jobType.equals(JobType.ONE_TIME.name())) {
                                    ScheduledTask task = scheduledTaskManager.findByPrimaryKey(Goid.parseGoid(entityGoid), true);
                                    if (!task.getJobStatus().equals(JobStatus.COMPLETED)) {
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
        if (job.getJobStatus().equals(JobStatus.DISABLED)) {
            removeJob(job.getGoid());
            return;
        }

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
        scheduleOneTimeJob(job, new Date(job.getExecutionDate()), null);
    }

    private void scheduleOneTimeJob(ScheduledTask job, final Date startAt, final String identitySuffix) {
        if (job.getJobStatus().equals(JobStatus.COMPLETED))
            return;
        try {
            final Trigger trigger = populateTriggerDetails(job, newTrigger().startAt(startAt).withSchedule(simpleSchedule().withMisfireHandlingInstructionIgnoreMisfires()), identitySuffix);
            if (getScheduler().checkExists(trigger.getKey())) {
                getScheduler().rescheduleJob(trigger.getKey(), trigger);
                logger.log(Level.INFO, "One time job rescheduled for " + job.getName());

            } else {
                final JobDetail jobDetail = getJobDetail(job);
                if (getScheduler().checkExists(jobDetail.getKey())) {
                    // job has already been added with another trigger
                    getScheduler().scheduleJob(trigger);
                } else {
                    getScheduler().scheduleJob(jobDetail, trigger);
                }
                logger.log(Level.INFO, "One time job scheduled for " + job.getName());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Fail to create one time job for scheduled task " + job.getName() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    private Date getOnCreateDate() {
        final Integer onCreateDelay = SyspropUtil.getInteger(DELAY_SYS_PROP, DEFAULT_DELAY_MILLIS);
        logger.log(Level.INFO, "On create delay set to " + onCreateDelay + " milliseconds");
        final GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(java.util.Calendar.MILLISECOND, onCreateDelay);
        return cal.getTime();
    }

    private JobDetail getJobDetail(ScheduledTask job) {
        return newJob(ScheduledServiceQuartzJob.class)
                .withIdentity(job.getId())
                .build();
    }

    private Trigger populateTriggerDetails(ScheduledTask job, TriggerBuilder triggerBuilder, @Nullable final String identitySuffix) {
        String identity = identitySuffix == null ? job.getId() : job.getId() + identitySuffix;
        return triggerBuilder
                .withIdentity(identity)
                .usingJobData(JOB_DETAIL_NODE, job.isUseOneNode() ? JOB_DETAIL_NODE_ONE : JOB_DETAIL_NODE_ALL)
                .usingJobData(JOB_DETAIL_NAME, job.getName())
                .usingJobData(JOB_DETAIL_JOBTYPE, job.getJobType().toString())
                .usingJobData(JOB_DETAIL_ENITTY_GOID, job.getId())
                .usingJobData(JOB_DETAIL_POLICY_GOID, job.getPolicyGoid().toString())
                .usingJobData(JOB_DETAIL_ID_PROVIDER_GOID, job.getIdProviderGoid() == null ? (String) null : job.getIdProviderGoid().toString())
                .usingJobData(JOB_DETAIL_USER_ID, job.getUserId())
                .forJob(job.getId())
                .build();

    }


    private Trigger populateTriggerDetails(ScheduledTask job, TriggerBuilder triggerBuilder) {
        return populateTriggerDetails(job, triggerBuilder, null);
    }

    public void scheduleRecurringJob(ScheduledTask job) {

        Trigger trigger = populateTriggerDetails(job, newTrigger().withSchedule(cronSchedule(job.getCronExpression()).withMisfireHandlingInstructionIgnoreMisfires()));
        try {
            if (getScheduler().checkExists(TriggerKey.triggerKey(job.getId()))) {
                getScheduler().rescheduleJob(TriggerKey.triggerKey(job.getId()), trigger);
                logger.log(Level.INFO, "Recurring job rescheduled for " + job.getName());

            } else {
                getScheduler().scheduleJob(getJobDetail(job), trigger);
                logger.log(Level.INFO, "Recurring job scheduled for " + job.getName());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Fail to create recurring job for scheduled task " + job.getName(), ExceptionUtils.getDebugException(e));
        }

    }

    public void removeJob(Goid taskGoid) {
        try {
            boolean deleted = getScheduler().unscheduleJob(TriggerKey.triggerKey(taskGoid.toString()));
            if (deleted) {
                logger.log(Level.INFO, "Job removed for scheduled task id:" + taskGoid.toString());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to remove job with id: " + taskGoid.toString(), ExceptionUtils.getDebugException(e));
        }
    }


}
