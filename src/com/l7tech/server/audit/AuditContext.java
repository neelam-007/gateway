/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.audit.AdminAuditRecord;
import com.l7tech.common.audit.AuditRecord;
import com.l7tech.common.audit.MessageSummaryAuditRecord;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.ServerConfig;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds the transient state of the audit system for the current thread.
 * <p>
 * Call {@link #getCurrent} to get this thread's audit context, then call {@link #add} to add any nubmer of
 * {@link AuditRecord}s to the context.
 * <p>
 * Records that are added to the context will be persisted to the database later, when {@link #flush} or {#link #close}
 * is called, if their level meets or exceeds the corresponding threshold.  Call {@link ServerConfig#getProperty(String)},
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
    public static final Level DEFAULT_MESSAGE_THRESHOLD = Level.WARNING;
    public static final Level DEFAULT_ADMIN_THRESHOLD = Level.INFO;

    private static final Logger logger = Logger.getLogger(AuditContext.class.getName());
    private static final ThreadLocal contextLocal = new ThreadLocal();

    private static Level systemMessageThreshold = DEFAULT_MESSAGE_THRESHOLD;
    private static Level systemAdminThreshold = DEFAULT_ADMIN_THRESHOLD;

    public void add(AuditRecord record) {
        if (record == null) return;
        if (closed) throw new IllegalStateException("Can't add new AuditRecords to a closed AuditContext");
        if (record.getLevel().intValue() > highestLevelYetSeen.intValue()) highestLevelYetSeen = record.getLevel();
        records.add(record);
    }

    public void flush() {
        if (closed) throw new IllegalStateException("Can't flush a closed AuditContext");
        Collection toSave = new ArrayList();
        try {
            for ( Iterator i = records.iterator(); i.hasNext(); ) {
                AuditRecord auditRecord = (AuditRecord)i.next();
                i.remove();

                if (auditRecord instanceof MessageSummaryAuditRecord) {
                    if (auditRecord.getLevel().intValue() < currentMessageThreshold.intValue()) {
                        logger.fine("MessageSummaryAuditRecord generated with level " + auditRecord.getLevel() +
                                    " will not be saved; current message audit threshold is " +
                                    currentMessageThreshold.getName() );
                        continue;
                    }
                } else if (auditRecord instanceof AdminAuditRecord) {
                    if (auditRecord.getLevel().intValue() < currentAdminThreshold.intValue()) {
                        logger.fine("AdminAuditRecord generated with level " + auditRecord.getLevel()
                                    + " will not be saved; current admin audit threshold is " +
                                    currentAdminThreshold.getName() );
                        continue;
                    }
                } else {
                    // System audit records are always saved
                }
                toSave.add(auditRecord);
            }
            auditRecordManager.save(toSave);
        } catch (SaveException e) {
            logger.log(Level.SEVERE, "Couldn't save audit records", e);
        } finally {
            flushed = true;
        }
    }

    public void close() {
        try {
            if (closed) return;
            if (!flushed) flush();
            records.clear();
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

    private Set records = new HashSet();
    private Level highestLevelYetSeen = Level.ALL;
}
