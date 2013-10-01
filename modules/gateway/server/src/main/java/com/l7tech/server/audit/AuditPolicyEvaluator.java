package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.PolicyCacheEvent;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import org.springframework.context.ApplicationEvent;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Callable;
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
public class AuditPolicyEvaluator implements PostStartupApplicationListener {
    private static final Logger logger = Logger.getLogger(AuditPolicyEvaluator.class.getName());

    private final Config config;
    private final PolicyCache policyCache;
    private final JdbcConnectionPoolManager jdbcConnectionPoolManager;

    private final AtomicBoolean sinkOpen = new AtomicBoolean(false);
    private final Queue<SystemAuditRecord> startupRecords = new ConcurrentLinkedQueue<SystemAuditRecord>();

    public AuditPolicyEvaluator(Config config, PolicyCache policyCache,PolicyManager policyManger, JdbcConnectionPoolManager jdbcConnectionPoolManager) {
        this.config = config;
        this.policyCache = policyCache;
        this.jdbcConnectionPoolManager = jdbcConnectionPoolManager;
    }

    private String loadAuditSinkPolicyGuid() {
        return config.getProperty( ServerConfigParams.PARAM_AUDIT_SINK_POLICY_GUID );
    }

    /**
     * Run the current audit sink policy to export the specified audit record; and, for Message Summary audit records only,
     * making available the request and response from the specified original PolicyEnforcementContext (if available).
     *
     * @param auditRecord  the audit record to give to the sink policy to export.  Required.
     * @param originalContext  the auditRecord is a message summary audit record, the not-yet-closed PolicyEnforcementContext from message processing.  May be null.
     * @return the assertion status from running the policy, or SERVER_ERROR if there was an error, or NONE if a system record was buffered for later off-boxing once the subsystem initializes.
     */
    public AssertionStatus outputRecordToPolicyAuditSink(final AuditRecord auditRecord, final PolicyEnforcementContext originalContext) {
        try {
            // Make sure a logging-only audit context is active while running the audit sink policy, so we don't
            // try to add details to the record that is currently being flushed via this very policy
            return AuditContextFactory.doWithCustomAuditContext(AuditContextFactory.createLogOnlyAuditContext(), new Callable<AssertionStatus>() {
                @Override
                public AssertionStatus call() throws Exception {
                    return doOutputRecordToPolicyAuditSink(auditRecord, originalContext);
                }
            });
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to execute audit sink policy: " + ExceptionUtils.getMessage(e), e);
            return AssertionStatus.SERVER_ERROR;
        }
    }


    private AssertionStatus doOutputRecordToPolicyAuditSink(final AuditRecord auditRecord, PolicyEnforcementContext originalContext) {
        PolicyEnforcementContext context = null;
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

            AssertionStatus status =  executePolicy(guid, context);

            // We won't bother processing any deferred assertions because they mostly deal with response processing
            // and we intend to ignore any response from this policy.

            if (!AssertionStatus.NONE.equals(status)) {
                logger.log(Level.WARNING, "Audit sink policy completed with assertion status of " + status);
            }

            return status;

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to execute audit sink policy: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } finally{
            ResourceUtils.closeQuietly(context);
        }
    }
    private AssertionStatus executePolicy( String guid, final PolicyEnforcementContext context) throws Exception {

        final ServerPolicyHandle sph = policyCache.getServerPolicy(guid);
        if (sph == null) {
            logger.log(Level.WARNING, "Unable to access configured audit sink policy -- no policy with GUID {0} is present in policy cache (invalid policy?)", guid);
            return AssertionStatus.SERVER_ERROR;
        }

        DataSource ds = null ;
        String connectionName = "";
        try {
            Assertion root = sph.getPolicyAssertion();
            connectionName = root != null ? getConnectionNames(root) : null;
            if( connectionName != null )
            {
                ds = jdbcConnectionPoolManager.getDataSource(connectionName);
            }

        } catch (NamingException e) {
            logger.warning("Unable to get jdbc connection datasource: "+connectionName);
        } catch (IOException e) {
            logger.warning("Unable to retrieve audit sink policy: "+ e.getMessage());
        }

        try{
            if(ds!=null){

                TransactionTemplate transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(ds));
                return transactionTemplate.execute(new TransactionCallback<AssertionStatus>() {
                    @Override
                    public AssertionStatus doInTransaction(TransactionStatus transactionStatus) {
                        try {
                            AssertionStatus status = sph.checkRequest(context);
                            if(status != AssertionStatus.NONE)
                                transactionStatus.setRollbackOnly();
                            return status;
                        } catch (PolicyAssertionException e) {
                            transactionStatus.setRollbackOnly();
                        } catch (IOException e) {
                            transactionStatus.setRollbackOnly();
                        }
                        return AssertionStatus.FAILED;
                    }
                });
            }
            return sph.checkRequest(context);
        }finally{
            ResourceUtils.closeQuietly(sph);
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

    private String getConnectionNames(Assertion root){
        Iterator<Assertion> it = root.preorderIterator();
        while (it.hasNext()) {
            Assertion kid = it.next();
            if (kid == null || !kid.isEnabled() || root.equals(kid))
                continue;

            if(kid instanceof JdbcConnectionable){
                return ((JdbcConnectionable) kid).getConnectionName();
            }

            if(kid instanceof CompositeAssertion){
                String name = getConnectionNames(kid);
                if(name!=null)
                    return name;
            }
        }
        return null;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof PolicyCacheEvent.Started) {
            startAuditSink();
        }
    }
}
