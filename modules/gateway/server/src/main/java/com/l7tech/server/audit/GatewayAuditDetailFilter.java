package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.MessagesUtil;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.Config;
import com.l7tech.util.Pair;
import com.l7tech.util.SyspropUtil;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Determine if a given AuditDetailMessage should be ignored.
 *
 * <p>Some audit detail messages will never be used depending on how the
 * audit thresholds are set. This filter allows us to avoid expensive event
 * publishing if we know that the information will definitely not be used.</p>
 *
 * @author steve
 */
public class GatewayAuditDetailFilter implements PropertyChangeListener, AuditDetailFilter {

    //- PUBLIC

    public GatewayAuditDetailFilter( final Config config ) {
        this.detailThreshold.set( getAssociatedLogsThreshold( config.getProperty( PROP_THRESHOLD ) ) );
    }

    public void propertyChange(PropertyChangeEvent event) {
        if ( PROP_THRESHOLD.equals(event.getPropertyName()) ) {
            this.detailThreshold.set( getAssociatedLogsThreshold( (String)event.getNewValue() ) );
        }
    }

    public boolean isAuditable( final AuditDetailMessage message ) {
        boolean auditable = true;

        // Ignore MessageProcessingMessages if these will not be recorded to the DB
        // Don't ignore assertion audits, since these can be returned in detailed
        // SOAP Faults if so configured.
        final int minLevel = this.detailThreshold.get();
        if ( message.getId() >= MIN_VALUE &&
             message.getId() <= MAX_VALUE &&
             message.getLevel().intValue() < minLevel)  {
            //check if the level has been modified
            final Pair<Boolean,AuditDetailMessage> pair = MessagesUtil.getAuditDetailMessageByIdWithFilter(message.getId());
            auditable = pair.left && pair.right != null && pair.right.getLevel().intValue() >= minLevel;
        }

        // Ignore audit archiver message if it has already been audited recently
        if (SystemMessages.AUDIT_ARCHIVER_MESSAGE_PROCESSING_SUSPENDED.equals(message)) {
            long last = lastArchiverWarning.get();
            long now = System.currentTimeMillis();
            if (now - last <= ARCHIVER_WARNING_THRESHOLD) {
                // Suppress this audit
                auditable = false;
            } else {
                // Time to send one, see if it is up to us
                long last2 = lastArchiverWarning.getAndSet(now);
                if (last2 != last) {
                    // Someone else got to it first
                    auditable = false;
                } else {
                    // Up to us to do the auditing, fall through and let it happen
                }
            }
        }

        return auditable;
    }


    //- PRIVATE

    private static final int MIN_VALUE = 3000; // range for MessageProcessingMessages audits
    private static final int MAX_VALUE = 3499;

    private static final String PROP_THRESHOLD = ServerConfigParams.PARAM_AUDIT_ASSOCIATED_LOGS_THRESHOLD;

    private static final long ARCHIVER_WARNING_THRESHOLD = SyspropUtil.getLong("com.l7tech.server.audit.archiver.suspendedSevereAuditInterval", 600000L);
    private static final AtomicLong lastArchiverWarning = new AtomicLong(0);

    private final AtomicInteger detailThreshold = new AtomicInteger();

    private static int getAssociatedLogsThreshold(final String msgLevel) {
        Level output;
        if ( msgLevel != null ) {
            try {
                output = Level.parse(msgLevel);
            } catch(IllegalArgumentException e) {
                output = Level.ALL;
            }
        } else {
            output = Level.ALL;
        }
        return output.intValue();
    }

}
