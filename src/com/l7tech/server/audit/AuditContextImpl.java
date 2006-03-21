/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.audit;

import com.l7tech.common.audit.*;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.ServerConfig;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds the transient state of the audit system for the current thread.
 *
 * Not thread-safe in the slightest!
 * <p>
 * Call {@link #setCurrentRecord} to attach a single {@link AuditRecord} describing the SSG's current
 * operation to the context, and {@link #addDetail} to add zero or more {@link AuditDetail} records.
 * <p>
 * {@link MessageSummaryAuditRecord}s, {@link AdminAuditRecord}s and {@link AuditDetail}s that are
 * added to the context may or may not be persisted to the database later, when {@link #flush} is called,
 * if their level meets or exceeds the corresponding threshold.  {@link SystemAuditRecord}s have no
 * minimum threshold; they are always persisted.
 *
 * @see ServerConfig#getProperty
 * @see ServerConfig#PARAM_AUDIT_MESSAGE_THRESHOLD
 * @see ServerConfig#PARAM_AUDIT_MESSAGE_THRESHOLD

 * @see MessageSummaryAuditRecord
 * @see AdminAuditRecord
 * @see SystemAuditRecord
 *
 * @author alex
 */
public class AuditContextImpl implements AuditContext {

    private static final Logger logger = Logger.getLogger(AuditContextImpl.class.getName());
    private volatile int ordinal = 0;
    private final ServerConfig serverConfig;

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

    public void addDetail(AuditDetail detail) {
        if (detail == null) throw new NullPointerException();

        Level severity = Messages.getSeverityLevelById(detail.getMessageId());
        if(severity == null) throw new RuntimeException("Cannot find the message (id=" + detail.getMessageId() + ")" + " in the Message Map.");
        if(severity.intValue() >= getAssociatedLogsThreshold().intValue()) {
            // set the ordinal (used to resolve the sequence as the time stamp in ms cannot resolve the order of the messages)
            detail.setOrdinal(ordinal++);
            details.add(detail);
            if(getUseAssociatedLogsThreshold() && severity.intValue() > highestLevelYetSeen.intValue()) {
                highestLevelYetSeen = severity;
            }
        }
    }

    /**
     * Get the currently acumulated hints.
     *
     * @return the Set of AuditDetailMessage.Hint's
     */
    public Set getHints() {
        Set hints = new HashSet();
        for(Iterator i=details.iterator(); i.hasNext(); ) {
            AuditDetail ad = (AuditDetail) i.next();
            Set dHints = Messages.getHintsById(ad.getMessageId());
            if(dHints!=null) {
                hints.addAll(dHints);
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
            } else {
                // System audit records are always saved
            }

            for (Iterator i = details.iterator(); i.hasNext();) {
                AuditDetail detail = (AuditDetail)i.next();
                detail.setAuditRecord(currentRecord);
            }

            currentRecord.setDetails(details);
            currentRecord.setLevel(highestLevelYetSeen);
            auditRecordManager.save(currentRecord);
        } catch (SaveException e) {
            logger.log(Level.SEVERE, "Couldn't save audit records", e);
        } finally {
            // Reinitialize in case this thread needs us again for a new request
            currentRecord = null;
            currentAdminThreshold = null;
            currentAssociatedLogsThreshold = null;
            currentUseAssociatedLogsThreshold = null;
            currentMessageThreshold = null;
            details.clear();
            highestLevelYetSeen = Level.ALL;
            ordinal = 0;
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (currentRecord != null || !details.isEmpty()) {
                logger.warning("AuditContext finalized before being flushed");
                flush();
            }
        } finally {
            super.finalize();
        }
    }

    private Level getSystemMessageThreshold() {
        if (currentMessageThreshold == null) {
            String msgLevel = serverConfig.getProperty(ServerConfig.PARAM_AUDIT_MESSAGE_THRESHOLD);
            Level output = null;
            if (msgLevel != null) {
                try {
                    output = Level.parse(msgLevel);
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
            String msgLevel = serverConfig.getProperty(ServerConfig.PARAM_AUDIT_ASSOCIATED_LOGS_THRESHOLD);
            Level output = null;
            if (msgLevel != null) {
                try {
                    output = Level.parse(msgLevel);
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
            String configStr = serverConfig.getProperty(ServerConfig.PARAM_AUDIT_USE_ASSOCIATED_LOGS_THRESHOLD);
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
            String msgLevel = serverConfig.getProperty(ServerConfig.PARAM_AUDIT_ADMIN_THRESHOLD);
            Level output = null;
            if (msgLevel != null) {
                try {
                    output = Level.parse(msgLevel);
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

    private Level currentMessageThreshold;
    private Level currentAdminThreshold;
    private Level currentAssociatedLogsThreshold;
    private Boolean currentUseAssociatedLogsThreshold;
    private final AuditRecordManager auditRecordManager;

    private AuditRecord currentRecord;
    private Set details = new HashSet();
    private Level highestLevelYetSeen = Level.ALL;
}
