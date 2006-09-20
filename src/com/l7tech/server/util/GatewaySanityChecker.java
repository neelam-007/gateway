/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util;

import com.l7tech.cluster.ClusterProperty;
import com.l7tech.cluster.ClusterPropertyManager;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.BootMessages;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.upgrade.UpgradeTask;
import com.l7tech.server.upgrade.NonfatalUpgradeException;
import com.l7tech.server.upgrade.FatalUpgradeException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.hibernate.TransactionException;

import java.util.*;
import java.util.logging.Logger;

/**
 * Bean that checks sanity of the database/cluster/local configuration before allowing bootup to proceed.
 * <p/>
 * Currently this just checks for flagged database upgrade tasks in the cluster properties table.
 */
public class GatewaySanityChecker extends ApplicationObjectSupport implements InitializingBean, ApplicationListener {
    private static final Logger logger = Logger.getLogger(GatewaySanityChecker.class.getName());
    private static final String CPROP_PREFIX_UPGRADE = "upgrade.task.";

    /** Currently, the whitelist of allowed upgrade task values is very simple -- there's only one! */
    private static final String VALUE_35_36_ADD_ROLES = "com.l7tech.server.upgrade.Upgrade35To36AddRoles";

    private final ClusterPropertyManager clusterPropertyManager;
    private final PlatformTransactionManager transactionManager; // required for TransactionTemplate
    private Auditor auditor;

    public GatewaySanityChecker(PlatformTransactionManager transactionManager,
                                ClusterPropertyManager clusterPropertyManager)
    {
        this.transactionManager = transactionManager;
        this.clusterPropertyManager = clusterPropertyManager;
        if (clusterPropertyManager == null) throw new NullPointerException("Cluster Property Manager is required");
    }

    public void afterPropertiesSet() throws Exception {
        this.auditor = new Auditor(this, getApplicationContext(), logger);
    }



    private void doSanityCheck() throws FatalUpgradeException {
        // Check for upgrade tasks flagged in the cluster properties table
        final Collection<ClusterProperty> props;
        try {
            props = clusterPropertyManager.findAll();
        } catch (FindException e) {
            throw new RuntimeException(e);
        }

        Map<Long, ClusterProperty> tasks = new HashMap<Long, ClusterProperty>();
        List<Long> taskOrder = new ArrayList<Long>();

        for (ClusterProperty prop : props) {
            String name = prop.getName();
            if (name.startsWith(CPROP_PREFIX_UPGRADE) && name.length() > CPROP_PREFIX_UPGRADE.length()) {
                String value = prop.getValue();

                if (!isUpgradeTaskRecognized(value)) {
                    auditor.logAndAudit(BootMessages.UPGRADE_TASK_IGNORED, new String[] {"unrecognized upgrade task value: " + name + "=" + value});
                    continue;
                }

                final long ordinal;
                try {
                    ordinal = Long.parseLong(name.substring(CPROP_PREFIX_UPGRADE.length()));
                    tasks.put(ordinal, prop);
                    taskOrder.add(ordinal);
                } catch (NumberFormatException nfe) {
                    auditor.logAndAudit(BootMessages.UPGRADE_TASK_IGNORED, new String[]{"invalid ordinal in upgrade.task cluster property: " + name + ": " + ExceptionUtils.getMessage(nfe)}, nfe);
                }
            }
        }

        if (!taskOrder.isEmpty()) {
            Collections.sort(taskOrder);
            List<ClusterProperty> taskProps = new ArrayList<ClusterProperty>();
            for (Long ordinal : taskOrder)
                taskProps.add(tasks.get(ordinal));
            assert !taskProps.isEmpty();
            doUpgradeTasks(taskProps);
        }
    }

    /** @return true if the specified upgrade task is on the whitelist of allowed upgrade tasks. */
    private boolean isUpgradeTaskRecognized(String value) {
        return value.equals(VALUE_35_36_ADD_ROLES);
    }

    /**
     * Execute the specified upgrade tasks, each in its own transaction.
     * Caller is responsible for ensuring that the upgrade task
     * classnames have already been whitelisted, and sorting the list.
     *
     * @param taskProps  cluster properties containing upgrade tasks to run, in the order to run them.
     */
    private void doUpgradeTasks(List<ClusterProperty> taskProps) throws FatalUpgradeException {
        for (ClusterProperty prop : taskProps) {
            doUpgradeTask(prop);
        }
    }

    private void doUpgradeTask(final ClusterProperty prop) throws FatalUpgradeException {
        final String propName = prop.getName();
        final String className = prop.getValue();
        logger.info("Running upgrade task: " + propName + "=" + className);

        // Instantiate the upgrade task
        final UpgradeTask task;
        try {
            task = instantiate(className);
        } catch (Exception e) {
            auditor.logAndAudit(BootMessages.UPGRADE_TASK_IGNORED,
                                new String[] {"Unable to instantiate upgrade task: " +
                                        propName + "=" + className + ": " + ExceptionUtils.getMessage(e)},
                                e);
            return;
        }

        // Remove the cluster property and do the work both in the same transaction
        final NonfatalUpgradeException[] enonfatal = new NonfatalUpgradeException[] { null };
        final FatalUpgradeException[] efatal = new FatalUpgradeException[] { null };

        try {
            new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    try {
                        // Remove the cluster property so noone else repeats the work
                        clusterPropertyManager.delete(prop);

                        // Do the actual work
                        task.upgrade(getApplicationContext());

                    } catch (NonfatalUpgradeException e) {
                        enonfatal[0] = e;
                        transactionStatus.setRollbackOnly();
                    } catch (FatalUpgradeException e) {
                        efatal[0] = e;
                        transactionStatus.setRollbackOnly();
                    } catch (DeleteException e) {
                        // Something has gone unexpectedly very wrong
                        efatal[0] = new FatalUpgradeException("Unable to delete upgrade task property: " + prop.getName() + ": " +
                                ExceptionUtils.getMessage(e), e);
                        transactionStatus.setRollbackOnly();
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

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof Started) {
            try {
                doSanityCheck();
            } catch (FatalUpgradeException e) {
                throw new IllegalStateException("Gateway sanity check failed: " + ExceptionUtils.getMessage(e), e);
            }
        }
    }
}
