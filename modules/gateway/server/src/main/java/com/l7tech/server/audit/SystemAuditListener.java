package com.l7tech.server.audit;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.server.event.system.*;
import static com.l7tech.util.CollectionUtils.join;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

import java.util.logging.Level;

/**
 * @author alex
 */
public class SystemAuditListener implements ApplicationListener, Ordered {
    private final String nodeId;
    private AuditContext auditContext;

    public SystemAuditListener(String nodeId, AuditContext auditContext) {
        this.nodeId = nodeId;
        this.auditContext = auditContext;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof SystemEvent) {
            final SystemEvent se = (SystemEvent)event;

            final SystemAuditRecord record;
            if (event instanceof AuditPurgeEvent) {
                AuditPurgeEvent ape = (AuditPurgeEvent)event;
                if (ape.isUpdate()) {
                    record = ape.getSystemAuditRecord();
                    auditContext.setUpdate(true);
                } else {
                    record = createSystemAuditRecord(se);
                    ape.setSystemAuditRecord(record);
                }
            } else {
                if ( event instanceof AuditAwareSystemEvent ) {
                    final AuditAwareSystemEvent aase = (AuditAwareSystemEvent) event;
                    if ( !aase.shouldBeAudited( join( auditContext.getDetails().values() ) ) ) {
                        return;
                    }
                }
                record = createSystemAuditRecord(se);
            }

            auditContext.setCurrentRecord(record);
            auditContext.flush();
            auditContext.setUpdate(false);
        }
    }

    @Override
    public int getOrder() {
        return 10000;
    }

    private SystemAuditRecord createSystemAuditRecord(SystemEvent event) {
        Level level = event.getLevel();
        if (event.getComponent() == Component.GW_SERVER) {
            level = (event instanceof Started || event instanceof Stopped) ? Level.INFO : Level.FINE;
        } else if (event.getComponent() == Component.GW_AUDIT_SYSTEM) {
            level = Level.SEVERE;
        }

        boolean alwaysAudit = true;
        if (event instanceof RoutineSystemEvent) {
            alwaysAudit = false;
        }

        final SystemAuditRecord record = new SystemAuditRecord(level,
                                                               nodeId,
                                                               event.getComponent(),
                                                               event.getMessage(),
                                                               alwaysAudit,
                                                               event.getIdentityProviderOid(),
                                                               event.getUserName(),
                                                               event.getUserId(),
                                                               event.getAction(),
                                                               event.getIpAddress());
        return record;
    }
}
