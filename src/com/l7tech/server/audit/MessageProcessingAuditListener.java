/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.audit.MessageSummaryAuditRecord;
import com.l7tech.common.util.Locator;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.objectmodel.HibernatePersistenceContext;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.PersistenceAction;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.event.MessageProcessingEventListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class MessageProcessingAuditListener implements MessageProcessingEventListener {
    public MessageProcessingAuditListener() {
        this.auditRecordManager = (AuditRecordManager)Locator.getDefault().lookup(AuditRecordManager.class);
        if (auditRecordManager == null) throw new IllegalStateException("Couldn't locate AuditRecordManager");
    }

    public void messageProcessed( Request request, Response response, AssertionStatus status ) {
        final MessageSummaryAuditRecord rec = MessageSummaryAuditFactory.makeEvent(Level.INFO, status);
        try {
            HibernatePersistenceContext context = (HibernatePersistenceContext) HibernatePersistenceContext.getCurrent();
            context.doInTransaction(new PersistenceAction() {
                public Object run() throws ObjectModelException {
                    return new Long(auditRecordManager.save(rec));
                }
            });
        } catch (Exception e) {
            logger.log( Level.SEVERE, "Couldn't save Message summary audit record", e );
        }
    }

    private final AuditRecordManager auditRecordManager;
    private static final Logger logger = Logger.getLogger(MessageProcessingAuditListener.class.getName());
}
