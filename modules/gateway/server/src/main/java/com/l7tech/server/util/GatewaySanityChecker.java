package com.l7tech.server.util;

import com.l7tech.gateway.common.audit.BootMessages;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.event.system.Starting;
import com.l7tech.server.event.system.SystemEvent;
import com.l7tech.server.upgrade.FatalUpgradeException;
import com.l7tech.server.upgrade.NonfatalUpgradeException;
import com.l7tech.server.upgrade.UpgradeTask;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ExceptionUtils;
import org.hibernate.TransactionException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.util.CollectionUtils.set;

/**
 * Bean that checks sanity of the database/cluster/local configuration before allowing bootup to proceed.
 * <p/>
 * Currently this just checks for flagged database upgrade tasks in the cluster properties table.
 * <p/>
 * Upgrade tasks are run when various system events occur.
 */
public class GatewaySanityChecker extends ApplicationObjectSupport implements InitializingBean {
    private static final Logger logger = Logger.getLogger(GatewaySanityChecker.class.getName());
    private static final String CPROP_PREFIX_UPGRADE = "upgrade.task.";

    /**
     * Key is the class of {@link SystemEvent} on which the task should be performed;
     * Value is a {@link Set} of classnames of the {@link UpgradeTask}s that are allowed
     * to be run on that event.  Tasks listed under the null key will be run immediately in
     * {@link #afterPropertiesSet()}.
     * TODO this metadata should probably come from the UpgradeTasks themselves, and we should just whitelist the entire server.upgrade package
     */
    private static final Map<Class<? extends SystemEvent>, Set<String>> whitelist = CollectionUtils.<Class<? extends SystemEvent>, Set<String>>mapBuilder()
            .put(null, set(
                    "com.l7tech.server.upgrade.Upgrade45To46AddPasswordExpiry",
                    "com.l7tech.server.upgrade.Upgrade44To45SwitchPolicyIncludesToGuids",
                    "com.l7tech.server.upgrade.Upgrade42To43MigratePolicies",
                    "com.l7tech.server.upgrade.Upgrade465To50UpgradeKeystores",
                    "com.l7tech.server.upgrade.Upgrade51To52CanonicalizeDNs",
                    "com.l7tech.server.upgrade.Upgrade51To52AddClusterProperties",
                    "com.l7tech.server.upgrade.Upgrade51To52UpdateRoles",
                    "com.l7tech.server.upgrade.Upgrade52To53UpdateRoles",
                    "com.l7tech.server.upgrade.Upgrade52to53UpdateCommunitySchemas",
                    "com.l7tech.server.upgrade.Upgrade52To53UpdateJmsProviderType",
                    "com.l7tech.server.upgrade.Upgrade531To54UpdateRoles",
                    "com.l7tech.server.upgrade.Upgrade61to615UpdateGatewayManagementWsdl",
                    "com.l7tech.server.upgrade.Upgrade61To615AddRoles",
                    "com.l7tech.server.upgrade.Upgrade62To70CanonicalizeFedUserSubjectDNs",
                    "com.l7tech.server.upgrade.Upgrade70to71UpdateGatewayManagementWsdl",
                    "com.l7tech.server.upgrade.Upgrade71To80SinkConfig",
                    "com.l7tech.server.upgrade.Upgrade71To80IdReferences") )
            .put(Starting.class, set(
                    "com.l7tech.server.upgrade.Upgrade365To37AddSampleMessagePermissions" ))
            .put(Started.class, set(
                    "com.l7tech.server.upgrade.Upgrade35To36AddRoles",
                    "com.l7tech.server.upgrade.Upgrade42To43AddPolicyPermissions",
                    "com.l7tech.server.upgrade.Upgrade42To43AddInitialPolicyVersions",
                    "com.l7tech.server.upgrade.Upgrade465To50UpdateRoles",
                    "com.l7tech.server.upgrade.Upgrade70To71UpdateRoles",
                    "com.l7tech.server.upgrade.Upgrade71To80UpdateRoles" ) )
        .unmodifiableMap();

    private final ClusterPropertyManager clusterPropertyManager;
    private final PlatformTransactionManager transactionManager; // required for TransactionTemplate
    private Auditor auditor;
    private List<ClusterProperty> taskProps;
    private UpgradeTask[] earlyTasks;


