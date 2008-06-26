package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditDetailMessage;
import com.l7tech.server.ServerConfig;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Determine if a given AuditDetailMessage should be ignored.
 *
 * <p>Some audit detail messagess will never be used depending on how the
 * audit thresholds are set. This filter allows us to avoid expensive event
 * publishing if we know that the information will definitely not be used.</p>
 *
 * @author steve
 */
public class GatewayAuditDetailFilter implements PropertyChangeListener, AuditDetailFilter {

    //- PUBLIC

    public GatewayAuditDetailFilter( final ServerConfig serverConfig ) {
        this.detailThreshold.set( getAssociatedLogsThreshold( serverConfig.getPropertyCached(PROP_THRESHOLD) ) );
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
        if ( message.getId() >= MIN_VALUE &&
             message.getId() <= MAX_VALUE &&
             message.getLevel().intValue() < this.detailThreshold.get() )  {
            auditable = false;           
        }

        return auditable;
    }


    //- PRIVATE

    private static final int MIN_VALUE = 3000; // range for MessageProcessingMessages audits
    private static final int MAX_VALUE = 3499;

    private static final String PROP_THRESHOLD = ServerConfig.PARAM_AUDIT_ASSOCIATED_LOGS_THRESHOLD;

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
