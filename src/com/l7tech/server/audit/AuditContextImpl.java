/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.audit.*;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.ServerConfig;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds the transient state of the audit system for the current thread.
 * <p>
 * Call {@link #setCurrentRecord} to add any nubmer of {@link AuditRecord}s to the context.
 * <p>
 * Records that are added to the context will be persisted to the database later, when {@link #flush} or {#link #close}
 * is called, if their level meets or exceeds the corresponding threshold.  Call {@link ServerConfig#getProperty},
 * specifying {@link ServerConfig#PARAM_AUDIT_MESSAGE_THRESHOLD} or {@link ServerConfig#PARAM_AUDIT_MESSAGE_THRESHOLD}
 * as the parameter, to determine the current threshold for {@link MessageSummaryAuditRecord} and {@link AdminAuditRecord}
 * records, respectively.
 * <p>
 * By contrast, {@link com.l7tech.common.audit.SystemAuditRecord} records are persisted in {@link #flush} or
 * {@link #close} regardless of their level.
 *
 * @author alex
 * @version $Revision$
 */
public class AuditContextImpl implements AuditContext {

    private static final Logger logger = Logger.getLogger(AuditContextImpl.class.getName());
    private static int ordinal = 0;
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

        currentMessageThreshold = getSystemMessageThreshold();
        currentAssociatedLogsThreshold = getAssociatedLogsThreshold();
        currentAdminThreshold = getSystemAdminThreshold();
    }

    /**
     * Sets the current {@link AuditRecord} for this context.
     */
    public void setCurrentRecord(AuditRecord record) {
        if (record == null) throw new NullPointerException();
        if (closed) throw new IllegalStateException("Can't set the current AuditRecord of a closed AuditContext");
        if (currentRecord != null) {
            throw new IllegalStateException("Only one audit record can be active at one time");
        }
        if (record.getLevel().intValue() > highestLevelYetSeen.intValue()) highestLevelYetSeen = record.getLevel();
        currentRecord = record;
    }

    public void addDetail(AuditDetail detail) {
        if (detail == null) throw new NullPointerException();
        if (closed) throw new IllegalStateException("Can't set the current AuditRecord of a closed AuditContext");

        Level severity = MessageMap.getInstance().getSeverityLevelById(detail.getMessageId());
        if(severity == null) throw new RuntimeException("Cannot find the message (id=" + detail.getMessageId() + ")" + " in the Message Map.");
        if(severity.intValue() >= currentAssociatedLogsThreshold.intValue()) {
            // set the ordinal (used to resolve the sequence as the time stamp in ms cannot resolve the order of the messages)
            detail.setOrdinal(ordinal++);
            details.add(detail);
        }
    }

    public void flush() {
        if (closed) throw new IllegalStateException("Can't flush a closed AuditContext");
        if (currentRecord == null) {
            if (details.isEmpty()) {
                logger.warning("flush() called with AuditDetails but no AuditRecord");
            }
            return;
        }

        try {
            if (currentRecord instanceof MessageSummaryAuditRecord) {
                if (currentRecord.getLevel().intValue() < currentMessageThreshold.intValue()) {
                    logger.fine("MessageSummaryAuditRecord generated with level " +
                                currentRecord.getLevel() +
                                " will not be saved; current message audit threshold is " +
                                currentMessageThreshold.getName() );
                    return;
                }
            } else if (currentRecord instanceof AdminAuditRecord) {
                if (currentRecord.getLevel().intValue() < currentAdminThreshold.intValue()) {
                    logger.fine("AdminAuditRecord generated with level " + currentRecord.getLevel()
                                + " will not be saved; current admin audit threshold is " +
                                currentAdminThreshold.getName() );
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
            auditRecordManager.save(currentRecord);
        } catch (SaveException e) {
            logger.log(Level.SEVERE, "Couldn't save audit records", e);
        } finally {
            currentRecord = null;
            details.clear();
            flushed = true;
        }
    }

    public void close() {
        try {
            if (closed) throw new IllegalStateException("Already closed");
            if (!flushed) flush();
        } finally {
            // Reinitialize in case this thread needs us again for a new request
            closed = false;
            flushed = false;
            currentRecord = null;
            details = new HashSet();
            highestLevelYetSeen = Level.ALL;
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (!flushed) {
                logger.warning("AuditContext finalized before being flushed");
                close();
            }
        } finally {
            super.finalize();
        }
    }

    private Level getSystemMessageThreshold() {
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
        return output;
    }

    private Level getAssociatedLogsThreshold() {
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
        return output;
    }

    private Level getSystemAdminThreshold() {
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
        return output;
    }

    private final Level currentMessageThreshold;
    private final Level currentAdminThreshold;
    private final Level currentAssociatedLogsThreshold;
    private final AuditRecordManager auditRecordManager;
    private boolean flushed = false;
    private boolean closed = false;

    private AuditRecord currentRecord;
    private Set details = new HashSet();
    private Level highestLevelYetSeen = Level.ALL;
}
