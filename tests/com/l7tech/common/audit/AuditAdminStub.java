/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.audit;

import com.l7tech.common.util.OpaqueId;
import com.l7tech.logging.SSGLogRecord;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ServerConfig;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.logging.Level;

/**
 * @author mike
 */
public class AuditAdminStub implements AuditAdmin {
    public AuditAdminStub() {
        // TODO populate array of sample audit records
    }

    public AuditRecord findByPrimaryKey(long oid) throws FindException, RemoteException {
        return null;
    }

    public Collection find(AuditSearchCriteria criteria) throws FindException, RemoteException {
        return null;
    }

    public void deleteOldAuditRecords() throws RemoteException {

    }

    public OpaqueId downloadAllAudits() throws RemoteException {
        throw new UnsupportedOperationException("Not supoprted in stub mode");
    }

    public DownloadChunk downloadNextChunk(OpaqueId context) throws RemoteException {
        throw new UnsupportedOperationException("Not supoprted in stub mode");
    }

    public SSGLogRecord[] getSystemLog(String nodeid, long startMsgNumber, long endMsgNumber, int size) throws RemoteException {
        return new SSGLogRecord[0];
    }

    public Level serverMessageAuditThreshold() throws RemoteException {
        return Level.INFO;
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

}
