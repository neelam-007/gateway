/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.audit.AdminAuditRecord;
import com.l7tech.common.audit.AuditRecord;
import com.l7tech.common.audit.MessageSummaryAuditRecord;
import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.HibernatePersistenceContext;
import com.l7tech.server.ServerConfig;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
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

    public Level getMessageThreshold() {
        return systemMessageThreshold;
    }

    public void setMessageThreshold( Level messageThreshold ) {
        AuditContext.systemMessageThreshold = messageThreshold;
    }

    public Level getAdminThreshold() {
        return systemAdminThreshold;
    }

    public void setAdminThreshold( Level adminThreshold ) {
        AuditContext.systemAdminThreshold = adminThreshold;
    }

    public Level getMinimumLevel() {
        return minimumLevel;
    }

    public void setMinimumLevel(Level minimumLevel) {
        if (minimumLevel.intValue() < this.minimumLevel.intValue()) throw new IllegalArgumentException("Attempted to downgrade minimum level from " + this.minimumLevel.getName() + " to " + minimumLevel.getName());
        this.minimumLevel = minimumLevel;
    }

    public void add(AuditRecord record) {
        if (closed) throw new IllegalStateException("Can't add new AuditRecords to a closed AuditContext");
        if (record.getLevel().intValue() > highestLevelYetSeen.intValue()) highestLevelYetSeen = record.getLevel();
        records.add(record);
    }

    public void flush() {
        if (closed) throw new IllegalStateException("Can't flush a closed AuditContext");
        HibernatePersistenceContext context = null;
        Transaction tx = null;
        try {
            context = (HibernatePersistenceContext)HibernatePersistenceContext.getCurrent();
            Session s = context.getAuditSession();
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
                if (tx == null) tx = s.beginTransaction();
                auditRecordManager.save(auditRecord);
            }
            if (tx != null) tx.commit();
        } catch (Throwable e) {
            try {
                if (tx != null) tx.rollback();
                logger.log(Level.SEVERE, "Couldn't save audit records", e);
            } catch ( HibernateException e2 ) {
                logger.log(Level.WARNING, "Couldn't rollback audit transaction after failed flush", e2);
            }
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
            if (!closed) {
                logger.warning("AuditContext finalized before being closed");
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
        String msgLevel = ServerConfig.getInstance().getProperty(ServerConfig.PARAM_MESSAGE_AUDIT_THRESHOLD);
        if (msgLevel != null) {
            try {
                systemMessageThreshold = Level.parse(msgLevel);
            } catch(IllegalArgumentException e) {
                logger.warning("Invalid message threshold value '" + msgLevel + "'. Will use default " + DEFAULT_MESSAGE_THRESHOLD.getName() + " instead.");
            }
        }

        String adminLevel = ServerConfig.getInstance().getProperty(ServerConfig.PARAM_ADMIN_AUDIT_THRESHOLD);
        if (adminLevel != null) {
            try {
                systemAdminThreshold = Level.parse(adminLevel);
            } catch(IllegalArgumentException e) {
                logger.warning("Invalid admin threshold value '" + adminLevel + "'. Will use default " + DEFAULT_ADMIN_THRESHOLD.getName() + " instead.");
            }
        }
    }

    private AuditContext() {
        currentAdminThreshold = systemAdminThreshold;
        currentMessageThreshold = systemMessageThreshold;

        auditRecordManager = (AuditRecordManager)Locator.getDefault().lookup(AuditRecordManager.class);
        if (auditRecordManager == null) throw new IllegalStateException("Couldn't locate AuditRecordManager");
    }

    public static AuditContext getCurrent() {
        AuditContext context = (AuditContext)contextLocal.get();
        if (context == null) {
            context = new AuditContext();
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
    private Level minimumLevel = Level.ALL;
}
