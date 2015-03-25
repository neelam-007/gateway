package com.l7tech.server.task;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.polback.BackgroundTask;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.polback.PolicyBackedServiceRegistry;
import com.l7tech.util.ExceptionUtils;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by luiwy01 on 3/12/2015.
 */
public class ScheduledPolicyRunner {
    private static Logger logger = Logger.getLogger(ScheduledPolicyRunner.class.getName());

    @Inject
    protected PolicyBackedServiceRegistry pbsReg;

    private static ScheduledPolicyRunner instance;



    private ScheduledTaskJobManager jobManager;
    public static void setInstance(ScheduledPolicyRunner serviceRunner) {
        instance = serviceRunner;
    }

    public static ScheduledPolicyRunner getInstance(ScheduledTaskJobManager jobManager) {
        if (instance == null) {
            instance = new ScheduledPolicyRunner(jobManager);
        }
        return instance;
    }

    private ScheduledPolicyRunner(ScheduledTaskJobManager jobManager) {
        this.jobManager = jobManager;

    }

    public boolean isClusterMaster() {
        return jobManager.clusterMaster.isMaster();
    }

    public void runBackgroundTask(Goid policyGoid) {

        // Get an implementation proxy for the "run" method of BackgroundTask that invokes the configured Policy
        final Method runMethod;
        Class[] args = {String.class};

        try {
            runMethod = BackgroundTask.class.getMethod("run", args);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        final BackgroundTask task = jobManager.pbsReg.getImplementationProxyForSingleMethod(runMethod, policyGoid);

        // Create an audit record in which to accumulate any audit details that appear.
        // INFO level is OK for scheduled task probably.
        // Work queue might want to default to FINE level, or maybe inherit level from calling policy's audit context
        AuditRecord auditRecord = new SystemAuditRecord(Level.INFO,
                jobManager.nodeId,
                Component.GW_SCHEDULED_TASK,
                "Executing background policy",
                false,
                null,
                null,
                null,
                "run",
                jobManager.clusterInfoManager.getSelfNodeInf().getAddress()); // todo util?

        jobManager.auditContextFactory.doWithNewAuditContext(auditRecord, new Runnable() {
            @Override
            public void run() {
                try {
                    logger.info("Scheduled policy executing");
                    // Invoke the actual policy
                    task.run(null);
                    // Policy executed successfully and evaluated to AssertionStatus.NONE
                    logger.info("Scheduled policy executed");

                } catch (RuntimeException e) {
                    // todo handle error and audit
                    // Policy produced non-successful assertion status, or failed or wasn't found
                    // Handle error

                    // Can add audit details to audit context if you want -- will be added to open audit record
                    // Here's an example of how you might add an audit detail to the current audit context
                    new Auditor(this, jobManager.applicationContext, logger).logAndAudit(SystemMessages.SCHEDULER_POLICY_ERROR,
                            new String[]{"badness!"}, ExceptionUtils.getDebugException(e));
                }

            }
        });
    }
}
