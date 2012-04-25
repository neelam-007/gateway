package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetailEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * Listens for audit details and adds to the context.
 *
 * <p>Can be used in place of {@link MessageProcessingAuditListener} if message processing
 * is not in use.</p>
 */
public class AuditDetailProcessingAuditListener implements ApplicationListener {

    public AuditDetailProcessingAuditListener() {
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof AuditDetailEvent ) {
            AuditDetailEvent auditDetailEvent = (AuditDetailEvent) event;
            AuditContextFactory.getCurrent().addDetail(auditDetailEvent.getDetailWithInfo());
        }
    }
}