/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.common.audit;

import com.l7tech.common.util.OpaqueId;
import com.l7tech.logging.SSGLogRecord;
import com.l7tech.objectmodel.FindException;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;

/**
 * @author mike
 */
public class AuditAdminStub implements AuditAdmin {
    public AuditAdminStub() {
        // TODO populate array of sample audit records
    }

    public AuditRecord findByPrimaryKey(long oid) throws FindException, RemoteException {
        throw new UnsupportedOperationException("Not supoprted in stub mode");
    }

    public Collection<AuditRecord> find(AuditSearchCriteria criteria) throws FindException, RemoteException {
        throw new UnsupportedOperationException("Not supoprted in stub mode");
    }

    public void deleteOldAuditRecords() throws RemoteException {
        throw new UnsupportedOperationException("Not supoprted in stub mode");
    }

    public Collection<AuditRecord> findAuditRecords(String nodeid, long startMsgNumber, long endMsgNumber, Date startMsgDate, Date endMsgDate, int size) throws RemoteException, FindException {
        throw new UnsupportedOperationException("Not supoprted in stub mode");
    }

    public Date getLastAcknowledgedAuditDate() {
        throw new UnsupportedOperationException("Not supoprted in stub mode");
    }

    public Date markLastAcknowledgedAuditDate() {
        throw new UnsupportedOperationException("Not supoprted in stub mode");
    }

    public OpaqueId downloadAllAudits(int chunkSizeInBytes) throws RemoteException {
        throw new UnsupportedOperationException("Not supoprted in stub mode");
    }

    public DownloadChunk downloadNextChunk(OpaqueId context) throws RemoteException {
        throw new UnsupportedOperationException("Not supoprted in stub mode");
    }

    public SSGLogRecord[] getSystemLog(String nodeid, long startMsgNumber, long endMsgNumber, Date startMsgDate, Date endMsgDate, int size) throws RemoteException {
        return new SSGLogRecord[0];
    }

    public int getSystemLogRefresh(int typeId) {
        return 5;
    }

    public Level serverMessageAuditThreshold() throws RemoteException {
        return Level.INFO;
    }

    public Level serverDetailAuditThreshold() throws RemoteException {
        return Level.INFO;
    }

    public int serverMinimumPurgeAge() throws RemoteException {
        return 168;
    }



}