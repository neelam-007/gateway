/*
 * Copyright (C) 2004-2006 Layer 7 Technologies Inc.
 */

package com.l7tech.server.audit;

import com.l7tech.common.audit.*;
import com.l7tech.common.Component;
import com.l7tech.common.util.HexUtils;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.KeystoreUtils;

import javax.crypto.Cipher;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.io.ByteArrayOutputStream;

/**
 * Holds the transient state of the audit system for the current thread.
 *
 * Must be thread-local; not thread-safe in the slightest!  Note that AuditContext instances
 * are likely to be reused for many different messages through time on the same thread,
 * so users must always call {@link #flush()} when finished processing each message.
 * <p>
 * Call {@link #setCurrentRecord} to attach a single {@link AuditRecord} describing the SSG's current
 * operation to the context, and {@link com.l7tech.common.audit.AuditContext#addDetail} to add zero or more {@link AuditDetail} records.
 * <p>
 * {@link MessageSummaryAuditRecord}s, {@link AdminAuditRecord}s and {@link AuditDetail}s that are
 * added to the context will only be persisted to the database later, when {@link #flush} is called,
 * if their level meets or exceeds the corresponding threshold.  {@link SystemAuditRecord}s have no
 * minimum threshold; they are always persisted.
 *
 * @see ServerConfig#getProperty
 * @see ServerConfig#PARAM_AUDIT_MESSAGE_THRESHOLD
 * @see ServerConfig#PARAM_AUDIT_MESSAGE_THRESHOLD

 * @see MessageSummaryAuditRecord
 * @see AdminAuditRecord
 * @see SystemAuditRecord
 */
public class AuditContextImpl implements AuditContext {

    private static final Logger logger = Logger.getLogger(AuditContextImpl.class.getName());
    private static final String CONFIG_AUDIT_SIGN = "auditSigningEnabled";
    private static final Boolean DEFAULT_AUDIT_SIGN = false;
    private volatile int ordinal = 0;
    private final ServerConfig serverConfig;
    private KeystoreUtils keystore;

    public AuditContextImpl(ServerConfig serverConfig, AuditRecordManager auditRecordManager) {
        if (serverConfig == null) {
            throw new IllegalArgumentException("Server Config is required");
        }
        if (auditRecordManager == null) {
            throw new IllegalArgumentException("Audit Record Manager is required");
        }
        this.serverConfig = serverConfig;
        this.auditRecordManager = auditRecordManager;
    }

    /**
     * Sets the current {@link AuditRecord} for this context.
     */
    public void setCurrentRecord(AuditRecord record) {
        if (record == null) throw new NullPointerException();
        if (currentRecord != null) {
            throw new IllegalStateException("Only one audit record can be active at one time");
        }
        if (record.getLevel().intValue() > highestLevelYetSeen.intValue()) highestLevelYetSeen = record.getLevel();
        currentRecord = record;
    }

    public void addDetail(AuditDetail detail, Object source) {
        if (detail == null) throw new NullPointerException();

        Level severity = Messages.getSeverityLevelById(detail.getMessageId());
        if(severity == null) throw new RuntimeException("Cannot find the message (id=" + detail.getMessageId() + ")" + " in the Message Map.");
        detail.setOrdinal(ordinal++);
        // set the ordinal (used to resolve the sequence as the time stamp in ms cannot resolve the order of the messages)
        getDetailList(source).add(detail);
        if(getUseAssociatedLogsThreshold() && severity.intValue() > highestLevelYetSeen.intValue()) {
            highestLevelYetSeen = severity;
        }
    }


    public KeystoreUtils getKeystore() {
        return keystore;
    }

    public void setKeystore(KeystoreUtils keystore) {
        this.keystore = keystore;
    }

    private List<AuditDetail> getDetailList(Object source) {
        List<AuditDetail> details = this.details.get(source);
        if (details == null) {
            details = new LinkedList<AuditDetail>();
            this.details.put(source, details);
        }
        return details;
    }

