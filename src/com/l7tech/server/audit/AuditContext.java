/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.audit.AdminAuditRecord;
import com.l7tech.common.audit.AuditDetail;
import com.l7tech.common.audit.AuditRecord;
import com.l7tech.common.audit.MessageSummaryAuditRecord;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.ServerConfig;
import org.springframework.context.ApplicationContext;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds the transient state of the audit system for the current thread.
 * <p>
 * Call {@link #getCurrent} to get this thread's audit context, then call {@link #setCurrentRecord} to add any nubmer of
 * {@link AuditRecord}s to the context.
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
public class AuditContext {
    /** Message audit threshold to be used if {@link ServerConfig#PARAM_AUDIT_MESSAGE_THRESHOLD} is unset or invalid */
    public static final Level DEFAULT_MESSAGE_THRESHOLD = Level.WARNING;
    /** Message audit threshold to be used if {@link ServerConfig#PARAM_AUDIT_ADMIN_THRESHOLD} is unset or invalid */
    public static final Level DEFAULT_ADMIN_THRESHOLD = Level.INFO;

    private static final Logger logger = Logger.getLogger(AuditContext.class.getName());
    private static final ThreadLocal contextLocal = new ThreadLocal();

    private static Level systemMessageThreshold = DEFAULT_MESSAGE_THRESHOLD;
    private static Level systemAdminThreshold = DEFAULT_ADMIN_THRESHOLD;

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
        details.add(detail);
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
            if (closed) return;
            if (!flushed) flush();
            contextLocal.set(null);
        } finally {
            closed = true;
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

    public boolean isFlushed() {
        return flushed;
    }

    public boolean isClosed() {
        return closed;
    }

    static {
        systemMessageThreshold = getSystemMessageThreshold();
        String adminLevel = ServerConfig.getInstance().getProperty(ServerConfig.PARAM_AUDIT_ADMIN_THRESHOLD);
        if (adminLevel != null) {
            try {
                systemAdminThreshold = Level.parse(adminLevel);
            } catch(IllegalArgumentException e) {
                logger.warning("Invalid admin threshold value '" + adminLevel + "'. Will use default " + DEFAULT_ADMIN_THRESHOLD.getName() + " instead.");
            }
        }
    }

    public static Level getSystemMessageThreshold() {
        String msgLevel = ServerConfig.getInstance().getProperty(ServerConfig.PARAM_AUDIT_MESSAGE_THRESHOLD);
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

    private AuditContext(ApplicationContext ctx) {
        currentAdminThreshold = systemAdminThreshold;
        currentMessageThreshold = systemMessageThreshold;

        auditRecordManager = (AuditRecordManager)ctx.getBean("auditRecordManager");
        if (auditRecordManager == null) throw new IllegalStateException("Couldn't locate AuditRecordManager");
    }

    public static AuditContext getCurrent(ApplicationContext applicationContext) {
        if (applicationContext == null) {
            throw new IllegalArgumentException("Application Context is required");
        }
        AuditContext context = (AuditContext)contextLocal.get();
        if (context == null) {
            context = new AuditContext(applicationContext);
            contextLocal.set(context);
        }
        return context;
    }

    public static AuditContext peek() {
        return (AuditContext)contextLocal.get();
    }

    private final Level currentMessageThreshold;
    private final Level currentAdminThreshold;
    private final AuditRecordManager auditRecordManager;
    private boolean flushed = false;
    private boolean closed = false;

    private AuditRecord currentRecord;
    private Set details = new HashSet();
    private Level highestLevelYetSeen = Level.ALL;
}
