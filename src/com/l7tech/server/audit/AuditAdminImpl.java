/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.audit.AuditRecord;
import com.l7tech.common.audit.AuditSearchCriteria;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.Locator;
import com.l7tech.logging.SSGLogRecord;
import com.l7tech.objectmodel.*;
import com.l7tech.remote.jini.export.RemoteService;
import com.l7tech.server.ServerConfig;
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;

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

    public RemoteBulkStream downloadAllAudits() throws RemoteException {
        enforceAdminRole();
        PipedOutputStream pos = new PipedOutputStream();
        try {
            AuditExporter.exportAuditsAsZipFile(pos,
                                                KeystoreUtils.getInstance().getSslCert(),
                                                KeystoreUtils.getInstance().getSSLPrivateKey());
            final PipedInputStream pis = new PipedInputStream(pos);
            return new RemoteBulkStream() {
                byte[] chunk = new byte[8192];

                public byte[] nextChunk() throws RemoteException {
                    try {
                        int i = pis.read(chunk, 0, chunk.length);
                        if (i < 1) return null;
                        byte[] got = new byte[i];
                        System.arraycopy(chunk, 0, got, 0, i);
                        return got;
                    } catch (IOException e) {
                        throw new RemoteException("Unable to read exported audit stream", e);
                    }
                }
            };
        } catch (Exception e) {
            throw new RemoteException("Unable to export audits", e);
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

    public Level serverMessageAuditThreshold() throws RemoteException {
        return AuditContext.getSystemMessageThreshold();
    }

    public int serverMinimumPurgeAge() throws RemoteException {
        String sAge = ServerConfig.getInstance().getProperty(ServerConfig.PARAM_AUDIT_PURGE_MINIMUM_AGE);
        int age = 168;
        try {
            return Integer.valueOf(sAge).intValue();
        } catch (NumberFormatException nfe) {
            throw new RemoteException("Configured minimum age value '" + sAge +
                                      "' is not a valid number. Using " + age + " (one week) by default" );
        }
    }

    private AuditRecordManager auditRecordManager;
}
