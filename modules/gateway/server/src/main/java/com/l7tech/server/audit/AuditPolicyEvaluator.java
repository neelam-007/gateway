package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.PolicyCacheEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes the audit sink policy.
 * <p/>
 * No audit sink policies will be invoked until a PolicyCacheEvent.Started event is received, indicating
 * that the policy cache is ready to use.  Before then, system records will be buffered for later processing
 * when the policy cache is opened.  Non-system records will trigger an exception since it is not expected
 * that admin or message audit records can be generated before the policy cache has started.
 */
public class AuditPolicyEvaluator implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(AuditPolicyEvaluator.class.getName());

    private final ServerConfig serverConfig;
    private final PolicyCache policyCache;

    private final AtomicBoolean sinkOpen = new AtomicBoolean(false);
    private final Queue<SystemAuditRecord> startupRecords = new ConcurrentLinkedQueue<SystemAuditRecord>();

    public AuditPolicyEvaluator(ServerConfig serverConfig, PolicyCache policyCache) {
        this.serverConfig = serverConfig;
        this.policyCache = policyCache;
    }

    private String loadAuditSinkPolicyGuid() {
        return serverConfig.getPropertyCached(ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID);
    }

    /**
     * Run the current audit sink policy to export the specified audit record; and, for Message Summary audit records only,
     * making available the request and response from the specified original PolicyEnforcementContext (if available).
     *
     * @param auditRecord  the audit record to give to the sink policy to export.  Required.
     * @param originalContext  the auditRecord is a message summary audit record, the not-yet-closed PolicyEnforcementContext from message processing.  May be null.
     * @return
     */
    public AssertionStatus outputRecordToPolicyAuditSink(final AuditRecord auditRecord, PolicyEnforcementContext originalContext) {
        PolicyEnforcementContext context = null;
        ServerPolicyHandle sph = null;
        try {
            final String guid = loadAuditSinkPolicyGuid();
            if (guid == null || guid.trim().length() < 1) {
                logger.log(Level.FINEST, "No audit sink policy is configured");
                return null;
            }

            if (!sinkOpen.get()) {
                if (auditRecord instanceof SystemAuditRecord) {
                    SystemAuditRecord sysrec = (SystemAuditRecord) auditRecord;
                    startupRecords.add(sysrec);
                    return AssertionStatus.NONE;
                } else {
                    throw new IllegalStateException("A non-System audit record was generated before BootProcess was started: " + auditRecord.getClass().getName() + ": " + auditRecord.getMessage());
                }
            }

            context = new AuditSinkPolicyEnforcementContext(
                    auditRecord,
                    PolicyEnforcementContextFactory.createPolicyEnforcementContext( null, null ),
                    originalContext);
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

    private void processStartupRecords() {
        if (startupRecords.isEmpty())
            return;

        logger.info("Sending startup system audit records to audit sink policy");
        while (!startupRecords.isEmpty()) {
            SystemAuditRecord startupRecord = startupRecords.remove();
            try {
                if (logger.isLoggable(Level.FINE))
                    logger.fine("Saving startup system event to audit sink policy: " + startupRecord);
                AssertionStatus result = outputRecordToPolicyAuditSink(startupRecord, null);
                if (result != null && !AssertionStatus.NONE.equals(result)) {
                    logger.log(Level.SEVERE, "Audit sink policy returned status " + result.getNumeric() + " while saving startup system event to audit sink policy: " + startupRecord);
                }
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Unable to save startup system event to audit sink policy: " + startupRecord + ": " + ExceptionUtils.getMessage(t), t);
            }
        }
    }

    private void startAuditSink() {
        if (!sinkOpen.getAndSet(true)) {
            if (!policyCache.isStarted())
                throw new IllegalStateException("AuditPolicyEvaluator was started before the PolicyCache");
            processStartupRecords();
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof PolicyCacheEvent.Started) {
            startAuditSink();
        }
    }
}