    public GatewaySanityChecker(PlatformTransactionManager transactionManager,
                                ClusterPropertyManager clusterPropertyManager)
    {
        this.transactionManager = transactionManager;
        this.clusterPropertyManager = clusterPropertyManager;
        if (clusterPropertyManager == null) throw new NullPointerException("Cluster Property Manager is required");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.auditor = new Auditor(this, getApplicationContext(), logger);

        ApplicationContext appCtx = getApplicationContext();
        ApplicationEventMulticaster eventMulticaster = appCtx.getBean( "applicationEventMulticaster", ApplicationEventMulticaster.class );
        eventMulticaster.addApplicationListener( new ApplicationListener(){
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                GatewaySanityChecker.this.onApplicationEvent( event );
            }
        } );

        // Run nonconditional sanity checking tasks
        if (earlyTasks != null) {
            for (UpgradeTask task : earlyTasks) {
                runTask(task, null);
            }
        }
        // Check for upgrade tasks flagged in the cluster properties table
        final Collection<ClusterProperty> props;
        try {
            props = clusterPropertyManager.findAll();
        } catch (FindException e) {
            throw new RuntimeException(e);
        }

        Map<Long, ClusterProperty> tasks = new HashMap<Long, ClusterProperty>();
        List<Long> taskOrder = new ArrayList<Long>();

        Map<Long, ClusterProperty> startupTasks = new HashMap<Long, ClusterProperty>();
        List<Long> startupTaskOrder = new ArrayList<Long>();

        for (ClusterProperty prop : props) {
            String name = prop.getName();
            if (name.startsWith(CPROP_PREFIX_UPGRADE) && name.length() > CPROP_PREFIX_UPGRADE.length()) {
                String classname = prop.getValue();

                if (!isUpgradeTaskRecognized(classname)) {
                    auditor.logAndAudit(BootMessages.UPGRADE_TASK_IGNORED, "unrecognized upgrade task class name: " + name + "=" + classname);
                    continue;
                }

                final long ordinal;
                try {
                    ordinal = Long.parseLong(name.substring(CPROP_PREFIX_UPGRADE.length()));

                    if (whitelist.get(null).contains(classname)) {
                        startupTasks.put(ordinal, prop);
                        startupTaskOrder.add(ordinal);
                    } else {
                        tasks.put(ordinal, prop);
                        taskOrder.add(ordinal);
                    }
                } catch (NumberFormatException nfe) {
                    auditor.logAndAudit(BootMessages.UPGRADE_TASK_IGNORED, new String[]{"invalid ordinal in upgrade.task cluster property: " + name + ": " + ExceptionUtils.getMessage(nfe)}, nfe);
                }
            }
        }

        if (!startupTaskOrder.isEmpty()) {
            Collections.sort(startupTaskOrder);
            for (Long ordinal : startupTaskOrder) {
                ClusterProperty prop = startupTasks.get(ordinal);
                try {
                    doUpgradeTask(prop);
                } catch (FatalUpgradeException e) {
                    auditor.logAndAudit(BootMessages.UPGRADE_TASK_FATAL, new String[] { "Startup task failed: " + ExceptionUtils.getMessage(e) }, e);
                    throw e;
                }
            }
        }

