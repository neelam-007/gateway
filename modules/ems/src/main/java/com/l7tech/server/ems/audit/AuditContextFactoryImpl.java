package com.l7tech.server.ems.audit;

import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.audit.AuditLogListener;

public class AuditContextFactoryImpl extends AuditContextFactory {

    public AuditContextFactoryImpl(AuditLogListener listener) {
        super(listener);
    }

    public AuditContextFactoryImpl() {
        super(null);
    }
}
