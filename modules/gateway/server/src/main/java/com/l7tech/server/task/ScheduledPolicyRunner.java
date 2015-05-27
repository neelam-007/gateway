package com.l7tech.server.task;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.polback.BackgroundTask;
import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.polback.PolicyBackedServiceRegistry;
import com.l7tech.server.policy.assertion.AssertionStatusException;
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
    private Auditor auditor;

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

    public void runBackgroundTask(Goid policyGoid, Goid providerId, String userId) {

        // Get an implementation proxy for the "run" method of BackgroundTask that invokes the configured Policy
        final Method runMethod;
        try {
            runMethod = BackgroundTask.class.getMethod("run");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        AuthenticationResult authenticationResult = null;
        if (providerId != null && userId != null) {
            try {
                IdentityProvider idProvider = jobManager.identityProviderFactory.getProvider(providerId);
                if (idProvider == null) {
                    getAuditor().logAndAudit(SystemMessages.SCHEDULER_POLICY_ERROR, new String[]{"Identity provider not found"});
                    return;
                }
                User user = idProvider.getUserManager().findByPrimaryKey(userId);
                authenticationResult = new AuthenticationResult(user, new OpaqueSecurityToken(user.getLogin(), null));
            } catch (FindException e) {
                getAuditor().logAndAudit(SystemMessages.SCHEDULER_POLICY_ERROR, new String[]{"Identity provider not found"});
            }

        }
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.getDefaultAuthenticationContext().addAuthenticationResult(authenticationResult);
        final BackgroundTask task = jobManager.pbsReg.getImplementationProxyForSingleMethod(runMethod, policyGoid, context);

        // Create an audit record in which to accumulate any audit details that appear.
        AuditRecord auditRecord = new SystemAuditRecord(Level.INFO,
                jobManager.nodeId,
                Component.GW_SCHEDULED_TASK,
                "Executing background policy",
                false,
                null,
                null,
                null,
                "run",
                jobManager.clusterInfoManager.getSelfNodeInf().getAddress());

        jobManager.auditContextFactory.doWithNewAuditContext(auditRecord, new Runnable() {
            @Override
            public void run() {
                try {
                    // Invoke the actual policy
                    task.run();
                    // Policy executed successfully and evaluated to AssertionStatus.NONE
                } catch (RuntimeException e) {
                    // not audit if Policy produced non-successful assertion status
                    if (e instanceof AssertionStatusException) {
                        getAuditor().logAndAudit(SystemMessages.SCHEDULER_POLICY_ERROR,
                                new String[]{((AssertionStatusException) e).getAssertionStatus().getMessage()}, ExceptionUtils.getDebugException(e));
                    } else {
                        getAuditor().logAndAudit(SystemMessages.SCHEDULER_POLICY_ERROR,
                                new String[]{e.getMessage()}, ExceptionUtils.getDebugException(e));
                    }
                }

            }
        });
    }

    private Auditor getAuditor() {
        if (auditor == null) {
            auditor = new Auditor(this, jobManager.applicationContext, logger);
        }
        return auditor;
    }
}
