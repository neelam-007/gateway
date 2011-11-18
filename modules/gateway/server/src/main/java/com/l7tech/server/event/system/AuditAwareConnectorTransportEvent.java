package com.l7tech.server.event.system;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.AuditDetail;
import static com.l7tech.gateway.common.audit.SystemMessages.CONNECTOR_ERROR;
import static com.l7tech.gateway.common.audit.SystemMessages.CONNECTOR_START;
import static com.l7tech.gateway.common.audit.SystemMessages.CONNECTOR_STOP;
import static com.l7tech.util.CollectionUtils.set;

import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;

/**
 * TransportEvent that only audits if there are relevant details
 */
public class AuditAwareConnectorTransportEvent extends TransportEvent implements AuditAwareSystemEvent {

    private static final Set<Integer> requiredMessages = set(
            CONNECTOR_START.getId(),
            CONNECTOR_STOP.getId(),
            CONNECTOR_ERROR.getId()
    );

    public AuditAwareConnectorTransportEvent( final Object source,
                                              final Component component,
                                              final String ipAddress,
                                              final Level level,
                                              final String action,
                                              final String message ) {
        super( source, component, ipAddress, level, action, message );
    }

    /**
     * If this transport event requires particular messages then ensure
     * that one of the required messages is present in the details.
     */
    @Override
    public boolean shouldBeAudited( final Collection<AuditDetail> details ) {
        boolean audit = false;
        for ( final AuditDetail detail : details ) {
            if ( requiredMessages.contains( detail.getMessageId() ) ) {
                audit = true;
                break;
            }
        }
        return audit;
    }
}
