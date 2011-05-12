package com.l7tech.server.processcontroller.monitoring.sampling;

import com.l7tech.server.management.api.monitoring.BuiltinMonitorables;
import com.l7tech.server.management.api.monitoring.MonitorableProperty;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;

/**
 * Named property sampler for the database replication delay property.
 */
public class DatabaseReplicationDelaySampler  extends NamedPropertySampler<Long> {
    private static final Logger logger = Logger.getLogger(AuditSizeSampler.class.getName());
    private static final MonitorableProperty DATABASE_REPLICATION_DELAY = BuiltinMonitorables.DATABASE_REPLICATION_DELAY;

    public DatabaseReplicationDelaySampler( final String componentId,
                                            final ApplicationContext spring ) {
        super(componentId, spring, DATABASE_REPLICATION_DELAY, logger);
    }

    @Override
    protected Long cast( final String value ) {
        return Long.valueOf( value );
    }
}