        if (!taskOrder.isEmpty()) {
            Collections.sort(taskOrder);
            List<ClusterProperty> taskProps = new ArrayList<ClusterProperty>();
            for (Long ordinal : taskOrder)
                taskProps.add(tasks.get(ordinal));
            assert !taskProps.isEmpty();
            this.taskProps = taskProps;
        } else {
            this.taskProps = Collections.emptyList();
        }
    }

    /**
     * Set some extra sanity checking tasks which will be run early.
     * <p/>
     * Unlike the regular upgrade tasks, which run when the STARTED application event is received,
     * these tasks will be run immediately from {@link #afterPropertiesSet}.
     *
     * @param earlyTasks  extra sanity checker tasks to run early, or null.
     */
    public void setEarlyTasks(UpgradeTask[] earlyTasks) {
        this.earlyTasks = earlyTasks;
    }

    /**
     * @param value a cluster property value that might be the name of a supported UpgradeTask subclass.
     * @return true if the specified upgrade task is on the whitelist of allowed upgrade tasks.
     */
    private boolean isUpgradeTaskRecognized(String value) {
        for (Set<String> strings : whitelist.values()) {
            for (String string : strings) {
                if (string.equals(value)) return true;
    }
        }
        return false;
    }

    /**
     * Execute the specified upgrade task, in its own transaction.
     * Caller is responsible for ensuring that the upgrade task
     * classnames have already been whitelisted, and sorting the list.
     *
     * @param property cluster property containing the upgrade task to run.
     */
    private void doUpgradeTask(final ClusterProperty property) throws FatalUpgradeException {
        final String taskName = property.getName();
        final String className = property.getValue();
        logger.info("Running upgrade task: " + taskName + "=" + className);

        // Instantiate the upgrade task
        final UpgradeTask task;
        try {
            task = instantiate(className);
        } catch (Exception e) {
            auditor.logAndAudit(BootMessages.UPGRADE_TASK_IGNORED,
                                new String[] {"Unable to instantiate upgrade task: " +
                                        taskName + "=" + className + ": " + ExceptionUtils.getMessage(e)},
                                e);
            return;
        }

        runTask(task, property);
        logger.info( "Completed upgrade task: " + taskName );
    }

    /**
     * Run the specified upgrade task.
     *
     * @param task          the task to run. required
     * @param propToDelete    a ClusterProperty to delete in the same transaction that runs the upgrade task.
     *                  If null, no cluster properties will be deleted (unless the task deletes any on its own, of course).
     * @throws FatalUpgradeException  if the task failed, no further tasks should be attempted, and the Gateway startup should abort
     */
    private void runTask(final UpgradeTask task, final ClusterProperty propToDelete) throws FatalUpgradeException {
        // Remove the cluster property and do the work both in the same transaction
        final NonfatalUpgradeException[] enonfatal = new NonfatalUpgradeException[] { null };
        final FatalUpgradeException[] efatal = new FatalUpgradeException[] { null };

        boolean wasSystem = AuditContextUtils.isSystem();
        AuditContextUtils.setSystem(true);
        try {
            new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    boolean completedTask = false;
                    try {
                        // If triggered by a cluster property, remove it so noone else repeats the work
                        if (propToDelete != null)
                            clusterPropertyManager.delete( propToDelete );

                        // Do the actual work
                        task.upgrade(getApplicationContext());
                        completedTask = true;
                    } catch (NonfatalUpgradeException e) {
                        enonfatal[0] = e;
                        transactionStatus.setRollbackOnly();
                    } catch (FatalUpgradeException e) {
                        efatal[0] = e;
                        transactionStatus.setRollbackOnly();
                    } catch (DeleteException e) {
                        // Something has gone unexpectedly very wrong
                        efatal[0] = new FatalUpgradeException("Unable to delete upgrade task property: " + propToDelete.getName() + ": " +
                                ExceptionUtils.getMessage(e), e);
                        transactionStatus.setRollbackOnly();
                    } finally {
                        if (!completedTask) transactionStatus.setRollbackOnly();
                    }
                }
            });
        } catch (TransactionException e) {
            // Was the underlying cause something under our control?
            if (enonfatal[0] == null && efatal[0] == null) {
                // No -- treat as non-fatal commit error (maybe a collision with another node)
                auditor.logAndAudit(BootMessages.UPGRADE_TASK_NONFATAL,
                                    new String[] { "Unexpected failure during upgrade task: " + ExceptionUtils.getMessage(e) },
                                    e);
                return;
            }

            // Yes -- FALLTHROUGH and handle it
        } finally {
            AuditContextUtils.setSystem(wasSystem);
        }

        // Handle upgrade-failed-but-continue-anyway result
        if (enonfatal[0] != null) {
            // The upgrade failed and was rolled back, but the Gateway can try to start anyway
            auditor.logAndAudit(BootMessages.UPGRADE_TASK_NONFATAL,
                                new String[] { ExceptionUtils.getMessage(enonfatal[0]) },
                                enonfatal[0]);
            return;
        }

        // Handle upgrade-failed-and-we-are-screwed result
        if (efatal[0] != null) {
            // The upgrade failed and was rolled back, and Gateway startup should be aborted
            auditor.logAndAudit(BootMessages.UPGRADE_TASK_FATAL,
                                new String[] { ExceptionUtils.getMessage(efatal[0]) },
                                efatal[0]);
            throw new FatalUpgradeException(efatal[0]);
        }

        // The upgrade task worked fine
    }

    private UpgradeTask instantiate(String className)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException, ClassCastException
    {
        Class c = Class.forName(className);
        return (UpgradeTask)c.newInstance();
    }

    private void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof SystemEvent) {
            SystemEvent event = (SystemEvent) applicationEvent;

            Set<String> possibleTasks = whitelist.get(event.getClass());
            if (possibleTasks == null || possibleTasks.isEmpty()) return;

            for (ClusterProperty prop : taskProps) {
                if (possibleTasks.contains(prop.getValue())) {
                    try {
                        doUpgradeTask(prop);
                    } catch (FatalUpgradeException e) {
                        throw new IllegalStateException("Gateway sanity check failed: " + ExceptionUtils.getMessage(e), e);
                    }
                }
            }
        }
    }
}
