/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.audit;

import com.l7tech.objectmodel.EntityManagerStub;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.common.audit.AuditSearchCriteria;
import com.l7tech.common.audit.AuditRecord;

import java.util.Collection;

/**
 *
 * @author emil
 * @version Feb 17, 2005
 */
public class AuditRecordManagerStub extends EntityManagerStub implements AuditRecordManager {
    public AuditRecord findByPrimaryKey(long oid) throws FindException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection find(AuditSearchCriteria criteria) throws FindException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public long save(AuditRecord rec) throws SaveException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void deleteOldAuditRecords() throws DeleteException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}