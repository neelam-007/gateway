package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.polback.BackgroundTask;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.InvokePolicyAsyncAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
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
    private final ApplicationContext applicationContext;

    public ServerInvokePolicyAsyncAssertion(final InvokePolicyAsyncAssertion assertion, ApplicationContext applicationContext) {
        super(assertion);
        workQueueExecutorManager = applicationContext.getBean("workQueueExecutorManager", WorkQueueExecutorManager.class);
        this.applicationContext = applicationContext;
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException {
        final ThreadPoolExecutor workQueueExecutor = this.workQueueExecutorManager.getWorkQueueExecutor(assertion.getWorkQueueName());
        if (workQueueExecutor != null) {
            // Send to queue
            workQueueExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    runBackgroundTask(assertion.getPolicyGoid(), assertion.getPolicyName());
                }
            });
        } else {
            logAndAudit(AssertionMessages.WORK_QUEUE_EXECUTOR_NOT_AVAIL, "Work queue executor does not exist.");
        }
        return AssertionStatus.NONE;
    }

    private void runBackgroundTask(Goid policyGoid, final String policyName) {
        final Method runMethod;
        Class[] args = {String.class};
        try {
            runMethod = BackgroundTask.class.getMethod("run", args);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        final BackgroundTask task = pbsReg.getImplementationProxyForSingleMethod(runMethod, policyGoid);
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
                    task.run("");
                    // Policy executed successfully and evaluated to AssertionStatus.NONE

                } catch (RuntimeException e) {
                    new Auditor(this, applicationContext, logger).logAndAudit(SystemMessages.BACKGROUND_TASK_RUNTIME_ERROR,
                            new String[]{policyName, ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
                }
            }
        });
    }
}
