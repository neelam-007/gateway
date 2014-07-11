package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.audit.LoggingAudit;

import java.util.logging.Logger;

/**
 * User: rseminoff
 * Date: 25/05/12
 *
 * This must be mocked as the Auditors don't have bean constructors by default.
 *
 */
public class MockLoggingAuditFactory implements AuditFactory {
    @Override
    public Audit newInstance(Object source, Logger logger) {
        return new LoggingAudit( logger );
    }



}
