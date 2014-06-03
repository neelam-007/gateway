package com.l7tech.server.audit;

import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.gateway.common.audit.AuditDetailEvent.AuditDetailWithInfo;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.Nullary;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.Pair;

import java.security.PrivateKey;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds the transient state of the audit system for the current operation on the current thread.
 *
 * Must be thread-local; not thread-safe in the slightest!
 * <p>
 * Call {@link #setCurrentRecord(com.l7tech.gateway.common.audit.AuditRecord)} to attach a single {@link AuditRecord} describing the SSG's current
 * operation to the context, and {@link AuditContext#addDetail(com.l7tech.gateway.common.audit.AuditDetail, Object)} to add zero or more {@link AuditDetail} records.
 * <p>
 * {@link MessageSummaryAuditRecord}s, {@link AdminAuditRecord}s and {@link AuditDetail}s that are
 * added to the context will only be persisted to the database later, when {@link #flush} is called,
 * if their level meets or exceeds the corresponding threshold.  {@link SystemAuditRecord}s have no
 * minimum threshold; they are always persisted.
 * <p/>
 * This audit context implementation may not be reused once it has been flushed.  A new instance must be
 * created for each operation.
 *
 * @see ServerConfig#getProperty(String)
 * @see ServerConfigParams#PARAM_AUDIT_ADMIN_THRESHOLD
 * @see ServerConfigParams#PARAM_AUDIT_MESSAGE_THRESHOLD
 * @see MessageSummaryAuditRecord
 * @see AdminAuditRecord
 * @see SystemAuditRecord
 */
public class AuditContextImpl implements AuditContext {

    /**
     * @param config   required
     * @param auditRecordManager   required
     * @param auditPolicyEvaluator  may be null
     * @param auditFilterPolicyManager may be null
     * @param nodeId should not be null
     */
    AuditContextImpl( final Config config,
                             final AuditRecordManager auditRecordManager,
                             final AuditPolicyEvaluator auditPolicyEvaluator,
                             final AuditFilterPolicyManager auditFilterPolicyManager,
                             final String nodeId,
                             final DefaultKey defaultKey,
                             final AuditLogListener auditLogListener) {
        if ( config == null) {
            throw new IllegalArgumentException("Server Config is required");
        }
        if (auditRecordManager == null) {
            throw new IllegalArgumentException("Audit Record Manager is required");
        }
        this.config = config;
        this.auditRecordManager = auditRecordManager;
        this.auditPolicyEvaluator = auditPolicyEvaluator;
        this.auditFilterPolicyManager = auditFilterPolicyManager;
        this.nodeId = nodeId;
        this.keystore = defaultKey;
        this.listener = auditLogListener;
    }

    /**
     * Sets the current {@link com.l7tech.gateway.common.audit.AuditRecord} for this context.
     *
     * @param record the record to set
     */
    void setCurrentRecord(AuditRecord record) {
        checkFlushed("setCurrentRecord");
        if (record == null) {
            throw new NullPointerException( "no audit record provided" );
        }
        if (currentRecord != null) {
            throw new IllegalStateException("Only one audit record can be active at one time (existing is '"+currentRecord.getMessage()+"', new is '"+record.getMessage()+"')");
        }
        if (record.getLevel().intValue() > highestLevelYetSeen.intValue()) {
            highestLevelYetSeen = record.getLevel();
        }
        currentRecord = record;
    }

    @Override
    public void addDetail(AuditDetail detail, Object source) {
        addDetail( new AuditDetailWithInfo( source, detail, null, null ));
    }

    @Override
    public void addDetail( final AuditDetailWithInfo detailWithInfo ) {
        checkFlushed("addDetail");
        if (detailWithInfo == null) {
            throw new NullPointerException( "no detailWithInfo provided" );
        }
        final AuditDetail detail = detailWithInfo.getDetail();

        AuditDetailMessage message = MessagesUtil.getAuditDetailMessageById(detail.getMessageId());
        if(message == null) {
            throw new RuntimeException("Cannot find the message (id=" + detail.getMessageId() + ")" + " in the Message Map.");
        }
        Level severity = getUseAssociatedLogsThreshold() ? MessagesUtil.getAuditLevelByIdWithFilter(detail.getMessageId()) : message.getLevel();
        detail.setOrdinal(ordinal++);
        // set the ordinal (used to resolve the sequence as the time stamp in ms cannot resolve the order of the messages)
        getDetailList(detailWithInfo.getSource()).add(detailWithInfo);
        if(getUseAssociatedLogsThreshold() && severity !=null && severity.intValue() > highestLevelYetSeen.intValue()) {
            highestLevelYetSeen = severity;
        }
    }

    /**
     * Sets whether the current record is an update to a previous audit record.
     *
     * @param update    true if updating; false if creating
     */
    public void setUpdate(boolean update) {
        checkFlushed("setUpdate");
        this.update = update;
    }

    private List<AuditDetailWithInfo> getDetailList(Object source) {
        List<AuditDetailWithInfo> details = this.details.get(source);
        if (details == null) {
            details = new LinkedList<AuditDetailWithInfo>();
            this.details.put(source, details);
        }
        return details;
    }

    /**
     * Get the currently acumulated hints.
     *
     * @return the Set of AuditDetailMessage.Hint's
     */
    @Override
    public Set getHints() {
        Set<AuditDetailMessage.Hint> hints = new HashSet<AuditDetailMessage.Hint>();
        for (List<AuditDetailWithInfo> list : details.values()) {
            for (AuditDetailWithInfo detailWithInfo : list) {
                AuditDetailMessage message = MessagesUtil.getAuditDetailMessageById(detailWithInfo.getDetail().getMessageId());
                Set<AuditDetailMessage.Hint> dHints = message==null ? null : message.getHints();
                if (dHints != null) {
                    hints.addAll(dHints);
                }
            }
        }
        return Collections.unmodifiableSet(hints);
    }

    /**
     * Flushes the current {@link AuditRecord} and any associated
     * {@link AuditDetail} records to the database or audit sink policy.
     * <p>
     * The context can not be flushed again once this operation has completed and should not be used to accumulate
     * any new audit record or audit details.  The context contents can be inspected after it has been flushed, however.
     * </p>
     */
    void flush() {
        if (flushed) {
            throw new IllegalStateException("flush() called on already-flushed AuditContextImpl");
        }
        flushed = true;

        if (currentRecord == null) {
            if (!details.isEmpty()) {
                logger.warning("flush() called with AuditDetails but no AuditRecord");
            }
            return;
        }

        PolicyEnforcementContext policyEnforcementContext = null;
        try {
            if (currentRecord instanceof MessageSummaryAuditRecord) {
                if (highestLevelYetSeen.intValue() < getSystemMessageThreshold().intValue()) {
                    if(logger.isLoggable(Level.FINE)) {
                        logger.fine("MessageSummaryAuditRecord generated with level " +
                                    currentRecord.getLevel() +
                                    " will not be saved; current message audit threshold is " +
                                    getSystemMessageThreshold().getName() );
                    }
                    return;
                }
                policyEnforcementContext = (PolicyEnforcementContext)((MessageSummaryAuditRecord)currentRecord).originalPolicyEnforcementContext();
            } else if (currentRecord instanceof AdminAuditRecord) {
                if (currentRecord.getLevel().intValue() < getSystemAdminThreshold().intValue()) {
                    if(logger.isLoggable(Level.FINE)) {
                        logger.fine("AdminAuditRecord generated with level " + currentRecord.getLevel()
                                    + " will not be saved; current admin audit threshold is " +
                                    getSystemAdminThreshold().getName() );
                    }
                    return;
                }
            } else if (currentRecord instanceof SystemAuditRecord) {
                SystemAuditRecord systemRecord = (SystemAuditRecord)currentRecord;
                Component component = Component.fromId(systemRecord.getComponentId());
                if (component!=null && component.isClientComponent()) {
                    if (highestLevelYetSeen.intValue() < getSystemSystemClientThreshold().intValue()) {
                        if(logger.isLoggable(Level.FINE)) {
                            logger.fine("SystemAuditRecord for client component generated with level " +
                                    currentRecord.getLevel() +
                                    " will not be saved; current system client threshold is " +
                                    getSystemSystemClientThreshold().getName() );
                        }
                        return;
                    }
                } else if(!systemRecord.alwaysAudit()) {
                    if (highestLevelYetSeen.intValue() < Level.WARNING.intValue()) {
                        if(logger.isLoggable(Level.FINEST)) {
                            logger.finest("SystemAuditRecord for optional audit generated with level " +
                                    currentRecord.getLevel() +
                                    " will not be saved; threshold for optional system audits is WARNING");
                        }
                        return;
                    }

                } else {
                    // Non client system audit records are always saved
                }
            }

            /*
             * 5.0 Audit Log Formatter will be passed into the listener
             */
            final AuditLogFormatter formatter = new AuditLogFormatter(logFormatContextVariables);

            currentRecord.setLevel(highestLevelYetSeen);
            listener.notifyRecordFlushed(currentRecord, formatter, true);

            Set<AuditDetail> sortedDetailsToSave = new TreeSet<AuditDetail>(new Comparator<AuditDetail>() {
                @Override
                public int compare(AuditDetail o1, AuditDetail o2) {
                    return new Integer(o1.getOrdinal()).compareTo( o2.getOrdinal() );
                }
            });
            for (List<AuditDetailWithInfo> list : details.values()) {
                for (int i = list.size()-1 ; i>=0 ; i-- ){
                    final AuditDetailWithInfo detailWithInfo = list.get(i);
                    int mid = detailWithInfo.getDetail().getMessageId();

                    final Pair<Boolean,AuditDetailMessage> pair = MessagesUtil.getAuditDetailMessageByIdWithFilter(mid);
                    if(!pair.left){
                        throw new RuntimeException("Cannot find the message (id=" + mid + ")" + " in the Message Map.");
                    }

                    final AuditDetailMessage message = pair.right;
                    if(message == null){
                        //audit has been filtered to NEVER.
                        continue;
                    }

                    final Level severity = message.getLevel();
                    if(severity.intValue() >= getAssociatedLogsThreshold().intValue()) {
                        // Call even if not saving
                        detailWithInfo.getDetail().setAuditRecord(currentRecord);

                        if (detailWithInfo.getDetail() instanceof ExtendedAuditDetail) {
                            ExtendedAuditDetail extendedAuditDetail = (ExtendedAuditDetail) detailWithInfo.getDetail();
                            if (!extendedAuditDetail.shouldSave()) continue; // we don't want to save this.
                        }

                        sortedDetailsToSave.add(detailWithInfo.getDetail());

                        HybridDiagnosticContext.doWithContext( detailWithInfo.getContext(), new Nullary<Void>(){
                            @Override
                            public Void call() {
                                listener.notifyDetailFlushed(
                                        getSource(detailWithInfo.getSource(), "com.l7tech.server.audit"),
                                        detailWithInfo.getLoggerName(),
                                        message,
                                        detailWithInfo.getDetail().getParams(),
                                        formatter,
                                        detailWithInfo.getException());
                                return null;
                            }
                        } );
                    }
                }
            }

            int newOrdinal = 0;
            for (AuditDetail auditDetail : sortedDetailsToSave) {
                auditDetail.setOrdinal(newOrdinal++);
            }

            currentRecord.setDetails(sortedDetailsToSave);

            outputRecord(currentRecord, this.update, policyEnforcementContext, formatter);
        } catch (SaveException e) {
            logger.log(Level.SEVERE, "Couldn't save audit records", e);
        } catch (UpdateException e) {
            logger.log(Level.SEVERE, "Couldn't update audit records", e);
        }
    }

    private void outputRecord(final AuditRecord rec,
                              final boolean update,
                              final PolicyEnforcementContext policyEnforcementContext,
                              final AuditLogFormatter formatter)
            throws UpdateException, SaveException {
        // Note: any audit details caused by either the audit sink policy or the audit message filter policy will not
        // be included in the audit as they do not get added to the AuditRecord. Adding details to the AuditRecord should
        // be done prior to this method.

        boolean sinkPolicyFailed = false;
        AssertionStatus sinkPolicyStatus = null;

        if(auditFilterPolicyManager != null){
            auditFilterPolicyManager.filterAuditRecord(rec, policyEnforcementContext, listener, formatter);
        }

        //flush after the AMF policy has had a chance to audit.
        listener.notifyRecordFlushed(currentRecord, formatter, false);

        if (auditPolicyEvaluator != null) {
            // Don't bother running the sink policy for update events
            if (update)
                return;

            if (isSignAudits()) {
                signRecord(rec);
            }

            sinkPolicyStatus = auditPolicyEvaluator.outputRecordToPolicyAuditSink(rec, policyEnforcementContext);
            if (AssertionStatus.NONE.equals(sinkPolicyStatus)) {
                // Sink policy succeeded
                if (!isAlwaysSaveToDatabase())
                    return;
            } else if (null == sinkPolicyStatus) {
                // No sink policy configured; fallthrough to regular handling
            } else {
                // Audit sink policy failed; fall back to built-in handling and audit failure
                if (!isAlwaysSaveToDatabase() && !isFallbackToDatabaseIfSinkPolicyFails()) {

                    if (isPCIDSSEnabled()) {
                        // If audit sink policy fails and internal audit fall back is disabled, then log and audit a warning "audit sink policy failed and internal audit fall back is disabled."
                        logger.warning(AuditClusterPropertiesChecker.AUDIT_SINK_FALL_BACK_WARNING);

                        SystemAuditRecord auditFallbackDisabled = new SystemAuditRecord(
                            Level.WARNING,
                            nodeId,
                            Component.GW_AUDIT_SYSTEM,
                            AuditClusterPropertiesChecker.AUDIT_SINK_FALL_BACK_WARNING,
                            false,
                            null,
                            null,
                            null,
                            "Audit Sink Properties Evaluation",
                            OUR_IP
                        );

                        if (isSignAudits()) {
                            signRecord(auditFallbackDisabled);
                        }

                        auditRecordManager.save(auditFallbackDisabled);
                    }

                    return;
                }
                sinkPolicyFailed = true;
            }
        }

        //check audit message size
        if( rec instanceof MessageSummaryAuditRecord){
            final long messageLimitSize = config.getLongProperty( ServerConfigParams.PARAM_AUDIT_MESSAGE_LIMIT_SIZE, 10485760L);  // 10MB
            if( messageLimitSize > 0L ){
                MessageSummaryAuditRecord messageSummaryAuditRecord = (MessageSummaryAuditRecord)rec;
                if(messageSummaryAuditRecord.getRequestXml()!=null  &&
                        (long) messageSummaryAuditRecord.getRequestXml().length() > messageLimitSize){
                    messageSummaryAuditRecord.setRequestXml(MESSAGE_TOO_LARGE);
                }
                if(messageSummaryAuditRecord.getResponseXml()!=null &&
                        (long) messageSummaryAuditRecord.getResponseXml().length() > messageLimitSize){
                    messageSummaryAuditRecord.setResponseXml(MESSAGE_TOO_LARGE);
                }
            }
        }

        if (isSignAudits()) {
            signRecord(rec);
        } else {
            rec.setSignature(null);   // in case of updating, remove any old signature
        }

        if (update) {
            auditRecordManager.update(rec);
        } else {
            auditRecordManager.save(rec);
        }

        if (sinkPolicyFailed) {
            // Need to audit something about the failure, says func spec
            SystemAuditRecord fail = new SystemAuditRecord(Level.WARNING, nodeId, Component.GW_AUDIT_SYSTEM,
                    "Audit sink policy failed; status = " + sinkPolicyStatus.getNumeric(), false, null, null, null, "Sink Failure", OUR_IP);
            if(isSignAudits()){
                signRecord(fail);
            }
            auditRecordManager.save(fail);
        }
    }

    private boolean isPCIDSSEnabled() {
        return config.getBooleanProperty( ServerConfigParams.PARAM_PCIDSS_ENABLED, false );
    }

    private boolean isFallbackToDatabaseIfSinkPolicyFails() {
        return config.getBooleanProperty( ServerConfigParams.PARAM_AUDIT_SINK_FALLBACK_ON_FAIL, true );
    }

    private boolean isAlwaysSaveToDatabase() {
        return config.getBooleanProperty( ServerConfigParams.PARAM_AUDIT_SINK_ALWAYS_FALLBACK, false );
    }

    /**
     * @see com.l7tech.server.audit.AuditContext#setContextVariables(java.util.Map)
     */
    @Override
    public void setContextVariables(Map<String, Object> variables) {
        checkFlushed("setContextVariables");
        this.logFormatContextVariables = variables;
    }

    void signRecord(AuditRecord signatureSubject) {
        try {
            SignerInfo signerInfo = keystore.getAuditSigningInfo();
            if (signerInfo == null) signerInfo = keystore.getSslInfo();

            PrivateKey pk = signerInfo.getPrivate();
            new AuditRecordSigner(pk).signAuditRecord(signatureSubject);
        } catch (Exception e) {
            logger.log(Level.WARNING, "ERROR GENERATING AUDIT SIGNATURE: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public Map<Object, List<AuditDetail>> getDetails() {
        Map<Object,List<AuditDetail>> ads = new HashMap<Object,List<AuditDetail>>();

        for ( Map.Entry<Object,List<AuditDetailWithInfo>> entry : details.entrySet() ) {
            List<AuditDetail> ds = new ArrayList<AuditDetail>();
            for ( AuditDetailWithInfo detailWithInfo : entry.getValue() ) {
                ds.add( detailWithInfo.getDetail() );
            }
            ads.put(entry.getKey(), ds);
        }

        return Collections.unmodifiableMap(ads);
    }

    private Level getSystemMessageThreshold() {
        if (currentMessageThreshold == null) {
            String msgLevel = config.getProperty( ServerConfigParams.PARAM_AUDIT_MESSAGE_THRESHOLD );
            Level output = null;
            if (msgLevel != null) {
                try {
                    output = getLevel(msgLevel);
                } catch(IllegalArgumentException e) {
                    logger.warning("Invalid message threshold value '" + msgLevel + "'. Will use default " +
                                   DefaultAuditThresholds.DEFAULT_MESSAGE_THRESHOLD.getName() + " instead.");
                }
            }
            if (output == null) {
                output = DefaultAuditThresholds.DEFAULT_MESSAGE_THRESHOLD;
            }
            currentMessageThreshold = output;
        }
        return currentMessageThreshold;
    }

    private Level getAssociatedLogsThreshold() {
        if (currentAssociatedLogsThreshold == null) {
            String msgLevel = config.getProperty( ServerConfigParams.PARAM_AUDIT_ASSOCIATED_LOGS_THRESHOLD );
            Level output = null;
            if (msgLevel != null) {
                try {
                    output = getLevel(msgLevel);
                } catch(IllegalArgumentException e) {
                    logger.warning("Invalid associated logs threshold value '" + msgLevel + "'. Will use default " +
                                   DefaultAuditThresholds.DEFAULT_ASSOCIATED_LOGS_THRESHOLD.getName() + " instead.");
                }
            }
            if (output == null) {
                output = DefaultAuditThresholds.DEFAULT_ASSOCIATED_LOGS_THRESHOLD;
            }
            currentAssociatedLogsThreshold = output;
        }
        return currentAssociatedLogsThreshold;
    }

    private boolean getUseAssociatedLogsThreshold() {
        if (currentUseAssociatedLogsThreshold == null) {
            String configStr = config.getProperty( ServerConfigParams.PARAM_AUDIT_USE_ASSOCIATED_LOGS_THRESHOLD );
            Boolean configValue = null;
            if (configStr != null) {
                configValue = Boolean.valueOf(configStr.trim());
            }
            if (configValue == null) {
                configValue = DefaultAuditThresholds.DEFAULT_USE_ASSOCIATED_LOGS_THRESHOLD;
            }
            currentUseAssociatedLogsThreshold = configValue;
        }
        return currentUseAssociatedLogsThreshold;
    }

    private Level getSystemAdminThreshold() {
        if (currentAdminThreshold == null) {
            String msgLevel = config.getProperty( ServerConfigParams.PARAM_AUDIT_ADMIN_THRESHOLD );
            Level output = null;
            if (msgLevel != null) {
                try {
                    output = getLevel(msgLevel);
                } catch (IllegalArgumentException e) {
                    logger.warning("Invalid admin threshold value '" + msgLevel + "'. Will use default " +
                                   DefaultAuditThresholds.DEFAULT_ADMIN_THRESHOLD.getName() + " instead.");
                }
            }
            if (output == null) {
                output = DefaultAuditThresholds.DEFAULT_ADMIN_THRESHOLD;
            }
            currentAdminThreshold = output;
        }
        return currentAdminThreshold;
    }

    private Level getSystemSystemClientThreshold() {
        if (currentSystemClientThreshold == null) {
            String msgLevel = config.getProperty( ServerConfigParams.PARAM_AUDIT_SYSTEM_CLIENT_THRESHOLD );
            Level output = null;
            if (msgLevel != null) {
                try {
                    output = getLevel(msgLevel);
                } catch(IllegalArgumentException e) {
                    logger.warning("Invalid system client threshold value '" + msgLevel + "'. Will use default " +
                                   DefaultAuditThresholds.DEFAULT_SYSTEM_CLIENT_THRESHOLD.getName() + " instead.");
                }
            }
            if (output == null) {
                output = DefaultAuditThresholds.DEFAULT_SYSTEM_CLIENT_THRESHOLD;
            }
            currentSystemClientThreshold = output;
        }
        return currentSystemClientThreshold;
    }

    private boolean isSignAudits() {
        if (currentSignAuditSetting == null) {
            String configStr = config.getProperty( CONFIG_AUDIT_SIGN );
            Boolean configValue = null;
            if (configStr != null) {
                configValue = Boolean.valueOf(configStr.trim());
            }
            if (configValue == null) {
                configValue = DEFAULT_AUDIT_SIGN;
            }
            currentSignAuditSetting = configValue;
        }
        return currentSignAuditSetting;
    }

    /*
     * Get the Level for the given name/value.
     *
     * Level.parse is synchronized so this is better for common level names.
     */
    private Level getLevel(final String levelName) {
        Level level;

        if (Level.WARNING.getName().equals(levelName)) {
            level = Level.WARNING;
        } else if (Level.INFO.getName().equals(levelName)) {
            level = Level.INFO;
        } else if (Level.SEVERE.getName().equals(levelName)) {
            level = Level.SEVERE;
        } else {
            level = Level.parse(levelName);
        }

        return level;
    }

    private String getSource( Object sourceObj, String defaultValue ) {
        String source = defaultValue;

        if ( sourceObj != null ) {
            source = sourceObj.getClass().getName();
        }

        return source;
    }

    private void checkFlushed(String action) {
        if (flushed)
            throw new IllegalStateException("Unable to change audit context (" + action + ") - this context has already been flushed");
    }

    private static final Logger logger = Logger.getLogger(AuditContextImpl.class.getName());
    private static final String CONFIG_AUDIT_SIGN = "auditSigningEnabled";
    private static final Boolean DEFAULT_AUDIT_SIGN = false;
    private static final String OUR_IP = InetAddressUtil.getLocalHost().getHostAddress();

    private final Config config;
    private final AuditRecordManager auditRecordManager;
    private final String nodeId;
    private AuditPolicyEvaluator auditPolicyEvaluator;
    private final AuditFilterPolicyManager auditFilterPolicyManager;
    private final DefaultKey keystore;
    private final AuditLogListener listener;
    private Map<String, Object> logFormatContextVariables;

    private Level currentMessageThreshold;
    private Level currentAdminThreshold;
    private Level currentSystemClientThreshold;
    private Level currentAssociatedLogsThreshold;
    private Boolean currentUseAssociatedLogsThreshold;
    private Boolean currentSignAuditSetting;

    private AuditRecord currentRecord;
    /** Indicates if {@link #currentRecord} is to be inserted or updated. */
    private boolean update;
    private Level highestLevelYetSeen = Level.ALL;
    private volatile int ordinal = 0;
    private boolean flushed = false;

    static final String MESSAGE_TOO_LARGE = "Message not audited, message size exceeds limit";

    /**
     * The source might be null, but HashMap allows the null key, so all the details
     * created by unknown objects will end up in the same List, which is fine.
     */
    private final Map<Object, List<AuditDetailWithInfo>> details = new LinkedHashMap<Object, List<AuditDetailWithInfo>>();

}
