/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.audit;

import com.l7tech.logging.SSGLogRecord;
import com.l7tech.objectmodel.FindException;

import java.rmi.RemoteException;
import java.util.Collection;

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

    public SSGLogRecord[] getSystemLog(String nodeid, long startMsgNumber, long endMsgNumber, int size) throws RemoteException {
        return new SSGLogRecord[0];
    }
}
