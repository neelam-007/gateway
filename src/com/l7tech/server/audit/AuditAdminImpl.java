/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.audit.AuditRecord;
import com.l7tech.common.audit.AuditSearchCriteria;
import com.l7tech.common.util.Locator;
import com.l7tech.logging.SSGLogRecord;
import com.l7tech.objectmodel.*;
import com.l7tech.remote.jini.export.RemoteService;
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Collection;

/**
 * @author alex
 * @version $Revision$
 */
public class AuditAdminImpl extends RemoteService implements AuditAdmin {
    public AuditAdminImpl( String[] options, LifeCycle lifeCycle ) throws ConfigurationException, IOException {
        super( options, lifeCycle );
    }

    public AuditRecord findByPrimaryKey( final long oid ) throws FindException, RemoteException {
        try {
            return (AuditRecord) doInTransactionAndClose(new PersistenceAction() {
                public Object run() throws ObjectModelException {
                    return getManager().findByPrimaryKey(oid);
                }
            });
        } catch ( ObjectModelException e ) {
            throw new FindException("Couldn't find AuditRecord", e);
        }
    }

    public Collection find(final AuditSearchCriteria criteria) throws FindException, RemoteException {
        try {
            return (Collection)doInTransactionAndClose(new PersistenceAction() {
                public Object run() throws ObjectModelException {
                    return getManager().find(criteria);
                }
            });
        } catch ( ObjectModelException e ) {
            throw new FindException("Couldn't find AuditRecords", e);
        }
    }

    public void deleteOldAuditRecords() throws RemoteException {
        enforceAdminRole();
        try {
            doInTransactionAndClose(new PersistenceAction() {
                public Object run() throws ObjectModelException {
                    getManager().deleteOldAuditRecords();
                    return null;
                }
            });
        } catch ( ObjectModelException e ) {
            throw new RemoteException("Couldn't find AuditRecords", e);
        }
    }

    public SSGLogRecord[] getSystemLog(final String nodeid, final long startMsgNumber, final long endMsgNumber, final int size) throws RemoteException {
        try {
            Collection c = (Collection)doInTransactionAndClose(new PersistenceAction() {
                public Object run() throws ObjectModelException {
                    return getManager().find(new AuditSearchCriteria(nodeid, startMsgNumber, endMsgNumber, size));
                }
            });
            return (SSGLogRecord[])c.toArray(new SSGLogRecord[0]);
        } catch ( ObjectModelException e ) {
            throw new RemoteException("Couldn't find AuditRecords", e);
        }
    }

    private Object doInTransactionAndClose(PersistenceAction r) throws ObjectModelException {
        HibernatePersistenceContext context = null;
        try {
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            return context.doInTransaction(r);
        } catch (SQLException e) {
            throw new ObjectModelException(e);
        } finally {
            if (context != null) context.close();
        }
    }

    private AuditRecordManager getManager() {
        if (auditRecordManager == null) {
            auditRecordManager = (AuditRecordManager)Locator.getDefault().lookup(AuditRecordManager.class);
            if (auditRecordManager == null) throw new IllegalStateException("Can't locate AuditRecordManager");
        }
        return auditRecordManager;
    }

    private AuditRecordManager auditRecordManager;
}
