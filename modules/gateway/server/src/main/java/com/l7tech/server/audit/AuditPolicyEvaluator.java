package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes the audit sink policy.
 */
public class AuditPolicyEvaluator {
    private static final Logger logger = Logger.getLogger(AuditPolicyEvaluator.class.getName());

    private final ServerConfig serverConfig;
    private final PolicyCache policyCache;

    public AuditPolicyEvaluator(ServerConfig serverConfig, PolicyCache policyCache) {
        this.serverConfig = serverConfig;
        this.policyCache = policyCache;
    }

    private String loadAuditSinkPolicyGuid() {
        return serverConfig.getPropertyCached(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID);
    }

    public AssertionStatus outputRecordToPolicyAuditSink(final AuditRecord auditRecord, final boolean wantsUpdate) {
        PolicyEnforcementContext context = null;
        ServerPolicyHandle sph = null;
        try {
            final String guid = loadAuditSinkPolicyGuid();
            if (guid == null || guid.trim().length() < 1) {
                logger.log(Level.FINEST, "No audit sink policy is configured");
                return null;
            }

            context = new AuditSinkPolicyEnforcementContext(auditRecord);
            context.setAuditContext(new NullAuditContext());
            context.setAuditLevel(Level.INFO);

            // Use fake service
            final PublishedService svc = new PublishedService();
            svc.setName("[Internal audit sink policy pseudo-service]");
            svc.setSoap(false);
            context.setService(svc);

            sph = policyCache.getServerPolicy(guid);
            if (sph == null) {
                logger.log(Level.WARNING, "Unable to access configured audit sink policy -- no policy with GUID {0} is present in policy cache (invalid policy?)", guid);
                return AssertionStatus.SERVER_ERROR;
            }

            AssertionStatus status = sph.checkRequest(context);

            // We won't bother processing any deferred assertions because they mostly deal with response processing
            // and we intend to ignore any response from this policy.

            if (!AssertionStatus.NONE.equals(status)) {
                logger.log(Level.WARNING, "Audit sink policy completed with assertion status of " + status);
            }

            return status;

        } catch (PolicyAssertionException e) {
            logger.log(Level.WARNING, "Failed to execute audit sink policy: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to execute audit sink policy: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to execute audit sink policy: " + ExceptionUtils.getMessage(e), e);
            return AssertionStatus.SERVER_ERROR;
        } finally {
            ResourceUtils.closeQuietly(sph);
            ResourceUtils.closeQuietly(context);
        }
    }

}