    /**
     * Get the currently acumulated hints.
     *
     * @return the Set of AuditDetailMessage.Hint's
     */
    public Set getHints() {
        Set<AuditDetailMessage.Hint> hints = new HashSet<AuditDetailMessage.Hint>();
        for (List<AuditDetail> list : details.values()) {
            for (AuditDetail detail : list) {
                Set<AuditDetailMessage.Hint> dHints = Messages.getHintsById(detail.getMessageId());
                if (dHints != null) {
                    hints.addAll(dHints);
                }
            }
        }
        return Collections.unmodifiableSet(hints);
    }

    public void flush() {
        if (currentRecord == null) {
            if (!details.isEmpty()) {
                logger.warning("flush() called with AuditDetails but no AuditRecord");
            }
            return;
        }

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

            Set<AuditDetail> detailsToSave = new HashSet<AuditDetail>();
            for (List<AuditDetail> list : details.values()) {
                for (AuditDetail detail : list) {
                    Level severity = Messages.getSeverityLevelById(detail.getMessageId());
                    if(severity == null) throw new RuntimeException("Cannot find the message (id=" + detail.getMessageId() + ")" + " in the Message Map.");
                    if(severity.intValue() >= getAssociatedLogsThreshold().intValue()) {
                        detail.setAuditRecord(currentRecord);
                        detailsToSave.add(detail);
                    }
                }
            }

            currentRecord.setDetails(detailsToSave);
            currentRecord.setLevel(highestLevelYetSeen);
            if ( isSignAudits() ) {
                signRecord(currentRecord);
            }
            auditRecordManager.save(currentRecord);
        } catch (SaveException e) {
            logger.log(Level.SEVERE, "Couldn't save audit records", e);
        } finally {
            // Reinitialize in case this thread needs us again for a new request
            currentRecord = null;
            currentAdminThreshold = null;
            currentSystemClientThreshold = null;
            currentAssociatedLogsThreshold = null;
            currentUseAssociatedLogsThreshold = null;
            currentMessageThreshold = null;
            currentSignAuditSetting = null;
            details.clear();
            highestLevelYetSeen = Level.ALL;
            ordinal = 0;
        }
    }

    private void signRecord(AuditRecord signatureSubject) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] siginput;
            try {
                signatureSubject.serializeSignableProperties(baos);
                byte[] tmp = baos.toByteArray();
                siginput = digest.digest(tmp);
            } finally {
                baos.close();
            }
            PrivateKey pk = keystore.getSSLPrivateKey();
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.ENCRYPT_MODE, pk);
            byte[] encrypteddata = rsaCipher.doFinal(siginput);
            String signature = HexUtils.encodeBase64(encrypteddata, true);
            signatureSubject.setSignature(signature);
        } catch (Exception e) {
            logger.log(Level.WARNING, "ERROR GENERATING AUDIT SIGNATURE", e);
        }
    }

    public Map<Object, List<AuditDetail>> getDetails() {
        return Collections.unmodifiableMap(details);
    }

    private Level getSystemMessageThreshold() {
        if (currentMessageThreshold == null) {
            String msgLevel = serverConfig.getPropertyCached(ServerConfig.PARAM_AUDIT_MESSAGE_THRESHOLD);
            Level output = null;
            if (msgLevel != null) {
                try {
                    output = getLevel(msgLevel);
                } catch(IllegalArgumentException e) {
                    logger.warning("Invalid message threshold value '" + msgLevel + "'. Will use default " +
                                   DEFAULT_MESSAGE_THRESHOLD.getName() + " instead.");
                }
            }
            if (output == null) {
                output = DEFAULT_MESSAGE_THRESHOLD;
            }
            currentMessageThreshold = output;
        }
        return currentMessageThreshold;
    }

    private Level getAssociatedLogsThreshold() {
        if (currentAssociatedLogsThreshold == null) {
            String msgLevel = serverConfig.getPropertyCached(ServerConfig.PARAM_AUDIT_ASSOCIATED_LOGS_THRESHOLD);
            Level output = null;
            if (msgLevel != null) {
                try {
                    output = getLevel(msgLevel);
                } catch(IllegalArgumentException e) {
                    logger.warning("Invalid associated logs threshold value '" + msgLevel + "'. Will use default " +
                                   DEFAULT_ASSOCIATED_LOGS_THRESHOLD.getName() + " instead.");
                }
            }
            if (output == null) {
                output = DEFAULT_ASSOCIATED_LOGS_THRESHOLD;
            }
            currentAssociatedLogsThreshold = output;
        }
        return currentAssociatedLogsThreshold;
    }

    private boolean getUseAssociatedLogsThreshold() {
        if (currentUseAssociatedLogsThreshold == null) {
            String configStr = serverConfig.getPropertyCached(ServerConfig.PARAM_AUDIT_USE_ASSOCIATED_LOGS_THRESHOLD);
            Boolean configValue = null;
            if (configStr != null) {
                configValue = Boolean.valueOf(configStr.trim());
            }
            if (configValue == null) {
                configValue = DEFAULT_USE_ASSOCIATED_LOGS_THRESHOLD;
            }
            currentUseAssociatedLogsThreshold = configValue;
        }
        return currentUseAssociatedLogsThreshold.booleanValue();
    }

    private Level getSystemAdminThreshold() {
        if (currentAdminThreshold == null) {
            String msgLevel = serverConfig.getPropertyCached(ServerConfig.PARAM_AUDIT_ADMIN_THRESHOLD);
            Level output = null;
            if (msgLevel != null) {
                try {
                    output = getLevel(msgLevel);
                } catch(IllegalArgumentException e) {
                    logger.warning("Invalid admin threshold value '" + msgLevel + "'. Will use default " +
                                   DEFAULT_MESSAGE_THRESHOLD.getName() + " instead.");
                }
            }
            if (output == null) {
                output = DEFAULT_MESSAGE_THRESHOLD;
            }
            currentAdminThreshold = output;
        }
        return currentAdminThreshold;
    }

    private Level getSystemSystemClientThreshold() {
        if (currentSystemClientThreshold == null) {
            String msgLevel = serverConfig.getPropertyCached(ServerConfig.PARAM_AUDIT_SYSTEM_CLIENT_THRESHOLD);
            Level output = null;
            if (msgLevel != null) {
                try {
                    output = getLevel(msgLevel);
                } catch(IllegalArgumentException e) {
                    logger.warning("Invalid system client threshold value '" + msgLevel + "'. Will use default " +
                                   DEFAULT_SYSTEM_CLIENT_THRESHOLD.getName() + " instead.");
                }
            }
            if (output == null) {
                output = DEFAULT_SYSTEM_CLIENT_THRESHOLD;
            }
            currentSystemClientThreshold = output;
        }
        return currentSystemClientThreshold;
    }

    private boolean isSignAudits() {
        if (currentSignAuditSetting == null) {
            String configStr = serverConfig.getPropertyCached(CONFIG_AUDIT_SIGN);
            Boolean configValue = null;
            if (configStr != null) {
                configValue = Boolean.valueOf(configStr.trim());
            }
            if (configValue == null) {
                configValue = DEFAULT_AUDIT_SIGN;
            }
            currentSignAuditSetting = configValue;
        }
        return currentSignAuditSetting.booleanValue();
    }

    /**
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

    private Level currentMessageThreshold;
    private Level currentAdminThreshold;
    private Level currentSystemClientThreshold;
    private Level currentAssociatedLogsThreshold;
    private Boolean currentUseAssociatedLogsThreshold;
    private Boolean currentSignAuditSetting;
    private final AuditRecordManager auditRecordManager;

    private AuditRecord currentRecord;
    private Level highestLevelYetSeen = Level.ALL;

    /**
     * The source might be null, but HashMap allows the null key, so all the details
     * created by unknown objects will end up in the same List, which is fine.
     */
    private final Map<Object, List<AuditDetail>> details = new LinkedHashMap<Object, List<AuditDetail>>();
}
