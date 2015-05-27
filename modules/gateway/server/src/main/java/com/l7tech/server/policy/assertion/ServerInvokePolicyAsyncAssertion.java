package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.polback.BackgroundTask;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.InvokePolicyAsyncAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.polback.PolicyBackedServiceRegistry;
import com.l7tech.server.workqueue.WorkQueueExecutorManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InetAddressUtil;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.reflect.Method;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerInvokePolicyAsyncAssertion extends AbstractServerAssertion<InvokePolicyAsyncAssertion> {
    private static final Logger logger = Logger.getLogger(ServerInvokePolicyAsyncAssertion.class.getName());

    @Inject
    @Named("policyBackedServiceRegistry")
    private PolicyBackedServiceRegistry pbsReg;

    @Inject
    private AuditContextFactory auditContextFactory;

    @Inject
    @Named("clusterNodeId")
    private String nodeId;

    private final WorkQueueExecutorManager workQueueExecutorManager;
    private final EntityFinder entityFinder;
    private final ApplicationContext applicationContext;

    public ServerInvokePolicyAsyncAssertion(final InvokePolicyAsyncAssertion assertion, ApplicationContext applicationContext) {
        super(assertion);
        workQueueExecutorManager = applicationContext.getBean("workQueueExecutorManager", WorkQueueExecutorManager.class);
        entityFinder = applicationContext.getBean("entityFinder", EntityFinder.class);
        this.applicationContext = applicationContext;
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException {
        try {
            Entity entity = entityFinder.find(new EntityHeader((assertion).getPolicyGoid(), EntityType.POLICY, null, null));
            if (entity == null) {
                logAndAudit(AssertionMessages.INVOKE_POLICY_ASYNC_ASSERTION_FAILED, "Policy fragment \"" + assertion.getPolicyName() + "\" does not exist.");
                return AssertionStatus.FALSIFIED;
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Error looking for policy fragment: " + ExceptionUtils.getMessage(e), e);
        }

        final ThreadPoolExecutor workQueueExecutor = this.workQueueExecutorManager.getWorkQueueExecutor(assertion.getWorkQueueGoid());
        if (workQueueExecutor != null) {
            // Send to queue
            try {
                workQueueExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        logAndAudit(AssertionMessages.WORK_QUEUE_EXECUTOR_INFO_FINER, assertion.getWorkQueueName(),
                                String.valueOf(workQueueExecutor.getQueue().size()),
                                String.valueOf(workQueueExecutor.getActiveCount()),
                                String.valueOf(workQueueExecutor.getPoolSize()),
                                workQueueExecutor.getRejectedExecutionHandler().getClass().getName());
                        runBackgroundTask(assertion.getPolicyGoid(), assertion.getPolicyName());
                    }
                });
            } catch (RejectedExecutionException ree) {
                logAndAudit(AssertionMessages.INVOKE_POLICY_ASYNC_ASSERTION_FAILED, "Work Queue \"" + assertion.getWorkQueueName() +
                        "\" is full. Rejected request ID: " + context.getRequestId() + ".");
            }
        } else {
            logAndAudit(AssertionMessages.WORK_QUEUE_EXECUTOR_NOT_AVAIL, assertion.getWorkQueueName(), "Work queue executor does not exist.");
            return AssertionStatus.FALSIFIED;
        }
        return AssertionStatus.NONE;
    }

    private void runBackgroundTask(Goid policyGoid, final String policyName) {
        final Method runMethod;
        try {
            runMethod = BackgroundTask.class.getMethod("run");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        final BackgroundTask task = pbsReg.getImplementationProxyForSingleMethod(runMethod, policyGoid, null);
        AuditRecord auditRecord = new SystemAuditRecord(Level.FINE,
                nodeId,
                Component.GW_WORK_QUEUE,
                "Executing background policy",
                false,
                null,
                null,
                null,
                "run",
                InetAddressUtil.getLocalHost().getHostAddress());

        auditContextFactory.doWithNewAuditContext(auditRecord, new Runnable() {
            @Override
            public void run() {
                try {

                    // Invoke the actual policy
                    task.run();
                    // Policy executed successfully and evaluated to AssertionStatus.NONE

                } catch (RuntimeException e) {
                    new Auditor(this, applicationContext, logger).logAndAudit(SystemMessages.BACKGROUND_TASK_RUNTIME_ERROR,
                            new String[]{policyName, ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
                }
            }
        });
    }
}
